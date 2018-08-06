package z.houbin.ins.ui.act;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.NestedScrollView;
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

import z.houbin.ins.Constant;
import z.houbin.ins.Ins;
import z.houbin.ins.R;
import z.houbin.ins.bean.HomePic;
import z.houbin.ins.download.DownloadManager;
import z.houbin.ins.download.FileOrder;
import z.houbin.ins.ui.InstagramWebActivity;
import z.houbin.ins.ui.SquareLayout;
import z.houbin.ins.util.ACache;
import z.houbin.ins.util.FileUtil;
import z.houbin.ins.util.VideoUtil;

/**
 * 主页图片下载
 */
public class HomeDownloadActivity extends AppCompatActivity implements DownloadManager.DownloadStatusUpdater {
    private String url, userName;
    private GridLayout gridLayout;
    private File downloadDir;
    private NestedScrollView scroll;
    private int totalDownloadCount = -1;
    private boolean isDownloadStart;
    private AlertDialog loadDialog;
    private ACache cache = ACache.get(Constant.cacheDir);
    private HashMap<String, View> items = new HashMap<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_download);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            finish();
        }

        items.clear();

        userName = url.substring(26, url.length() - 1);

        setTitle(userName);

        downloadDir = new File(DownloadManager.getImpl().getHomePicDir(userName));
        //清空以前下载的
        FileUtil.deleteDir(downloadDir.getPath());

        DownloadManager.getImpl().addUpdater(this);

        gridLayout = findViewById(R.id.grid_layout);
        gridLayout.removeAllViews();
        update(downloadDir);

        scroll = findViewById(R.id.scroll);
        scroll.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                GridLayout child = (GridLayout) scroll.getChildAt(0);
                Rect hitRect = new Rect();
                child.getHitRect(hitRect);
                for (int i = 0; i < child.getChildCount(); i++) {
                    if (child.getChildAt(i) instanceof SquareLayout) {
                        SquareLayout squareLayout = (SquareLayout) child.getChildAt(i);
                        if (squareLayout.getLocalVisibleRect(hitRect) && !squareLayout.isInit()) {
                            squareLayout.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });

        showLoading();
    }

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

    private void update(File dir) {
        closeLoading();
        List<File> files = new ArrayList<>();
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] fileArray = dir.listFiles(new FileFilter() {
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
        System.out.println("HomeDownloadActivity.update " + files.size());
        if (getSupportActionBar() != null) {
            if (files.size() != 0) {
                String progress = VideoUtil.getFileTag(files.get(files.size() - 1));
                getSupportActionBar().setSubtitle(String.valueOf(totalDownloadCount) + " 帖子#已下载 " + progress);
            }
        }

        if (gridLayout.getChildCount() != totalDownloadCount) {
            for (int i = 0; i < totalDownloadCount; i++) {
                TextView textView = new TextView(getApplicationContext());
                gridLayout.addView(textView, i);
            }
        }

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (!items.containsKey(file.getName()) || file.isDirectory()) {
                View child = View.inflate(getApplicationContext(), R.layout.item_home_pic, null);

                TextView childTag = child.findViewById(R.id.tag);
                String tag = VideoUtil.getFileTag(file);
                int tagInt = Integer.parseInt(tag) - 1;
                childTag.setText(tag);
                if (file.getName().endsWith(".mp4")) {
                    childTag.append("(视频)");
                }
                if (file.isDirectory()) {
                    childTag.append("(多图)");
                }
                items.put(file.getName(), child);
                gridLayout.removeViewAt(tagInt);
                gridLayout.addView(child, tagInt);
                child.setTag(file);
                child.setVisibility(View.VISIBLE);
                child.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            File f = (File) v.getTag();
                            Intent intent;
                            if (f.isDirectory()) {
                                intent = new Intent(getApplicationContext(), FolderActivity.class);
                                intent.putExtra("path", f.getPath());
                            } else {
                                intent = new Intent("android.intent.action.VIEW");
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
                            }
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(HomeDownloadActivity.this, "打开文件失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    @Override
    public void blockComplete(BaseDownloadTask task) {

    }

    private Handler handler = new Handler();

    @Override
    public void complete(BaseDownloadTask task) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                update(downloadDir);
            }
        }, 1500);
    }

    @Override
    public void update(BaseDownloadTask task) {

    }

    @Override
    public void error(BaseDownloadTask task, Throwable throwable) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "某资源下载错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class DownloadThread extends Thread {
        private String url, userId;
        private Ins ins = new Ins();
        private int tag;

        public DownloadThread(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            super.run();
            isDownloadStart = true;
            //https://www.instagram.com/taaarannn/
            userId = ins.queryUserIdByUserName(userName);
            String url = String.format(Locale.CHINA, "https://www.instagram.com/graphql/query/?query_hash=6305d415e36c0a5f0abb6daba312f2dd&variables={\"id\":\"%s\",\"first\":20,\"after\":\"\"}", userId);
            String html = ins.get(url);
            HomePic homePic = new Gson().fromJson(html, HomePic.class);
            disposeJson(true, homePic);
        }

        private void disposeJson(boolean first, HomePic homePic) {
            if (homePic == null) {
                return;
            }
            if (homePic.getStatus().equals("ok")) {
                final HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean timelineMediaBean = homePic.getData().getUser().getEdge_owner_to_timeline_media();
                totalDownloadCount = timelineMediaBean.getCount();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setSubtitle(String.valueOf(totalDownloadCount) + " 帖子");
                        }
                    }
                });
                if (first) {
                    if (getFiles().length >= totalDownloadCount) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeLoading();
                            }
                        });
                        return;
                    }
                }
                System.out.println("count:" + timelineMediaBean.getCount());
                List<HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX> edges = timelineMediaBean.getEdges();
                for (HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX edge : edges) {
                    HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX.NodeBeanXX node = edge.getNode();
                    switch (node.get__typename()) {
                        case "GraphImage":
                            //图片
                            List<HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX.NodeBeanXX.DisplayResourcesBean> displayResourcesBeanList = node.getDisplay_resources();
                            String pic = displayResourcesBeanList.get(displayResourcesBeanList.size() - 1).getSrc();
                            tag++;
                            DownloadManager.getImpl().downloadHomePic(userName, pic, String.valueOf(tag));
                            break;
                        case "GraphVideo":
                            //视频
                            String video = node.getVideo_url();
                            tag++;
                            DownloadManager.getImpl().downloadHomePic(userName, video, String.valueOf(tag));
                            break;
                        case "GraphSidecar":
                            //多图
                            HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX.NodeBeanXX.EdgeSidecarToChildren sidecarToChildren = node.getEdge_sidecar_to_children();
                            List<HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX.NodeBeanXX.EdgeSidecarToChildren.EdgesBean> sidecarEdges = sidecarToChildren.getEdges();
                            tag++;
                            int index = 1;
                            for (HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX.NodeBeanXX.EdgeSidecarToChildren.EdgesBean sidecarEdge : sidecarEdges) {
                                HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX.NodeBeanXX.EdgeSidecarToChildren.EdgesBean.NodeBean sidebarNode = sidecarEdge.getNode();
                                if ("GraphImage".equals(sidebarNode.get__typename())) {//图片
                                    List<HomePic.DataBean.UserBean.EdgeOwnerToTimelineMediaBean.EdgesBeanXX.NodeBeanXX.EdgeSidecarToChildren.EdgesBean.NodeBean.DisplayResourcesBean> sidecarDisplayResourcesBeanList = sidebarNode.getDisplay_resources();
                                    String url = sidecarDisplayResourcesBeanList.get(sidecarDisplayResourcesBeanList.size() - 1).getSrc();
                                    DownloadManager.getImpl().downloadHomePic(userName + "/" + tag, url, String.valueOf(index));
                                } else if ("GraphVideo".equals(sidebarNode.get__typename())) {
                                    DownloadManager.getImpl().downloadHomePic(userName + "/" + tag, sidebarNode.getVideo_url(), String.valueOf(index));
                                }
                                index++;
                            }
                            break;
                        default:
                            break;
                    }
                }
                if (timelineMediaBean.getPage_info().isHas_next_page()) {
                    String nextCursor = timelineMediaBean.getPage_info().getEnd_cursor();
                    url = String.format(Locale.CHINA, "https://www.instagram.com/graphql/query/?query_hash=6305d415e36c0a5f0abb6daba312f2dd&variables={\"id\":\"%s\",\"first\":20,\"after\":\"%s\"}", userId, nextCursor);
                    String html = ins.get(url);
                    HomePic homeBean = new Gson().fromJson(html, HomePic.class);
                    disposeJson(false, homeBean);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeDownloadActivity.this, "解析完成,所有任务后台执行中", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String cookie = cache.getAsString("cookie");
        String csrftoken = cache.getAsString("csrftoken");
        if (TextUtils.isEmpty(cookie) || TextUtils.isEmpty(csrftoken)) {
            Toast.makeText(getApplicationContext(), "请登录", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), InstagramWebActivity.class);
            intent.putExtra("url", "http://www.instagram.com");
            startActivity(intent);
        } else {
            if (TextUtils.isEmpty(url)) {
                finish();
            } else {
                if (isDownloadStart) {
                    return;
                }
                if (downloadDir != null && downloadDir.exists()) {
                    File[] files = getFiles();
                    if (files.length == totalDownloadCount && totalDownloadCount != -1) {
                        Toast.makeText(this, "已经下载完成", Toast.LENGTH_SHORT).show();
                        closeLoading();
                        return;
                    }
                }
                new DownloadThread(url).start();
            }
        }
    }

    private File[] getFiles() {
        if (downloadDir != null && downloadDir.exists()) {
            File[] files = downloadDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.getName().endsWith(".temp")) {
                        return false;
                    }
                    return true;
                }
            });
            return files;
        } else {
            return new File[0];
        }
    }
}
