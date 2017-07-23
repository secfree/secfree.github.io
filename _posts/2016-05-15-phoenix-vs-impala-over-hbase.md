---
layout: blog
title: "Phoenix vs Impala over HBase"
---

What I want is just an RDBMS which supports scale, or a Big Data database which supports SQL operation. So my comparison will be based on this view.

I have experience on Phoenix, but not on Impala. So my comparison of these two components may be completely wrong, and if you find some, please let me know.

1. Abstraction

    Phoenix is a layer over HBase, using it is just like using an RDBMS, You even do not need to know about HDFS, Hive, HBase.

    Impala over HBase is a combination of Hive, HBase and Impala. Before you start, you must get some understanding of these.

    They both support `JDBC` and fast read/write.

1. Ease of use

    As described above, when you using Impala over HBase, you have to do a combination with Hive and HBase. You should create tables in Hive and HBase separately, and then map the columns of each other. You should be careful about some details when you define the table schema. Actually, it used the technology [HBase via Hive](http://www.n10k.com/blog/hbase-via-hive-pt1/).

1. Deployment

    Deploying Phoenix is very easy, you just need to copy some libs to HBase's regionserver, add a few items a configuration and restart the HBase.

    If you want to deploy Impala, you must use the `cdh` branch, but not `hdp` or the original Apache's release, otherwise they will be not compatible. As I know, `cdh` is not as flexible as DIY.

1. Document

    I think Impala's document is not nice to read and not easy to understand. At this point, Phoenix is of course better.

So, if I just want a simple BigData-RDBMS, I would like to choice Phoenix.

Refer:

1. [Using Impala to Query HBase Tables](http://www.cloudera.com/documentation/enterprise/latest/topics/impala_hbase.html)
