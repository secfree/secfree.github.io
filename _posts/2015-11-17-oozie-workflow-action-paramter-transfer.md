---
layout: blog
title: "在 oozie 配置中向 action 执行程序传递参数"
---

# 简介

<br />

oozie 的 workflow.xml 中配置的 action 调用的程序, 需要的参数有时候是和时间
相关的或者动态生成的, 这时就需要从配置中向 action 传递它需要的参数值.

oozie 本身的文档中有说明:
[oozie Java Action](http://oozie.apache.org/docs/4.2.0/WorkflowFunctionalSpec.html#a3.2.7_Java_Action).
但是 oozie 文档的排版和逻辑阅读起来很是吃力, 因此个人在测试之后, 这这里记录一下
demo, 以供后续查阅.

---

# workflow.xml 中配置参数

<br />

在 `<action>` 中配置 `<configuration><property>`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<workflow-app xmlns="uri:oozie:workflow:0.3" name="${jobName}">
    <start to="test"/>
    <action name="test">
        <java>
            <job-tracker>${jobTracker}</job-tracker>
            <name-node>${nameNode}</name-node>
            <configuration>
               <property>
                    <name>mapred.job.queue.name</name>
                    <value>${queueName}</value>
               </property>

               <property>
                    <name>test.prop</name>
                    <value>anoymous</value>
               </property>

            </configuration>
            <main-class>LoopPrint</main-class>
            <capture-output/>
        </java>
        <ok to="end" />
        <error to="fail" />
    </action>
    <kill name="fail">
        <message>Java Daily Action failed, error message[
            ${wf:errorMessage(wf:lastErrorNode())}]</message>
    </kill>
    <end name="end"/>
</workflow-app>
```

---

# action 执行程序中取参数

<br />

> A java action can create a Hadoop configuration for interacting with a cluster (e.g. launching a map-reduce job). Oozie prepares a Hadoop configuration file which includes the environments site configuration files (e.g. hdfs-site.xml, mapred-site.xml, etc) plus the properties added to the section of the java action. The Hadoop configuration file is made available as a local file to the Java application in its running directory. It can be added to the java actions Hadoop configuration by referencing the system property: oozie-action.conf.xml .

下面为 action 的 demo 代码:

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

public class LoopPrint {
    public static void main(String args[]) {
        Configuration actionConf = new Configuration(false);
        actionConf.addResource(new Path("file:///",
                System.getProperty("oozie.action.conf.xml")));
        while (true) {
            System.out.println(
                    "Test oozie prop: " + actionConf.get("test.prop"));
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                break;
            }
        }
    }
}
```

使用 oozie 提交该测试 job 之后, 在 action 运行的 yarn 上可以看到程序的输出:

```
Test oozie prop: anoymous
Test oozie prop: anoymous
```

证明 `test.prop` 从 workflow.xml 中成功传递到 action.
