---
layout: blog
title: 上海电信华为 HG8240R 更改用户限制数
---

新工作在张江高科这边, 原来住在松江, 相隔太远, 只好在浦东再租了一间房. 看着小小的合租的一个单间, 感觉又回到了两年前. 新的工作也的确是重新开始. 向旁边住的一哥们问到 wifi 密码. 连接后, 手机可以正常上网, 笔记本不能访问网页. 怀疑路由对同时上线的用户数有限制, 断开手机连接, 笔记本立马可以正常访问网页.
用浏览器打开 "http://192.168.1.1", 显示"我的e家"登录页面, 用的是华为的 HG8240R .电信总干这种事情, 强烈鄙视.

Google 搜索到 [上海电信 华为HG8240R 光猫 破解](http://www.cnblogs.com/anan/archive/2011/12/25/2301057.html) , 按照里面的操作.

```
telent 192.168.1.1
Login: root
Password: admin
```

但是 " grep telecomadmin /mnt/jffs2/hw_ctree.xml" 为空.
在 "/mnt/jffs2/" 下执行 "grep UserName * " 也为空. 
"/html " 下有登录控制的文件, 但是在系统中找不到可以下载文件的命令, 直接在终端上用 cat 看代码太吃力, 放弃.

在 WAP (telnet 登录成功后, 输入 shell 前)中输入 ? , 显示

```
acc add typelimit
acc del typelimit
acc get accesslimit
acc get typelimit
acc set accesslimit
acc set typelimit
...
```

再执行:

```
WAP>acc get accesslimit                                
Accesslimit Info:
    Mode=GlobalLimit
    TotalTerminalNumber=6
```

可见默认只能同时 6 个终端使用网络. 修改限制:

```
WAP>acc set accesslimit ?
acc set accesslimit mode[Off|GlobalLimit|TypeLimit] totalnumber[int]
WAP>acc set accesslimit mode GlobalLimit totalnumber 30
success!
WAP>acc get accesslimit                                
Accesslimit Info:
    Mode=GlobalLimit
    TotalTerminalNumber=30
WAP>quit
success!
```

至此手机, 笔记本, ipad都可上网, 证明修改生效.