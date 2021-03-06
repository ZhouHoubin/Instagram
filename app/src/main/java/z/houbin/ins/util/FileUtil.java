package z.houbin.ins.util;

import java.io.File;

public class FileUtil {
    //删除文件夹和文件夹里面的文件
    public static void deleteDir(final String pPath) {
        try {
            File dir = new File(pPath);
            deleteDirWihtFile(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteDirWihtFile(file); // 递规的方式删除文件夹
        }
        dir.delete();// 删除目录本身
    }
}
