---
layout: blog
title: "FR02: SSH 采用 FreeRADIUS 验证"
---

1. 安装 pam\_radius\_auth

   ```
   $ yum install pam-devel
   $ wget ftp://ftp.freeradius.org/pub/radius/pam_radius-1.3.17.tar.gz
   $ tar zxvf pam_radius-1.3.17.tar.gz
   $ cd pam_radius-1.3.17
   $ make
   $ cp pam_radius_auth.so /lib/security/
   $ cp pam_radius_auth.so /lib64/security/
   $ mkdir /etc/raddb
   $ cp pam_radius_auth.conf /etc/raddb/server
   $ vi /etc/raddb/server
   # 修改secret，缺省localhost的secret是testing123,见FreeRADIUS server 的clients.conf
   # 修改内容为
   freeradius_server_ip       secret_value             1

   $ vi /etc/pam.d/sshd
   #%PAM-1.0
   auth    sufficient      pam_radius_auth.so   //添加这一行，添加的位置有讲究，放到下面可能会出错。
   auth       include      system-auth
   ```

1. 重启 sshd 服务即可

   ```
   $ service sshd restart
   ```

   NAS 上 原本设有密码的账户, otp 和 原来的密码都能够登录.

   原本没有密码的账户, otp 登录.


**注意:**

FreeRADIUS server 端需要用相应的 client 信息. 假设 NAS 的 ip 为 192.168.1.10, 在需要在 server 的 clients.conf 中添加

```
client client01 {
        ipaddr = 192.168.1.10
        secret =secret_value
        nas_type  = other
        require_message_authenticator = no
}
```

或者 ip 为在 clients.conf 中 ( ipaddr, netmask ) 的范围中.

**参考**

1. [freeradius+pam+ssh简单测试 ](http://orzee.blog.51cto.com/3105498/618098)
