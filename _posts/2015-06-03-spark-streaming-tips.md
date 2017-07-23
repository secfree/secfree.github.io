---
layout: blog
title: "Spark Streaming 编程 Tips"
---

**什么时候用 cache**

```java
DStream streamA = stream.map(new funA());
DStream streamB = streamA.map(new funB());
DStream streamC = streamA.map(new funC());
```

如上面的代码, 当一个 DStream 有分支, 或者说会被多次使用时, 需要 cache . 

原因和 RDD cache 一样:

> By default, each transformed RDD may be recomputed each time you run an action on it. However, you may also persist an RDD in memory using the persist (or cache) method, in which case Spark will keep the elements around on the cluster for much faster access the next time you query it. 

如果 streamA 不 cache, 可以从 funA 的 log 中看到被 `recomputed` .

使用 cache 的代码示例:

```java
DStream streamA = stream.map(new funA());
streamA.cache();
DStream streamB = streamA.map(new funB());
DStream streamC = streamA.map(new funC());
```

---

**window 和 slide 的设置**

在流式数据处理中, 使用 `reduceByKeyAndWindow`  按照小时来聚合数据是很常见的. "小时"并不是以程序启动开始计时一个的小时长度, 而是按照自然的时间分隔. 如果 window 和 slide 都设置为一个小时, 则程序需要整点启动, 才能输出期望的数据, 这会在运维时很不方便.

另一种可选的方法是, 将 slide 设置为一个比 window 小的值. Example:

```
window = 60 min
slide = 10 min
```

此时程序每个 slide (10 min) 会有一次输出, 只需要程序启动时的分钟数为 10 的整数倍, 并且过滤掉非整点的输出, 就可以得到一个小时内的数据聚合结果. 这比需要在整点启动方便很多.

如果按照满足下面条件的方式设置:

```
// example: window = 70 min, slide = 10 min
hour < n * slide <= window < (n+1) * slide, 其中 (n > 1)
```

并过滤掉:

1. 非上一个小时的数据.
1. 非上一个小时的第一次数据.

就可以在任意的时刻启动程序, 并在一个小时的第一个 slide 时间范围内, 得到上一个小时的聚合数据.

值得注意的一点是, 当 window 和 slide 的值不同时, 程序刚启动, `reduceByKeyAndWindow` 的第一次输出是在一个 slide 后, 而不是在一个 window 后. 也就是说, 第一次的输出结果, 数据的聚合时间跨度没有一个 window.

上面提到的 `过滤非上一个小时的第一次数据`, 如果第一次数据有存入数据库之类, 并且查询代价不大, 可以通过数据查询来判断是否为第一次. 另一种可选的方式是用 updateStateByKey 维护一个如下结构的 state:

```
state {
    int hour;
    boolean flag;
}
```

---

**updateStateByKey 的 key 的选择**

因为 `updateStateByKey` 需要 checkpoint, 而 checkpoint 的代价比较大, 一般有较大的延时. 因此, key 应该尽量选择为一个固定的较小范围的集合, 而不是增长的很大范围的集合.

Example, 选择 `user_id` 是一个固定的较小范围的集合, 而 `(user_id, hour)` 则是随着时间而增长的集合.