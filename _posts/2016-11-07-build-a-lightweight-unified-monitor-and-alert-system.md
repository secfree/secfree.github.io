---
layout: blog
title: "Build a Lightweight Unified Monitor and Alert system"
---

I have a lot of programs run on different platforms, some are small scripts, some are large frameworks. I've monitored them by a lot of scripts and found it's hard to track and maintain. So I want to build a unified system for monitor as well as alert.

`Zabbix` and `Nagios` are not my choice, they are too heavy. What I need should meet the conditions:

1. Without agent. Some programs are running in docker, it's not convinent to install a agent.
1. Lightweight enough.
1. Concise concepts and simple principles.
1. Send metrics by UDP. The monitor part should not affect the main program.
1. > Measure Anything, Measure Everything

After the survey, I choice [graphite](http://graphiteapp.org/). Send a metric to graphite is very simple, such as:

```
echo "secfree.test 4 `date +%s`" | nc server port
```

As above, you can send a message of three component (key, value, timestamp) by TCP or UDP. So I can send metrics from shell, python scripts, java program easily. The key supports hierarchy. After send metrics to graphite, we can view it on graphite-web.

![graphite-composer]({{ site.url }}/downloads/graphite-composer.png)

[graphite events](http://graphite.readthedocs.io/en/latest/events.html) can be used to track something that is not numeric.

![graphite-events]({{ site.url }}/downloads/graphite-events.png)

With the collected metrics, we can check and alert when necessary. An optional tools is [graphite-beacon](https://github.com/klen/graphite-beacon): a simple alerting system for Graphite metrics.

Refer:

1. [Measure Anything, Measure Everything](https://codeascraft.com/2011/02/15/measure-anything-measure-everything/)
1. [Tracking Every Release](https://codeascraft.com/2010/12/08/track-every-release/)
1. [Counting & Timing](http://code.flickr.net/2008/10/27/counting-timing/)
1. [Monitoring 101: Collecting the right data](https://www.datadoghq.com/blog/monitoring-101-collecting-data/)
1. [Monitoring 101: Alerting on what matters](https://www.datadoghq.com/blog/monitoring-101-alerting/)
