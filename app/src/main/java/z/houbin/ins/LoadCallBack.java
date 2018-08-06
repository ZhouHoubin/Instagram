package z.houbin.ins;

public interface LoadCallBack {
    public void onLoadStart(BaseModule module,String code);
    public void onLoadEnd(BaseModule module);
    public void onLoadError(BaseModule module, Exception e);
}
