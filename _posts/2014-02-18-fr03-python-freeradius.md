---
layout: blog
title: "FR03: Python 程序采用 FreeRADIUS 验证"
---

FreeRADIUS 3 对各种编程语言的支持可参考: [RADIUS-Clients](http://wiki.freeradius.org/glossary/RADIUS-Clients) .

Python 程序使用 [pyrad](https://pypi.python.org/pypi/pyrad) .

下载 [pyrad-2.0.tar.gz](https://pypi.python.org/packages/source/p/pyrad/pyrad-2.0.tar.gz) 并安装 .

以下为一个可用的接口:

```python
#!/usr/bin/env python
#coding=utf-8
 
# author: dengzhaoqun
 
import sys
import pyrad. packet
from pyrad. client import Client
from pyrad. dictionary import Dictionary
 
def otp_auth ( server, port , secret , user, otp ):
    """
    Auth otp.

    :param server: hostname or IP address of FreeRADIUS server
    :type params: str

    :param port: port to use for authentication packets
    :type port: int

    :param secret:  secret set by FreeRADIUS server
    :type secret: str

    :param user: username
    :type user: str

    :param otp: otp value
    :type otp: str

    :returns: success/fail, reply code
    :rtype: tuple
    """
    # init and make request package
    clnt = Client (
        server = server ,
        authport = port,
        secret = secret ,
        dict = Dictionary ("dictionary.rfc2865" , "dictionary.acc" )
        )
    req = clnt . CreateAuthPacket (
        code = pyrad. packet .AccessRequest ,
        User_Name = user
        )
    req ["User-Password" ] = req .PwCrypt ( otp)
 
    # auth
    try :
        reply = clnt. SendPacket (req )
    except pyrad . client. Timeout :
        return ( False , 0 )
    if reply . code == pyrad .packet . AccessAccept :
        return ( True , None )
    else :
        return ( False , reply . code)
 
 
if __name__ == "__main__" :
    if len ( sys. argv ) < 3 :
        print '%s user otp' % sys .argv [ 0]
 
    config = [
        ( '127.0.0.1' , 1828, 'test' ),
        ( '192.168.1.3' , 1828, 'test' )
    ]
 
    for cf in config :
        flag , code = otp_auth (
            server = cf[ 0 ],
            port = cf[ 1 ],
            secret = cf[ 2 ],
            user = sys. argv [1 ],
            otp = sys. argv [2 ]
            )
        if flag:
            print 'Pass'
            break
        elif code == 0 :
            continue
        else :
            print 'Fail'
            break
    else :
        print 'Server down'
```

其中 "dictionary.rfc2865" ,"dictionary.acc" 可在 FreeRADIUS server 上找到. 这里给出文件:

- [dictionary.rfc2865]({{ site.url }}/downloads/dictionary.rfc2865)
- [dictionary.acc]({{ site.url }}/downloads/dictionary.acc)

如果报错

```
pyrad.dictionary.ParseError: dictionary.rfc2865(34): Parse error: Illegal type: vsa
```

注释掉 dictionary.rfc2865 中报错的那行即可.