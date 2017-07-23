---
layout: blog
title: "shadowsocks on Ubuntu has no response"
---

I installed shadowsocks on Ubuntu by pip

    pip install shadowsocks

I configured the server information in `Shadowsocks-Qt5`, and it shows connected to the shadowsocks server succeed. But which is stange is that the request has no response, I can only see request logs in the log of `Shadowsocks-Qt5`.

I have tried closing or removing the iptables, which has no effect.

I could not find out the reason of this problem, but I bypass it by start the shadownsocks agent in another way.

1. Write the configuration in `/etc/shadowsocks.json`:

   ```json
    {
        "server":"server address",
        "server_port": 1080,
        "local_address": "0.0.0.0",
        "local_port": 1080,
        "password":"password",
        "timeout":300,
        "method":"aes-256-cfb",
        "fast_open": false
    }
   ```

1. Start the shadowsocks agent by command line

   ```
   sudo /usr/local/bin/sslocal -c /etc/shadowsocks.json  -d start
   ```

Now it works.
