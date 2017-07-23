---
layout: blog
title: Python多进程不要使用TimedRotatingFileHandler
---

在一个多进程的程序中使用TimedRotatingFileHandler, 以自动的切分日志.

运行一段时间后, 发现有大量的日志丢失, 并且程序报下面的异常

```
Traceback (most recent call last):
  File "/home/work/python27/lib/python2.7/logging/handlers.py", line 77, in emit
    self.doRollover()
  File "/home/work/python27/lib/python2.7/logging/handlers.py", line 347, in doRollover
    os.remove(dfn)
OSError: [Errno 2] No such file or director
```

程序报异常原因: doRollover()对文件缺失的异常没有处理.

日志丢失原因: 每个进程都会分别rotate, 导致日志丢失.

假设日志的backupCount=3, 按天rotate, 则第一个进程rotate时, 会执行

```
rm log.3
mv log.2 log.3
mv log.1 log.2
mv log log.1
...
```


如果进程共用一个日志文件, rotate只需要执行一次, 但实际是每个进程都会执行一次上面的过程, 第一个rotate之外的进程, 在rm的时候删除的都是没有过期的日志.

其实, Python的logging本身对多进程不支持.

> Although logging is thread-safe, and logging to a single file from multiple threads in a single process is supported, logging to a single file from multiple processes is not supported, because there is no standard way to serialize access to a single file across multiple processes in Python. If you need to log to a single file from multiple processes, the best way of doing this is to have all the processes log to a SocketHandler, and have a separate process which implements a socket server which reads from the socket and logs to file. (If you prefer, you can dedicate one thread in one of the existing processes to perform this function.) The following section documents this approach in more detail and includes a working socket receiver which can be used as a starting point for you to adapt in your own applications.

但是一般而言, 多进程使用同一个日志文件, 也不会造成日志混乱.

如果需要保证多进程的日志安全的输出到同一个日志文件, 则可以:

- 将日志发送到同一个进程, 由这个进程负责输出.
- 对日志输出进行加锁, 每个进程输出日志前需要先获得锁, 如[ConcurrentLogHandler](https://github.com/mpasternacki/ConcurrentLogHandler).

但这样会因为日志使程序复杂, 或影响性能.

回到初始多进程日志切分的问题, 不能使用TimedRotatingFileHandler, 可以用cron配合日志切分脚本实现.

另外需要注意的是, logging不应该用FileHandler, 而应该用WatchedFileHandler.

当日志文件被移动或删除后:

- FileHandler会继续将日志输出至原有的文件描述符, 从而导致日志切分后日志丢失.
- WatchedFileHandler会检测文件是否被移动或删除, 如果有, 会新建日志文件, 并输出日志到新建的文件.

