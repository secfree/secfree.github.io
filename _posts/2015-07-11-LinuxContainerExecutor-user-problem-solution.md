---
layout: blog
title: "LinuxContainerExecutor 中 user 问题解决"
---

# 问题

<br />

`Hadoop-2.6.0` 集群在 `Secure Mode` 下, YARN 的 NodeManager 必须使用 LinuxContainerExecutor . 而 LinuxContainerExecutor  要求执行任务的 user 存在于 NodeManger 所在的机器上. 如果 user 不存在, 则会报错:

> user test not found

当有大量的机器作为计算节点运行 NodeManager, 同时集群的使用 user 数目又比较多时, 在每个节点上的 `/etc/passwd` 中都添加每个 user, 显然不是一种好的解决方法.

当然, 如果不需要在 YARN 中实现针对单个 user 的资源控制, 那使用统一的用户名就可以.

---

# 思路

<br />

1. 是否可以设置 LinuxContainerExecutor 使用默认 user ?
1. YARN 中是否可以设置统一的代理 user, 任务在 NodeManager 上以代理 user 执行 ?
1. 是否可以配置 NodeManager 机器不使用 LDAP 认证, 但是可以查找到 LDAP 用户 ?
1. 采用脚本在每个 nodemanager 上添加用户.

Check 结果:

1. 在 [`yarn-default.xml`](http://hadoop.apache.org/docs/r2.6.0/hadoop-yarn/hadoop-yarn-common/yarn-default.xml) 中查找含有 `linux-container-executor` 的项, 发现只能在 `nonsecure-mode` 中设置使用默认的本地 user, 而在 `Secure Mode` 中并不生效.

1.  [`yarn-default.xml`](http://hadoop.apache.org/docs/r2.6.0/hadoop-yarn/hadoop-yarn-common/yarn-default.xml) 中查找含有 `proxy` 的项, 发现其中的代理功能只能代理提交任务的 user 更新 Kerberos 的 ticket, 并没有发现可以统一使用同一个代理 user 的功能.

1. `采用脚本在每个 nodemanager 上添加用户` 这种方法肯定是可行的, 但是运维和管理比较麻烦. 比如批量添加有失败时的处理问题.

下面介绍第 3 中思路的实现.

---

# LDAP 中查找 user

<br />

## 启用选项

<br />

首先搜索怎样部署 Linux 使用 LDAP 账户登录, 相关资源比较多. 环境中统一使用的是 CentOS 的系统, 个人查阅的 page 中这篇说的最为简明合适: [Configuring LDAP Authentication on CentOS 6.0](http://wiki.centos.org/AdrianHall/CentralizedLDAPAuth) .

page 中使用 `authconfig` 来启用 LDAP 账户登录 Linux 系统, 查看其中的两个选项有:

```
--enableldap          enable LDAP for user information by default
...
--enableldapauth      enable LDAP for authentication by default
```

很明显, 只需要启用 `--enableldap`, 不启用 `--enableldapauth` 就可以实现 `不使用 LDAP 认证, 但是可以查找到 LDAP 用户` .

---

## 基本安装

<br />

在节点上执行:

```
$ yum install nss-pam-ldapd
$ authconfig --enableldap --ldapserver=ldap://test.com/ --ldapbasedn="cn=TEST.COM,cn=krbcontainer,dc=test,dc=com" --disablecache --disablecachecreds --disablefingerprint --kickstart
$ service nscd restart && service nslcd restart
```

如果 ldapbasedn 并非对所有的用户设置有访问权限, 则还需要在 `/etc/nslcd.conf` 中设置:

```
binddn cn=admin,dc=test,dc=com
bindpw password
```

测试:

```
$ id testuser
```

---

## `/etc/nslcd.conf`

<br />

个人上面的测试结果是:

```
$ id testuser
id: testuser: No such user
```

查看 LDAP server 的日志, 发现执行的搜索是:

```
(&(objectClass=posixAccount)(uid=testuser))
```

因为个人的环境中配置的是 `LDAP + Kerberos`, LDAP 中的相关 user 都是使用 Kerberos 的 `addprinc` 命令添加的. 具有的格式如下:

```
# testuser@TEST.COM, TEST.COM, krbcontainer, test.com
dn: krbPrincipalName=testuser@TEST.COM,cn=TEST.COM,cn=krbcontainer,dc=test,dc=com
krbPrincipalName: testuser@TEST.COM
...
objectClass: krbPrincipal
objectClass: krbPrincipalAux
objectClass: krbTicketPolicyAux
```

显然搜索项和用户属性不匹配.

发现 `/etc/nslcd.conf` 中有对 search 的相关设置, 执行 `man nslcd.conf` 查看帮助文档, 在 `/etc/nslcd.conf` 中设置:

```
filter passwd (objectClass=krbPrincipal)
map passwd uid krbPrincipalName
```

设置之后, 测试确认搜索的 filter 为:

```
(&(objectClass=krbPrincipal)(krbPrincipalName=testuser))
```

但是 username 的后置 `@TEST.COM` 始终不能匹配, 因此也就还是找不到对应的 user. (`/etc/nslcd.conf` 中 `ATTRIBUTE MAPPING EXPRESSIONS` 和 `pam_authz_search` 都不能设置)

---

## 更改 LDAP user 添加方式

<br />

因此, 只能更改 LDAP 中 Kerberos 添加的用户的属性.

1. 新建 add_testuser.ldif

   ```
    dn: uid=testuser,cn=TEST.COM,cn=krbcontainer,dc=test,dc=com
    cn: testuser
    objectClass: posixAccount
    objectClass: inetOrgPerson
    uid: testuser
    sn: testuser
    uidNumber: 100010
    gidNumber: 100010
    homeDirectory: /home/testuser
   ```

1. 在 LDAP server 上执行

   ```
    ldapadd -x -D "cn=admin,dc=test,dc=com" -w password -f add_testuser.ldif
   ```

1. 从 Kerberos 中添加 user

   ```
    addprinc -x dn="uid=testuser,cn=TEST.COM,cn=krbcontainer,dc=test,dc=com" testuser
   ```

这样就基于 dn 添加了一个 Kerberos 关联的 user, 具有的属性如下:

```
dn: uid=testuser,cn=TEST.COM,cn=krbcontainer,dc=test,dc=com
cn: testuser
krbPrincipalName: testuser@TEST.COM
...
objectClass: posixAccount
objectClass: inetOrgPerson
objectClass: krbPrincipalAux
objectClass: krbTicketPolicyAux
uid: testuser
sn: testuser
uidNumber: 100010
gidNumber: 100010
homeDirectory: /home/testuser
```

再测试用户已经存在:

```
$ id testuser
uid=100010(testuser) gid=100010 groups=100010
```

注意, 需要去掉前面在 `/etc/nslcd.conf` 中设置的两行

```
filter passwd (objectClass=krbPrincipal)
map passwd uid krbPrincipalName
```

---

此时, 已经可以使用 testuser 提交任务到 YARN 执行成功.
