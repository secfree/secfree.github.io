---
layout: blog
title: 对 Rove 和 Puphpet 的失败试用
---

个人发现 [PHP 之道](http://wulijun.github.io/php-the-right-way/)对我这样的新手比较有帮助. 其中有推荐 [Vagrant](http://www.vagrantup.com/)

> Create and configure lightweight, reproducible, and portable development environments.

Vagrant 可以轻易地用来建立一个方便地开发环境 -- 本地开发, 本地测试. 我以前在 Win 上写代码, 用 Rsync 同步到测试 Linux 服务器, 每次同步都需要几秒的延时, 无疑是一件令人烦躁的事情. 并且测试服务器会越来越"重", 有时会有程序或包的冲突.

当对 Vagrant 有一个入门的了解后, 看到 \<\<PHP 之道\>\> 中:

> 如果你想获取一些Vagrant的使用帮助的话，可以参考下面的三个服务：
- Rove: 通过Chef来提供常用的Vagrant构建，其中包含了PHP选项。
- Puphpet: 简单的图形界面来设置虚拟机的PHP开发环境，专注于PHP开发，不仅可以配置本地的虚拟机，还可以部署到云服务上，它的底层是通过Puppet实现。

[Rove](http://rove.io/) 和 [Puphpet](https://puphpet.com/) 貌似都可以用来一键部署 PHP 开发需要的环境. 于是我先尝试 Rove:

1. 安装 Ruby 和 gem ;
2. 执行 "gem install librarian-chef", 报错, 搜索, 说是 gem 版本的问题;
3. 卸载 Ruby 和 gem, 安装新的合适的版本 Ruby, gem;
4. 执行 "gem install librarian-chef";
5. 执行 "librarian-chef install";
6. 执行 "vagrant up"; 

好吧, 至此耗时不短, 宿主机上装了一堆东西, 但是 vagrant 虚机中的 PHP 开发环境并没有安装成功. 搜索原因, 和 Rove 相关资料很少, 个人不了解也没有兴趣学习 Chef. 无果而终.

或许 Puphpet 简单些, 我接着尝试:

1. 在 [Puphpet](https://puphpet.com/) 上选择配置, 下载文件;
2. 解压文件到 vagrant 目录, 执行 "vagrant up", 长时间等待, 然后 Puppet 报错;
3. 搜索错误, 网上有说重启 Puppet 即可. 因此 "Ctrl + C" 中断 "vagrant up";
4. 执行 "vagrant ssh"失败, 无法连接虚机. 执行 "vagrant halt" 失败;
5. 先前在 [Puphpet](https://puphpet.com/) 上选择的是 Ubuntu precise, 尝试更通用的 CentOS, Puphpet 网站报 

    > Oops! An Error OccurredThe server returned a "500 Internal Server Error";

至此耗时又不短, 还好主机上并没有装什么东西, 只是下载一个文件包. 去学习 Puppet ? 可是我的本意是想轻松地搭建一个开发环境而已.

为了达到完全自动化, 往往需要引入一些使用较为复杂的工具. 当因为系统和软件的版本, 网络引发问题时, 解决这些问题所消耗的时间比手动配置几台 MySQL 的密码更多. 对于一个普通用户, 所有操作越简单越好, 而适度的手工操作是可以接受的.

这搭建开发环境这种情况下, 只用 Vagrant 和 shell 的组合无疑是一种简单易行的方案. 并不需要牵涉到 Ruby, Chef 和 Puppet . 写好需要执行的 shell 脚本, 在 Vagrantfile 中配置 Provisoning, 并在需要的时候设置 always 选项. 可参考: [http://docs.vagrantup.com/v2/provisioning/basic_usage.html](http://docs.vagrantup.com/v2/provisioning/basic_usage.html)

因为个人的性格原因, 经常会被技术的细节拉住脚步, 消耗太多的时间. 但在商业的环境中, 产品的快速迭代是更重要的. 和产品无关的技术细节, 不应该纠缠太多. 

以此为戒!