package z.houbin.ins.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import z.houbin.ins.Constant;
import z.houbin.ins.R;
import z.houbin.ins.ui.act.FollowActivity;
import z.houbin.ins.util.ACache;

//粉丝追踪
public class FollowFragment extends Fragment implements View.OnClickListener {
    private EditText inputUserName;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return View.inflate(getActivity(), R.layout.layout_following, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inputUserName = view.findViewById(R.id.edit);
        view.findViewById(R.id.check).setOnClickListener(this);
        view.findViewById(R.id.login).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.login) {
            Intent intent = new Intent(getActivity(), InstagramWebActivity.class);
            intent.putExtra("url", "http://www.instagram.com");
            startActivity(intent);
        } else if (v.getId() == R.id.check) {
            ACache cache = ACache.get(Constant.cacheDir);
            if (TextUtils.isEmpty(cache.getAsString("cookie")) || TextUtils.isEmpty(cache.getAsString("csrftoken"))) {
                Toast.makeText(getActivity(), "请先登录", Toast.LENGTH_SHORT).show();
            } else if (inputUserName.getText().length() != 0) {
                Intent intent = new Intent(getActivity(), FollowActivity.class);
                intent.putExtra("name", inputUserName.getText().toString());
                startActivity(intent);
            }
        }
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
