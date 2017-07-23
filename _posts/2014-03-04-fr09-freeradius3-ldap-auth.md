---
layout: blog
title: "FR09: FreeRADIUS 3 配置 LDAP 验证"
---

在公司中, 使用域帐号登录的需求是很常见的.

以下为使用 FreeRADIUS 验证 LDAP 帐号的流程示意图:

            Request
    Client  ----->   FreeRADIUS server  <--> LDAP
            <-----
             Reply

1. 编辑 mods-available/ldap

    {% raw %}

   ```
   ldap {
       server = "server_ip"
       port = 389

       # php编程和ldap通信,
       # ldap_config['base_dn'] 相当于此处的 identity
       # ldap_config['search_dn'] 相当于此处的 base_dn

       identity = "CN=oaclient,CN=Users,DC=qiyi,DC=domain"
       password = password

       base_dn = "ou=qiyi,dc=qiyi,dc=domain"

       update {
               control:Password-With-Header    += 'userPassword'

               # 设置此处可以将相应LDAP信息添加到 reply:Reply-Message 中
               # 使用其他方法查看得到LDAP的属性名称,在此不一定完全有效
               reply:Reply-Message += 'distinguishedName'
       }

       edir = no
       user {
               base_dn = "${..base_dn}"
               filter = "(sAMAccountName=%{%{Stripped-User-Name}:-%{User-Name}})"
       }

       # 其余采用默认配置即可
   }
   ```

    {% endraw %}

    在 mods-enabled/ 下执行 "ln -s ../mods-available/ldap ."

2. 创建 virtual server

    在 sites-availab/ 下创建  site_ldap

   ```
   server site_ldap {
       listen {
           ipaddr = 0.0.0.0
           port = 1833
           type = auth
       }
       authorize {

           update {
               control:Auth-Type := ldap
           }

       }
       authenticate {

           # define Auth-Type ldap here
           Auth-Type ldap {
               ldap
           }

       post-auth {
           Post-Auth-Type Reject {
           }
       }
   }
   ```

   Note:

   1. 直接在 authorize 中调用 ldap, 会用先前在 mods-availabe/ldap 中配置的 identity, password 在 base_dn 下搜索验证的用户. 并在返回的内容中取用户的 password 和验证时提供的 password 比较. 但 LDAP 为了安全考虑, 并不会返回用户的 password 属性. 因此这种方法并不适用.
   1. 当设置 update{ control:Auth-Type := ldap } 时, 是采用 "bind as user" 的方式, 即将用户验证提供的用户名和密码发送到 LDAP 验证是否有效.

   在 sites-enabled/ 下执行 `ln -s ../sites-available/site_ldap .`

3. 测试

    执行

   ```
   $ radtest username password localhost:1833 0 testing123
   ```

    返回 "Access-Accept" 证明配置 OK.

    如果在  mods-available/ldap  中有设置 "reply:Reply-Message += 'distinguishedName'",

    则返回的内容中会多一条 Reply-Message .
