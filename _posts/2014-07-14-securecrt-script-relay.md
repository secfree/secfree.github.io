---
layout: blog
title: SecureCRT 脚本在有 relay 时的应用
---

公司出于安全方面的考虑, 很多时候都会限制 host 从 relay 登陆, 并且经常需要经过多层 relay. 而保存的 SecureCRT Tab 只能够连接到第一级的 host . 这会导致每次都需要在 relay 上输入 host 以及用户密码信息, 比较麻烦.

可以用脚本实现:
1. Clone 当前的 Tab .
2. 给新的 Tab 重命名.
3. 发送命令到 Tab, 如登陆 host, 输入密码, 切换路径.

以下是示例代码:

```
#$language = "VBScript"
#$interface = "1.0"

crt.Screen.Synchronous = True

'' This automatically generated script may need to be
'' edited in order to work correctly.

Sub Main
    Set oldTab = crt.GetScriptTab
    '' Clone Tab
    Set newTab = oldTab.Clone
    newTab.Activate
    '' 设置 Tab 名称
    newTab.Caption = "Test_Host"
    '' 登陆 host
    newTab.Screen.Send "ssh  root@test-host.test.com" & chr(13)
    newTab.Screen.WaitForString "password: ",100
    '' 设置登陆 密码
    newTab.Screen.Send "mypassword" & chr(13)
    newTab.Screen.WaitForString "$ ",100
    '' 切换路径
    newTab.Screen.Send "cd /home/user/" & chr(13)
End Sub
```

这样在登陆第一个 relay 后, 选择 "脚本"->"执行"->"test.vbs", 就可以登陆到 test-host .

给常用的 host 都实现一个脚本, 在工作中, 省很多事情.

参考:
[SecureCRT脚本:Clone Current Tab Script](http://hi.baidu.com/suther/item/52445901b0011814addc700d)