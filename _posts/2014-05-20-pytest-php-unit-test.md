---
layout: blog
title: 用 pytest 为 PHP 对外接口做单元测试
---

个人刚开始写 PHP, 相比而言, 对 Python 跟熟悉一些.

[pytest](http://pytest.org/latest/) 可以方便地对 Python 进行单元测试. 可以参考: [pytest introduction](http://pythontesting.net/framework/pytest/pytest-introduction/)

下面举一个简单的例子来说明.

test.php 会检查 $_POST['hash'] 是否有效:

```php
<?php
    
    function valid_hash($hash)
    {
       $is_valid = true;

       if (!preg_match('/^[0-9a-z]{32}$/', $hash)){
          $is_valid = false;
       }

       return $is_valid;
    }

    $hash = empty($_POST['hash'])?'':trim($_POST['hash']);
    if(!$hash or !valid_hash($hash)){
        die('{"code": -1, "msg": "invalid hash ."}');
    }

    exit('{"code": 0, "msg": ""}');
?>
```

用如下的 Python 代码来测试:

```python
#!/usr/bin/env python
#coding=utf-8
 
import urllib
 
def post(request):
    url = 'http://127.0.0.1/test.php'
    return urllib.urlopen(url, data=urllib.urlencode(request)).read()
 
def test_invalid_hash():
    request = {
        "hash": 'c601f4f32be81eb262b23b1077a25c8Z'
    }
    assert '{"code": -1, "msg": "invalid hash ."}' == post(request)
 
def test_ok_hash():
    request = {
        "hash": 'c601f4f32be81eb262b23b1077a25c8b'
    }
    assert '{"code": 0, "msg": ""}' == post(request)  
```

执行:

```
python -m pytest test.py
```

结果如下:

```
$ python -m pytest test.py 
======================================================= test session starts ========================================================
platform linux2 -- Python 2.7.3 -- py-1.4.20 -- pytest-2.5.2
collected 2 items 
 
test.py .F
 
============================================================= FAILURES =============================================================
___________________________________________________________ test_ok_hash ___________________________________________________________
 
    def test_ok_hash():
        request = {
            "hash": 'c601f4f32be81eb262b23b1077a25c8b'
        }
>       assert '{"code": -1, "msg": ""}' == post(request)
E       assert '{"code": -1, "msg": ""}' == '{"code": 0, "msg": ""}'
E         - {"code": -1, "msg": ""}
E         ?          ^^
E         + {"code": 0, "msg": ""}
E         ?          ^
 
test_eg.py:20: AssertionError
================================================ 1 failed, 1 passed in 0.09 seconds ================================================
```