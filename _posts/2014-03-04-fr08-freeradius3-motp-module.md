---
layout: blog
title: "FR08: FreeRADIUS 3 中 MOTP 验证采用 module"
---

前面提到的配置 MOTP 验证是在 users 文件中设置, 有时候需要将 MOTP 和其他认证联合起来配置双因素认证. 这个时候将 MOTP 配置为一个单独的 module 更为方便.

以下为配置步骤:

1. 创建 motp module

    在 mods-available/ 下新建 motp 文件

   ```
   exec motp {
       wait = yes
       program = "/usr/local/bin/otpverify.sh %{User-Name} %{User-Password} %{reply:Secret} %{reply:Pin} %{reply:Offset}"
       input_pairs = request
       output_pairs = reply
   }
   ```

    在 mods-enabled/ 下执行 "ln -s ../mods-availabe/motp ."

2. 创建测试 motp module 的 virtual server

    在 sites-available/ 下新建 site_motp 文件

   ```
   server site_motp {
            listen {
                ipaddr = 0.0.0.0
                port = 1828
                type = auth
            }
            client local {
                ipaddr  = 127.0.0.1
                secret  = testing123
            }
            authorize {
                -sql
                files
                update {
                    control:Auth-Type := motp
                }
            }
            authenticate {
                # define Auth-Type motp in authenticate, referenced in authorize
                Auth-Type motp {
                    motp
                }
            }
            post-auth {
                    Post-Auth-Type Reject {
                            update reply {
                                    Reply-Message = "Otp authenticate. "
                            }
                    }
            }
   }
   ```

    在 sites-enabled/ 下执行 "ln -s ../sites-available/site_motp ."

3. 测试

    启动 "radiusd -X" .

    在另一个 shell 中执行执行

   ```
   $ radtest user_name otp localhost:1828 0 testing123
   ```

    如果收到 "Access-Accept"则证明配置成功.
