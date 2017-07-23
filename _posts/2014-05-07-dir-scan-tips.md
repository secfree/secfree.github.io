---
layout: blog
title: 周期性 Web 目录扫描 Tips
---

从网络安全来讲, 对服务器上的 Web 目录进行周期性 webshell 扫描是有必要的. 这里从编程的角度记录几点经验.

- #### **资源占用**

    即使是单进程的扫描, 在遍历含有大量文件的目录或者连续扫描大量的脚本文件时, 也会造成很高的 IO 和 CPU 占用. 而安全方面不应该对服务造成影响. 需要在程序中设置遍历 M 个文件 sleep 一秒, 扫描 N 个脚本文件 sleep 一秒, 这样可以很有效的降低 IO 和 CPU 占用. 至于合适的 M, N 值, 需要在实际的程序中测试.

- #### **文件编码检测**

    很多脚本文件中会含有中文, 常以 gb2312 和 utf-8 两种编码形式存在, 还有可能存在一些特殊的编码文件. 如果只是直接打开读取文件内容, 在匹配各种 pattern 时有可能会出现意料之外的事情. 因此需要确保读取的内容为 utf-8 编码, 以下为 Python 中一种读取文件内容的方式:

  ```python
    with open(path) as f:
        content = f.read()
    f_encoding = chardet.detect(content)['encoding']
    if not f_encoding:
        return
    if f_encoding not in ['utf-8', 'ascii']:
        content = content.decode(f_encoding, 'ignore').encode('utf-8', 'ignore')
  ```

- #### **部署方式**

    现在很多扫描程序都是用 Python 实现, 但不同的服务器上 Python 版本经常不一致. 另外程序可能用到一些 Python 没有自带的库, 从源码或直接用 yum 安装都可能碰到一些特殊问题.

    另一种可以选择的方式是使用 PyInstaller 将 Python 源码转为可执行程序, 这样在服务器上就可以直接执行. 需要注意的一点是尽量在 glibc 版本为较低的机器上用 PyInstaller 生成可执行程序. PyInstaller 生成的可执行程序依赖于所在机器的 glibc 版本, 而 glibc 版本向后兼容. 以 RedHat 为例, RHEL5.4 的 glibc 版本为 2.5, 而 RHEL6.3 的 glibc 版本为 2.12, 因此需要在 RHEL5.4 上生成才能通用.
