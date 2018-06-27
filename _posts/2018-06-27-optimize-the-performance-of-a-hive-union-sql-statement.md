---
layout: blog
title: "Optimize the Performance of a Hive Union SQL Statement"
---

# Background

<br />

A Hive SQL statement like below cost several hours when querying with Spark on more than 100 executors.

```sql
select *
from
   (
     select table_a.tid, table_b.name, table_c.type
     from table_a
     join table_b on table_a.tid = table_b.tid
     left join table_c on table_a.tid = table_c.tid
     where to_date(from_unixtime(table_a.start_time)) <= '2018-05-27'
     and to_date(from_unixtime(table_a.end_time)) >= '2018-05-20'
   )
Union
  (
    select table_a.tid, table_b.name, table_c.type
    from table_a
    join table_b on table_a.tid = table_b.tid
    left join table_c on table_a.tid = table_c.tid
    where to_date(from_unixtime(table_a.start_time)) <= '2018-06-27'
    and to_date(from_unixtime(table_a.end_time)) >= '2018-06-20'
  )
```

| table | rows |
| --- | --- |
| table_a | 27,837,053 |
| table_b |	317,261,723 |
| table_c |	888,925 |

---

# Reason

<br />

The real bottleneck is the **union** action in the SQL statement. The union will do the **distinct** action, which is very expensive.

After joining, the union action will distinct on a very large dataset.

Usually, the join action cost about 10 minutes, but the distinct action cost nearly 2 hours.

---

# Solution

<br />

It's not easy to optimize the performance of the distinct algorithm. It's easy to optimize the size of the dataset to distinct.

Below is the optimized SQL statement

```sql
select *
from
   (
     select a.tid, b.name, c.type
     from
        (
          select distinct tid from table a
          where to_date(from_unixtime(table_a.start_time)) <= '2018-05-27'
          and to_date(from_unixtime(table_a.end_time)) >= '2018-05-20'
        ) a
     join (select distinct tid, name from table_b) b
     on a.tid = b.tid
     left join (select distinct tid, type from table_c) c
     on a.tid = c.tid
   )
Union
  (
    select a.tid, b.name, c.type
    from
       (
         select distinct tid from table a
         where to_date(from_unixtime(table_a.start_time)) <= '2018-06-27'
         and to_date(from_unixtime(table_a.end_time)) >= '2018-06-20'
       ) a
    join (select distinct tid, name from table_b) b
    on a.tid = b.tid
    left join (select distinct tid, type from table_c) c
    on a.tid = c.tid
  )
```

The skill is well explained in [Use Subqueries to Count Distinct 50X Faster](https://www.periscopedata.com/blog/use-subqueries-to-count-distinct-50x-faster) and [Performance Tuning SQL Queries](https://community.modeanalytics.com/sql/tutorial/sql-performance-tuning/)
