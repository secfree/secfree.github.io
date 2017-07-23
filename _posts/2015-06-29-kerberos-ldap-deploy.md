---
layout: blog
title: "Kerberos + OpenLDAP 配置"
---

Kerberos

> Kerberos is a network authentication protocol. It is designed to provide strong authentication for client/server applications by using secret-key cryptography.

LDAP

> Lightweight Directory Access Protocol

用 LDAP 来做账户管理, 用 Kerberos 来认证, 是一种常见的方案.

网上有很多集成两者的文档, 个人查阅的当中, Kerberos 官方给出的: [Configuring Kerberos with OpenLDAP back-end](http://web.mit.edu/kerberos/krb5-1.13/doc/admin/conf_ldap.html) 最有条理, 详细解释了每一步的作用. 不过这个文档没有给出执行操作的细节, 我这里补充一下, 必要时可以做一个参考.

---

# SASL

<br />

在 OpenLDAP 编译 configure 的时候, 需要先:

```
yum install cyrus-sasl-ldap
```

然后

```
./configure --prefix=/... --with-cyrus-sasl
```

---

# kerberos.schema

<br />

`kerberos.schema` 可以用两种方式得到:

1. `yum install krb5-server-ldap`, 会有: `/usr/share/doc/krb5-server-ldap-x.x/kerberos.schema` .
1. 下载 Kerberos 的源码包, 包中含有该文件.

---

# 在 LDAP 中创建 kdc 和 kadmin 的 dn

<br />

创建 `add_kdc_kadmin.ldif` 文件, 内容为:

```
dn: uid=kadmind,ou=people,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
userPassword: kadmind_pass
cn: LDAP admin account
uid: kadmind
sn: kadmind
uidNumber: 1002
gidNumber: 100
homeDirectory: /home/ldap
loginShell: /bin/bash

dn: uid=krb5kdc,ou=people,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
userPassword: kdc_pass
cn: LDAP admin account
uid: krb5kdc
sn: krb5kdc
uidNumber: 1003
gidNumber: 100
homeDirectory: /home/ldap
loginShell: /bin/bash
```

在 LDAP server 上执行

```
./bin/ldapadd -x -D "cn=root,dc=example,dc=com" -w secret -f add_kdc_kadmin.ldif -H ldap:///
```

上面的步骤已经在 LDAP 中创建了要给 Kerberos 的 kdc 和 kadmin 使用的 dn.

---

# 为 kdc 和 kadmin 的 dn 生成密码文件

<br />

文档中有

> their passwords can be stashed with “kdb5_ldap_util stashsrvpw” and the resulting file specified with the ldap_service_password_file directive.

`kdb5_ldap_util stashsrvpw`

> Allows an administrator to store the password for service object in a file so that KDC and Administration server can use it to authenticate to the LDAP server.


在 Kerberos 的 kdc 上执行下面的语句并分别输入在 `add_kdc_kadmin.ldif` 中配置的对应的密码

```
kdb5_ldap_util stashsrvpw -f /var/kerberos/krb5kdc/ldap.stash "uid=kadmind,ou=people,dc=example,dc=com"
kdb5_ldap_util stashsrvpw -f /var/kerberos/krb5kdc/ldap.stash "uid=krb5kdc,ou=people,dc=example,dc=com"
```

---

# 设置 kdc 和 kadmin 的 dn 的 ACL

<br />

选择的 `global Kerberos container` 的 dn 为:

```
cn=krbcontainer,dc=example,dc=com
```

`realm container` 直接创建在 `global Kerberos container` 下面, 则有:

```
access to dn.base=""
    by * read

access to dn.base="cn=Subschema"
    by * read

access to attrs=userPassword,userPKCS12
    by self write
    by * auth

access to attrs=shadowLastChange
    by self write
    by * read

# Providing access to realm container
access to dn.subtree="cn=EXAMPLE.COM,cn=krbcontainer,dc=example,dc=com"
    by dn.exact="uid=krb5kdc,ou=people,dc=example,dc=com" read
    by dn.exact="uid=kadmind,ou=people,dc=example,dc=com" write
    by * none

access to *
    by * read
```

添加上面的内容到 LDAP 的 `slapd.conf` 中并重启 slapd.

---

# kdc.conf 样本

<br />

```
[kdcdefaults]
 kdc_ports = 88
 kdc_tcp_ports = 88

[realms]
 DATA.SCLOUD = {
  #master_key_type = aes256-cts
  acl_file = /var/kerberos/krb5kdc/kadm5.acl
  dict_file = /usr/share/dict/words
  admin_keytab = /var/kerberos/krb5kdc/kadm5.keytab
  supported_enctypes = aes128-cts:normal des3-hmac-sha1:normal arcfour-hmac:normal des-hmac-sha1:normal des-cbc-md5:normal des-cbc-crc:normal
  database_module = openldap_ldapconf
 }

[dbdefaults]
    ldap_kerberos_container_dn = cn=krbcontainer,dc=example,dc=com

[dbmodules]
  openldap_ldapconf = {
    db_library = kldap
    ldap_kdc_dn = uid=krb5kdc,ou=people,dc=example,dc=com
    ldap_kadmind_dn = uid=kadmind,ou=people,dc=example,dc=com
    ldap_service_password_file = /var/kerberos/krb5kdc/ldap.stash
    ldap_servers = ldap://ldap_hostname/
    ldap_conns_per_server = 5
  }
```

---

# 创建 container

<br />

在 Kerberos kdc 上执行

```
kdb5_ldap_util -D cn=root,dc=example,dc=com create  -r EXAMPLE.COM -s -H ldap://ldap_hostname/
```

会同时创建 global 以及 realm container.

---

# 测试

<br />

在 Kerberos kdc 添加测试用户

```
$ kadmin.local
Authenticating as principal ...
kadmin.local:  addprinc my_test
...
kadmin.local:  exit
```

用 kinit 测试添加的用户

```
$ kinit my_test
```

在 LDAP server 上查看

```
$ slapcat |grep "my_test"
dn: krbPrincipalName=my_test@EXAMPLE.COM,cn=EXAMPLE.COM,cn=krbcontainer,dc=example,dc=com
krbPrincipalName: my_test@EXAMPLE.COM
```

---

# 问题记录

<br />

1. 使用 ldapsearch 报错:

   ```
    ldap_sasl_bind(SIMPLE): Can't contact LDAP server (-1)
   ```

    可以尝试将 slapd 启动时 '-h' 参数 `ldapi:///` 改为 `ldap:///`.

    ldapi: [Enabling LDAPI](https://access.redhat.com/documentation/en-US/Red_Hat_Directory_Server/8.2/html/Administration_Guide/ldapi-enabling.html)

1. 创建 `container` 时报错:

   ```
    Kerberos container is missing. Creating now...
    kdb5_ldap_util: Kerberos Container create FAILED: No such object while creating realm 'EXAMPLE.COM'
   ```

    有可能是 `ldap_kerberos_container_dn` 设置的问题. 例如你的 `ldap_kerberos_container_dn` 为

   ```
    cn=krbcontainer,cn=root,dc=example,dc=com
   ```

    会报错, 但是为

   ```
    cn=krbcontainer,dc=example,dc=com
   ```

    就没有问题.

1. 创建 `container` 时报错:

   ```
    kdb5_ldap_util: Kerberos Container create FAILED: Invalid syntax while creating realm 'EXAMPLE.COM'
   ```

    有可能是没有在 `slapd.conf` 中添加正确的

   ```
    include    /path/to/kerberos.schema
   ```
