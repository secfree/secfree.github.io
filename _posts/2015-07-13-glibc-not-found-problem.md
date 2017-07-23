---
layout: blog
title: "GLIBC not found 问题"
---

在直接运行下载的已经编译好的程序时, 可能会遇到 `version GLIBC_X.XX not found` 的问题, 原因是程序编译时使用了高于系统支持的 glibc 版本.

解决的方法一般有三种:

1. 升级系统的 glibc.
1. 安装需要的 glibc 版本到系统的非默认目录.
1. 在系统上重新编译程序.

---

# 升级 glibc

<br />

glibc introduction

> The C language provides no built-in facilities for performing such common operations as input/output, memory management, string manipulation, and the like. Instead, these facilities are defined in a standard library, which you compile and link with your programs.

> The GNU C Library, described in this document, defines all of the library functions that are specified by the ISO C standard, as well as additional features specific to POSIX and other derivatives of the Unix operating system, and extensions specific to GNU systems.

可见 glibc 是系统中最底层的 API, 也有人说:

> glibc is your OS .

普遍不推荐升级系统的 glibc 版本, 说是容易导致系统不稳定. 我曾经升级后也的确遇到问题.

但是也有人说:

> Updating glibc to a version supported by your distribution is low-risk. It is written to handle compatibility with versions that date far back, and (baring bugs) a new version should just be a drop in replacement. Installing a new version in some strange place is riskier, IMHO.

不过估计步骤也是会比较麻烦, 有细节需要注意, 所以还是不推荐.

---

# 安装 glibc

<br />

安装需要的 glibc 版本到系统的非默认目录, 这篇文章已经很详细: [解决libc.so.6: version `GLIBC_2.14' not found问题](http://blog.csdn.net/cpplang/article/details/8462768) .

但是有时候并不能解决问题, 原因如下:

> A statically linked executable already includes code for all the C library calls it needs to make, so you cannot separately compile a new glibc and link the executable to it. However, programs using glibc are never completely static: some library calls (all those connected with the "Name Service", i.e., getuid() and similar) still make use of dynamically-loaded modules .

据说这个 [rtldi -- indirect runtime loader](http://www.bitwagon.com/rtldi/rtldi.html) 可以解决两个 glibc 版本并存遇到的问题, 但是这是一个专家级的工具, 没有简单的 step-by-step 文档.

---

# 重新编译程序

<br />

重新编译经常需要装一堆库, 关注一些细节的 configure 选项. 但在有大量相同系统的环境中部署时, 这是最省事的方式.

一般不会遇到程序依赖于较高版本 glibc 的特性, 而在低版本中不支持. 如果遇到了 ...

---

# 参考

<br />

1. [Running a statically linked binary with a different glibc](http://unix.stackexchange.com/questions/2717/running-a-statically-linked-binary-with-a-different-glibc)
1. [how to run new software without updating GLIBC?](http://unix.stackexchange.com/questions/62940/how-to-run-new-software-without-updating-glibc)
1. [Multiple glibc libraries on a single host](http://stackoverflow.com/questions/847179/multiple-glibc-libraries-on-a-single-host)
1. [查看当前系统的glibc版本](http://my.oschina.net/acmfly/blog/77211)