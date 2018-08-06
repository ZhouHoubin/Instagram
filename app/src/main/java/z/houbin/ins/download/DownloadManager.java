package z.houbin.ins.download;

import android.os.Environment;
import android.text.TextUtils;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DownloadManager {
    private final static class HolderClass {
        private final static DownloadManager INSTANCE = new DownloadManager();
    }

    public static DownloadManager getImpl() {
        return HolderClass.INSTANCE;
    }

    private ArrayList<DownloadStatusUpdater> updaterList = new ArrayList<>();

    public void startDownload(final String url) {
        String path = DownloadUtil.getDownloadPath(url);
        if (TextUtils.isEmpty(path)) {
            System.out.println("文件已经存在");
            return;
        }
        FileDownloader.getImpl().create(url)
                .setPath(path)
                .setListener(lis)
                .start();
    }

    public String getHomePicDir(String userName) {
        return Environment.getExternalStorageDirectory().getPath() + "/InstagramDownload/Home/" + userName + "/";
    }

    public String getStoriesDir(String userName) {
        return Environment.getExternalStorageDirectory().getPath() + "/InstagramDownload/Stories/" + userName + "/";
    }

    public void downloadStories(String userName, final String url, String tag) {
        String path = getStoriesDir(userName);
        if (tag.length() == 1) {
            tag = "00" + tag;
        } else if (tag.length() == 2) {
            tag = "0" + tag;
        }
        path += tag;
        path += "-";
        path += DownloadUtil.md5(url);
        path += getSuffix(url);

        System.out.println("下载地址: " + url);
        System.out.println("下载路径: " + path);

        if (new File(path).exists()) {
            System.out.println("文件已经存在");
            return;
        }
        FileDownloader.getImpl().create(url)
                .setPath(path)
                .setListener(lis)
                .start();
    }

    public void downloadHomePic(String userName, final String url, String tag) {
        String path = getHomePicDir(userName);
        if (tag.length() == 1) {
            tag = "00" + tag;
        } else if (tag.length() == 2) {
            tag = "0" + tag;
        }
        path += tag;
        path += "-";
        path += DownloadUtil.md5(url);
        path += getSuffix(url);

        System.out.println("下载地址: " + url);
        System.out.println("下载路径: " + path);

        if (new File(path).exists()) {
            System.out.println("文件已经存在");
            return;
        }
        FileDownloader.getImpl().create(url)
                .setPath(path)
                .setListener(lis)
                .start();
    }

    private String getSuffix(String url) {
        return url.substring(url.lastIndexOf("."));
    }

    public String startDownload2(final String url) {
        if (url.endsWith(".jpg")) {
            return startDownload(url, ".jpg");
        } else if (url.endsWith(".png")) {
            return startDownload(url, ".png");
        } else if (url.endsWith(".mp4")) {
            return startDownload(url, ".mp4");
        }
        return url;
    }

    public String startDownload(final String url, String suffix) {
        String path = DownloadUtil.getDownloadPath(url, suffix);
        if (TextUtils.isEmpty(path)) {
            System.out.println("文件已经存在");
            return null;
        }
        FileDownloader.getImpl().create(url)
                .setPath(path)
                .setListener(lis)
                .start();
        return url;
    }


    public String startDownload(final String url, String suffix, String tag) {
        String path = DownloadUtil.getDownloadPath(url, suffix);
        if (TextUtils.isEmpty(path)) {
            System.out.println("文件已经存在");
            return null;
        }
        FileDownloader.getImpl().create(url)
                .setPath(path)
                .setListener(lis)
                .setTag(0, tag)
                .start();
        return url;
    }

    public void addUpdater(final DownloadStatusUpdater updater) {
        if (!updaterList.contains(updater)) {
            updaterList.add(updater);
        }
    }

    public boolean removeUpdater(final DownloadStatusUpdater updater) {
        return updaterList.remove(updater);
    }


    private FileDownloadListener lis = new FileDownloadListener() {
        @Override
        protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            update(task);
        }

        @Override
        protected void started(BaseDownloadTask task) {
            super.started(task);
            update(task);
        }

        @Override
        protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
            super.connected(task, etag, isContinue, soFarBytes, totalBytes);
            update(task);
        }

        @Override
        protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            update(task);
        }

        @Override
        protected void blockComplete(BaseDownloadTask task) {
            listBlockComplete(task);
        }

        @Override
        protected void completed(BaseDownloadTask task) {
            if (isVideo(task.getTargetFilePath())) {
                //changeMd5(task.getTargetFilePath());
            }
            listComplete(task);
        }

        private void changeMd5(String path) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(path, true));
                writer.write(System.currentTimeMillis() + "");
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean isVideo(String path) {
            if (path.endsWith(".3gp") || path.endsWith(".mp4") || path.endsWith(".avi") || path.endsWith(".rmvb")) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            update(task);
        }

        @Override
        protected void error(BaseDownloadTask task, Throwable e) {
            listError(task, e);
        }

        @Override
        protected void warn(BaseDownloadTask task) {
            update(task);
        }
    };

    private void listBlockComplete(final BaseDownloadTask task) {
        final List<DownloadStatusUpdater> updaterListCopy = (List<DownloadStatusUpdater>) updaterList.clone();
        for (DownloadStatusUpdater downloadStatusUpdater : updaterListCopy) {
            downloadStatusUpdater.blockComplete(task);
        }
    }

    private void listComplete(final BaseDownloadTask task) {
        final List<DownloadStatusUpdater> updaterListCopy = (List<DownloadStatusUpdater>) updaterList.clone();
        for (DownloadStatusUpdater downloadStatusUpdater : updaterListCopy) {
            downloadStatusUpdater.complete(task);
        }
    }

    private void listError(final BaseDownloadTask task, Throwable e) {
        final List<DownloadStatusUpdater> updaterListCopy = (List<DownloadStatusUpdater>) updaterList.clone();
        for (DownloadStatusUpdater downloadStatusUpdater : updaterListCopy) {
            downloadStatusUpdater.error(task, e);
        }
    }

    private void update(final BaseDownloadTask task) {
        final List<DownloadStatusUpdater> updaterListCopy = (List<DownloadStatusUpdater>) updaterList.clone();
        for (DownloadStatusUpdater downloadStatusUpdater : updaterListCopy) {
            downloadStatusUpdater.update(task);
        }
    }

    public interface DownloadStatusUpdater {
        void blockComplete(BaseDownloadTask task);

        void complete(BaseDownloadTask task);

        void update(BaseDownloadTask task);

        void error(BaseDownloadTask task, Throwable throwable);
    }
}
