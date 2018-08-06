package z.houbin.ins;

import java.util.Iterator;
import java.util.LinkedHashMap;

import okhttp3.Headers;
import okhttp3.OkHttpClient;

public abstract class BaseModule {
    protected String mTitle;
    protected String mHost;
    protected String mUrl;
    protected LoadCallBack mCallBack;
    protected OkHttpClient mClient = new OkHttpClient();
    protected Headers mHeaders;
    protected LinkedHashMap<String, Object> metas = new LinkedHashMap<>();
    protected BaseInfo mInfo;

    public BaseModule() {
    }

    public void parse(final String text) {
        onLoadStart("");
        new Thread() {
            @Override
            public void run() {
                super.run();
                doInThread(text);
            }
        }.start();
    }

    protected void doInThread(String text) {

    }

    protected void onLoadStart(String code) {
        if (mCallBack != null) {
            mCallBack.onLoadStart(this, code);
        }
    }

    protected void onLoadEnd() {
        if (mCallBack != null) {
            mCallBack.onLoadEnd(this);
        }
    }

    protected void onLoadError(Exception e) {
        if (mCallBack != null) {
            mCallBack.onLoadError(this, e);
        }
    }

    public void setLoadListener(LoadCallBack listener) {
        this.mCallBack = listener;
    }

    public BaseInfo getInfo() {
        return mInfo;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        Iterator<String> iterator = metas.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            builder.append("[");
            builder.append(key);
            builder.append(":");
            builder.append(metas.get(key));
            builder.append("],");
        }
        return builder.toString();
    }

    public void download() {

    }

    public void download(String tag) {

    }
}
