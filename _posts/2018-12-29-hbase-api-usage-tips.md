---
layout: blog
title: "HBase API Usage Tips"
---

# Why calling API got stuck for hours?

<br />

Sometimes we may get stuck when calling an HBase API, such as `get`. The reason is, when the HBase server is down, the `createConnection` and `getTable` actions will return without exceptions. And in such situations, the `get` action of the table will get stuck.

Let's show this in detail. First, create an HBase connection to an invalid (host, port)

```scala
scala> val conf = HBaseConfiguration.create()
conf: org.apache.hadoop.conf.Configuration = Configuration: core-default.xml, core-site.xml, hbase-default.xml, hbase-site.xml
scala> conf.set("hbase.zookeeper.quorum", "localhost")
scala> var con = ConnectionFactory.createConnection(conf)
2018-08-01 12:09:44 WARN ClientCnxn:1102 - Session 0x0 for server null, unexpected error, closing socket connection and attempting reconnect
java.net.ConnectException: Connection refused
        at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
        at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:717)
        at org.apache.zookeeper.ClientCnxnSocketNIO.doTransport(ClientCnxnSocketNIO.java:361)
        at org.apache.zookeeper.ClientCnxn$SendThread.run(ClientCnxn.java:1081)
2018-08-01 12:09:44 WARN ClientCnxn:1102 - Session 0x0 for server null, unexpected error, closing socket connection and attempting reconnect
java.net.ConnectException: Connection refused
        at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
        at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:717)
        at org.apache.zookeeper.ClientCnxnSocketNIO.doTransport(ClientCnxnSocketNIO.java:361)
        at org.apache.zookeeper.ClientCnxn$SendThread.run(ClientCnxn.java:1081)
        ...

scala> console.setThreshold(Level.ERROR)
```

We can see that it keeps reconnecting in the background, but the "createConnection" returns normally.

And then we can see that we can not know if the connection is succeeded by the return value.

```scala
scala> con.
abort   close   getAdmin   getBufferedMutator   getConfiguration   getRegionLocator   getTable   isAborted   isClosed
scala> con.isClosed() res1: Boolean = false
scala> con.isAborted() res2: Boolean = false
```

Finally, when we try to get a rowkey, the program gets stuck and keeps outputting ERROR messages

```scala
scala> val table = con.getTable(TableName.valueOf("test"))
table: org.apache.hadoop.hbase.client.Table = test;hconnection-0x75d6b1dd
scala> table.get(new Get(Bytes.toBytes("test")))
2018-08-01 12:11:42 ERROR RecoverableZooKeeper:274 - ZooKeeper getData failed after 4 attempts
2018-08-01 12:11:42 ERROR ZooKeeperWatcher:744 - hconnection-0x45bc9c0a0x0, quorum=localhost:2181, baseZNode=/hbase Received unexpected KeeperException, re-throwing exception
org.apache.zookeeper.KeeperException$ConnectionLossException: KeeperErrorCode = ConnectionLoss for /hbase/meta-region-server
        at org.apache.zookeeper.KeeperException.create(KeeperException.java:99)
        at org.apache.zookeeper.KeeperException.create(KeeperException.java:51)
        at org.apache.zookeeper.ZooKeeper.getData(ZooKeeper.java:1155)
        ...
2018-08-01 12:12:00 ERROR RecoverableZooKeeper:274 - ZooKeeper getData failed after 4 attempts
2018-08-01 12:12:00 ERROR ZooKeeperWatcher:744 - hconnection-0x45bc9c0a0x0, quorum=localhost:2181, baseZNode=/hbase Received unexpected KeeperException, re-throwing exception
org.apache.zookeeper.KeeperException$ConnectionLossException: KeeperErrorCode = ConnectionLoss for /hbase/meta-region-server
        at org.apache.zookeeper.KeeperException.create(KeeperException.java:99)
        at org.apache.zookeeper.KeeperException.create(KeeperException.java:51)
        at org.apache.zookeeper.ZooKeeper.getData(ZooKeeper.java:1155)
        ...
        ...
```

In order to throw exception quickly if the HBase cluster is in an invalid status,  we should check the status at first

```scala
HBaseAdmin.checkHBaseAvailable(conf)
```

If the connection is created successfully but disconnected when executing "get", in order to not block for hours, we should set some configures

```scala
conf.set("hbase.client.operation.timeout", "3000")
conf.set("hbase.client.retries.number", "2")
conf.set("zookeeper.session.timeout", "60000")
conf.set("zookeeper.recovery.retry", "0")
conf.set("hbase.client.meta.operation.timeout", "3000")
conf.set("hbase.rpc.timeout", "600")
```

All these items determine the block time of `table.get()`.

---

# What's the difference between HBase Connection and other connections?

<br />

Is there any difference between HBase Connection and other connections(such as MySQL ...)?

> From HBase 0.98.x you don't need to pool HTable object anymore. Just get Table object from Connection object when you need, all the resource caching and management is done internally by Connection class.

That's to say, the HBase Connection is already a connection pool. You do not need to create a lot of connections.
