package z.houbin.ins;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloader;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import z.houbin.ins.download.DownloadManager;
import z.houbin.ins.ui.FollowFragment;
import z.houbin.ins.ui.HelpFragment;
import z.houbin.ins.ui.InstagramFragment;
import z.houbin.ins.util.ACache;

public class MainActivity extends AppCompatActivity implements DownloadManager.DownloadStatusUpdater {

    private List<Fragment> fragmentList = new ArrayList<>();

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    showFragment(0);
                    return true;
                case R.id.navigation_follow:
                    Toast.makeText(MainActivity.this, "不可用", Toast.LENGTH_SHORT).show();
                    //showFragment(1);
                    return true;
                case R.id.navigation_help:
                    showFragment(2);
                    return true;

            }
            return false;
        }
    };

    private void showFragment(int index) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        for (int i = 0; i < fragmentList.size(); i++) {
            if (i == index) {
                transaction.show(fragmentList.get(i));
            } else {
                transaction.hide(fragmentList.get(i));
            }
        }
        transaction.commit();
    }

    private void add() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        for (Fragment fragment : fragmentList) {
            transaction.add(R.id.frame, fragment);
        }
        transaction.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FileDownloader.setup(getApplicationContext());

        fragmentList.add(new InstagramFragment());
        fragmentList.add(new FollowFragment());
        fragmentList.add(new HelpFragment());

        add();

        showFragment(0);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        DownloadManager.getImpl().addUpdater(this);

        checkPermission();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, final Throwable e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), e.getMessage() + "", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        ACache cache = ACache.get(Constant.cacheDir);
        String count = cache.getAsString("launcher_count");
        if (TextUtils.isEmpty(count)) {
            showFirstDialog();
            count = "0";
        }
        cache.put("launcher_count", String.valueOf(Integer.parseInt(count) + 1));
    }

    private void showFirstDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("温馨提示");
        builder.setMessage("#所有功能需要开VPN\r\n#所有功能需要登录\r\n#文件会自动下载到系统图库或者相册\r\n#有问题右下角加群反馈");
        builder.show();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
    }

    @Override
    public void blockComplete(BaseDownloadTask task) {

    }

    @Override
    public void complete(BaseDownloadTask task) {
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
        //Toast.makeText(this, "下载完成", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void update(BaseDownloadTask task) {
        System.out.println(task.getFilename() + " / " + task.getSmallFileTotalBytes() + " / " + task.getSmallFileSoFarBytes());
    }

    @Override
    public void error(BaseDownloadTask task, final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "下载错误:" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownloadManager.getImpl().removeUpdater(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
