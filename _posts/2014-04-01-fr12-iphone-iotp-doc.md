---
layout: blog
title: "FR12: iPhone 上 iOTP 使用文档"
---

OTP 的值一般由下面的值取 MD5 的前6位得到

1. 当前的时间值, 以10秒为粒度 .

2. 用户输入的 4位数字的 Pin .

3. 初始化时生成的16位的16进制 secert .

---

iOTP 是 iPhone 上一个可以生成 OTP 的 App.

下面是安装, 初始化和使用过程.

从 App Store 安装 iOTP:

![]({{ site.url }}/downloads/iotp01.jpg)

进入 iOTP, 选择创建帐户

![]({{ site.url }}/downloads/iotp02.jpg)

添加账户

![]({{ site.url }}/downloads/iotp03.jpg)

设置名字和初始化 secret

![]({{ site.url }}/downloads/iotp04.jpg)

选择 16 位的 secret

![]({{ site.url }}/downloads/iotp05.jpg)

生成的 secret 需要记录, 提供给 server 端.

![]({{ site.url }}/downloads/iotp06.jpg)

![]({{ site.url }}/downloads/iotp07.jpg)

切换到 otp 生成页面

![]({{ site.url }}/downloads/iotp08.jpg)

输入 Pin 值, 生成 OTP

![]({{ site.url }}/downloads/iotp09.jpg)