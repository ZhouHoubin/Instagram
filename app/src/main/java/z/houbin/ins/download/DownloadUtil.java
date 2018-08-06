package z.houbin.ins.download;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

public class DownloadUtil {
    public static String trimJson(String json) {
        return json.substring(json.indexOf("{"), json.length() - 1);
    }

    public static String getDownloadPath(String url, String... suffix) {
        String dir = Environment.getExternalStorageDirectory().getPath() + "/InstagramDownload/";

        String name = url.substring(url.lastIndexOf("."));
        if (name.indexOf("?") != -1) {
            name = name.substring(0, name.indexOf("?"));
        }

        StringBuilder fileName = new StringBuilder();

        Calendar calendar = Calendar.getInstance();
        fileName.append(calendar.get(Calendar.YEAR));
        int month = calendar.get(Calendar.MONTH) + 1;
        if (month < 10) {
            fileName.append("0");
        }
        fileName.append(month);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        if (day < 10) {
            fileName.append("0");
        }
        fileName.append(day);
        fileName.append("/");
        fileName.append(md5(url));
        if (name.length() < 5) {
            fileName.append(name);
        } else {
            fileName.append(suffix[0]);
        }
        String path = dir + fileName.toString();
        if (new File(path).exists()) {
            return null;
        }
        return path;
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
