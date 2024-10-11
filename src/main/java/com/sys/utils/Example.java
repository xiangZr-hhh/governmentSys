package com.sys.utils;

import com.apistd.uni.Uni;
import com.apistd.uni.UniException;
import com.apistd.uni.UniResponse;
import com.apistd.uni.sms.UniSMS;
import com.apistd.uni.sms.UniMessage;

import java.util.HashMap;
import java.util.Map;

public class Example {

    public static String ACCESS_KEY_ID = "jtwBsMEj3Zj4tGZpgC9HNqnrrUMSRGdp6YENQkEJVEy8gqXF1";

    public static void main(String[] args) {
        // 初始化
        Uni.init(ACCESS_KEY_ID); // 若使用简易验签模式仅传入第一个参数即可
        Map<String, String> templateData = new HashMap<String, String>();
        templateData.put("code", "6731");

        // 构建信息
        UniMessage message = UniSMS.buildMessage()
                .setSignature("睿说abc")
                .setTo("19519926861")
                .set
                .setContent("您好");

        // 发送短信
        try {
            UniResponse res = message.send();
            System.out.println(res);
        } catch (UniException e) {
            System.out.println("Error: " + e);
            System.out.println("RequestId: " + e.requestId);
        }
    }
}
