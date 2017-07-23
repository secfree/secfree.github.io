---
layout: blog
title: "FR11: FreeRADIUS 安装配置 tips"
---

FreeRADIUS 安装过程中, 会去检测相应模块以来的库文件, 如果库文件存在, 则编译和安装相应的模块, 否则会跳过.

在 "./confgiure" 时, 有可能遇到

> configure: error: in `/root/download/freeradius-server-3.0.1': configure: error: failed locating OpenSSL headers

和

> configure: WARNING: talloc headers not found. Use --with-talloc-include-dir=<path>. configure: error: FreeRADIUS requires libtalloc

需要先执行

```
yum install openssl-devel 
yum install libtalloc-devel
```

在配置 FreeRADIUS 时, mysql 和 ldap 是两个常用的模块. 在安装 FreeRADIUS 前, 需要先执行

```
yum install mysql-devel 
yum install openldap-devel
```

否则在运行 FreeRADIUS 时会报类似下面的错误

> Could not link driver rlm_sql_mysql: rlm_sql_mysql.so: cannot open shared object file: No such file or directory

为了安全需要, 要记录 FreeRADIUS 的验证记录.

配置在 radius.log 中记录失败的验证的方法是, 修改 radiusd.conf 中

```
auth = yes 
auth_badpass = yes
```