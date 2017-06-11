package okdeeplink;


/**
 * Created by zhangqijun on 2017/5/1.
 */

public abstract class Interceptor {


    public abstract void intercept(Call call);

    public void onCall(Request request) {

    }

    public void onNotFound(Request request) {

    }

    public void onError(Request request, Throwable throwable) {

    }

    public interface Call {
        Request getRequest();

        void proceed(Request request);

        void proceed();

        void cancel();
    }

}