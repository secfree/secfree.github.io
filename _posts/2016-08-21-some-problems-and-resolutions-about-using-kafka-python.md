---
layout: blog
title: "Some Problems and Resolutions about Using kafka-python"
---

I've been using [kafka-python](https://github.com/dpkp/kafka-python) for several weeks. I came across some problems and tryed to fix them. I recorded all of them here.

Version info:

| program | version|
| --- | --- |
| kafka | 0.9 |
| kafka-python | 1.3.1 |
| python | 2.7 |

---

# Unassigned partition

<br />

Got the below error log when using `KafkaConsumer` and try to seek offset

```
AssertionError: Unassigned partition
```

I've posted my answer on [stackoverflow AssertionError: Unassigned partition](http://stackoverflow.com/questions/36381328/assertionerror-unassigned-partition/38949407#38949407)

---

# Get messages from broker unstable

<br />

When I trying to consume messages from brokers concurrently, I found it's unstable. Sometimes I can get back enough messages and sometimes not, even though there a large lag of the consume group.

I's caused by the following two reasons.

1. The consume thread number is bigger than the partition number. At the same time, there is just one thread can consume a partition of the same (topic, group).
1. The parameter `consume_timeout_ms` is too small. If you set a small value of this parameter and it's not enough to wait to get the partition, it may exit the `for msg in consumer` iteration.

---

# Kafka is always rebalancing

<br />

The effect of rebalance

> Rebalancing is the process where a group of consumer instances (belonging to the same group) co-ordinate to own a mutually exclusive set of partitions of topics that the group is subscribed to. At the end of a successful rebalance operation for a consumer group, every partition for all subscribed topics will be owned by a single consumer instance within the group. The way rebalancing works is as follows. Every broker is elected as the coordinator for a subset of the consumer groups. The co-ordinator broker for a group is responsible for orchestrating a rebalance operation on consumer group membership changes or partition changes for the subscribed topics. It is also responsible for communicating the resulting partition ownership configuration to all consumers of the group undergoing a rebalance operation.

And so

> When a new consumer joins a consumer group the set of consumers attempt to "rebalance" the load to assign partitions to each consumer. If the set of consumers changes while this assignment is taking place the rebalance will fail and retry.

In my environment, it's always rebalancing because of my incorrect use of group with topic. I used the same group_id consume another topic in another program. Which make the set of consumers for the group_id always changing. I set it to a different group_id and the always rebalancing problem disappeared.

Refer: [What exactly IS Kafka Rebalancing?](http://stackoverflow.com/questions/30988002/what-exactly-is-kafka-rebalancing)

---

# Offset not commited automatically

<br />

When I use the consumer like the demo

```python
for msg in consumer:
    process(msg)
```

I monitored the consume offset by [Kafka Manager](https://github.com/yahoo/kafka-manager) and found sometimes the offset is not commited even messages consumed. I test the code in ipython and it's OK.

Actually the offset is commited async with a interval 5000 ms. If the consumer instance is freed before commit, the offset is lost. So I add `consumer.commit()` explicitly and the offset in `Kafka Manager` became normal.

```python
for msg in consumer:
    process(msg)
consumer.commit()
```

---

# Too many open files

<br />

After my consume program running several days, it complained the folloing error

```
ERROR: 2016-08-16 09:11:07,053: kafka_fetcher.py:34 Get message from kafka failed: [Errno 24] Too many open files
```

This problem's reason it's obvious and it became OK after adding `consumer.close()`

```
for msg in consumer:
    process(msg)
consumer.commit()
consumer.close()
```

---

# Fetch to node failed because of ConnectionError

<br />

After add `consumer.close()` it complained

```
ERROR: 2016-08-17 15:55:42,092: future.py:79 Fetch to node 1001 failed: ConnectionError: <BrokerConnection host=hostname_01/ip_address_01 port=9092>
```

I can't google any information about this problem. So I read the source code and locate the reason by traceback. I write all in detail here: [Help: Fetch to node failed because of ConnectionError](https://github.com/dpkp/kafka-python/issues/805).
