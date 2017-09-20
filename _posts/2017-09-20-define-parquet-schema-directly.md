---
layout: blog
title: "Define parquet schema directly?"
---

After reading some pages and slides about Parquet, I get some questions about defining the schema:

1. Do I need to define the schema directly?
1. Should I define the schema directly?
1. Can I define the schema directly?

---

[Parquet documentation](https://parquet.apache.org/documentation/latest/) did not give definition language or any demos.

Viewed a lot of pages and founding them all get Parquet schema from other format's schema, such as JSON, AVRO. Here's a well-known example: [Spark SQL load and write parquet](http://spark.apache.org/docs/latest/sql-programming-guide.html#parquet-files)

Parquet is oriented from [Dremel paper](https://research.google.com/pubs/pub36632.html), which described the record shredding and assembly algorithm. Below is my understanding of it,  please be skeptical.

Parquet is the format of columnar representation of a nested record, its schema depends on and oriented from the record's schema. So, the answers to the above questions are all "NO".
