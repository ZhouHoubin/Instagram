package z.houbin.ins.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

import z.houbin.ins.Constant;
import z.houbin.ins.R;
import z.houbin.ins.download.DownloadManager;
import z.houbin.ins.util.ACache;

public class InstagramWebActivity extends AppCompatActivity implements DownloadManager.DownloadStatusUpdater {
    private String url;
    private String name;
    private boolean justUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        setTitle("Instagram");

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (getIntent().getStringExtra("url") != null) {
            url = getIntent().getStringExtra("url");
            justUrl = true;
            getSupportActionBar().setSubtitle("登录中...(登录成功会自动关闭,请勿手动返回)");
        } else {
            url = getIntent().getStringExtra("data");
            name = url.substring(26, url.length() - 1);
            getSupportActionBar().setSubtitle("快拍下载(需要登录)(加载中)");
        }
        final WebView web = findViewById(R.id.web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        web.setWebChromeClient(new WebChromeClient() {

        });

        web.setWebViewClient(new WebViewClient() {
            private boolean firstPicture = true;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                firstPicture = true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager cookieManager = CookieManager.getInstance();
                String cookie = cookieManager.getCookie(url);
                ACache cache = ACache.get(Constant.cacheDir);
                if (cookie.contains("csrftoken") && cookie.contains("sessionid")) {
                    cache.put("cookie", cookie, ACache.TIME_DAY);

                    int start = cookie.indexOf("csrftoken=") + 10;
                    int end = start + 32;
                    String csrftoken = cookie.substring(start, end);
                    cache.put("csrftoken", csrftoken, ACache.TIME_DAY);
                    if (justUrl) {
                        Toast.makeText(getApplicationContext(), "登录完成", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                System.out.println("Cookie  :" + cookie);
                super.onPageFinished(view, url);
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                System.out.println(url);
                if (justUrl) {
                    return super.shouldInterceptRequest(view, url);
                }
                if (url.endsWith(".mp4")) {
                    startDownload(url);
                } else if (url.endsWith(".jpg")) {
                    if (!firstPicture) {
                        startDownload(url);
                    } else {
                        firstPicture = false;
                    }
                }
                return super.shouldInterceptRequest(view, url);
            }
        });
        web.loadUrl(getStories());
        DownloadManager.getImpl().addUpdater(this);
    }

    private void startDownload(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String res = DownloadManager.getImpl().startDownload2(url);
                if (res == null) {
                    Toast.makeText(InstagramWebActivity.this, "文件已经存在", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(InstagramWebActivity.this, "开始下载 - " + res, Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownloadManager.getImpl().removeUpdater(this);
    }

    private String getStories() {
        if (justUrl) {
            return url;
        }
        return String.format(Locale.CHINA, "https://www.instagram.com/%s/", name);
    }

    @Override
    public void blockComplete(BaseDownloadTask task) {
        System.out.println("InstagramWebActivity.blockComplete");
    }

    @Override
    public void complete(BaseDownloadTask task) {
        System.out.println("InstagramWebActivity.complete");
        try {
            String path = task.getPath();
            if (path.endsWith(".jpg") || path.endsWith(".png") | path.endsWith(".gif") || path.endsWith(".webp")) {
                //插入图片
                MediaStore.Images.Media.insertImage(getContentResolver(), path, "title", "description");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(task.getPath()))));
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(task.getPath()).getParentFile())));
        Toast.makeText(this, "下载完成", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void update(BaseDownloadTask task) {
        System.out.println("InstagramWebActivity.update");
    }

    @Override
    public void error(BaseDownloadTask task, final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(InstagramWebActivity.this, "下载错误:" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(getClass().getName());
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(getClass().getName());
    }
}
