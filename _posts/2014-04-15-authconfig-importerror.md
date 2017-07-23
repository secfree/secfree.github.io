---
layout: blog
title: "authconfig 报 \"ImportError: No module named acutil\" 解决方法"
---

用 yum 安装 authconfig 后, 如果 Linux 系统原本自带的 Python 版本不是 2.7, 而 "/usr/bin/python" 版本为 2.7 时, 执行 authconfig 有可能报下面的错误:

```
Traceback (most recent call last):
    File "/usr/sbin/authconfig", line 27, in <module> 
        import authinfo, acutil 
    File "/usr/share/authconfig/authinfo.py", line 35,in <modulle> 
        import dnsclient 
    File "/usr/share/authconfig/dnsclient.py", line 23,in <module> 
        import acutil 
    ImportError: No module named acutil
```

可以有下面的解决方法:

- 将 "/usr/bin/python" 替换为 Linux 系统自带的 Python 版本, 重新用 yum 安装 authconfig .

    如果在 "/usr/bin/python" 随时可能被调用的服务器上, 这种方法是不可取的.

- 用自带的 Python 版本执行 authconfig .

    可用下面的命令得到 yum 所用的 Python 版本执行

    ```bash
    pv=$(head /usr/bin/yum -n 1) 
    py=${pv:2} 

    # 不带 option 执行可能会报错
    py authconfig options
    ```