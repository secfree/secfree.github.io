---
layout: blog
title: "Maybe I should Give up Using Gobblin"
---

[Gobblin](http://gobblin.readthedocs.io/en/latest/) is a great tool for ETL. It has good abstract concepts. It helps me a lot in the past two years.

But, the following reasons made me want to give up.

1. Meet some exceptions which are difficult to fix
    1. OutOfMemory
    1. Task process got hung
1. Difficult to run Gobblin in cluster mode
    1. MapReduce mode is hard to use
    1. YARN mode needs [Helix](http://helix.apache.org/0.8.1-docs/Quickstart.html), which is not as common as HDFS and YARN
1. The components have good abstract concepts. But it's not easy to do some change for some basic classes
1. A lot of accumulated questions

    <img src="/downloads/gobblin-questions-1.png" width="70%">

    <img src="/downloads/gobblin-questions-2.png" width="70%">


They made my job delayed several times. In such cases, I turned to Spark, which solve the problems elegantly. The operators of Spark are at a lower level compared to Gobblin's components, but they are high enough and flexible. With the combination of workflow schedule tools, it's able to schedule a lot of Spark applications across the cluster. Most important, Spark is robust and has no risk on feasibility.
