---
layout: blog
title: "FR01: 部署 FreeRADIUS 3 用 MOTP 验证"
---

[FreeRADIUS](http://freeradius.org/) 可以用来部署集中式的验证.  FreeRADIUS server 在接收到 NAS ( Network Access Server 网络接入服务器) 的包时, 可以根据配置选择不同的验证方式验证, 如 PAP, CHAP, PAM 等. 我们需要配置为采用 [MOTP](http://motp.sourceforge.net/) ( Mobile One Time Passwords ) .

同事测试过 FreeRADIUS 2, 在验证 ssh 时出现问题没有解决. 因此我使用最新版本 3.

> Version 3 introduces a number of major changes over Version 2. The debugging output is clearer (and colorized!), more errors are found while the configuration files are parsed instead of at run-time, RADIUS over TLS (i.e. RadSec) and RADIUS over TCP are supported. Theraddb/ directory has been re-arranged so that files are easier to find.

网上的文档基本上都是关于版本 2 的, 对于版本 3 经测试发现并不适用. 而我在[官网的 wiki](http://wiki.freeradius.org/Home)  也没有找到详细适用的配置,
在此记录的是多次尝试所得. 并不一定是标准配置, 在某些条件些可能引发一些问题.

验证过程示意图:

![]({{ site.url }}/downloads/freeradius_auth_process.jpg)

1. #### **安装 FreeRADIUS 3**

    在官网下载 freeradius-server-3.0.1.tar.gz , 解压后进入目录执行

   ```
   $ ./configure
   $ make
   $ make install
   ```

    需要测试是否安装成功.

    修改 users 文件:

   ```
   $ cd /usr/local/etc/raddb
   $ vim users
   ...
   # 去掉和steve 相关注释, 使用户有效
   steve  Cleartext-Password := "testing"
      Service-Type = Framed-User,
      Framed-Protocol = PPP,
      Framed-IP-Address = 172.16.3.33,
      Framed-IP-Netmask = 255.255.255.0,
      Framed-Routing = Broadcast-Listen,
      Framed-Filter-Id = "std.ppp",
      Framed-MTU = 1500,
      Framed-Compression = Van-Jacobsen-TCP-IP
   ...
   ```

    以调试模式启动 radiusd 服务

   ```
   $ radiusd -X
   ```

   在另一个 shell 用 radtest 测试

   ```
   $ radtest steve testing localhost 1812 testing123
   ...
   rad_recv: Access-Accept packet...
   ...
   ```

    出现 "rad_recv: Access-Accept packet" 则证明安装成功.

2. #### **安装生成 OTP 的 APP .**

    OTP 有很多的程序可以生成, 方便起见, 我采用在 Android 上用"豌豆荚"安装 DroidOTP .

    首次进入 DroidOTP, 需要在 "settings" 中初始化, 即 "add profile". 点击 "add", ( 晃动手机或输入一个数值 )生成一个 Secret, 需要记录为 secret_value .

    输入 Pin 值 按 "OK" 即可生成 otp . 同一用户一般使用固定的 Pin 值, 记为 pin_value .

3. #### **安装 MOTP .**

   ```
   $ cd /usr/local/bin
   $ wget http://motp.sourceforge.net/otpverify.sh
   $ chmod +x otpverify.sh
   $ vi /usr/local/bin/otpverify.sh
   ...
   # 在 PATH=$PATH:/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin下加入:
   alias checksum=md5sum
   have_md5="true"
   ```

    因为 FreeRADIUS 的返回值和 otpverify.sh 的有冲突, 需要将 otpverify.sh 中 exit 除 0 以外的值都加上 10 . 例如:

   ```
   exit 1 ==> exit 11
   exit 3 ==> exit 13
   ```

    修改后的 otpverify.sh 可在此[下载]({{ site.url }}/downloads/otpverify.sh)

    另外 otpverify.sh 是由 ksh 解析执行而不是 bash, 所以需要先安装 ksh .

    添加 MOTP 对应的 dictionary .

   ```
   $ cd /usr/local/etc/raddb
   $ wget http://motp.sourceforge.net/dictionary.motp
   $ vi dictionary
   ...
   # 添加
   $INCLUDE        dictionary.motp
   ```

    创建 MOTP 需要的目录

   ```
   $ mkdir /var/motp
   $ mkdir /var/motp/cache
   $ mkdir /var/motp/users
   ```

    在 /usr/local/etc/raddb/users 底部添加

   ```
   DEFAULT Auth-Type := Accept, Simultaneous-Use := 1
   #下面两行前面的空白为 tab.  secret_value, pin_value 为 DroidOTP 中记录的值.
       Exec-Program-Wait = "/usr/local/bin/otpverify.sh '%{User-Name}' '%{User-Password}' 'secret_value' 'pin_value' '2'",
       Fall-Through = Yes
   ```

    接下来测试 MOTP 验证.

    启动 radiusd 服务.

   ```
   $ radiusd -X
   ```

    在 DroidOTP 中输入 Pin, 生成 otp 为 opt_value .

    用 radtest 进行验证测试

   ```
   radtest dzqtest otp_value 127.0.0.1 1812 testing123
   ```

    返回 "rad_recv: Access-Accept" 则证明 MOTP 验证成功.

    其中 testing123 为 "/usr/local/etc/raddb/clients.conf" 中 "client localhost" 下的 "secret" 值决定.

#### **注:**

1. 标题 FR01 中的 FR 表示 FreeRADIUS .    

#### **参考:**

1. [Mobile-OTP和freeradius配置](http://blog.sina.com.cn/s/blog_704836f40101iqk9.html)

2. [freeradius+pam+ssh简单测试](http://orzee.blog.51cto.com/3105498/618098)

3. [RFC 2865 RADIUS 中文翻译](http://blog.chinaunix.net/uid-2628744-id-2454869.html)
