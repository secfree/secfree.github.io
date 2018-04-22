---
layout: blog
title: "Measure the Variation of a Metric in Big Data"
---

# When do we need to measure the variation?

<br />

Sometimes we want to check if the value of a metric is abnormal. By locating abnormal points, we may be able to fix program exceptions, adjust business strategies.

But how to determine if the value if abnormal or not? All points in the following three diagrams can be considered normal.

<img src="/downloads/normal-up.png" width="70%">

<img src="/downloads/normal-down.png" width="70%">

<img src="/downloads/normal-wave.png" width="70%">

The latest point in these two diagrams can be considered abnormal.

<img src="/downloads/abnormal-up.png" width="70%">

<img src="/downloads/abnormal-down.png" width="70%">

As we can see, there's a significant distance between normal and abnormal points. So, we can detect the abnormal points by measure the variation of the metric.

---

# How to measure the variation?

<br />

There's a classic method:

1. Calculate out the `mean` and `variance` of the previous values
1. Compare the latest value with

  $$
    a * mean + b * \sqrt{variance}
  $$

  a and b are parameters can be adjusted with experience.

---

# How to computing in growing Big Data?

<br />

With the data growing, it could be slow to load all the historical data. In such a case, we can compute the mean and variance iteratively. The iterative algorithm is here: [Algorithms for calculating variance: Online algorithm](https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm).

By applying this method, we just need to read the latest data and the (mean, variance) pair generated last time.

---

# How to computing the mean and variance of the latest N days in Big Data?

<br />

The iterative/online algorithm introduced above is not applicable to this case. But, the data size in such case is firmed(n days). Here is a solution:

1. Aggregate the metric daily
1. Partition the daily metric by each metric's identifier separately

Then, the size need to load is

$$
  cardinality(set(item)) * days
$$

In most times, it's not a large number.

By applying this method, we can aggregate each metric parallelly without loading unrelated data.
