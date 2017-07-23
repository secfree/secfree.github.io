---
layout: blog
title: FreeRADIUS 3 配置认证记录入数据库
---

为了更好地监控攻击和审计, 可以将 FreeRADIUS 的认证记录存入数据库中, 然后通过 Web 前端展示和设置报警规则.

本文使用 FreeRADIUS 版本为 3.0.1 .

{% raw %}

1. 配置好 FreeRADIUS 使用数据库, 这里用 Mysql, 可参考 [FR07: 配置 FreeRADIUS MOTP验证使用 MySQL](/blog/2014/02/19/fr07-freeradius-motp-mysql-auth.html).

2. 修改在 ".../raddb/sites-enabled/" 下对应的 virtual_server 中的 post-auth 段为:

   ```
    post-auth {
        # 返回 Access-Accept 时执行 sql
        -sql

        Post-Auth-Type Reject {
            # 返回 Access-Reject 时执行 sql
            -sql
        }
    }
   ```

3. 重启 radiusd 并进行认证, 会发现数据库的 radpostauth 中已经有数据, radpostauth 表结构为:

   ```
    id
    username # 用户名
    pass # 密码
    reply # Access-Accept 或 Access-Reject, 认证是否成功
    authdate # 认证发起时间
   ```

    个人认为, 数据库中并不合适记录 pass, 因为这样相当于明文存储, 有泄露用户密码的风险.

    另外, 很有必要发起认证的来源 IP.

4. 修改 radpostauth 的表结构:

   ```
    mysql> drop table radpostauth;
    mysql> CREATE TABLE radpostauth (
           id int(11) NOT NULL auto_increment,
           username varchar(64) NOT NULL default '',
           nasipaddress varchar(15) NOT NULL default '',
           nasname varchar(100) NOT NULL default '',
           reply varchar(32) NOT NULL default '',
           authdate timestamp NOT NULL,
           PRIMARY KEY  (id)
           ) ENGINE = INNODB;
   ```

    修改 "raddb/mods-config/sql/main/mysql/queries.conf" 中的

   ```
    post-auth {
        # Write SQL queries to a logfile. This is potentially useful for bulk inserts
        # when used with the rlm_sql_null driver.
        # logfile = ${logdir}/post-auth.sql
        query = "\
                INSERT INTO ${..postauth_table} \
                        (username, pass, reply, authdate) \
                VALUES ( \
                        '%{SQL-User-Name}', \
                        '%{%{User-Password}:-%{Chap-Password}}', \
                        '%{Calling-Station-Id}', \
                        '%{reply:Packet-Type}', \
                     '%S')"
    }
   ```

    为

   ```
    post-auth {
        # Write SQL queries to a logfile. This is potentially useful for bulk inserts
        # when used with the rlm_sql_null driver.
        # logfile = ${logdir}/post-auth.sql
        query = "\
                INSERT INTO ${..postauth_table} \
                        (username, nasipaddress, nasname, reply, authdate) \
                VALUES ( \
                        '%{SQL-User-Name}', \
                        '%{Client-IP-Address}', \
                        '%{Calling-Station-Id}', \
                        '%{reply:Packet-Type}', \
                        '%S')"
    }
   ```

    其中 nasipaddress 是 Client-IP-Address, 是发起认证请求的来源 IP. 此处不能用 NAS-IP-Address, 因为 NAS-IP-Address 很多时候都是 "127.0.0.1" .

    nasname 是 [Calling-Station-Id](http://freeradius.org/rfc/rfc2865.html#Calling-Station-Id), 它的用处可以看配置好后的两条记录:

   ```
    mysql> select * from radpostauth;
    +----+-------------+--------------+--------------+---------------+---------------------+
    | id | username    | nasipaddress | nasname      | reply         | authdate            |
    +----+-------------+--------------+--------------+---------------+---------------------+
    | 1  | test_user   | 192.168.1.3  | 192.168.56.1 | Access-Accept | 2014-04-25 10:31:42 |
    | 2  | test_user   | 192.168.1.3  | 192.168.56.1 | Access-Reject | 2014-04-25 10:32:07 |
    +----+-------------+--------------+--------------+---------------+---------------------+
   ```

    nasipaddress 为我个人电脑的 IP, nasname 为我个人电脑上用 NAT 方式配置虚机的 IP. 这两个值结合可以教精确地定位认证请求来源.

{% endraw %}
