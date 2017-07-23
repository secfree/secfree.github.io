---
layout: blog
title: "协程与并行"
---

从协程的经典例子"生产者-消费者"领略到它的魅力, 网上随处可见的也都是说协程的长处, 很少提及协程的局限.

我要实现一个爬虫程序, 要求可以高速的加载和分析 url, 很明显, 单进程是不满足需求的. 在爬虫中, 通过网络加载 url 的 IO 操作是最消耗时间的, 可以用多线程并行的加载 url . 但是可否能以协程代替多线程实现呢 ?

> 协程(coroutine)顾名思义就是“协作的例程”（co-operative routines）。

我的理解是协程的作用是`程序的协作而不是并行`. 因此是不适用爬虫中 url 的并行加载的.

但在 [高性能python编程之协程(stackless)](http://www.pythontab.com/html/2014/pythonhexinbiancheng_0107/660.html) 中有下面的阐述和示例:

> 协程间是协同调度的，这使得并发量在上万的时候，协程的性能是远高于线程的。

```python
import stackless
import urllib2
def output ():
    while 1:
        url =chan. receive()
        print url
        f =urllib2.urlopen(url )
        #print f.read()
        print stackless.getcurrent()

def input ():
    f=open ('url.txt')
    l=f .readlines()
    for i in l:
        chan .send( i)
chan=stackless .channel()
[stackless.tasklet(output )() for i in xrange (10)]
stackless.tasklet (input)()
stackless.run ()

```

在知乎上找到了可以证实自己理解的答案: [协程的好处是什么](http://www.zhihu.com/question/20511233)

> 协程既不是空间并行也不是时间并行，所以它并不是并行计算。

-

> corutine 是从另一个方向演化而来，它是对 continuation 概念的简化。Lua 设计者反复提到，corutine is one-shot semi-continuation。PiL 中给出了 iterator 例子，就是 continuation 如何可以把本来的多次回调变成一个连续的过程。理解了 continuation 也就理解了 corutine。

因此, 协程的适用场合是例程间的回调和协作, 而不并能实现并行. 爬虫这种需要并行的 IO 等待的情况, 还是需要使用线程.