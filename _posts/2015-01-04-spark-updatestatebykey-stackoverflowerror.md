---
layout: blog
title: "spark中updateStateByKey引发StackOverflowError的解决"
---

**问题描述**

写的spark程序, 运行几个小时候总是会出现 StackOverflowError. 

程序使用 spark-1.1 运行.

代码的逻辑大概是:

```
streamB = streamA.map().fiter().recudeByKeyAndWindow()
streamC = streamB.updateStateByKey()
streamD = streamC.updateStateByKey()
streamD.foreachRDD()
```

**原因**

1. 逐行注释代码, 发现error由updateStateByKey引发.
2. 在每个 updateStateByKey 之后用 foreachRDD 输出 toDebugString, 发现是因为 rdd 的 dependency chain 太长导致的StackOverflowError.

这就比较奇怪, 因为 updateStateByKey 默认会 checkpoint, 而 checkpoint 会切断 dependency chain.

可参考: [spark streaming checkpointing](https://spark.apache.org/docs/latest/streaming-programming-guide.html#checkpointing)

**默认的checkpoint机制和interval**

spark在代码中有 updateStateByKey 和 reduceByKeyAndWindow设置有 inverse function 的时候, 会自动 checkpoint .

checkpoint 的 interval 必须是 duration 的整数倍, 默认值为满足下面条件的最小值: 

```
interval >= max(duration, Duration(10000))
interval % duration = 0
```

其中 duration 是 streaming 的 batch duration.

如 [spark streaming checkpointing](https://spark.apache.org/docs/latest/streaming-programming-guide.html#checkpointing) 所讲

> checkpointing too infrequently causes the lineage and task sizes to grow which may have detrimental effects

如果将 batch duration 设置为 1 ms, checkpoint 的 interval 则会默认设为 10000 ms, 此时 batch 相对于 interval 来说就是 too infrequently. 这种设置下updateStateByKey很快就会引发 StackOverflowError.

另外需要注意的是, 每个 job 只会 checkpoint 一次.  可看源码:

```java
  /**
   * Run a function on a given set of partitions in an RDD and pass the results to the given
   * handler function. This is the main entry point for all actions in Spark. The allowLocal
   * flag specifies whether the scheduler can run the computation on the driver rather than
   * shipping it out to the cluster, for short actions like first().
   */
  def runJob[T, U: ClassTag](
      rdd: RDD[T],
      func: (TaskContext, Iterator[T]) => U,
      partitions: Seq[Int],
      allowLocal: Boolean,
      resultHandler: (Int, U) => Unit) {
    if (dagScheduler == null) {
      throw new SparkException("SparkContext has been shutdown")
    }
    val callSite = getCallSite
    val cleanedFunc = clean(func)
    logInfo("Starting job: " + callSite.shortForm)
    dagScheduler.runJob(rdd, cleanedFunc, partitions, callSite, allowLocal,
      resultHandler, localProperties.get)
    progressBar.foreach(_.finishAll())
    rdd.doCheckpoint()
  }
```

也就是说, 下面的代码只会 checkpoint 一次. (spark streaming中, 一个 output operation 有一个 job.)

```
streamC = streamB.updateStateByKey()
streamD = streamC.updateStateByKey()
streamD.foreachRDD()
```

streamC一直没有checkpoint, 从而导致 dependency chain 过长产生 StackOverflowError.

**foreachRDD 和 print 的不同**

我将代码改为:

```
streamB = streamA.map().fiter().recudeByKeyAndWindow()
streamC = streamB.updateStateByKey()
streamC.foreachRDD()
streamD = streamC.updateStateByKey()
streamD.foreachRDD()
```

发现虽然 streamC 的 checkpoint 已经生效, 但是它的 dependency chain 依旧没有截断. 个人陷入困境.

leader Ning 使用 print 而非 foreachRDD, 发现 dependency chain 有被截断.

foreachRDD 源码:

```java
  /**
   * Apply a function to each RDD in this DStream. This is an output operator, so
   * 'this' DStream will be registered as an output stream and therefore materialized.
   */
  def foreachRDD(foreachFunc: (RDD[T], Time) => Unit) {
    // because the DStream is reachable from the outer object here, and because 
    // DStreams can't be serialized with closures, we can't proactively check 
    // it for serializability and so we pass the optional false to SparkContext.clean
    new ForEachDStream(this, context.sparkContext.clean(foreachFunc, false)).register()
  }
```

print 源码:

```java
  /**
   * Print the first ten elements of each RDD generated in this DStream. This is an output
   * operator, so this DStream will be registered as an output stream and there materialized.
   */
  def print() {
    def foreachFunc = (rdd: RDD[T], time: Time) => {
      val first11 = rdd.take(11)
      println ("-------------------------------------------")
      println ("Time: " + time)
      println ("-------------------------------------------")
      first11.take(10).foreach(println)
      if (first11.size > 10) println("...")
      println()
    }
    new ForEachDStream(this, context.sparkContext.clean(foreachFunc)).register()
  }
```

发现两者都是调用了 ForEachDStream, 区别在于 foreachRDD 设置了 context.sparkContext.clean 的 checkSerializable 为 false. 

sparkContext.clean 的源码为:

```java
  /**
   * Clean a closure to make it ready to serialized and send to tasks
   * (removes unreferenced variables in $outer's, updates REPL variables)
   * If <tt>checkSerializable</tt> is set, <tt>clean</tt> will also proactively
   * check to see if <tt>f</tt> is serializable and throw a <tt>SparkException</tt>
   * if not.
   *
   * @param f the closure to clean
   * @param checkSerializable whether or not to immediately check <tt>f</tt> for serializability
   * @throws <tt>SparkException<tt> if <tt>checkSerializable</tt> is set but <tt>f</tt> is not
   *   serializable
   */
  private[spark] def clean[F <: AnyRef](f: F, checkSerializable: Boolean = true): F = {
    ClosureCleaner.clean(f, checkSerializable)
    f
  }
```

至于 checkSerializable 为 false 导致 rdd 的 dependency chain 不截断的原因, 貌似和 [一个KCore算法引发的StackOverflow奇案](http://rdc.taobao.org/?p=2417) 相同, 都是 $outer 导致, 这里没有深究.

**fix的方法**

因此, 将代码调整为:

```
streamB = streamA.map().fiter().recudeByKeyAndWindow()
streamC = streamB.updateStateByKey()
streamC.print()
streamD = streamC.updateStateByKey()
streamD.foreachRDD()
```

虽然 print 会将数据传回 driver, 会有一点影响性能, 但是可以接受.