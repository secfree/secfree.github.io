---
layout: blog
title: "FR06: Android 上 DroidOTP 使用文档"
---

OTP 的值一般由下面的值取 MD5 的前6位得到

1. 当前的时间值, 以10秒为粒度 .

2. 用户输入的 4位数字的 Pin .

3. 初始化时生成的16位的16进制 secert .

DroidOTP 是 Android 上一个可以生成 OTP 的 App, 我从 "豌豆荚" 安装它. 

下面是初始化和使用的过程.

1. 进入 App 

    ![]({{ site.url }}/downloads/droidotp01.png)

2. 添加 Profile

    ![]({{ site.url }}/downloads/droidotp02.png)

3. 初始化

    ![]({{ site.url }}/downloads/droidotp03.png)

    ![]({{ site.url }}/downloads/droidotp04.png)

4. 生成的 Secret 需要保存到 FreeRADIUS server, 如果你选择 "Hide secret",  需要先把它记录下来.

    ![]({{ site.url }}/downloads/droidotp05.png)

    ![]({{ site.url }}/downloads/droidotp06.png)

**参考**

1. [http://motp.sourceforge.net/](http://motp.sourceforge.net/)