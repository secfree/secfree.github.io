---
layout: blog
title: "YARN 中 Scheduler 选择"
---

网上已经有一些对 `Capacity Scheduler` 和 `Fair Scheduler` 的比较, 如: [Fair Scheduler与Capacity Scheduler比较](http://www.programgo.com/article/39985011423/). 但是从中看不出应该选择哪一个, 来对 YARN 管理的资源来做用户级的配置.

仔细对比了下官方文档 [Capacity Scheduler](http://hadoop.apache.org/docs/r2.6.0/hadoop-yarn/hadoop-yarn-site/CapacityScheduler.html) 和 [Fair Scheduler](http://hadoop.apache.org/docs/r2.6.0/hadoop-yarn/hadoop-yarn-site/FairScheduler.html) 中的配置项, 最终选择 `Fair Scheduler`, 原因如下:

1. `Fair Scheduler` 的 `allocation file` -- 一般为 `fair-scheduler.xml`, 每隔 10 秒会自动重新载入. 可以方便的更新配置. 而 `Capacity Scheduler` 的配置文件更改后需要重启 YARN 才能生效.

1. `Fair Scheduler` 支持对特定单个用户的配置, 有灵活 queue 分配方式. 如可以将 user 分配到和用户名相同的 queue. 而 `Capacity Scheduler` 中的 queue 中对用户的配置会对该 queue 中所有的用户生效.

1. `Fair Scheduler` 中对资源按实际值来配置, 如内存可以为 MB, vcore 的个数. 而 `Capacity Scheduler` 对资源是按百分比控制. 当集群的范围有改动时, 可能需要更新对 queue 的资源分配比例.