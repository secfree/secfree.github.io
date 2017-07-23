---
layout: blog
title: "FR05: Java 程序采用 FreeRADIUS 验证"
---

Java 程序可用 [JRadius](http://www.coova.org/JRadius) 和 FreeRADIUS server 通信.

下载 [jradius-client-1.1.4-release.zip ](http://coova-dev.s3.amazonaws.com/mvn/net/jradius/jradius-client/1.1.4/jradius-client-1.1.4-release.zip), 解压得到多个 jar 文件.

---

编写下面的测试代码 RadiusAuthTest.java

```java
import net. jradius.client .RadiusClient;
import net. jradius.client .auth. RadiusAuthenticator;

import net. jradius.packet .AccessRequest;
import net. jradius.packet .RadiusRequest;
import net. jradius.packet .RadiusResponse;
import net. jradius.packet .attribute.AttributeFactory;
import net. jradius.packet .attribute.AttributeList;
import net. jradius.packet .attribute.RadiusAttribute;
import net. jradius.packet .attribute.value.AttributeValue ;
import net. jradius.dictionary .Attr_UserName;
import net. jradius.dictionary .Attr_UserPassword;

import java. io.IOException;

import java. net.InetAddress ;
import java. net.UnknownHostException ;


/**
 *
 * need jar:
 *    1.jradius-core-1.1.4.jar
 *    2.jradius-dictionary-1.1.4.jar
 * @author xtong
 *
 * edit by dengzhaoqun at 2014/2/18 19:12
 */

public class RadiusAuthTest {
    private static final String METHOD_PAP = "PAP" ;
    private static final int TIMEOUT = 3 ;
    private static final int RETRIES = 3 ;
    private static final int CUSTOMISE_CODE = 33333 ; // vendor code
    private static final int ATTR_CODE_1 = 1 ;
    private static final byte RADIUS_CODE_ACCESS_ACCEPT = ( byte) 2;
    private static final byte RADIUS_CODE_ACCESS_REJECT = ( byte) 3;
    private final String radius_server_ip = "freeradius_server_ip" ;
    private final String secret = "secret_value" ;
    private final int authPort = 1812 ;
    private final int acctPort = 1813 ;
    private final String userName = "dzq" ;

    public void doAuth (String password) {

        String protocol = METHOD_PAP;

        // build attribute list by attributes
        AttributeList sendAttributes = new AttributeList();

        sendAttributes .add( new Attr_UserName("user_name" ));
        sendAttributes .add( new Attr_UserPassword(password));

        RadiusAuthenticator auth = RadiusClient.getAuthProtocol(protocol);
        InetAddress inet = null ;

        try {
            inet = InetAddress.getByName(radius_server_ip );
        } catch (UnknownHostException e) {
            System .out. println("get ip address failed, e=" + e.getMessage());
        }

        // initial radius client and create Access-Request packet
        RadiusClient client = null ;
        try {
            client = new RadiusClient(inet , secret, authPort, acctPort , TIMEOUT);
        } catch (IOException e) {
            System .out. println("radius connect failed, e=" + e.getMessage());
            return;
        }
        RadiusRequest request = new AccessRequest(client , sendAttributes);
        RadiusResponse reply = null ;

        // send RADIUS Access-Request packet and receive response may be RADIUS
        // Access-Accept packet or RADIUS Access-Reject packet
        try {
            reply = client.authenticate((AccessRequest ) request, auth, RETRIES);
        } catch (Exception e) {
            System .out. println("No response from RADIUS server->" +
                radius_server_ip + ":" + authPort);
            return;
        }

        if ( reply.getCode () == RADIUS_CODE_ACCESS_REJECT) {
            System .out. println("Request be rejected by RADIUS server->" +
                radius_server_ip + ":" + authPort);

            return;
        }

        System .out. println("request=" + request.toString ());
        System .out. println("reply=" + reply.toString ());

        // get attribute by analyze Access-Accept package
        RadiusAttribute attr = reply. findAttribute((CUSTOMISE_CODE * 65536) +
                ATTR_CODE_1 );

        if ( attr == null) {
        } else {
            AttributeValue av = attr. getValue();
            int retValue = bytes2int(av .getBytes(), 0, av.getBytes().length);
            System .out. println("Value of attribute 1 is " + retValue);
        }
    }

    /**
     * <p>
     * Convert bytes into int.
     * </p>
     *
     * @param a_bytes
     *            byte array to be converted.
     * @param a_offset
     *            Start postion relative to the given byte array.
     * @param a_offset_len
     *            Number of bytes need to be converted.
     * @return int value converted from bytes.
     */
    public static int bytes2int (byte[] a_bytes, int a_offset, int a_offset_len) {
        if ( a_offset_len > 4) {
            throw new NumberFormatException("Offset length[" + a_offset_len +
                "] should be no more than 4." );
        }

        if (( a_offset + a_offset_len ) > a_bytes.length ) {
            throw new NumberFormatException("The sum of offset[" + a_offset +
                "] and offset length[" + a_offset_len +
                "] should be no more than the length of given bytes." );
        }

        int result = 0;

        for ( int i = 0 ; i < a_offset_len; i ++) {
            result += (( a_bytes[(a_offset + a_offset_len) - 1 - i] & 0xff) << ( i * 8));
        }

        return result;
    }

    public static void main (String[] args) {
        if ( args.length < 1 ){
            System .out. println("java RadiusAuthTest password" );
            return;
        }
        RadiusAuthTest authTest = new RadiusAuthTest();
        authTest .doAuth( args[0 ]);
    }
}
```

---

将jradius-client-1.1.4-release.zip 中的 jar 文件拷贝到代码所在的目录, 执行

```
$ javac -cp ".:./*" RadiusAuthTest.java
$ java -cp ".:./*" RadiusAuthTest password
```

**参考**

1. [用Jradius 实现RADIUS Auth](http://hi.baidu.com/kefengw/item/a50aa1ccd5a4a924a1b50adb)

2. [JRadius ClientAPI](http://www.coova.org/JRadius/ClientAPI)
