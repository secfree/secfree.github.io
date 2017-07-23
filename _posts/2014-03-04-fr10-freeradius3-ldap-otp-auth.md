---
layout: blog
title: "FR10: FreeRADIUS 3 配置 LDAP + OTP 双因素验证"
---

前面已经提交怎样分别配置 LDAP 和 MOTP 验证, 本文基于它们已有的配置.

如果发起验证请求端由自己编程实现, 则配置双因素验证很简单. 

1. 在 FreeRADIUS 中将 LDAP 和 MOTP 验证分别配置一个有效的 virtual server, 
     设为 ldap\_site, motp_site.

2. 将 ldap 的验证信息 username, password 发送到 ldap_site, 
     如果返回 Reject 则验证失败, 返回 Accept 进入下一步.

3. 将 username, otp 发送到 motp_site, 
     如果返回 Reject 则验证失败, 返回 Accept 则验证成功.

但很多时候, 给一些特定的程序配置认证时, 只能设置一个 "ip:port", 

例如这次我们测试的 juniper vpn, 这个时候的验证流程是

1. client 发送 ( username, password ) 到 FreeRADIUS (FR)  server;

2. FR验证 (username, password) 有效, 则返回一个 "Access-Challenge".
     如果无效, 则验证失败.

3. client 接到 "Access-Challenge", 让用户输入 Challenge, 此处为 OTP .
     然后将 (username, OTP) 发送到 FR .

4. FR 验证 OTP, 如果有效, 则验证成功,否则验证失败.

此时 virtual server 的配置应该为

```
server ldap_otp {
        listen {
            ipaddr = 0.0.0.0
            port = 1827
            type = auth
        }
       
        authorize {
            if (State) {
                -sql
                files
                update {
                    control:Auth-Type := motp
                }
            }
            else {
                update {
                    control:Auth-Type := ldap
                }
            }
        }
       
        authenticate {
            Auth-Type ldap {
                ldap
                if (ok) {
                    update {
                        control:Response-Packet-Type := Access-Challenge
                        reply:Reply-Message = "OTP"
                        # 根据 RFC 2865, client 端不能修改 server 端设置的 State .
                        # 因此, 可用 State 区别验证 ldap 和 otp
                        reply:State = "2"
                    }
                }
            }
       
            Auth-Type motp {
                motp
            }
        }
       
        post-auth {
                Post-Auth-Type Reject {
                }
        }
}
```

*注: 这个配置花了我很长时间的, 有不少人在 maillist 中向 FR 的开发者提过达到同样需求的问题, 开发者都是建议提问者自己去写模块实现.*
    
*可参考 unlang (FR的配置语言, "man unlang") 和 RFC 2865 radius 协议.*
