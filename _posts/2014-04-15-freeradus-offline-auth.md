---
layout: blog
title: "FreeRADIUS 离线认证"
---

我们可以用 [pam_radius](ftp://ftp.freeradius.org/pub/radius/pam_radius-1.3.17.tar.gz) 配置 Linux 系统登录用 FreeRADIUS 认证. 为了保证稳定性, 有一个普遍的需求是: 当 FreeRADIUS server 挂掉或者网络故障导致登录的机器无法连接 FreeRADIUS server 时, 系统采用本地或其他认证方式.

幸运的是, pam\_radius 本身自带这个功能, [pam_radius Usage](https://github.com/FreeRADIUS/pam_radius/blob/master/USAGE) 中的 localifdown 参数:

> This option tells pam_radius to return PAM_IGNORE instead of PAM_AUTHINFO_UNAVAIL if RADIUS auth failed due to network unavailability. PAM_IGNORE tells the pam stack to continue down the stack regardless of the control flag.

因此, 我们只需要在 PAM 中这样配置:

```
# auth sufficient
auth [default=die success=done ignore=ignore] pam_radius_auth.so localifdown
```

或者

```
# auth required
auth [default=die success=ok ignore=ignore] pam_radius_auth.so localifdown
```