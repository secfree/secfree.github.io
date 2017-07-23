---
layout: blog
title: "A bug of Ambari's move namenode action"
---

Ambari supports moving namenode from one host to another in an action of HDFS service. You can do it by selecting `HDFS -> Service Actions -> Move NameNode`. It's really convenient compare to do all the work step by step manually.

My hdfs service deployed HA. Two namenodes are H1 and H2. I set the configure to move namenode on H1 to H3.

In the step `Configure Component` of `MOVE MASTER WIZARD`, I came across `Start NameNode` failed. Retry is no use and I have to terminate the `move namenode` action. The result is that all services on Ambari halted.

I had to fix the inconsistent configures and do left actions manually, basic steps are:

1. fix the wrong host substitution of namenode in `hdfs-site.xml`
1. run `hdfs zkfc -formatZK` on H2
1. run `hdfs namenode -bootstrapStandby` on H3

I recorded the details and submit an issue to Ambari: [Update wrong namenode config when moving namenode](https://issues.apache.org/jira/browse/AMBARI-15126)
