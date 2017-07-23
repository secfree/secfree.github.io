---
layout: blog
title: "Query csv format file by Spark SQL without extra dependency"
---

What I want to emphasize is that csv is such a convenient intermediate format for processing and saving data. It's concise and can be analysised by SQL statement through a simple step:

1. load to MySQL by `load data infile`
1. or query by Spark SQL directly

Methods of querying csv files from Spark SQL often refers `CsvContext` dependency. Here I introduce another way.

1. Data sample

   ```
   2016,11,abc,jack
   2016,10,bb,jim
   2015,03,test,paul
   2014,09,good,simth
   ```

1. Create a hive table for data

   ```sql
   CREATE EXTERNAL TABLE IF NOT EXISTS test (
       year STRING,
       day STRING,
       tag STRING,
       name STRING
   )
   row format delimited
   fields terminated by ','
   stored as textfile
   location 'file:///tmp/test/csvs'
   ```

1. Query and analysis

   ```
   $ ./bin/spark-shell
   Type :help for more information.
   Spark context available as sc.
   SQL context available as sqlContext.

   scala> val table = """
     | CREATE EXTERNAL TABLE IF NOT EXISTS test (
     |     year STRING,
     |     day STRING,
     |     tag STRING,
     |     name STRING
     | )
     | row format delimited
     | fields terminated by ','
     | stored as textfile
     | location 'file:///tmp/test/csvs'"""

    scala> sqlContext.sql(table)
    res0: org.apache.spark.sql.DataFrame = [result: string]

    scala> val res = sqlContext.sql("select * from test")
    res: org.apache.spark.sql.DataFrame = [year: string, day: string, tag: string, name: string]

    scala> res.show()
    +----+---+----+-----+
    |year|day| tag| name|
    +----+---+----+-----+
    |2016| 11| abc| jack|
    |2016| 10|  bb|  jim|
    |2015| 03|test| paul|
    |2014| 09|good|simth|
    +----+---+----+-----+
   ```
