---
layout: blog
title: "用 \"wget -N\" 更新文件"
---

程序经常需要有自动更新的功能, 我以前都是比较新旧文件的 MD5 值来决定是否更新.

今天同事告诉我一个使用 wget 通过 http 更新的简单实用的方法.

> wget -N http://ip:port/path/file

"-N" 的在 "man wget"中

> -N (for timestamp-checking)

如例:

```
[root@localhost 3]# wget -N http://10.1.14.59/shelscan/db
--2014-03-12 17:41:56--  http://10.1.14.59/shelscan/db
正在连接 10.1.14.59:80... 已连接。
已发出 HTTP 请求，正在等待回应... 200 OK
长度：6041600 (5.8M) [application/octet-stream]
正在保存至: “db”

100%[================================================================================>] 6,041,600   3.77M/s   in 1.5s    

2014-03-12 17:41:57 (3.77 MB/s) - 已保存 “db” [6041600/6041600])

[root@localhost 3]# wget -N http://10.1.14.59/shelscan/db
--2014-03-12 17:41:59--  http://10.1.14.59/shelscan/db
正在连接 10.1.14.59:80... 已连接。
已发出 HTTP 请求，正在等待回应... 200 OK
长度：6041600 (5.8M) [application/octet-stream]
远程文件比本地文件 “db” 更老 -- 不取回。
```

有兴趣的还可以看下 [wget 使用技巧](https://linuxtoy.org/archives/wget-tips.html).

很多常用的工具其实很强大, 自己编程实现相关功能前, 可以先看看类似功能工具的 option .