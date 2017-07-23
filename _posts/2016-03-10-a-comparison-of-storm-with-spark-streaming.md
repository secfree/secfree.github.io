---
layout: blog
title: "A comparison of Storm with Spark Streaming"
---

### Compare

<br />

| Feature | Spark Streaming | Storm |
| --- | --- | --- |
| Latency | Few seconds | Sub-second |
| Data guarantee | Exactly once | At least once <sup>1</sup> |
| Process model | Batch | One |
| Fail recovery price | Low <sup>2</sup> | High <sup>3</sup> |
| Resource manager integration | YARN, Mesos | YARN, Mesos |
| Consistency break condition | Output operation failure | Replay an event |
| Popular <sup>4</sup> | More <sup>5</sup> | Less |
| Development cost | Less <sup>6</sup> | More |
| Batch framework integration | Spark | N/A |
| Message Passing Layer | Netty, Akka | Netty or ZeroMQ |
| Implement Language | Scala | Clojure |
| Hadoop distribution support | Hortonworks, Cloudera, MapR | Hortonworks, MapR |
| Company support | Databricks | N/A |
| Origin | Uc Berkeley | BackType, Twitter |
| Production use | 2013 | 2011 |

<br />

1. Actually, Storm's Trident library also provides exactly once processing. But, it relies on transactions to update state, which is slower and often has to be implemented by the user.
1. Because of the dependency chain of Spark RDD, it's easy to recovery from failure by relaying it from the source, need not to track every middle state.
1. Each individual record has to be tracked as it moves through the system
1. Judged by code commit velociy and issue velocity.
1. Spark also has a better ecosystem

    ![Spark ecosystem stack]({{ site.url}}/downloads/spark_ecosystem_stack.png)
1. With Spark, the same code base can be used for batch processing and stream processing.


### Refer

<br />

1. [Apache storm vs. Spark Streaming](http://www.slideshare.net/ptgoetz/apache-storm-vs-spark-streaming)
1. [Storm vs. Spark Streaming: Side-by-side comparison](http://xinhstechblog.blogspot.com/2014/06/storm-vs-spark-streaming-side-by-side.html)
1. [Apache Storm vs Spark Streaming](http://www.ericsson.com/research-blog/data-knowledge/apache-storm-vs-spark-streaming/)
1. [Apache Storm vs. Apache Spark](http://zdatainc.com/2014/09/apache-storm-apache-spark/)
