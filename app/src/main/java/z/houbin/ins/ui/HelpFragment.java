package z.houbin.ins.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import z.houbin.ins.R;
import z.houbin.ins.guide.StepDialog;

public class HelpFragment extends Fragment implements View.OnClickListener {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return View.inflate(getActivity(), R.layout.layout_help, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_guide).setOnClickListener(this);
        view.findViewById(R.id.btn_group).setOnClickListener(this);
        view.findViewById(R.id.btn_about).setOnClickListener(this);
        view.findViewById(R.id.btn_version).setOnClickListener(this);
        TextView textVersion = view.findViewById(R.id.btn_version);
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            textVersion.setText(String.format("版本: %s", packageInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_version:
                break;
            case R.id.btn_guide:
                StepDialog.getInstance()
                        .setImages(new int[]{R.drawable.guide_1, R.drawable.guide_2})
                        .show(getFragmentManager());
                break;
            case R.id.btn_group:
                enterGroup();
                break;
            case R.id.btn_about:
                showAbout();
                break;
            default:
                break;
        }
    }

    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("关于");
        builder.setMessage("#支持快拍/单图/多图/视频/个人主页所有帖子/下载\r\n\r\n#下个版本增加粉丝功能");
        builder.create().show();
    }

    private void enterGroup() {
        String url = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=3772543&card_type=group&source=qrcode";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
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
