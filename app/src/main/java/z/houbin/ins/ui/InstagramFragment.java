package z.houbin.ins.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.squareup.picasso.Picasso;
import com.umeng.analytics.MobclickAgent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import z.houbin.ins.BaseModule;
import z.houbin.ins.Constant;
import z.houbin.ins.Ins;
import z.houbin.ins.Instagram;
import z.houbin.ins.LoadCallBack;
import z.houbin.ins.R;
import z.houbin.ins.bean.__A;
import z.houbin.ins.download.DownloadManager;
import z.houbin.ins.ui.act.HomeDownloadActivity;
import z.houbin.ins.ui.act.StoriesDownloadActivity;
import z.houbin.ins.util.ACache;

/**
 * Instagram
 */
public class InstagramFragment extends Fragment implements View.OnClickListener, LoadCallBack, DownloadManager.DownloadStatusUpdater {
    protected Handler handler = new Handler();
    protected EditText iEditText;
    protected Button iDownload;
    protected Button iDownloadStories;
    protected Button iDownloadHome;
    protected Button iDownloadHead;
    protected Button iPaste;
    protected Button iClear;
    protected String label;
    private Instagram instagram = new Instagram();
    private ACache cache = ACache.get(Constant.cacheDir);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_ins, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        iEditText = view.findViewById(R.id.edit);
        iPaste = view.findViewById(R.id.btnPaste);
        iClear = view.findViewById(R.id.btnClear);
        iDownload = view.findViewById(R.id.download);
        if (iClear != null) {
            iClear.setOnClickListener(this);
        }
        if (iPaste != null) {
            iPaste.setOnClickListener(this);
        }
        if (iDownload != null) {
            iDownload.setOnClickListener(this);
        }

        instagram.setLoadListener(this);
        iDownloadHome = view.findViewById(R.id.download_home);
        iDownloadHome.setOnClickListener(this);

        iDownloadHead = view.findViewById(R.id.download_head);
        iDownloadHead.setOnClickListener(this);

        iDownloadStories = view.findViewById(R.id.download_stories);
        iDownloadStories.setOnClickListener(this);

        iEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0) {
                    String text = s.toString();
                    String text2 = text.replaceAll("/", "");
                    int len = text.length() - text2.length();
                    if (len == 4) {
                        //主页地址
                        iDownloadHome.setVisibility(View.VISIBLE);
                        iDownloadStories.setVisibility(View.VISIBLE);
                        iDownloadHead.setVisibility(View.VISIBLE);
                        iDownload.setVisibility(View.GONE);
                    } else {
                        //单帖子
                        iDownloadHome.setVisibility(View.GONE);
                        iDownloadHead.setVisibility(View.GONE);
                        iDownloadStories.setVisibility(View.GONE);
                        iDownload.setVisibility(View.VISIBLE);
                    }
                } else {
                    //单帖子
                    iDownloadHome.setVisibility(View.GONE);
                    iDownloadStories.setVisibility(View.GONE);
                    iDownloadHead.setVisibility(View.GONE);
                    iDownload.setVisibility(View.VISIBLE);
                }
            }
        });

        DownloadManager.getImpl().addUpdater(this);
    }

    protected String getInput() {
        if (iEditText != null) {
            return iEditText.getText().toString();
        } else {
            return null;
        }
    }

    public void onDownloadClick() {
        if (getInput() != null) {
            if (getInput().contains("https://www.instagram.com/p/")) {
                instagram.parse(getInput());
            } else if (getInput().startsWith("https://www.instagram.com/")) {
                //个人主页
                Intent intent = new Intent(getActivity(), InstagramWebActivity.class);
                intent.putExtra("data", getInput());
                startActivity(intent);
            }
        }
    }

    public void clear() {
        if (iEditText != null) {
            iEditText.getEditableText().clear();
        }
    }

    public void paste() {
        ClipboardManager mClipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = mClipboardManager.getPrimaryClip();
        if (clip.getItemCount() != 0 && iEditText != null) {
            if (iEditText.getText() != null) {
                String text = clip.getItemAt(0).getText().toString();
                pasteText(text);
            }
        }
    }

    public void pasteText(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        Pattern pattern = Pattern.compile("(http[https]?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            iEditText.setText(matcher.group());
            System.out.println(matcher.group());
        }
    }

    public void onLoadEnd(final BaseModule module) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                showInfo(module);
            }
        });
    }

    @Override
    public void onLoadError(BaseModule module, Exception e) {

    }

    protected void showInfo(final BaseModule module) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(module.getInfo().author);

        StringBuilder msg = new StringBuilder();
        msg.append(module.getInfo().content);
        msg.append("\r\n");
        msg.append("\r\n");
        msg.append("图片").append(module.getInfo().image.size());
        if (!TextUtils.isEmpty(module.getInfo().video)) {
            msg.append(",视频1");
            msg.append("\r\n");
        }
        builder.setMessage(msg.toString());
        builder.setPositiveButton("下载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                module.download("Home");
            }
        });
        builder.create().show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnClear:
                clear();
                break;
            case R.id.btnPaste:
                paste();
                break;
            case R.id.download:
                onDownloadClick();
                break;
            case R.id.download_home:
                if (TextUtils.isEmpty(cache.getAsString("cookie"))) {
                    Toast.makeText(getActivity(), "请登录", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(getActivity(), InstagramWebActivity.class);
                    loginIntent.putExtra("url", "http://www.instagram.com");
                    startActivity(loginIntent);
                    return;
                }
                Intent intent = new Intent(getActivity(), HomeDownloadActivity.class);
                intent.putExtra("url", getInput());
                startActivity(intent);
                break;
            case R.id.download_stories:
                if (TextUtils.isEmpty(cache.getAsString("cookie"))) {
                    Toast.makeText(getActivity(), "请登录", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(getActivity(), InstagramWebActivity.class);
                    loginIntent.putExtra("url", "http://www.instagram.com");
                    startActivity(loginIntent);
                    return;
                }
                Intent storiesIntent = new Intent(getActivity(), StoriesDownloadActivity.class);
                storiesIntent.putExtra("url", getInput());
                startActivity(storiesIntent);
                break;
            case R.id.download_head:
                if (TextUtils.isEmpty(cache.getAsString("cookie"))) {
                    Toast.makeText(getActivity(), "请登录", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(getActivity(), InstagramWebActivity.class);
                    loginIntent.putExtra("url", "http://www.instagram.com");
                    startActivity(loginIntent);
                    return;
                }
                String url = getInput();
                String userName = url.substring(26, url.length() - 1);
                new UserInfoThread(userName).start();
                break;
        }
    }

    private class UserInfoThread extends Thread {
        private String userName;

        public UserInfoThread(String userName) {
            this.userName = userName;
        }

        @Override
        public void run() {
            super.run();

            Ins ins = new Ins();
            String url = String.format(Locale.CHINA, "https://www.instagram.com/%s/?__a=1", userName);
            String json = ins.get(url);
            __A a = null;
            try {
                a = new Gson().fromJson(json, __A.class);
            } catch (Exception e) {
                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "用户不存在", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            final String pic = a.getGraphql().getUser().getProfile_pic_url_hd();
            final String userName = a.getGraphql().getUser().getUsername();
            final String fullName = a.getGraphql().getUser().getFull_name();
            final String fans = a.getGraphql().getUser().getEdge_followed_by().getCount() + "";
            final String follows = a.getGraphql().getUser().getEdge_follow().getCount() + "";
            final String biography = a.getGraphql().getUser().getBiography() + "";
            final boolean ifollow = a.getGraphql().getUser().isFollowed_by_viewer();
            final boolean followme = a.getGraphql().getUser().isFollows_viewer();


            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    View root = View.inflate(getActivity(), R.layout.dialog_userinfo, null);
                    //头像
                    ImageView infoHead = root.findViewById(R.id.info_head);
                    Picasso.get().load(pic).into(infoHead);
                    //昵称
                    TextView infoUserName = root.findViewById(R.id.info_username);
                    infoUserName.setText(String.format(Locale.CHINA, infoUserName.getText().toString(), userName));
                    //用户名
                    TextView infoFullName = root.findViewById(R.id.info_fullname);
                    infoFullName.setText(String.format(Locale.CHINA, infoFullName.getText().toString(), fullName));
                    //粉丝
                    TextView infoFans = root.findViewById(R.id.info_fans);
                    infoFans.setText(String.format(Locale.CHINA, infoFans.getText().toString(), fans));
                    //追踪
                    TextView infoFollow = root.findViewById(R.id.info_follow);
                    infoFollow.setText(String.format(Locale.CHINA, infoFollow.getText().toString(), follows));
                    //简介
                    TextView infoBiography = root.findViewById(R.id.info_biography);
                    infoBiography.setText(String.format(Locale.CHINA, infoBiography.getText().toString(), biography));

                    //是否关注我
                    TextView infoFollowMe = root.findViewById(R.id.info_followme);
                    if (followme) {
                        infoFollowMe.setText("关注了我");
                    } else {
                        infoFollowMe.setText("没有关注我");
                    }
                    //我是否关注对方
                    TextView infoFollowing = root.findViewById(R.id.info_following);
                    if (ifollow) {
                        infoFollowing.setText("关注了对方");
                    } else {
                        infoFollowing.setText("没有关注对方");
                    }
                    builder.setView(root);
                    builder.show();
                }
            });
        }
    }

    @Override
    public void onLoadStart(final BaseModule module, final String code) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (code == null) {
                    Toast.makeText(getActivity(), "文件已存在,不用重复下载", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "开始下载 - " + code, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void blockComplete(BaseDownloadTask task) {

    }

    @Override
    public void complete(BaseDownloadTask task) {
        if (task.getTag(0) != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "下载完成", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void update(BaseDownloadTask task) {

    }

    @Override
    public void error(BaseDownloadTask task, Throwable throwable) {

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
