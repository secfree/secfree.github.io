---
layout: blog
title: "dbutils中BeanHandler字段名的匹配"
---

[dbutils](http://commons.apache.org/proper/commons-dbutils/)最初吸引我用的功能之一就是BeanHander, 可以自动将select出来的一行数据构造为一个bean object.

今天遇到这样一个case: bean中有一个属性名为host\_id, 对应的方法为getHostId()和setHostId(). 数据库中对应的table的字段名也是host\_id. 使用BeanHandler去除来的结果却总是空值. bean中的类型为int, 数据库中的类型为long.

初始以为是int和long不匹配导致, 修改为一致后问题依旧.

又怀疑是bean的属性数量和table的字段数量不一致导致, 修改为一致后问题依旧.

如此只能看[源码](http://svn.apache.org/viewvc/commons/proper/dbutils/)了.

源码中发现, dbutils在取bean属性名的时候, 是通过

```java
pds = Introspector.getBeanInfo(Bean.class).getPropertyDescriptors();
```

[BeanProcessor](http://svn.apache.org/viewvc/commons/proper/dbutils/trunk/src/main/java/org/apache/commons/dbutils/BeanProcessor.java?view=markup)中是以 pd.getName() 来和 column 用 equalsIgnoreCase 比较, 而 getName 返回的是 hostId, 因此会导致字段名匹配失败.

![]({{ site.url }}/downloads/bean_desc.png)

原本自己实现了一个可以匹配含下划线字段的RowProcessor, 但是后面又发现dbutils的最新版本已经有了类似的实现, 那就是 [GenerousBeanProcessor](http://svn.apache.org/viewvc/commons/proper/dbutils/trunk/src/main/java/org/apache/commons/dbutils/GenerousBeanProcessor.java?view=log), 它有很强大的匹配能力.

用下面的方法设置ResultSetHandler即可

```java
ResultSetHandler<Bean> rsh = new BeanHandler<Bean>(Bean.class, new BasicRowProcessor(new GenerousBeanProcessor()));
```

而int和long的类型, dbutils会自动转化. 字段的数量也不要求匹配. dbutils只会尽量匹配多的字段.