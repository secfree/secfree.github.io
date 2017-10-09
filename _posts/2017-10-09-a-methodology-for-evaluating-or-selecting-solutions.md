---
layout: blog
title: "A methodology for evaluating or selecting solutions"
---

I am tired of thinking about how to evaluate or select a proper solution. What I need is a methodology. Below is my idea, and I've applied it several times, it's not bad.

---

Methodology:

1. define features, such as
    - functionality
    - performance
    - ease of use
    - ease of operation
    - scalability
    - relation with current product/platform
    - potential (popularity + community + ...)
1. give a weight between [1-10] for each feature i, mark as w(i)
    - the weight value should be adjusted based on the real environment
1. give a score between [1-10] for each feature in each solution, mark as s(ij)
    - I don't have a standard for score assignment
1. calculate sum(s(ij) * w(i)) for each solution and compare
