---
layout: blog
title: "Version Upgrading Strategy for a Tool Used by Thousands of Jobs"
---

Background

1. A tool/lib is used by thousands of jobs
2. Each job has the version configuration item for the tool

When we released a new version for the tool, we have two candidate upgrading strategies for the jobs:

1. Update all jobs to use the latest version of the tool
2. Only use the latest version for new created jobs, and upgrade old jobs only when necessary

Finally, we dicided to use the second strategy, below are the reasons:

- The risk of the first strategy is too high. We cannot assure the new version will not introduce new bugs.
- The cost of the first strategy is too high. For example, if we upgrade "m" jobs with "v_a -> v_a+1 -> ... -> v_b", we need to do "m * (b - a)" upgrading units. If using the second strategy, we only need to do "v_a -> v_b", the upgrading units number is "m".
- However, having too many versions at the same time will also magnify the maintain cost and risk. So we need to do batch upgrading for jobs which is too much behind.

Actually, I think the first strategy is just like streaming processing - process each message when receiving it. While the second strategy likes batch processing, we can control the batch size and decide the processing time, which is of course has a lower cost and easy to maintain.
