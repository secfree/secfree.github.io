---
layout: blog
title: sqlmap 扫描对 "connection timed out" 的处理
---

一年前开发公司的扫描器时, 我选用 [sqlmap](http://sqlmap.org/) 来扫描 sql 注入.

当用 sqlmap 连续扫描同一域名的多个 url 后, 经常会返回:

```
[CRITICAL] connection timed out to the target URL or proxy. sqlmap is going to retry the request
```

因为对 server 端如何防御扫描不清楚, 我以为是 server 端对一个时间段内连接数达到某一值的 source_ip 采取拒绝连接操作. 因此我用 sqlmap 扫描时, 对连续的 N 个 url 用 "--proxy=PROXY" 设置一个不同的代理. 为了保证代理的数量和可用, 又实现了一个代理更新程序.

这样的确在很大程度上减少 "connection timed out" 的出现率, 但是今天发现了另外一个更为简单和有效的方法, 让我在心里直骂自己当初 SB.

方法很简单, 在用 sqlmap 扫描时, 添加选项 "--random-agent" 即可. 它会在每次连接时采用随机的 User-Agent .

可参考今年二月份 stackoverflow 上问的问题. : [Getting 'connection time out' error each time on the same step in sqlmap](http://stackoverflow.com/questions/21621337/getting-connection-time-out-error-each-time-on-the-same-step-in-sqlmap)

个人实测 "--random-agent" 是非常有效的. 由此可以推测, server 端应该是对同一 (source_ip, User-Agent) 的连接有限制, 一段时间内超过某个值则会拒绝链接.
