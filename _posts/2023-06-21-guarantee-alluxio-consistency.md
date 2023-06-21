---
layout: blog
title: "Guarantee Consistency of Alluxio without Redundant Metadata Sync"
---

# Background

Alluxio helps unify users’ data across a variety of platforms while also helping to increase overall I/O throughput. It can be used as a distributed cache layer. We have used Alluxio for a lot of use cases, for example

- Provide POSIX interfaces to clients with alluxio-fuse
- Provide S3 interfaces to clients with alluxio-proxy
- Reduce inter-datacenter traffic with caching data in Alluxio
- Provide faster accessing speed for AI training jobs

For most cases, consistency is a strong concern and we need to trade off it with latency.

The existing sync mechanism of Alluxio is based on the value of  "sync.interval" - the interval for syncing UFS metadata before invoking an operation on a path

- "-1" means no sync will occur
- "0" means Alluxio will always sync the metadata of the path before an operation
- ">0" means Alluxio will (best effort) not re-sync a path within that time interval

However, this mechanism cannot meet the requirements of users sometimes

- If "sync.interval = 0"
  - The latency may increase a lot. As one main target of Alluxio is for speeding up, this is a serious negative.
  - The metadata sync is redundant/expensive for cases where data does not change frequently
- If "sync.interval > 0"
  - It may have consistency issues. To keep consistency with this setting, we implemented an external service named CacheManager which listens to the audit log of HDFS, and then syncs the updates from HDFS to Alluxio, but it's hard to maintain and cannot resolve the consistency issue completely, as there is still a time gap for the sync.

# Proposal

A possible solution to avoid redundant metadata sync is

1. Client reads metadata from UFS directly AND
2. Client reads data from Alluxio AND
3. Alluxio syncs metadata/data from UFS only when needed

"listStatus" and "getStatus" are the top 2 frequency RPCs, and we only introduce them in this doc.

For listing a directory, it only needs to read metadata from UFS, which is easy to achieve. 

For reading a file, below are the rough steps to achieve the above idea

<img src="/downloads/230621-alluxio-01.png" width="70%">

1. Currently alluxio-client calls "getStatus"(or "getFileInfo" at server side) one or multiple times while reading a file. The return value of "getStatus" contains (based on branch "master-2.x")

  - The metadata of the file
  - The block list and block locations of the file

  It can be split into two different methods

  - "getMeta", which only returns the metadata of the file
  - "getBlocksAndLocations", which returns the block list and locations of the file

2. alluxio-client calls "getMeta"(or reuse the name "getStatus") to get metadata from UFS
3. alluxio-client calls "getBlocksAndLocations" with "path" and "lastModificationTime" from alluxio-master
4. alluxio-master does the following checks
  - If the file does not exist in Alluxio, try to load it from UFS
  - If the file is in Alluxio but has an older version compared to "lastModificationTime", try to re-sync it from UFS
  - Otherwise just returns the block list and locations

# Benefits

The benefits of this solution
- The client can always get consistent results without redundant metadata sync
- RPCs are more lightweight. Compared to the original "getStatus", "getMeta/getBlocksAndLocations" return less information and should have better performance. Actually, there are a lot of cases in the code that call "getStatus" but do not use block-related information of the result. They will also benefit from the more lightweight RPCs.
- The total number of RPCs can be reduced or at least equal if data in Alluxio is not outdated. This will be explained in the following part.

# RPC Comparisons

We will prove the benefits with real cases in this part.

While using "sync.interval=0",  an action from the client will trigger at least two RPCs. For "listStatus",

- "listStatus" from client to alluxio-master
- "listStatus" from alluxio-master to UFS

This proposal solution just needs one RPC

- "listStatus" from client to UFS
- The number of RPCs for "listStatus" can decrease 50%.

Even though not all actions can have a 50% decrease, they can also benefit from it. Below are some concrete examples.

## "fs head"

If executing "alluxio fs head {file}", it will generate 2x "getFileInfo" audit log records

- "HeadCommand.runPlainPath" calls it to check if the path "isFolder" and "getLength"
- "BaseFileSystem.openFile" calls it to
  - Checks if it "isFolder" and "isCompleted"
  - Gets block list and locations

For this case, if using "sync.interval=0", it has the following RPCs

<img src="/downloads/230621-alluxio-02.png" height="50%">

| RPC | times |
| --- | --- |
| client -> Alluxio (getStatus) | 2 |
| Alluxio -> HDFS (getFileStatus) | 2 |

If using the proposed solution

<img src="/downloads/230621-alluxio-03.png" width="70%">

| RPC | times |
| --- | --- |
| client -> HDFS (getMeta) | 2 |
| client -> Alluxio (getBlocksAndLocations) | 1 |

For this case, the proposed solution has less and more lightweight RPCs.

## "head" with alluxio-fuse

If executing "head {file}" in an alluxio-fuse mount path, it will generate 3x "getFileInfo" audit log records

- "FuseFileStream$Factory.create" uses it to check if the file `isPresent` and `isCompleted`
- "BaseFileSystem.openFile" calls it to
  - Checks if it "isFolder" and "isCompleted"
  - Gets block list and locations
- "AlluxioJniFuseFileSystem.getattr" uses it to set the "FileStat" parameter

For this case, if using "sync.interval=0", it has the following RPCs

| RPC | times |
| --- | --- |
| client -> Alluxio (getStatus) | 3 |
| Alluxio -> HDFS (getFileStatus) | 3 |

If using the proposed solution

| RPC | times |
| --- | --- |
| client -> HDFS (getMeta) | 3 |
| client -> Alluxio (getBlocksAndLocations) | 1 |

## "fs ls"

If executing "alluxio fs ls {path}", it will generate 1x "listStatus" audit log record.
For this case, if using "sync.interval=0", it has the following RPCs

| RPC | times |
| --- | --- |
| client -> Alluxio (listStatus) | 1 |
| Alluxio -> HDFS (listStatus) | 1 |

If using the proposed solution

| RPC | times |
| --- | --- |
| client -> HDFS (listStatus) | 1 |

For this case, the proposed solution can reduce 50% RPCs.

# Implementation and Plan

Here are some considerations for implementation.

\> There should exist a flag to enable/disable the feature in client configuration

- If disabled, "getMeta" still connects to Alluxio
- If enabled, "getMeta" connects to UFS

\> We are still assessing this proposal. If it's ok, we will implement it based on the latest Alluxio DORA architecture

- We need to test the DORA version first
- DORA has very strong scalability, if we can also achieve consistency with a low cost, then we can apply Alluxio to more cases.

\> We will share the results of applying this solution in future.
