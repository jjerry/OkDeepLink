package okdeeplink;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;


/**
 * Created by zhangqijun on 2017/3/8.
 */

public class DeepLinkClient {


    public static final String TAG = "OkDeepLink";

    private static Map<String, Address> addresses = new Hashtable<>();

    private List<Interceptor> interceptors = new ArrayList<>();


    private Context context;

    private int interceptTimeOut = 0;

    private static boolean debug = true;


    public DeepLinkClient(Context context) {
        this.context = context;
    }

    public DeepLinkClient(Fragment fragment) {
        this.context = fragment.getActivity();
    }

    public static void setDebug(boolean debugFlag) {
        debug = debugFlag;
    }

    public static boolean isDebug() {
        return debug;
    }

    /**
     * 每个matcher都会通过aop往这里注册路由表
     */
    public void init() {

    }

    public DeepLinkClient setInterceptTimeOut(int interceptTimeOut) {
        this.interceptTimeOut = interceptTimeOut;
        return this;
    }

    public int getInterceptTimeOut() {
        return interceptTimeOut;
    }

    public <T> T build(Class<T> service) {
        return null;
    }


    public Observable<Response> createResponse(Request request) {
        return new RealCall(request).createResponse();
    }

    public Observable<Request> createRequest(Request request) {
        return new RealCall(request).createRequest();
    }

    public void start(Request request) {
        createResponse(request)
                .subscribe(new Subscriber<Response>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Response response) {

                    }
                });
    }

    public Observable dispatch(Request request) {
       return new RealCall(request).dispatchRequest();
    }

    public Intent buildIntent(String url) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(url));
        return buildRequest(intent).getIntent();
    }

    public Request buildRequest(String url) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(url));
        return buildRequest(intent);
    }

    public Request buildRequest(Intent sourceIntent) {
        if (sourceIntent == null) {
            return null;
        }
        Intent newIntent = new Intent(sourceIntent);
        Uri uri = newIntent.getData();

        addNewTaskFlag(newIntent);

        if (uri != null) {
            addBundleQuery(newIntent, uri);

            Address entry = new DeepLinkClient(context).matchUrl(uri.toString());
            if (entry == null || entry.getActivityClass() == null) {
                return new Request(newIntent, this).setDeepLink(false);
            }
            newIntent.setComponent(new ComponentName(context, entry.getActivityClass()));

            return new Request(newIntent, this);
        }
        return new Request(newIntent, this).setDeepLink(false);

    }

    private void addNewTaskFlag(Intent newIntent) {
        if (context.getClass() == Context.class) {
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }

    private void addBundleQuery(Intent newIntent, Uri uri) {
        Map<String, String> uriParams = new HashMap<>();
        try {
            for (String queryParameter : uri.getQueryParameterNames()) {
                for (String queryParameterValue : uri.getQueryParameters(queryParameter)) {
                    if (uriParams.containsKey(queryParameter)) {
                        Log.w(TAG, "Duplicate parameter name in path and query param: " + queryParameter);
                    }
                    uriParams.put(queryParameter, queryParameterValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bundle bundle = newIntent.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        for (Map.Entry<String, String> parameterEntry : uriParams.entrySet()) {
            String value = parameterEntry.getValue();
            bundle.putString(parameterEntry.getKey(), value);
        }
        newIntent.putExtras(bundle);
    }


    public DeepLinkClient addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }


    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public List<Interceptor> getGlobalInterceptors() {
        return new ArrayList<>();
    }

    public Map<String, Interceptor> getPathInterceptors() {
        return new Hashtable<>();
    }


    public Context getContext() {
        return context;
    }

    public DeepLinkClient setContext(Context context) {
        this.context = context;
        return this;
    }

    public Address matchUrl(String url) {
        if (DeepLinkClient.isEmpty()) {
            init();
        }
        String path = url;
        try {
            Uri uri = Uri.parse(url);
            path = uri.getEncodedPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Address entry : addresses.values()) {
            if (entry.getPath().equals(path)) {
                return entry;
            }
        }
        return null;
    }

    public static boolean isEmpty() {
        return addresses.isEmpty();
    }


    public static void addAddress(Address address) {
        addresses.put(address.getPath(), address);
    }
}
