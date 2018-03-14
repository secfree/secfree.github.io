---
layout: blog
title: "How to assign a weight value for a gitlab issue?"
---

I'm using [gitlab](http://gitlab.com/) to manage my personal issues about life and study. Gitlab issues have a `weight` attribute with the value range from 1 to 9. I've been thinking for a long time about the assigning standard. Because for judging by direct, it's hard to choose between two adjacent values.

I used three tags to represent weight value when gitlab did not have the "weight" attribute yet. They are:

- important
- normal
- slave

I think it's convenient and easily distinguishable. I got an idea from this.

First, map [1, 9] to the three tags.

| importance | weight |
| --- | --- |
| important | [7, 9] |
| normal | [4, 6] |
| slave | [1, 3] |


Then map the three tags again.

| importance (first level) | importance (second level) | weight |
| --- | --- | --- |
| important | important | 9 |
| important | normal |  8 |
| important | slave | 7 |
| normal | important | 6 |
| normal | normal | 5 |
| normal | slave | 4 |
| slave | important | 3 |
| slave | normal | 2 |
| slave | slave | 1 |

For example, when you are creating an issue. You think it's important, so it will get a weight value located in [7, 9]. While between all important cases, it's normal. So you get the final value 7.
