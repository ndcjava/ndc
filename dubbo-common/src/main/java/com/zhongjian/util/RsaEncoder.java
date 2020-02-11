package com.zhongjian.util;


import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.Map.Entry;

public class RsaEncoder {

    private static final String CHARSET = "UTF-8";
    private static final String RSA_ALGORITHM = "RSA";

    public static Map genKeyPair() {
        HashMap map = null;
        try {
            map = new HashMap();
            KeyPairGenerator e = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            e.initialize(1024);
            KeyPair kp = e.generateKeyPair();
            String pubKeyStr = byteArr2HexString(kp.getPublic().getEncoded());
            String priKeyStr = byteArr2HexString(kp.getPrivate().getEncoded());
            map.put("publicKey", pubKeyStr);
            map.put("privateKey", priKeyStr);
        } catch (Exception var5) {
            var5.printStackTrace();
            map = null;
        }
        return map;
    }

    public static String sign(String data, String privateKey) {
        String sign = null;
        try {
            PKCS8EncodedKeySpec e = new PKCS8EncodedKeySpec(hexString2ByteArr(privateKey));
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey priKey = keyFactory.generatePrivate(e);
            Signature si = Signature.getInstance("SHA1WithRSA");
            si.initSign(priKey);
            si.update(data.getBytes(CHARSET));
            byte[] dataSign = si.sign();
            sign = byteArr2HexString(dataSign);
        } catch (Exception var8) {
            var8.printStackTrace();
            sign = null;
        }
        return sign;
    }

    public static boolean verify(String data, String sign, String publicKey) {
        boolean succ = false;
        try {
            Signature e = Signature.getInstance("SHA1WithRSA");
            KeyFactory keyFac = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey puk = keyFac.generatePublic(new X509EncodedKeySpec(hexString2ByteArr(publicKey)));
            e.initVerify(puk);
            e.update(data.getBytes(CHARSET));
            succ = e.verify(hexString2ByteArr(sign));
        } catch (Exception var7) {
            var7.printStackTrace();
            succ = false;
        }
        return succ;
    }

    public static boolean verify(Map data, String sign, String publicKey) {
        boolean succ = false;
        try {
            Signature e = Signature.getInstance("SHA1WithRSA");
            KeyFactory keyFac = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey puk = keyFac.generatePublic(new X509EncodedKeySpec(hexString2ByteArr(publicKey)));
            e.initVerify(puk);
            e.update(sort((Map) data).getBytes(CHARSET));
            succ = e.verify(hexString2ByteArr(sign));
        } catch (Exception var7) {
            var7.printStackTrace();
            succ = false;
        }
        return succ;
    }

    public static String encrypt(String data, String publicKey) {
        String encryptData = null;
        try {
            KeyFactory e = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey pubKey = e.generatePublic(new X509EncodedKeySpec(hexString2ByteArr(publicKey)));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(1, pubKey);
            byte[] result = cipher.doFinal(data.getBytes(CHARSET));
            encryptData = byteArr2HexString(result);
        } catch (Exception var7) {
            var7.printStackTrace();
            encryptData = null;
        }
        return encryptData;
    }

    public static String decrypt(String encryptedData, String privateKey) {
        String decryptData = null;
        try {
            PKCS8EncodedKeySpec e = new PKCS8EncodedKeySpec(hexString2ByteArr(privateKey));
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey priKey = keyFactory.generatePrivate(e);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(2, priKey);
            byte[] result = cipher.doFinal(hexString2ByteArr(encryptedData));
            decryptData = new String(result, CHARSET);
        } catch (Exception var8) {
            var8.printStackTrace();
            decryptData = null;
        }
        return decryptData;
    }

    private static String sort(Map data) {
        LinkedList list = new LinkedList(data.entrySet());
        Collections.sort(list);
        StringBuffer buffer = new StringBuffer();
        Iterator i$ = list.iterator();
        while (i$.hasNext()) {
            Entry entry = (Entry) i$.next();
            buffer.append(String.format("%s=%s", new Object[]{entry.getKey(), entry.getValue()})).append("&");
        }
        buffer.deleteCharAt(buffer.length() - 1);
        return buffer.toString();
    }

    private static String escape(String data) {
        data = data.replaceAll("&gt;", "#gt;").replaceAll("&lt;", "#lt;");
        return data;
    }

    private static String decode(String data) {
        data = data.replaceAll("#gt;", "&gt;").replaceAll("#lt;", "&lt;");
        return data;
    }

    private static String sort(String data) {
        TreeMap dataMap = new TreeMap();
        data = escape(data);
        String[] params = data.split("&");
        for (int paramsStr = 0; paramsStr < params.length; ++paramsStr) {
            String[] ret = params[paramsStr].split("=", 2);
            String entry = "";
            String value = "";
            if (ret.length == 2) {
                entry = ret[0];
                value = ret[1];
            } else if (ret.length == 1) {
                entry = ret[0];
            }
            dataMap.put(entry, value);
        }
        StringBuffer var7 = new StringBuffer();
        Iterator var8 = dataMap.entrySet().iterator();
        while (var8.hasNext()) {
            Entry var10 = (Entry) var8.next();
            var7.append((String) var10.getKey()).append("=").append(var10.getValue() == null ? "" : (String) var10.getValue()).append("&");
        }
        var7.deleteCharAt(var7.length() - 1);
        String var9 = var7.toString();
        var9 = decode(var9);
        return var9;
    }

    public static String byteArr2HexString(byte[] bytearr) {
        if (bytearr == null) {
            return "null";
        } else {
            StringBuffer sb = new StringBuffer();
            for (int k = 0; k < bytearr.length; ++k) {
                if ((bytearr[k] & 255) < 16) {
                    sb.append("0");
                }
                sb.append(Integer.toString(bytearr[k] & 255, 16));
            }
            return sb.toString();
        }
    }

    public static byte[] hexString2ByteArr(String hexString) {
        if (hexString != null && hexString.length() % 2 == 0) {
            byte[] dest = new byte[hexString.length() / 2];
            for (int i = 0; i < dest.length; ++i) {
                String val = hexString.substring(2 * i, 2 * i + 2);
                dest[i] = (byte) Integer.parseInt(val, 16);
            }
            return dest;
        } else {
            return new byte[0];
        }
    }

    public static Boolean getUrlAndVerify(TreeMap<String, String> treeMap,String sign,String publicKey) {
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<Entry<String, String>> it = treeMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            stringBuffer.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        String result = stringBuffer.substring(0, stringBuffer.length() - 1);
        return RsaEncoder.verify(result, sign, publicKey);
    }

    public static String sortParameterByUrl(TreeMap<String, String> treeMap) {
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<Entry<String, String>> it = treeMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            stringBuffer.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        String result = stringBuffer.substring(0, stringBuffer.length() - 1);
        System.out.println(result);
        return result;
    }

    public static String create_nonce_str() {
        return UUID.randomUUID().toString().replace("-","");
    }
  
}