---
layout: blog
title: "CAP Theorem and Impossible Trinity"
---

It's interesting to realize there is some similarity between the `CAP theorem` and the `Impossible Trinity`.

[The CAP theorem](https://en.wikipedia.org/wiki/CAP_theorem) works in theoretical computer science, states that it is impossible for a distributed computer system to simultaneously provide all three of the following guarantees:

- Consistency (every read receives the most recent write or an error)
- Availability (every request receives a response, without guarantee that it contains the most recent version of the information)
- Partition tolerance (the system continues to operate despite arbitrary partitioning due to network failures)

[The Impossible Trinity](https://en.wikipedia.org/wiki/Impossible_trinity) works in international economics, states that it is impossible to have all three of the following at the same time:

- A fixed foreign exchange rate
- Free capital movement (absence of capital controls)
- An independent monetary policy

![]({{ site.url}}/downloads/Impossible_trinity_diagram.png)

There is some kind of equality as described below.

| CAP Theorem | Impossible Trinity |
| --- | --- |
| Consistency | A fixed foreign exchange rate |
| Partition tolerance | Free capital movement |
| Availability | An independent monetary policy |

Like a distributed architecture in computer science, a country has to choose between of the three. If a economic entity choosed `consitency` and `availability` at some time, because of the capital movement can not be blocked totally, it can not guarantee `consitency` forever.
