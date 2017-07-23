---
layout: blog
title: mysql中三种插入数据方法
---

```python
# 第一种
cur.executemany(sql, rows)
con.commit()

m = 0
n = 1000
num = len(rows)

# 第二种
while m < num:
    cur.executemany(sql, rows[m:m + n])
    con.commit()
    m += n

# 第三种
m = 0
while m < num:
    cur.executemany(sql, rows[m:m + n])
    m += n
con.commit()
```

测试的行数 num = 200000

设三种方法的耗时分别为 t1, t2, t3 .

当每一行的大小较小时(200B左右), 耗时为: t1 < t3 < t2 .

当每一行的大小稍大时(2048B左右), 使用方法1报错:

```
mysql.connector.errors.OperationalError: 1153 (08S01): Got a packet bigger than 'max_allowed_packet' bytes
```

方法2和3的耗时为: t3 < t2 .

因此, 为了保险起见, 在代码中还是尽量采用分段插入.

t2 和 t3 相差不大, 一般情况下可以通用.
