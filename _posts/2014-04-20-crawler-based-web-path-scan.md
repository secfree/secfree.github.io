---
layout: blog
title: 基于爬虫结果的 web 路径扫描程序
---

运维和开发人员可能会将程序源码包或备份文件放在 Web 目录中, 可以通过网络直接访问. 此时 Web 路径扫描就可以发挥作用. 一般都是通过迭代大量的目录和文件名列表发起 HTTP 请求, 根据结果判断文件是否存在.

在网络安全方面, 爬虫已经广泛应用, 且功能强大. 通过爬虫可以获得大量的 url,  [`bcrpscan`](https://github.com/secfree/bcrpscan) 会基于已有的 url 发起 HTTP 请求, 发现可疑的文件.

对于一个是目录的 url: http://test.com/a/, 它会尝试:

```
http://test.com/a.zip
http://test.com/a.rar
http://test.com/a.tar.gz
...
```

对于一个是文件的 url: http://test.com/b.php, 它会尝试:

```
http://test.com/b.php.bak
http://test.com/b.php.1
...
```

示例:

```
$ python bcrpscan.py -i test_urls
2014-04-20 19:43:03,484  INFO: http://192.168.1.6/test
2014-04-20 19:43:13,625  INFO: http://192.168.1.6/test67187c0f
2014-04-20 19:43:13,632  INFO: http://192.168.1.6/test.tar.gz
2014-04-20 19:43:13,638  INFO: http://192.168.1.6/test.zip
2014-04-20 19:43:13,646  INFO: http://192.168.1.6/test.rar
2014-04-20 19:43:13,733  INFO: http://192.168.1.667187c0f
2014-04-20 19:43:13,862  INFO: http://192.168.1.6/test.tar.bz2
2014-04-20 19:43:13,867  INFO: [+] http://192.168.1.6/test.rar
2014-04-20 19:43:23,847  INFO: http://192.168.1.6/test.rar250
------------------------------
Probed web paths:

http://192.168.1.6/test.rar
```

详情请点击: [`bcrpscan`](https://github.com/secfree/bcrpscan)
