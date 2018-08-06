package z.houbin.ins.ui.act;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import z.houbin.ins.Ins;
import z.houbin.ins.R;
import z.houbin.ins.bean.Follow;
import z.houbin.ins.bean.UserInfo;

public class FollowActivity extends AppCompatActivity implements View.OnClickListener {
    private AlertDialog loadDialog;

    private Button btnFollows;
    private Button btnLikes;
    private Button btnSingles;
    private ListView list;

    private Follow likeFollow;//关注
    private Follow fansFollow;//粉丝
    private Follow singleFollow;//单向

    private MainAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_following_activity);

        btnFollows = findViewById(R.id.btn_follows);
        btnFollows.setOnClickListener(this);

        btnLikes = findViewById(R.id.btn_likes);
        btnLikes.setOnClickListener(this);

        btnSingles = findViewById(R.id.btn_singles);
        btnSingles.setOnClickListener(this);

        list = findViewById(R.id.list);

        final String userName = getIntent().getStringExtra("name");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage("检测中,请稍后\r\n检测时间取决于粉丝量+关注量+网速");
        loadDialog = builder.create();
        loadDialog.show();

        new Thread() {
            @Override
            public void run() {
                super.run();
                Ins ins = new Ins();
                likeFollow = ins.queryLike(userName);
                fansFollow = ins.queryFollow(userName);

                singleFollow = new Follow();
                singleFollow.setUserInfos(new ArrayList<UserInfo>());

                List<UserInfo> fansList = fansFollow.getUserInfos();
                List<UserInfo> likeList = likeFollow.getUserInfos();

                out:
                for (UserInfo likeInfo : likeList) {
                    for (UserInfo fansInfo : fansList) {
                        if (likeInfo.getId().equals(fansInfo.getId())) {
                            continue out;
                        }
                    }
                    likeInfo.setSingle(true);
                    singleFollow.getUserInfos().add(likeInfo);
                }
                singleFollow.setCount(singleFollow.getUserInfos().size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadDialog != null && loadDialog.isShowing()) {
                            loadDialog.dismiss();
                        }
                        Toast.makeText(getApplicationContext(), "检测完成", Toast.LENGTH_SHORT).show();
                        btnLikes.append("(" + likeFollow.getCount() + ")");
                        btnFollows.append("(" + fansFollow.getCount() + ")");
                        btnSingles.append("(" + singleFollow.getCount() + ")");
                    }
                });
            }
        }.start();
    }

    private Button lastClickView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_follow, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_del_single:
                if (singleFollow != null && singleFollow.getCount() != 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("提示");
                    builder.setMessage("删除单向好友任务会在后台执行,不会删除互关好友,删除有一定延迟(每次延迟30秒),结果以个人主页为准,操作锁定或者任务完成会自动停止");
                    builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "任务后台执行,可以返回", Toast.LENGTH_LONG).show();
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    Ins ins = new Ins();
                                    ins.unFlow(singleFollow.getUserInfos());
                                }
                            }.start();
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
                break;
            case R.id.menu_follow_fans:
                if (singleFollow != null && singleFollow.getCount() != 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("提示");
                    builder.setMessage("任务会在后台执行,每次延迟30秒,结果以个人主页为准,操作锁定或者任务完成会自动停止");
                    builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "任务后台执行,可以返回", Toast.LENGTH_LONG).show();
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    Ins ins = new Ins();
                                    ins.flow(singleFollow.getUserInfos());
                                }
                            }.start();
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (lastClickView != null) {
            lastClickView.setScaleX(1.0f);
            lastClickView.setScaleY(1.0f);
        }
        switch (v.getId()) {
            case R.id.btn_follows:
                if (adapter == null) {
                    adapter = new MainAdapter(fansFollow);
                    adapter.setTag(0);
                    list.setAdapter(adapter);
                } else {
                    adapter.setTag(0);
                    adapter.change(fansFollow);
                }
                if (lastClickView != null) {
                    lastClickView.getPaint().setUnderlineText(true);
                }
                btnFollows.getPaint().setUnderlineText(true);
                lastClickView = btnFollows;
                break;
            case R.id.btn_likes:
                if (adapter == null) {
                    adapter = new MainAdapter(likeFollow);
                    adapter.setTag(1);
                    list.setAdapter(adapter);
                } else {
                    adapter.setTag(1);
                    adapter.change(likeFollow);
                }
                if (lastClickView != null) {
                    lastClickView.getPaint().setUnderlineText(true);
                }
                btnLikes.getPaint().setUnderlineText(true);
                lastClickView = btnLikes;
                break;
            case R.id.btn_singles:
                if (adapter == null) {
                    adapter = new MainAdapter(singleFollow);
                    adapter.setTag(2);
                    list.setAdapter(adapter);
                } else {
                    adapter.setTag(2);
                    adapter.change(singleFollow);
                }
                if (lastClickView != null) {
                    lastClickView.getPaint().setUnderlineText(true);
                }
                btnSingles.getPaint().setUnderlineText(true);
                lastClickView = btnSingles;
                break;
        }
        lastClickView.setScaleX(1.5f);
        lastClickView.setScaleY(1.5f);
    }

    private class MainAdapter extends BaseAdapter {
        private Follow follow;

        private int tag;

        public MainAdapter(Follow follow) {
            this.follow = follow;
        }

        public void change(Follow follow) {
            this.follow = follow;
            notifyDataSetChanged();
        }

        public int getTag() {
            return tag;
        }

        public void setTag(int tag) {
            this.tag = tag;
        }

        @Override
        public int getCount() {
            return follow.getUserInfos().size();
        }

        @Override
        public UserInfo getItem(int position) {
            return follow.getUserInfos().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(getApplicationContext(), R.layout.item_follow, null);
                holder.itemIcon = convertView.findViewById(R.id.item_pic);
                holder.itemName = convertView.findViewById(R.id.item_name);
                holder.itemFullName = convertView.findViewById(R.id.item_fullname);
                holder.itemStatus = convertView.findViewById(R.id.item_status);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            UserInfo info = getItem(position);
            Picasso.get().load(info.getProfile_pic_url()).into(holder.itemIcon);

            holder.itemName.setText(info.getUsername());

            holder.itemFullName.setText(info.getFull_name());

            if (info.isSingle()) {
                holder.itemStatus.setText("单向");
            } else {
                holder.itemStatus.setText("互关");
            }

            return convertView;
        }

        private class ViewHolder {
            private ImageView itemIcon;
            private TextView itemName, itemFullName;
            private Button itemStatus;
        }
    }
}
