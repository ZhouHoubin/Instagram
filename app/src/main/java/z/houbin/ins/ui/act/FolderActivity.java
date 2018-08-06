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
 * 文件夹图片浏览
 */
public class FolderActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private NestedScrollView scroll;
    private HashMap<String, View> items = new HashMap<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_download);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String path = getIntent().getStringExtra("path");
        if (TextUtils.isEmpty(path)) {
            finish();
        }

        setTitle(path);

        gridLayout = findViewById(R.id.grid_layout);
        gridLayout.removeAllViews();

        update(new File(path));

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
                gridLayout.addView(child, tagInt);
                child.setTag(file);
                child.setVisibility(View.VISIBLE);
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
                            Toast.makeText(FolderActivity.this, "打开文件失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

}
