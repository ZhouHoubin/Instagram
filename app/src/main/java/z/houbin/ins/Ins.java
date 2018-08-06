package z.houbin.ins;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import z.houbin.ins.bean.Follow;
import z.houbin.ins.bean.UserInfo;
import z.houbin.ins.util.ACache;

public class Ins {
    private Headers.Builder mHeaderBuilder;
    private OkHttpClient mClient;
    private ACache cache = ACache.get(Constant.cacheDir);
    private Follow mFollow;

    public Ins() {
        mClient = new OkHttpClient();
        mHeaderBuilder = new Headers.Builder();
        mHeaderBuilder.add("Host", "www.instagram.com");
        mHeaderBuilder.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
        mHeaderBuilder.add("Accept-Language", "zh-HK,zh-CN;q=0.9,zh;q=0.8,ja-JP;q=0.7,ja;q=0.6,zh-TW;q=0.5,en-US;q=0.4,en;q=0.3");

        mHeaderBuilder.add("Cookie", cache.getAsString("cookie"));
        mHeaderBuilder.add("Origin: https://www.instagram.com");
        mHeaderBuilder.add("X-Requested-With: XMLHttpRequest");
    }

    /**
     * 查询关注
     */
    private Follow queryLike(String userId, String cursor) {
        String url = "";
        if (cursor == null) {
            url = "https://www.instagram.com/graphql/query/?query_hash=58712303d941c6855d4e888c5f0cd22f&variables={\"id\":\"" + userId + "\",\"first\":24}";
        } else {
            url = "https://www.instagram.com/graphql/query/?query_hash=58712303d941c6855d4e888c5f0cd22f&variables={\"id\":\"" + userId + "\",\"first\":24,\"after\":\"" + cursor + "\"}";
        }
        String json = get(url);
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject edge = jsonObject.getJSONObject("data").getJSONObject("user").getJSONObject("edge_follow");
            if (mFollow == null) {
                mFollow = new Follow();
                mFollow.setCount(edge.getInt("count"));
                mFollow.setUserInfos(new ArrayList<UserInfo>());
            }

            JSONArray edges = edge.getJSONArray("edges");
            for (int i = 0; i < edges.length(); i++) {
                JSONObject node = edges.getJSONObject(i).getJSONObject("node");
                UserInfo info = new UserInfo();
                info.setId(node.getString("id"));
                info.setFull_name(node.getString("full_name"));
                info.setProfile_pic_url(node.getString("profile_pic_url"));
                info.setUsername(node.getString("username"));
                mFollow.getUserInfos().add(info);
            }
            boolean hasNext = edge.getJSONObject("page_info").getBoolean("has_next_page");
            String nextCursor = edge.getJSONObject("page_info").getString("end_cursor");
            if (hasNext) {
                queryLike(userId, nextCursor);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return mFollow;
    }

    public Follow queryLike(String userName) {
        mFollow = null;
        return queryLike(queryUserIdByUserName(userName), null);
    }

    private Follow queryFollow(String userId, String cursor) {
        String url = "";
        if (cursor == null) {
            url = "https://www.instagram.com/graphql/query/?query_hash=37479f2b8209594dde7facb0d904896a&variables={\"id\":\"" + userId + "\",\"first\":24}";
        } else {
            url = "https://www.instagram.com/graphql/query/?query_hash=37479f2b8209594dde7facb0d904896a&variables={\"id\":\"" + userId + "\",\"first\":24,\"after\":\"" + cursor + "\"}";
        }
        String json = get(url);
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject edge = jsonObject.getJSONObject("data").getJSONObject("user").getJSONObject("edge_followed_by");
            if (mFollow == null) {
                mFollow = new Follow();
                mFollow.setCount(edge.getInt("count"));
                mFollow.setUserInfos(new ArrayList<UserInfo>());
            }

            JSONArray edges = edge.getJSONArray("edges");
            for (int i = 0; i < edges.length(); i++) {
                JSONObject node = edges.getJSONObject(i).getJSONObject("node");
                UserInfo info = new UserInfo();
                info.setId(node.getString("id"));
                info.setFull_name(node.getString("full_name"));
                info.setProfile_pic_url(node.getString("profile_pic_url"));
                info.setUsername(node.getString("username"));
                info.setSingle(!node.getBoolean("followed_by_viewer"));
                mFollow.getUserInfos().add(info);
            }
            boolean hasNext = edge.getJSONObject("page_info").getBoolean("has_next_page");
            String nextCursor = edge.getJSONObject("page_info").getString("end_cursor");
            if (hasNext) {
                queryFollow(userId, nextCursor);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mFollow;
    }

    /**
     * 查询粉丝
     */
    public Follow queryFollow(String userName) {
        mFollow = null;
        return queryFollow(queryUserIdByUserName(userName), null);
    }

    public String queryUserIdByUserName(String userName) {
        String url = "https://www.instagram.com/" + userName;
        String content = get(url);
        if (content != null) {
            Document document = Jsoup.parse(content);
            Elements scripts = document.select("script");
            for (Element script : scripts) {
                String text = script.data();
                if (text.contains("sharedData")) {
                    int start = text.indexOf("{");
                    int end = text.lastIndexOf("}") + 1;
                    try {
                        String json = text.substring(start, end);
                        JSONObject jsonObject = new JSONObject(json);
                        JSONObject user = jsonObject.getJSONObject("entry_data").getJSONArray("ProfilePage").getJSONObject(0).getJSONObject("graphql").getJSONObject("user");
                        return user.getString("id");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public String get(String url) {
        String content = null;
        Request request = new Request.Builder().url(url).headers(mHeaderBuilder.build()).get().build();
        try {
            Response response = mClient.newCall(request).execute();
            content = response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }


    private String post(String url) {
        String content = null;
        FormBody body = new FormBody.Builder().build();
        Request request = new Request.Builder().url(url).headers(mHeaderBuilder.build()).post(body).build();
        try {
            Response response = mClient.newCall(request).execute();
            content = response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    //取消关注
    public void unFlow(List<UserInfo> infoList) {
        mHeaderBuilder.set("Content-Type", "application/x-www-form-urlencoded");
        mHeaderBuilder.set("X-CSRFToken", cache.getAsString("csrftoken"));
        for (UserInfo info : infoList) {
            String url = "https://www.instagram.com/web/friendships/" + info.getId() + "/unfollow/";
            mHeaderBuilder.set("Referer", "https://www.instagram.com/" + info.getUsername());
            String data = post(url);
            System.out.println(String.format(Locale.CHINA, "取消关注:%s,%s", info.getId(), data));
            if (data.contains("請幾分鐘後再試一次")) {
                sleep(60 * 5);
            } else {
                sleep(30);
            }
        }
    }

    private void sleep(int second) {
        try {
            Thread.sleep(second * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //关注
    public void flow(List<UserInfo> infoList) {
        mHeaderBuilder.set("Content-Type", "application/x-www-form-urlencoded");
        for (UserInfo info : infoList) {
            String url = "https://www.instagram.com/web/friendships/" + info.getId() + "/follow/";
            mHeaderBuilder.set("Referer", "https://www.instagram.com/" + info.getUsername());
            String data = post(url);
            System.out.println(String.format(Locale.CHINA, "关注:%ss,%s", info.getId(), data));
            if (data.contains("ok")) {
                sleep(30);
            } else {
                sleep(60 * 5);
            }
        }
    }
}
