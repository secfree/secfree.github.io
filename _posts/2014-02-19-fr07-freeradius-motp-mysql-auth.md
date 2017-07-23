---
layout: blog
title: "FR07: 配置 FreeRADIUS MOTP验证使用 MySQL"
---

1. 在 mysql 中创建 freeradius 需要的 schema

   ```sql
   mysql > create database radius;
   mysql > GRANT ALL ON radius.* TO radius@localhost IDENTIFIED BY "password";
   mysql > source /usr/local/etc/raddb/mods-config/sql/main/mysql/schema.sql
   ```

    如果 source  schema.sql 时报错

   ```
   ERROR 1064 (42000): You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'unsigned int(12) default NULL,
     acctauthentic varchar(32) default NULL,
     conne' at line 15
   ```

    则修改 schema.sql 中

   ```diff
   -31  acctsessiontime unsigned int(12) default NULL,
   +31  acctsessiontime int(12) unsigned default NULL,
   ```

2. freeradius 中 sql 的配置.

    /usr/local/etc/raddb/mods-availabl/sql

   ```diff
   -58     #driver = "rlm_sql_null"
   +59     driver = "rlm_sql_mysql"
   ```

   ```udiff
   +70     server = "localhost"
   +71     port = 3306
   +72     login = "radius"
   +73     password = "password"
   ```

    在 /usr/loca/etc/raddb/mods-enabled/ 下执行

   ```console
   $ ln -s ../mods-available/sql .
   ```

    使 sql 模块起作用 .

    在我的机器上启动 "radiusd -X" 报错

   ```
   Could not link driver rlm_sql_mysql: rlm_sql_mysql.so: cannot open shared object file: No such file or directory
   ```

    使用 find / -name "rlm\_sql\_mysql*" 并没有找到 rlm\_sql_mysql.so .

    猜测是因为测试机上安装 freeradius 时, 还没有装 mysql, 因此没有编译出 rlm\_sql_mysql.so .

    在 freeradius安装包解压的目录下, 重新 configure 和 make . 用 find 发现有编译出 rlm\_sql_mysql.so .

    freeradius 中有一个模块叫 exec, 在 /usr/local/lib 下发现有 rlm\_exec.a, rlm\_exec.so, rlm_exec.la 三个文件,

    因此将 rlm\_sql\_mysql.a, rlm\_sql\_mysql.so, rlm\_sql_mysql.la 拷贝到 /usr/local/lib ,  "radiusd -X" 正常启动.

3. 测试 freeradius 是否正常使用 sql .

    如果有按 "Rd01: 部署 FreeRADIUS 3 用 MOTP 验证" 在 /usr/local/etc/raddb/users 底部添加

   ```
   DEFAULT Auth-Type := Accept, Simultaneous-Use := 1
   #下面两行前面的空白为 tab.  secret_value, pin_value 为 DroidOTP 中记录的值.
       Exec-Program-Wait = "/usr/local/bin/otpverify.sh '%{User-Name}' '%{User-Password}' 'secret_value' 'pin_value' '2'",
       Fall-Through = Yes
   ```

    则注释掉上面添加的内容.

    在 mysql 的 radius 库中执行

   ```sql
   mysql> insert into radcheck(username, attribute, op, value) values('dzqts', 'Cleartext-Password', ':=', 'xAjhlyp');
   ```

    使用 radtest 测试

   ```console
   $ radtest dzqts xAjhlyp localhost 1812 testing123  => Accept
   $ radtest dzqts xajhlyp localhost 1812 testing123  => Reject
   ```

    证明数据库配置的用户信息被正确引用.

4. 配置 freeradius motp 验证采用数据库

    在 mysql 的 radius 库中插入验证 otp 需要的 Secret, Pin, Offset

   ```sql
   mysql> insert into radreply(username, attribute, op, value) values('dzqts', 'Secret', ':=', 'a9678416afe4eafc');
   mysql> insert into radreply(username, attribute, op, value) values('dzqts', 'Pin', ':=', '6111');
   mysql> insert into radreply(username, attribute, op, value) values('dzqts', 'Offset', ':=', '2');
   ```

    在 /usr/local/etc/raddb/users 底部添加

   ```
   DEFAULT Auth-Type := Accept, Simultaneous-Use := 1
       Exec-Program-Wait = "/usr/local/bin/otpverify.sh '%{User-Name}' '%{User-Password}' '%{reply:Secret}' '%{reply:Pin}' '%{reply:Offset}'",
       Fall-Through = Yes
   ```

    启动 "radiusd -X", 使用 radtest 测试收到 "Access-Reject" .

    "radiusd -X"输出显示执行的 Exec-Program-Wait 为

   ```
   "/usr/local/bin/otpverify.sh 'dzqts' 'otp' '' '' '' "
   ```

    可见

   ```
   '%{reply:Secret}' '%{reply:Pin}' '%{reply:Offset}'
   ```

    都没有被 expand .

    尝试了多个思路, 没有解决. 让人汗的是, FreeRADIUS 3 还有很多东西没有文档.

    仔细观察发现, Exec-Program-Wait 语句的 expand, 是在 sql 检索 radcheck, radreply 之前. 因此肯定是不能解析 %{reply:Secret} 等的.

    由 "radiusd -X"输出知, Exec-Program-Wait 语句的 expand 是在 files 阶段, sql 的检索是在 sql 阶段, 应在 "/usr/local/etc/raddb/sites-available/default"中修改

   ```
   authorize{
      ...
      files
      -sql
      ...
   }
   ```

    将 "-sql" 移到 files 前

   ```
   authorize{
      ...
      -sql
       files
      ...
   }
   ```

    重新启动 "radiusd -X" , reply 展开成功.

    现在只需要在数据库的表 radcheck, radreply 中添加相应信息, 就能方便支持多用户用 MOTP 验证.
