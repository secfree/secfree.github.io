---
layout: blog
title: "Hadoop 配置 Data Transfer Protocol 使用 SASL 认证"
---

# Hadoop 2.6.0 特性

<br />

Hadoop 2.5.2 的 Secure Mode 下, 必须用 root 通过 jsvc 启动 DataNode, 运维不方便.

Hadoop 2.6.0 中有:

> As of version 2.6.0, SASL can be used to authenticate the data transfer protocol. In this configuration, it is no longer required for secured clusters to start the DataNode as root using jsvc and bind to privileged ports.

参考 [Hadoop in Secure Mode](http://hadoop.apache.org/docs/r2.6.0/hadoop-project-dist/hadoop-common/SecureMode.html#DataNode)

---

# hdfs-site.xml 配置

<br />

按照 [Hadoop in Secure Mode](http://hadoop.apache.org/docs/r2.6.0/hadoop-project-dist/hadoop-common/SecureMode.html#DataNode) 说明, 应该在 `hdfs-site.xml` 中添加

```xml
<property>
  <name>dfs.data.transfer.protection</name>
  <value>authentication</value>
</property>
<property>
  <name>dfs.http.policy</name>
  <value>HTTPS_ONLY</value>
</property>
<property>
  <name>dfs.datanode.address</name>
  <value>0.0.0.0:8030</value>
</property>
```

其中 `dfs.datanode.address` 中的端口号需要大于 1024, 否则需要 root 权限启动.

此时直接启动 Hadoop, 会报下面的错:

```
FileNotFoundException: /home/hadoop/.keystore
```

原因是因为上面配置了使用 `HTTPS_ONLY`, 但是并没有配置加密用的证书.

---

# SSL 证书生成和签名

<br />

这篇 Hadoop 开发者写的 [How to configure HTTPS for HDFS in a Hadoop cluster](http://hortonworks.com/blog/deploying-https-hdfs/) 对在 HDFS 中部署 HTTPS 的步骤和 SSL 证书加密和签名的原理已经说的很清楚, 但是其中 `keytool` 工具在为 `jdk 1.7.0` 所带的版本时, 参数使用有所不同. 这里列一下我执行的命令.

选定一台机器作为签名的 server, 在上面创建 CA 的 key 和 cert

```
openssl req -new -x509 -keyout test_ca_key -out test_ca_cert -days 9999 -subj '/C=CN/ST=shanghai/L=shanghai/O=test_company/OU=security/CN=ca.test.com'
```

接下来的操作, 需要在集群中的每台机器上执行, 或者是把每台机器上的 cert 拿到 CA server 上签名.

1. 生成 keystore

   ```
    keytool -keystore keystore -alias localhost -validity 9999 -genkey -keyalg RSA -keysize 2048 -dname "CN=test01.com, OU=test, O=test, L=shanghai, ST=shanghai, C=cn"
   ```

    注意: `-keyalg RSA -keysize 2048` 是必须的, 否则默认会生成 DSA 类型的. 而现在的浏览器一般不支持 DSA 类型, 当用浏览器访问 https 的 NameNode web 服务时, 可能不能访问.

1. 添加 CA 到 truststore

   ```
    keytool -keystore truststore -alias CARoot -import -file test_ca_cert
   ```

1. 从 keystore 中导出 cert

   ```
    keytool -certreq -alias localhost -keystore keystore -file cert
   ```

1. 用 CA 对 cert 签名

   ```
    openssl x509 -req -CA test_ca_cert -CAkey test_ca_key -in cert -out cert_signed -days 9999 -CAcreateserial -passin pass:password
   ```

1. 将 CA 的 cert 和用 CA 签名之后的 cert 导入 keystore

   ```
    keytool -importcert -alias CARoot -file test_ca_cert -keystore keystore
    keytool -importcert -alias localhost -file cert_signed -keystore keystore
   ```

---

# SSL 配置

<br />

hadoop 的 `etc/hadoop/` 下已经有 `ssl-client.xml.example` 和 `ssl-server.xml.example`, 分别复制为 `ssl-client.xml`, `ssl-server.xml` 并填写其中的配置项就可以.

这样就可以直接用 hadoop 账户启动 Secure Mode 下的 DataNode, 比之前方便不少.

---

# 参考

<br />

- [Set up WebHDFS/YARN with SSL (HTTPS)](http://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.0.9.0/bk_reference/content/ch_wire-yarnssl.html)

- [数字证书及 CA 的扫盲介绍](http://program-think.blogspot.com/2010/02/introduce-digital-certificate-and-ca.html)
