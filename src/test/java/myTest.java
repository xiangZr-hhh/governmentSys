import com.google.common.annotations.VisibleForTesting;
import com.sys.utils.MyMD5Util;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/*
        张睿相   Java
*/
public class myTest {

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        System.out.println(MyMD5Util.getEncryptedPwd("123456"));
    }

}
