package okdeeplink;

import android.util.Log;

/**
 * Created by zhangqijun on 2017/5/7.
 */

public class LogInterceptor extends Interceptor {


    @Override
    public void intercept(Call call) {
        Request request = call.getRequest();
        log("deep link is : " + request.getUrl());
        call.proceed();
    }

    @Override
    public void onNotFound(Request request) {
        super.onNotFound(request);
        log("deep link is not found : " + request.getUrl());

    }

    @Override
    public void onError(Request request, Throwable throwable) {
        super.onError(request, throwable);
        log("deep link is  throw throwable : [" + request.getUrl() + "] -------->" + throwable.getMessage());
    }

    public void log(String msg) {
        if (DeepLinkClient.isDebug()) {
            Log.v(DeepLinkClient.TAG, msg);
        }
    }
}
