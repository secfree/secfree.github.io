---
layout: blog
title: "Linux 用 LDAP 授权时的访问控制"
---

LDAP 可以用来方便的实施集中式的授权(Authorize)和认证(Authenticate). 当 Linux  服务器 host\_A 配置有 LDAP 授权时, 如果不设置额外的访问控制, LDAP server 中的每一个 user 都能登录 host_A, 这在很多情况下是不适合的.

假如 host\_A 需要限制为 LDAP 只有 security 组的成员可以登录.

**LDAP server 和 client 配置**

在 LDAP server 添加:

```
#  host_A 的信息
dn: cn=secHost,ou=hosts,dc=test,dc=com
objectClass: device
ipNumber: 10.10.10.11
cn: secHost
member: uid=sec01,ou=user,ou=UNIX,dc=test,dc=com
member: uid=sec02,ou=user,ou=UNIX,dc=test,dc=com
member: uid=sec03,ou=user,ou=UNIX,dc=test,dc=com
```

其中 memeber 的信息:

```
dn: uid=sec01,ou=users,ou=UNIX,dc=test,dc=com
# gid 1017 对应 security 组
gidNumber: 1017
objectClass: account
uidNumber: 2165
uid: sec01
cn: sec01
```

需要在 host\_A 的 "/etc/pam_ldap.conf" 中添加:

```
# pam_groupdn 对应 LDAP server 中 host_A 的 dn 
pam_groupdn cn=secHost,ou=hosts,dc=test,dc=com
pam_member_attribute member
```

可以 host_A 只有 security 组成员可以访问. 但这种方案维护并不轻松:

- security 组添加一个成员, 就需要在每一个相应的 host 中添加 memeber .

- host_A 中有跟 ipNumber 相关, 如果 security 组新增一台机器, 则又需要在 LDAP server 配置一个 host 信息, 添加每个 security 组的 member .

`注: 因为个人测试配置时, 并没有操作 LDAP server 的权限, 因此没有尝试和研究其他在 LDAP server 的配置方式. 上面提及的局限通过其他方式配置可能并不存在.`

不少文档中有提到可以在 "/etc/pam\_ldap.conf" 或 "/etc/ldap.conf" 中设置 "pam_fiter"进行访问控制, 如:

```
# gid 1017 对应 security 组
pam_filter gidNumber=1017
```

但个人测试在只用 LDAP 进行授权而不用 LDAP 认证时, pam_filter 貌似比起作用.

**SSH 配置**

通过设置 SSH 的 AllowUsers 和 AllowGroups 来配置, 具体可以参考 [LDAP, SSH and Access Control on Linux](http://ruiz-ade.com/2011/02/20/ldap-ssh-and-access-control/) .
这种方法的不足之处是不能够只对 LDAP 用户访问控制, 有可能会影响到 /etc/passwd 中的用户.

**SSSD 配置**

具体可参考: [Creating Domains: Access Control](https://access.redhat.com/site/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Deployment_Guide/config-sssd-domain-access.html)

这个功能比较强大, 但需要安装 SSSD 服务, 而且 sssd.conf 中配置项貌似很多, 个人并没有进行测试.

**PAM 中配置**

前面的三种方法都是查阅所得, 这种是自己尝试得出, 个人认为也是最简洁和有效的.

在配置好 LDAP 授权后, host_A 的 "/etc/pam.d/sshd" 中会有:

```
include system-auth
```

而 "/etc/pam.d/system-auth" 的内容会含有:

```
auth        required      pam_env.so
auth        sufficient    pam_unix.so nullok try_first_pass
auth        requisite     pam_succeed_if.so uid >= 500 quiet
auth        sufficient    pam_ldap.so use_first_pass
auth        required      pam_deny.so

account     required      pam_unix.so broken_shadow
account     [default=bad success=ok user_unknown=ignore] pam_ldap.so
account     required      pam_permit.so
```

PAM 相关可参考:

- [pam.d(5) - Linux man page](http://linux.die.net/man/5/pam.d)
- [PAM 認 證 模 組](http://www.suse.url.tw/sles10/lesson20.htm)

如果需要限定 host_A 只能 security 组登录, 只需要添加一行:

```
account     required      pam_unix.so broken_shadow
account     [default=bad success=ok user_unknown=ignore] pam_ldap.so
# 添加在 account pam_ldap.so 后
# gid 1017 对应 security 组
account     required      pam_succeed_if.so gid = 1017 quiet
account     required      pam_permit.so
```

如果又需要限定出 host\_A 外, 还有 user_A 可以登录, 则这样添加一行:

```
account     required      pam_unix.so broken_shadow
account     [default=bad success=ok user_unknown=ignore] pam_ldap.so
# 添加在 account pam_ldap.so 后
# uid 2153 对应 user_A
account     sufficient    pam_succeed_if.so uid = 2153 quiet
# gid 1017 对应 security 组
account     required      pam_succeed_if.so gid = 1017 quiet
account     required      pam_permit.so
```


将 "gid = 1017" 限定添加在 "account pam_ldap.so" 的原因:

- 如果添加在 "account pam_ldap.so" 前 .
  这时还没有执行 "account ... pam\_ldap.so", 也就是 LDAP 还没有对用户进行授权. 这时只能有一个限定条件, 或者是一组需要同时满足的条件, 即只能为 "account required pam\_succeed\_if.so ..." . 如果有 "account sufficient ..." 则满足条件后就不会进行接下来的 "account ... pam_ldap.so", 用户不能得到授权.

- 如果添加在 auth 中 .
  添加在 "auth sufficient pam\_ldap.so use\_first\_pass" 前则同样不能满足复合条件的限定, 添加在其后则不会有作用, 因为 "auth pam\_ldap" 的 控制旗标(control_flags) 为 sufficient .

由上可知, 通过 "account sufficient pam\_succeed\_if.so ..." 和  "account required pam\_succeed\_if.so ..." 组合的控制的方法兼具灵活和方便, 可以满足很多需求. 个人实用效果还是很赞的.
