---
layout: blog
title: "XSS 之开发者的疑问"
---

- **什么是 XSS ?**

    > XSS 全称(Cross Site Scripting) 跨站脚本攻击， 是Web程序中最常见的漏洞。指攻击者在网页中嵌入脚本(例如JavaScript), 当用户浏览此网页时，脚本就会在用户的浏览器上执行，从而达到攻击者的目的. 比如获取用户的Cookie，导航到恶意网站,携带木马等。

    参考[Web安全测试之XSS](http://www.cnblogs.com/TankXiao/archive/2012/03/21/2337194.html)


- **怎样判断是否存在 XSS ?**

    安全人员发现 XSS 后, 经常会构造一个含有 alert 的 url 给开发人员. 例如:

    ```
    xss01: http://www.56123.org/wuliu.asp?c=0576<script>alert(1)</script>
    xss02: http://bbs.ifeng.com/viewthread.php?tid=14831697');alert(document.domain);alert('1
    ```

    alert 的作用是用来方便地定位 XSS 的位置. 在 Chrome 中打开 url xss01 查看网页源码有

    ![]( {{ site.url}}/downloads/xss_01.png)

    其中的 script. 标签高亮表示在 url 中插入的 js 代码有效, 证明 XSS 存在.

    打开 xss02 查看源码有:

    ![]( {{ site.url}}/downloads/xss_02.png)

    插入的 js 在 script. 标签中会执行, 证明 XSS 存在.


- **浏览器为什么不弹窗?**

    虽然插入的 js 有效, 但用 Chrome 打开 xss01 不会弹窗. 这是因为现在的浏览器, Chrome, IE10 等, 本身都有 XSS 过滤的功能. 

    查看 Chrome 的 Console 有:

    ![]( {{ site.url}}/downloads/xss_03.png)

    既然现在的浏览器基本都有 XSS 过滤的功能, 为什么网站还要防御 XSS 呢?

    - IE6 和一些小众的浏览器的并没有 XSS 过滤功能. 在中国, 浏览器市场份额是:

        ![]( {{ site.url}}/downloads/xss_04.png)

    - 当插入的 js 在源码本身的 script. 标签中时, 很多浏览器不能过滤. 用 Chrome 打开 xss02 有:

        ![]( {{ site.url}}/downloads/xss_05.png)

    - 在一些特殊的场合, js 会被执行.


- **应该在什么地方处理?**

    很多时候, 开发的同学会就 XSS 由前端还是后端修复而争论.

    个人觉得 [XSS解决方案系列之一：淘宝、百度、腾讯的解决方案之瑕疵](http://www.freebuf.com/articles/web/9928.html) 的三原则说得很有道理, 即 `何时展示何时解决`.

    个人理解, 对于提交一个 url 来说, 返回 html 或者 返回数据 的接口即为"展示"处.

    也就是说, 该接口负责处理 xss .


- **是否可以在攻击发起的地方（web前端，或者form提交的地方）杜绝掉?**

    攻击者可以构造任意的 url 诱使用户点击. 来自用户的数据都是不可信的.


- **怎样修复 XSS ?**

    需要将下面的特殊字符转义:

    - & （和号） 成为 \&amp;

    - " （双引号） 成为 \&quot;

    - ' （单引号） 成为 \&#039;

    - < （小于） 成为 \&lt;

    - > （大于） 成为 \&gt;

    在 PHP 中一般采用 htmlspecialchars 函数.

    JSP 中可自己实现转义, 可参考:

    [JSP – htmlspecialchars() htmlentities() PHP like function – version 2.0](http://www.stratulat.com/blog/jsp-htmlspecialchars-htmlentities-php-like-function-version-20)

    [How to escape HTML Special characters in JSP and Java](http://java67.blogspot.com/2012/10/how-to-escape-html-special-characters-JSP-Java-Example.html)


- **过滤掉 script. 之类的标签是否解决问题?**

    对于 xss01, 过滤 html 的 tag 是可行的, 但是对于 xss02 这种插入的 js 代码在原本代码的 script. 标签中的情况, 单纯过滤 tag 是不行的.
