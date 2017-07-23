---
layout: blog
title: "phoenix client 线程数的控制"
---

这个问题记录下来有一段时间, 一直没有发出来, 今天补上.

为了使存储在 HBase 中的数据支持 SQL 查询, 我们采用 [Phoenix](http://phoenix.apache.org/)
入库. 入库数据的 client 代码用到的 jar 版本是: `phoenix-4.5.0-HBase-0.98-client.jar`.

我们的入库程序运行一个 streaming 的平台上, 每个 worker 会分配一个 container 类似的东西,
也就是会限制 worker 使用的相关资源, 如内存, CPU 等.

程序运行一段时间后, 报错:

```
java.lang.OutOfMemoryError: unable to create new native thread
        at java.lang.Thread.start0(Native Method)
        at java.lang.Thread.start(Thread.java:714)
        at java.util.concurrent.ThreadPoolExecutor.addWorker(ThreadPoolExecutor.java:949)
        at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1360)
        at java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:132)
        at org.apache.hadoop.hbase.client.HTable.coprocessorService(HTable.java:1625)
        at org.apache.hadoop.hbase.client.HTable.coprocessorService(HTable.java:1598)
        at org.apache.phoenix.cache.ServerCacheClient.removeServerCache(ServerCacheClient.java:308)
        at org.apache.phoenix.cache.ServerCacheClient.access$000(ServerCacheClient.java:82)
        at org.apache.phoenix.cache.ServerCacheClient$ServerCache.close(ServerCacheClient.java:142)
        at org.apache.phoenix.execute.MutationState.commit(MutationState.java:486)
        at org.apache.phoenix.jdbc.PhoenixConnection$3.call(PhoenixConnection.java:465)
        at org.apache.phoenix.jdbc.PhoenixConnection$3.call(PhoenixConnection.java:462)
        at org.apache.phoenix.call.CallRunner.run(CallRunner.java:53)
        at org.apache.phoenix.jdbc.PhoenixConnection.commit(PhoenixConnection.java:462)
        at scloud.data.db.PhoenixClient.batch(PhoenixClient.java:48)
        at scloud.data.parser.Parser.insert(Parser.java:78)
        at scloud.data.parser.AccessParser.parse(AccessParser.java:85)
        at scloud.data.parser.Parser.run(Parser.java:47)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
        at java.lang.Thread.run(Thread.java:745)
```

`unable to create new native thread` 并不是 jvm 的 memory 不足导致,
这里有一篇较好的分析文章:
[解决 - java.lang.OutOfMemoryError： unable to create new native thread](http://sesame.iteye.com/blog/622670) .

排查后发现, 入库程序本身没有线程失控的状况, 只创建很少的线程.
但是 Phoenix 的 jdbc 库中和 HBase 连接和创建大量的线程, 可以用 `ps -eLf` 看到数量很多.

通过查看 phoenix 和 hbase 的源码,
发现可以通过参数 `hbase.htable.threads.max` 限制线程数, 默认值是:

```
int maxThreads = conf.getInt("hbase.htable.threads.max", Integer.MAX_VALUE);
```

Phoenix 的文档中对这个参数也有提及:
[Secondary Indexing](https://phoenix.apache.org/secondary_indexing.html)

但是我在 `hbase-site.xml` 中设置该参数并不生效,
(该文件中其他参数已经生效, 因此确认该文件被正确加载).
在 Phoenix jdbc 建立连接使用的 Properites 中设置也不生效.

在 Phoenix 邮件组中询问
[Phoenix Client create too many threads](https://www.mail-archive.com/user@phoenix.apache.org/msg02994.html),
 没有得到解答.

因此, 个人在 `Phoenix-4.5.0-HBase-0.98` 代码中更改
 `phoenix-core/src/main/java/org/apache/phoenix/query/HConnectionFactory.java`:

```diff
 static class HConnectionFactoryImpl implements HConnectionFactory {
         @Override
         public HConnection createConnection(Configuration conf) throws IOException {
-            //return HConnectionManager.createConnection(conf);
+            ThreadPoolExecutor pool = new ThreadPoolExecutor(
+                1,
+                conf.getInt("hbase.htable.threads.max", 100),
+                conf.getLong("hbase.htable.threads.keepalivetime", 60),
+                TimeUnit.SECONDS,
+                new SynchronousQueue<Runnable>(),
+                Threads.newDaemonThreadFactory("htable"));
+            ((ThreadPoolExecutor) pool).allowCoreThreadTimeOut(true);
+
+            return HConnectionManager.createConnection(conf, pool);
        }
    }
```

pool 的创建方式和 hbase 代码中默认的 pool 创建是一致的.

通过上面的代码, 可以在 Phoenix jdbc 建立连接使用的 Properites
设置 `hbase.htable.threads.max` 生效.

这样就可以控制线程的数量, 让程序在 streaming 的平台上正常运行而不被 kill.
