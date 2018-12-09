---
layout: blog
title: "Think about Implementing a Spark Streaming Application Systematically"
---

Below are some of my notes about Spark Streaming. I only add answers to not all known questions.

- Input
    - Manage offsets by Kafka or self?
    - Does "createDirectStream" support group mode?
        - "createDirectStream" in "spark-streaming-kafka-0-8" uses the old low level Kafka API, does not support "group" mode
        - "spark-streaming-kafka-0-10" use the new Kafka API, supports "group" mode
    - How to control the consuming speed?
        - "maxRatePerPartition"
        - "backpressure"
- Process
    - How to determine the executor number and core number?
        - What's the relationship between executors, partitions, tasks and cores?
        - Does Spark distribute Kafka partitions to executors by executor number or core number?
    - How much memory does an executor need?
        - What's Spark's memory management mechanism?
        - Can RDD size bigger than executor memory?
        - Why Spark stream state cost so much memory?
            - "MEMORY_ONLY" costs 2x-5x of data size
            - Default cache 20 (2 * checkpoint_duration(10 * batch_duration)) state RDDs
        - What are the possible reasons of "OutOfMemoryError"?
    - How to maintain the state?
        - Manage Kafka by Spark directly or outside?
        - What's the difference between "updateStateByKey", "mapWithState" and "mapGroupsWithState"?
        - What's the added cost to checkpoint?
        - Why "mapWithState" changed the partition number?
    - What are the possible reasons for Spark Streaming straggler?
        - Data skew
        - Checkpoint
- Output
    - What's the mechanism of distributing messages to Kafka?
        - Without key, round-robin
        - With key, hash by key
    - Speculation may break up data consistency
- Monitor
    - Which point should the monitor covered?
        - Input speed, process speed, output speed, process offset lag, event_time delay
- Advance
    - Think about data recovery systematically. Every points in the data flow may corrupt, we should build a easy recover mechanism for each case
    - How to correlate different streamings?
    - How to ensure stability?
