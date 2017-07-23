---
layout: blog
title: "YARN 中 container-executor 配置"
---

# 权限和 owner

<br />

当 Hadoop, YARN 以 Secure Mode (使用 Kerberos 认证) 部署时, 必须使用 LinuxContainer

```xml
<property>
    <name>yarn.nodemanager.container-executor.class</name>
    <value>org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor</value>
</property>
```

否则会报错

```
ERROR org.apache.hadoop.mapred.ShuffleHandler: Shuffle error : java.io.IOException: Error Reading IndexFile
```

可参考: [Reduce phase is failing with shuffle error in kerberos enabled cluster](https://issues.apache.org/jira/browse/YARN-1432)

HADOOP_HOME 下的 `bin/container-executor` 和 `etc/hadoop/container-executor.cfg` 对权限和 ower 都有要求:

1. 参考: [Configure the Linux Container](http://pivotalhd-210.docs.pivotal.io/doc/2100/webhelp/topics/ConfiguringKerberosforHDFSandYARNMapReduce.html#configurethelinuxcontainer)
1. 要求 `container-executor.cfg` 的所有父目录 owner 都为 root.

配置好后, 可以运行:

```
./bin/container-executor --checksetup
```

如果配置有误, 则会输出错误信息.

---

# 配置文件的坑

<br />

1. 配置项末尾不能有空格.

    我的配置:

   ```
    #configured value of yarn.nodemanager.linux-container-executor.group
    yarn.nodemanager.linux-container-executor.group=hadoop
    #comma separated list of users who can not run applications
    banned.users=root
    #Prevent other super-users
    min.user.id=500
    ##comma separated list of system users who CAN run applications
    allowed.system.users=hadoop
   ```

    checksetup 总是报错:

   ```
    Can't get group information for hadoop  - Success.
   ```

    Google 得到的结果都不能解决.

    查看源码有:

   ```c
    struct group *group_info = getgrnam(nm_group);
    if (group_info == NULL) {
    fprintf(ERRORFILE, "Can't get group information for %s - %s.\n", nm_group,
            strerror(errno));
    flush_and_close_log_files();
    exit(INVALID_CONFIG_FILE);
    }
   ```

    但是很明显, 测试也表明,

   ```c
    const char *nm_group = "hadoop";
    struct group *group_info = getgrnam(nm_group);
   ```

    是可以得到正确结果的. 继续 check, 发现是配置中

   ```
    yarn.nodemanager.linux-container-executor.group=hadoop
   ```

    末尾有空格导致. 真让人对代码质量大跌眼镜, 看样子 hadoop 的开发者也不全是大牛.

    此问题在 Hadoop 2.5.2 和 2.6.0 中测试都存在.

1. 如果文件以 CRLF 换行, 则会报错:

   ```
    configuration tokenization failed
    Can't get configured value for yarn.nodemanager.linux-container-executor.group.
   ```

    需要使用 dos2unix 转换.

    可以用 `file container-executor.cfg` 检查文件格式:

   ```
    # unix format
    etc/hadoop/container-executor.cfg: ASCII text

    # dos format
    etc/hadoop/container-executor.cfg: ASCII text, with CRLF line terminators  
   ```

1. 如果 `banned.users=` 为空, 也会报上面的错误.
