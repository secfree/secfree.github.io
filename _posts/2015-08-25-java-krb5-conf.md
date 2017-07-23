---
layout: blog
title: "Java 环境 krb5.conf 的设置"
---

个人 Java 程序需要 Kerberos 认证, 但是是在集群的环境中用 Map-Reduce 跑, 没有权限修改默认的 `/etc/krb5.conf` 文件.

刚开始我参考的这个: [Kerberos Environment variables](http://web.mit.edu/kerberos/krb5-current/doc/krb_admins/env_variables.html), 设置 `KRB5_CONFIG`

```
export KRB5_CONFIG=/path/to/krb5.conf
```

程序运行中, 报各种错, 如:

```
javax.security.auth.login.LoginException: Unable to obtain password from user
```

以及

```
javax.security.auth.login.LoginException: java.lang.IllegalArgumentException: Illegal principal name ..
...
org.apache.hadoop.security.authentication.util.KerberosName$NoMatchingRule: No rules applied to
```

各种 check 和测试都未能解决.

偶然发现修改系统的 `/etc/krb5.conf` 后, 程序能正常运行, 由此确定 Java 环境并没有使用 `KRB5_CONFIG` 环境变量.

在 [Java Kerberos Requirements](https://docs.oracle.com/javase/7/docs/technotes/guides/security/jgss/tutorials/KerberosReq.html) 找到生效的配置方式, 将 `krb5.conf` 放在 `$JAVA_HOME/jre/lib/security/` 下即可.

当然, 也可以通过 java 命令行参数设置:

```
-Djava.security.krb5.conf=./conf/krb5.conf
```

安全相关的设置实在是比较繁琐, 而 Kerberos 认证很多时候很难从报错找到实际的对应原因. 从目前来看, 安全很多地方在运维和开发中维护都不容易. 可见任重而道远.
