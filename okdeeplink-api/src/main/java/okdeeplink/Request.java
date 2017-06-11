package okdeeplink;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Observable;

/**
 * Created by zhangqijun on 2017/5/2.
 */

public class Request {


    private Intent intent;
    private Context context;
    private DeepLinkClient deepLinkClient;
    private int requestCode = -1;

    private boolean isDeepLink = true;

    private List<Interceptor> interceptors = new ArrayList<>();


    public Request(Intent intent, DeepLinkClient deepLinkClient) {
        this.intent = intent;
        this.context = deepLinkClient.getContext();
        this.deepLinkClient = deepLinkClient;
    }


    public Request addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public Request(Intent intent, DeepLinkClient deepLinkClient, int requestCode) {
        this.requestCode = requestCode;
        this.intent = intent;
        this.context = deepLinkClient.getContext();
        this.deepLinkClient = deepLinkClient;
    }


    public Request setDeepLink(boolean deepLink) {
        isDeepLink = deepLink;
        return this;
    }

    public boolean isDeepLink() {
        return isDeepLink;
    }

    public Request setIntent(Intent intent) {
        this.intent = intent;
        return this;
    }

    public Request setRequestCode(int requestCode) {
        this.requestCode = requestCode;
        return this;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public Intent getIntent() {
        return intent;
    }


    public String getUrl() {
        return getUrl(intent);
    }

    public String getScheme() {
        if (intent != null && intent.getData() != null) {
            return intent.getData().getScheme();
        }
        return "";
    }

    public String getHost() {
        if (intent != null && intent.getData() != null) {
            return intent.getData().getHost();
        }
        return "";
    }

    public String getPath() {
        if (intent != null && intent.getData() != null) {
            return intent.getData().getEncodedPath();
        }
        return "";
    }


    public static String getUrl(Intent intent) {
        if (intent != null && intent.getData() != null) {
            return intent.getData().toString();
        }
        return "";
    }

    public Map<String, Object> getQuery() {
        return getQuery(intent);
    }


    public static Map<String, Object> getQuery(Intent intent) {
        Map<String, Object> query = new HashMap<>();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Set<String> keys = bundle.keySet();
                Iterator<String> it = keys.iterator();
                while (it.hasNext()) {
                    String key = it.next();

                    bundle.get(key);
                    query.put(key, bundle.get(key));

                }
            }
        }
        return query;
    }

    public <T> T build(Class<T> service) {
        if (deepLinkClient != null && context != null) {
            return deepLinkClient.build(service);
        }
        return null;
    }

    public Context getContext() {
        return context;
    }


    public Request setContext(Context context) {
        this.context = context;
        if (deepLinkClient != null) {
            deepLinkClient.setContext(context);
        }
        return this;
    }

    public DeepLinkClient getDeepLinkClient() {
        return deepLinkClient;
    }


    public Observable<Response> createResponse() {
        return deepLinkClient.createResponse(this);
    }

    public Observable<Request> createRequest() {
        return deepLinkClient.createRequest(this);
    }

    public void start() {
        deepLinkClient.start(this);
    }

    public Observable<Request> dispatch() {
        return deepLinkClient.dispatch(this);
    }

}
