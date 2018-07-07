---
layout: blog
title: "How to Do Big Data Integrity Validation"
---

# Background

<br />

For one input record, it could be applied:

1. map -> convert to another record
1. flatMap -> convert to some other records
1. filter -> keep or drop
1. aggregate -> group by some keys and aggregate to another record
1. ...

And in a data flow, one record may be processed a lot of times.

We want to do data validation in such a situation, in the Big Data environment. To check

1. If there is data loss
1. If there is any calculation error

---

# Ideas

<br />

<img src="/downloads/data-integrity-validation.png" width="70%">

As the picture shows above, from 0% to 100% of checking output records

- None(0%)

  It has a very high risk. It's unacceptable.

- Every(100%)

  It has a very high cost. It's infeasible. For `n` input records, after several processing steps, it may output `n*n*...` records. In the Big Data environment, we are unable to validate each step for every record.

- Statistic

  It's done by most teams. We can do aggregation on some special features/dimensions and check the statistical value. By correlate related metrics, we can improve our confidence about the conclusion. However, the problem is that the statistic is just a roughly view in a lot of situations. We can not get the precise result. For example, if the count of one metric is 100 yesterday, we may suspect it if the count becomes 50 today. But what about 75? 85?

- Sampling

  By generating predefined inputs with special identifies, the output for each determined processing step can be validated by determined values. The sampling is always running with the online data flow to detect possible exceptions.

For Big Data, we can't get a perfect solution for data integrity validation. By combining `statistic` and `sampling`, we can get an acceptable solution.
