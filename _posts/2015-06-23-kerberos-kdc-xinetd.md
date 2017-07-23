---
layout: blog
title: "Kerberos 多 KDC 中使用 xinetd"
---

**多 KDC 的需要**

部署 Kerberos 认证服务时, 一般需要部署多个 KDC . 虽然 KDC 的 master, slave 切换很不方便, 包含下面的步骤

```
If the master KDC is still running, do the following on the old master KDC:

1. Kill the kadmind process.
2. Disable the cron job that propagates the database.
3. Run your database propagation script manually, to ensure that the slaves all have the latest copy of the database (see Propagate the database to each slave KDC).

On the new master KDC:

1. Start the kadmind daemon (see Start the Kerberos daemons on the master KDC).
2. Set up the cron job to propagate the database (see Propagate the database to each slave KDC).
3. Switch the CNAMEs of the old and new master KDCs. If you can’t do this, you’ll need to change the krb5.conf file on every client machine in your Kerberos realm.
```

但是 slave 的 KDC 在 master KDC 挂掉的时候, 还可以提供 Kerberos 的认证服务

> Slave KDCs provide Kerberos ticket-granting services, but not database administration, when the master KDC is unavailable.

---

**xinetd 配置**

Kerberos 的官方文档 [`Installing KDCs`](http://web.mit.edu/kerberos/krb5-latest/doc/admin/install_kdc.html#installing-kdcs) 已经很详细, 但里面提到需要在 `/etc/inetd.conf` 中添加:

```
krb5_prop stream tcp nowait root /usr/local/sbin/kpropd kpropd
```

现在的服务器上一般都是用 xinetd 而非 inetd, 使用 xinet 的设置方式如下:

1. 创建 `/etc/xinetd.d/krb5_prop` 文件, 内容为:

   ```
    service krb5_prop
    {
            disable         = no
            socket_type     = stream
            protocol        = tcp
            user            = root
            wait            = no
            server          = /usr/sbin/kpropd
    }
   ```

    service 的 name 和 `/etc/services` 中保持一致, 因此用 krb5_prop

   ```
    # grep krb /etc/services |grep prop
    krb5_prop       754/tcp         tell            # Kerberos slave propagation
   ```

    这篇文章 [MIT Kerberos V slave on Debian squeeze](http://www.rjsystems.nl/en/2100-d6-kerberos-slave.php) 中用 `kr_prop` 在 centos 6.3 上 kpropd 启动失败.

2. 执行 `service xinetd restart`.

---

**xinetd 介绍**

- 根据网络请求来调用相应的服务进程处理连接.
- xinetd监听它控制的守护进程所使用的所有端口。当请求一个连接时，xinetd会确定是否允许这个客户机访问。如果允许客户机访问，则xinetd启动所需的服务，并把客户机连接传递给它。
- 原则上任何系统服务都可以使用xinetd，然而最适合的应该是那些常用的网络服务，同时，这个服务的请求数目和频繁程度不会太高。 参考: [linux Xinetd服务简介](http://blog.csdn.net/cymm_liu/article/details/9372255)
