---
layout: blog
title: "oozie 调度任务运行问题"
---

这里记录由 oozie 调度运行的任务出现的因为 oozie 本身配置相关的问题.
虽然只是配置, 但 oozie 和 Hadoop 生态系统的其他多个组件一起使用时遇到问题,
很多时候日志并不明确, 也很难查找到对应的资料, 排查还是比较消耗时间.

---

# Map-Reduce local 运行

<br />

## Description

在 Ambari 中, 运行的 oozie 调用 `java action`,
`java action` 中用 crunch 调用 map-reduce.

oozie 在运行 `java action` 时, 会将其作为一个单一的 Mapper 任务.

> Java applications are executed in the Hadoop cluster as map-reduce job
with a single Mapper task.

程序在 yarn 上运行时, 发现 crunch 创建的 map-reduce 在 `java action` Mapper 的
NodeManager 机器上 local 运行.
也就是实际 crunch 没有能够在 YARN 中以 map-reduce 运行.

而在以前的测试环境中, 没有用 Ambari 部署, crunch 是可以正常运行 map-reduce 的.

oozie 任务的最终返回码是: `JA019` .

> JA019 is error while executing distcp action.

## Solution

需要将 Hadoop 和 YARN 的

```
core-site.xml
hdfs-site.xml
yarn-site.xml
mapred-site.xml
```

四个文件拷贝到 oozie 任务配置文件的 `lib` 目录下.
oozie 调度的任务会将 lib 目录下的文件加到 classpath 中.

暂时不知道为什么, 在 oozie server 的 `oozie-server/lib/` 中放置这些文件没有生效.
之前在测试环境中是可以的.

---

# token can't be found in cache

<br />

## Description

oozie 调用 `java action`, `java action` 中用 crunch 调用 map-reduce.

当 reduce 任务的 final status 为 FAIL 时, jobhistory 上找不到该任务的记录,
同时 oozie 的 workflow 失败:

```
JA017: Could not lookup launched hadoop Job ID [job_1448357535020_0128] which was associated with  action [0001954-151124202619144-oozie-oozi-W@aggregate].  Failing this action!
    at org.apache.oozie.action.hadoop.JavaActionExecutor.check(JavaActionExecutor.java:1359)
    at org.apache.oozie.command.wf.ActionCheckXCommand.execute(ActionCheckXCommand.java:182)
    at org.apache.oozie.command.wf.ActionCheckXCommand.execute(ActionCheckXCommand.java:56)
    at org.apache.oozie.command.XCommand.call(XCommand.java:286)
    at org.apache.oozie.service.CallableQueueService$CompositeCallable.call(CallableQueueService.java:321)
    at org.apache.oozie.service.CallableQueueService$CompositeCallable.call(CallableQueueService.java:250)
    at org.apache.oozie.service.CallableQueueService$CallableWrapper.run(CallableQueueService.java:175)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
    at java.lang.Thread.run(Thread.java:722)
```

reduce 任务显示 final status 为 FAIL 前, 报错:

```
11-24 15:37:51 026 [org.apache.hadoop.ipc.Client]-[WARN] Exception encountered while connecting to the server : org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.token.SecretManager$InvalidToken): token (HDFS_DELEGATION_TOKEN token 692 for work) can't be found in cache
11-24 15:37:51 027 [org.apache.hadoop.hdfs.LeaseRenewer]-[WARN] Failed to renew lease for [DFSClient_NONMAPREDUCE_1014707616_1] for 911 seconds.  Will retry shortly ...
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.token.SecretManager$InvalidToken): token (HDFS_DELEGATION_TOKEN token 692 for work) can't be found in cache
     at org.apache.hadoop.ipc.Client.call(Client.java:1468)
     at org.apache.hadoop.ipc.Client.call(Client.java:1399)
     at org.apache.hadoop.ipc.ProtobufRpcEngine$Invoker.invoke(ProtobufRpcEngine.java:232)
        at com.sun.proxy.$Proxy14.renewLease(Unknown Source)
     at org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolTranslatorPB.renewLease(ClientNamenodeProtocolTranslatorPB.java:571)
     at sun.reflect.GeneratedMethodAccessor10.invoke(Unknown Source)        
     at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
     at java.lang.reflect.Method.invoke(Method.java:601)
     at org.apache.hadoop.io.retry.RetryInvocationHandler.invokeMethod(RetryInvocationHandler.java:187)       
     at org.apache.hadoop.io.retry.RetryInvocationHandler.invoke(RetryInvocationHandler.java:102)
     at com.sun.proxy.$Proxy15.renewLease(Unknown Source)
        at org.apache.hadoop.hdfs.DFSClient.renewLease(DFSClient.java:879)
        at org.apache.hadoop.hdfs.LeaseRenewer.renew(LeaseRenewer.java:417)
        at org.apache.hadoop.hdfs.LeaseRenewer.run(LeaseRenewer.java:442)
        at org.apache.hadoop.hdfs.LeaseRenewer.access$700(LeaseRenewer.java:71)
     at org.apache.hadoop.hdfs.LeaseRenewer$1.run(LeaseRenewer.java:298)
     at java.lang.Thread.run(Thread.java:722)
```

## Solution

在 `mapred-site.xml` 中添加:

```xml
<property>
    <name>mapreduce.job.complete.cancel.delegation.tokens</name>
    <value>false</value>
    <description> if false - do not unregister/cancel delegation tokens from
        renewal, because same tokens may be used by spawned jobs
    </description>
</property>
```

这个其实是 `mapred-site.xml` 的参数, 但是需要运行周期较长的任务才能触发,
因此由 oozie 调度遇到此问题可能性较大.

该修改需要同步到 ResourceManager 和所有的 NodeManager 上.

---

# E0505: App definition does not exist

<br />

## Description

run oozie job 时报错:

```
E0505: App definition [hdfs://path/to/app/] does not exist
```

job.properties 中配置有:

```
oozie.coord.application.path=hdfs://namenode/apps/example
```

## Solution

因为 `oozie.coord.application.path` 指定的是目录,
所以 coordinate 的文件名必须是标准的:

```
coordinator.xml
```

如果直接用的是 `workflow`, 则文件名必须为:

```
workflow.xml
```

也可以将 `oozie.coord.application.path` 直接指向文件而非目录.

另外:

> The workflow application path must be specified in the file with the oozie.wf.application.path property. The coordinator application path must be specified in the file with the oozie.coord.application.path property.The bundle application path must be specified in the file with the oozie.bundle.application.path property. Specified path must be an HDFS path.
