---
layout: blog
title: "spark中pool的创建"
---

在 spark streaming 中输出数据时, 使用pool是比较合适的方法. 原因在 [Design Patterns for using foreachRDD](https://spark.apache.org/docs/latest/streaming-programming-guide.html#output-operations-on-dstreams) 中已经说的很详细, 主要是两点:

1. 在driver上创建的connection对象传输到worker上无效;

2. rdd.foreach()或rdd.foreachPartition()中创建connection代价比较大;

而如果每个worker能够共用一个 pool, 那对资源的调配和利用都将比较好控制. spark streaming中已经给出了pool需要满足的条件:

> Note that the connections in the pool should be lazily created on demand and timed out if not used for a while. This achieves the most efficient sending of data to external systems.

team需要用java实现代码, 个人对java熟悉又很有限, 并不知道怎样实现一个"lazily"的pool.

找到这篇[Which is the best way to get a connection to an external database per task in Spark Streaming?](http://apache-spark-user-list.1001560.n3.nabble.com/Which-is-the-best-way-to-get-a-connection-to-an-external-database-per-task-in-Spark-Streaming-td8937.html), 文中有提到:

> Yes, that would be the Java equivalence to use static class member, but you should carefully program to prevent resource leakage. A good choice is to use third-party DB connection library which supports connection pool, that will alleviate your programming efforts.

因此, 将pool作为一个类的static属性即可满足要求.

至于怎样实现一个pool的逻辑, 可以直接用[commons pool](http://commons.apache.org/proper/commons-pool/).

这里给出一个简单的demo:

```java
package test;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.sql.Connection;

public class MysqlConPool {

    private KeyedObjectPool<String, Connection> pool = null;
    private static MysqlConPool instance = null;
    
    protected MysqlConPool() {
        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotalPerKey(1);
        pool = new GenericKeyedObjectPool<String, Connection>(new MysqlConFactory(), config);
    }

    public static MysqlConPool getInstance() {
        if (instance == null) {
            synchronized (MysqlConPool.class) {
                if (instance == null) {
                    instance = new MysqlConPool();
                }
            }
        }
        return instance;
    }
}
```

ConFactory的实现参考commons pool的examples即可.

Commons pool本身是线程安全的, 但pool的初始化还是需要用 synchronized 来保证. 因为spark中application的executor执行tasks是多线程的.

> In Spark’s execution model, each application gets its own executors, which stay up for the duration of the whole application and run 1+ tasks in multiple threads.

可参考: [Integrating Kafka and Spark Streaming: Code Examples and State of the Game](http://www.michael-noll.com/blog/2014/10/01/kafka-spark-streaming-integration-example-tutorial/)