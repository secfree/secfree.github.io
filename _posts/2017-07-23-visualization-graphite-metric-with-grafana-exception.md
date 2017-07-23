---
layout: blog
title: "Visualization Graphite Metrics with Grafana Exception"
---

When changeing Graphite's visualization to Grafana, I met the following exception

```
TypeError: Cannot read property 'length' of undefined
    at convertDataPointsToMs (http://hostname:80/public/app/boot.74ebeed4.js:15:17351)
    ...
```

I inspected the request in Chrome, searched the exception and found some pages such as [sometimes format=json requests return png's](https://github.com/graphite-project/graphite-web/issues/576), which is outdated. The problem described has been fixed in my Graphite version `0.9.10`.

I added more log in graphite-web code and compared Grafana's request with graphite-web's, founding that they are using different http method when calling `/render`:

- graphite-web: get
- Grafana: post

Searched again and founded [Why are you POSTing to graphite /render?](https://github.com/grafana/grafana/issues/1767), which said that upgrading to Graphite `0.9.13` would fix this.

The code raises/solves exception is

```diff
# graphite-web/webapp/graphite/render/views.py
def parseOptions(request):
-  queryParams = request.GET
+  queryParams = request.GET.copy()
+  queryParams.update(request.POST)
```
