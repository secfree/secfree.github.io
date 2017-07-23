---
layout: blog
title: "FR04: PHP 程序采用 FreeRADIUS 验证"
---

PHP 可用 [PHP Radius Extension](http://php.net/manual/en/ref.radius.php) 和 FreeRADIUS 通信. 

下载 [radius.class.php](http://developer.sysco.ch/php/radius_class_pure_php.zip) , 编写下面的测试代码 php_rad.php

```php
<?php
    require_once ('radius.class.php');
    $radius = new Radius ('freeradius_server_ip', 'secret_value');
    if ( $radius->AccessRequest ('user_name', $_GET[ 'pass']))
    {
        echo "Authentication accepted." ;
    }
    else
    {
        echo "Authentication rejected." ;
    }
?>

```

带参数 "pass=value" 访问 php_rad.php 即可.