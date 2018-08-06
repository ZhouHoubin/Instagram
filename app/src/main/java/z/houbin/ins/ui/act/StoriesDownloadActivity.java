package z.houbin.ins.ui.act;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.liulishuo.filedownloader.BaseDownloadTask;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import z.houbin.ins.Ins;
import z.houbin.ins.R;
import z.houbin.ins.bean.Stories;
import z.houbin.ins.download.DownloadManager;
import z.houbin.ins.download.FileOrder;

/**
 * 快拍下载
 */
public class StoriesDownloadActivity extends AppCompatActivity implements DownloadManager.DownloadStatusUpdater {
    private String url, userName;
    private GridLayout gridLayout;
    private File downloadDir;
    private HashMap<String, View> items = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storeies_download);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            finish();
        }

        userName = url.substring(26, url.length() - 1);

        setTitle(userName);

        downloadDir = new File(DownloadManager.getImpl().getStoriesDir(userName));

        gridLayout = findViewById(R.id.grid_layout);

        update();

        showLoading();

        DownloadManager.getImpl().addUpdater(this);

        new Thread() {
            private boolean hasVideo = false;

            @Override
            public void run() {
                super.run();
                Ins ins = new Ins();
                String userId = ins.queryUserIdByUserName(userName);
                String url = String.format(Locale.CHINA, "https://www.instagram.com/graphql/query/?query_hash=45246d3fe16ccc6577e0bd297a5db1ab&variables={\"reel_ids\":[\"%s\"],\"tag_names\":[],\"location_ids\":[],\"highlight_reel_ids\":[],\"precomposed_overlay\":false}", userId);
                String html = ins.get(url);
                try {
                    Stories stories = new Gson().fromJson(html, Stories.class);
                    Stories.DataBean.ReelsMediaBean reelsMediaBean = stories.getData().getReels_media().get(0);
                    final List<Stories.DataBean.ReelsMediaBean.ItemsBean> itemsBeanList = reelsMediaBean.getItems();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setSubtitle(itemsBeanList.size() + " 帖子");
                            }
                        }
                    });
                    for (int i = 0; i < itemsBeanList.size(); i++) {
                        Stories.DataBean.ReelsMediaBean.ItemsBean itemsBean = itemsBeanList.get(i);
                        switch (itemsBean.get__typename()) {
                            case "GraphStoryImage":
                                //图片
                                List<Stories.DataBean.ReelsMediaBean.ItemsBean.DisplayResourcesBean> displayResourcesBeanList = itemsBean.getDisplay_resources();
                                String pic = displayResourcesBeanList.get(displayResourcesBeanList.size() - 1).getSrc();
                                DownloadManager.getImpl().downloadStories(userName, pic, String.valueOf(i));
                                break;
                            case "GraphStoryVideo":
                                //视频
                                List<Stories.DataBean.ReelsMediaBean.ItemsBean.VideoResourcesBean> videoResourcesBeans = itemsBean.getVideo_resources();
                                String video = videoResourcesBeans.get(videoResourcesBeans.size() - 1).getSrc();
                                DownloadManager.getImpl().downloadStories(userName, video, String.valueOf(i));
                                hasVideo = true;
                                break;
                            default:
                                break;
                        }
                    }
                    if (itemsBeanList.size() != 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (hasVideo) {
                                    Toast.makeText(StoriesDownloadActivity.this, "快拍下载较慢,稍等", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(StoriesDownloadActivity.this, "下载中...", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StoriesDownloadActivity.this, "该用户没有发布快拍", Toast.LENGTH_SHORT).show();
                            closeLoading();
                            finish();
                        }
                    });
                }
            }
        }.start();
    }

    private void update() {
        closeLoading();
        List<File> files = new ArrayList<>();
        if (downloadDir != null && downloadDir.exists() && downloadDir.isDirectory()) {
            File[] fileArray = downloadDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (!pathname.getName().contains(".temp")) {
                        return true;
                    }
                    return false;
                }
            });
            files = FileOrder.orderByName(fileArray);
        }

        System.out.println("StoriesDownloadActivity.update " + +files.size());

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (!items.containsKey(file.getName())) {
                View child = View.inflate(getApplicationContext(), R.layout.item_home_pic, null);

                TextView childTag = child.findViewById(R.id.tag);
                childTag.setText(String.valueOf(gridLayout.getChildCount() + 1));
                if (file.getName().endsWith(".mp4")) {
                    childTag.append("(视频)");
                }
                items.put(file.getName(), child);

                gridLayout.addView(child);
                child.setTag(file);
                if (i < 10) {
                    child.setVisibility(View.VISIBLE);
                } else {
                    child.setVisibility(View.INVISIBLE);
                }
                child.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent("android.intent.action.VIEW");
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            File f = (File) v.getTag();
                            Uri uri = null;
                            if (Build.VERSION.SDK_INT >= 24) {
                                uri = FileProvider.getUriForFile(getApplicationContext(), "ins.downloader", f);
                            } else {
                                uri = Uri.fromFile(f);
                            }
                            if (f.getName().endsWith(".mp4")) {
                                intent.setDataAndType(uri, "video/*");
                            } else {
                                intent.setDataAndType(uri, "image/*");
                            }
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(StoriesDownloadActivity.this, "打开文件失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    private AlertDialog loadDialog;

    private void showLoading() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("解析下载中,等一下就好。。。");
        loadDialog = builder.show();
    }

    private void closeLoading() {
        if (loadDialog != null && loadDialog.isShowing()) {
            loadDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void blockComplete(BaseDownloadTask task) {

    }

    @Override
    public void complete(BaseDownloadTask task) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                update();
            }
        });
    }

    @Override
    public void update(BaseDownloadTask task) {

    }

    @Override
    public void error(BaseDownloadTask task, Throwable throwable) {

    }
}
