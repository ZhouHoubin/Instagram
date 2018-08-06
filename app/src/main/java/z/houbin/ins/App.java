package z.houbin.ins;

import android.app.Application;

import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        /*注意: 即使您已经在AndroidManifest.xml中配置过appkey和channel值，也需要在App代码中调用初始化接口（如需要使用AndroidManifest.xml中配置好的appkey和channel值，UMConfigure.init调用中appkey和channel参数请置为null）。*/
        UMConfigure.init(this, "5b3cb73df43e4843730000ae", "dev", UMConfigure.DEVICE_TYPE_PHONE, "6655d636d833844502d001f9b4e2ee0e");
        /**
         * 设置组件化的Log开关
         * 参数: boolean 默认为false，如需查看LOG设置为true
         */
        UMConfigure.setLogEnabled(true);
        /**
         * 设置日志加密
         * 参数：boolean 默认为false（不加密）
         */
        UMConfigure.setEncryptEnabled(true);
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);
        //手动统计
        MobclickAgent.openActivityDurationTrack(false);

        Constant.cacheDir = getCacheDir();
    }
}
