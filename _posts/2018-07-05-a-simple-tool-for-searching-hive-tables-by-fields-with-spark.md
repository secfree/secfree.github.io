---
layout: blog
title: "A Simple Tool for Searching Hive Tables by Fields with Spark"
---

# Background

<br />

You may have a huge volume of data to analyse, include

1. Hundred of tables in a Hive
1. Hundred of fields in a table

It's very common to do correlation by joining different tables when analysing. The joining condition it's a tuple of fields. In order to finish analyse as soon as possible, you need to check which tables contains some special fields.

---

# Solution

<br />

The simple code listed below is able to do the search.

```scala
import org.apache.spark.sql.SparkSession

import scala.collection.mutable

object Test {
  val db = "test"
  def main(args: Array[String]): Unit = {
    // Fields are separated by ","
    search(args(0).split(",").toSet)
  }

  def getSparkSession(name: String): SparkSession = {
    SparkSession.builder().appName(name).enableHiveSupport().getOrCreate()
  }

  def search(fields: Set[String]): Unit = {
    val spark = getSparkSession("Search-Table-Catalog")
    val tables = spark.catalog.listTables(db).collect()
    for (t <- tables) {
      val cols = spark.catalog.listColumns(db, t.name).collect()
      val colNames = new mutable.HashSet[String]()
      for (c <- cols) {
        colNames.add(c.name)
      }
      if (fields.subsetOf(colNames)) {
        println("Found match table: " + t)
      }
    }
  }
}
```

You can execute it in `spark-shell` or by `spark-submit`.
