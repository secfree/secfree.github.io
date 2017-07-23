---
layout: blog
title: "No permissions found of the default group in Hue"
---

When I logged to Hue with a non-superuser, the main menus such as `Query Editor`, `File Browser` are all hidden. The `View Profile` action returned `permission denied`.

But in my another setup of Hue, a non-superuser can see all munu items and has priviledge to do the `View Profile` action.

I checked the log of Hue, there are records in `runcpserver.log`:

```
[29/Feb/2016 18:45:11 +0800] access       INFO     172.18.27.32 dengzhaoqun - "GET /useradmin/users/edit/dengzhaoqun HTTP/1.1" -- permission denied
```

I then logged with a superuser and found that the group `default` is `No permissions found` at `Manage Users > Group`, which has a permission list in another setup. I checked the configure, but it's OK. Finally I found it's bacause of the `useradmin_huepermission` and `useradmin_grouppermission` tables in MySQL are empty, it must has something been wrong when migrating to MySQL.

So I recovered the values of `useradmin_huepermission`:

```
mysql > INSERT INTO `useradmin_huepermission` VALUES ('access','indexer',28,'Launch this application'),('access','about',29,'Launch this application'),('access','beeswax',30,'Launch this application'),('access','filebrowser',31,'Launch this application'),('write','hbase',32,'Allow writing in the HBase app.'),('access','hbase',33,'Launch this application'),('access','help',34,'Launch this application'),('access','jobbrowser',35,'Launch this application'),('access','jobsub',36,'Launch this application'),('write','metastore',37,'Allow DDL operations. Need the app access too.'),('access','metastore',38,'Launch this application'),('dashboard_jobs_access','oozie',39,'Oozie Dashboard read-only user for all jobs'),('access','oozie',40,'Launch this application'),('access','proxy',41,'Launch this application'),('access','rdbms',42,'Launch this application'),('access','search',43,'Launch this application'),('access_view:useradmin:edit_user','useradmin',44,'Access to profile page on User Admin'),('access','useradmin',45,'Launch this application'),('access','zookeeper',46,'Launch this application');
```

The values of `useradmin_grouppermission` can be updated by choice. After doing these, a non-superuser is able to do the normal action.
