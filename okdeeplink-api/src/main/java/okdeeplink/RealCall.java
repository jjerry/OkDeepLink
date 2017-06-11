package okdeeplink;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rxactivityresult.ActivityResult;
import rxactivityresult.RxActivityResult;

/**
 * Created by zhangqijun on 2017/5/5.
 */

public final class RealCall {


    private Request request;
    private DeepLinkClient deepLinkClient;

    private List<Interceptor> interceptors;
    private int interceptTimeOut;

    public RealCall(Request request) {
        this.request = request;
        this.deepLinkClient = request.getDeepLinkClient();
        List<Interceptor> interceptorList = new ArrayList<>();

        interceptorList.addAll(request.getInterceptors());
        interceptorList.addAll(deepLinkClient.getInterceptors());
        interceptorList.addAll(getPathInterceptors(request));
        interceptorList.addAll(deepLinkClient.getGlobalInterceptors());

        interceptorList.add(new LogInterceptor());
        this.interceptors = interceptorList;
        this.interceptTimeOut = deepLinkClient.getInterceptTimeOut();
    }

    private List<Interceptor> getPathInterceptors(Request request) {
        String path = request.getPath();
        List<Interceptor> list = new ArrayList<>();
        for (Map.Entry<String, Interceptor> pathInterceptors : deepLinkClient.getPathInterceptors().entrySet()) {
            String pathKey = pathInterceptors.getKey();
            Interceptor interceptor = pathInterceptors.getValue();
            if (pathKey.equals(path)) {
                list.add(interceptor);
            }
        }
        return list;
    }


    public Observable<Request> createRequest() {

        return buildRequest()
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        callThrowable(throwable);
                    }
                });
    }


    private Observable<Request> buildRequest() {
        RealCallChain chain = new RealCallChain(interceptors, 0, request);
        chain.setTimeout(interceptTimeOut);
        chain.call();
        return chain
                .getRequestObservable()
                .map(new Func1<Request, Request>() {
                    @Override
                    public Request call(Request request) {
                        if (interceptors != null) {
                            for (Interceptor interceptor : interceptors) {
                                interceptor.onCall(request);
                            }
                        }
                        return request;
                    }
                });
    }


    public Observable<Response> createResponse() {

        return buildRequest()
                .flatMap(new Func1<Request, Observable<Response>>() {
                    @Override
                    public Observable<Response> call(final Request request) {

                        Context context = getStartContext(request.getContext());

                        return RxActivityResult
                                .on(context)
                                .startIntent(request.getIntent())
                                .map(new Func1<ActivityResult, Response>() {
                                    @Override
                                    public Response call(ActivityResult activityActivityResult) {
                                        return new Response(request, activityActivityResult.getResultCode(), activityActivityResult.getData());
                                    }
                                });
                    }
                }).doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        callThrowable(throwable);
                    }
                });
    }


    private Context getStartContext(Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            boolean isDestroyed = false;
            if (activity.isFinishing()) {
                isDestroyed = true;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (activity.isDestroyed()) {
                    isDestroyed = true;
                }

            }
            FragmentManager fragmentManager = activity.getFragmentManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (fragmentManager.isDestroyed()) {
                    isDestroyed = true;
                }
            }
            if (context instanceof FragmentActivity) {
                FragmentActivity fragmentActivity = (FragmentActivity) context;
                android.support.v4.app.FragmentManager fragmentManagerV4 = fragmentActivity.getSupportFragmentManager();

                if (fragmentManagerV4.isDestroyed()) {
                    isDestroyed = true;
                }
            }
            if (isDestroyed) {
                return context.getApplicationContext();
            }
        }
        return context;
    }

    private void callThrowable(Throwable throwable) {
        if (throwable instanceof ActivityNotFoundException) {
            if (interceptors != null) {
                for (Interceptor interceptor : interceptors) {
                    interceptor.onNotFound(request);
                }
            }
        }
        if (interceptors != null) {
            for (Interceptor interceptor : interceptors) {
                interceptor.onError(request, throwable);
            }
        }
    }

    public Observable<Request> dispatchRequest() {
        return buildRequest()
                .flatMap(new Func1<Request, Observable<Request>>() {
                    @Override
                    public Observable<Request> call(Request request) {
                        if (!request.isDeepLink()) {
                            return Observable.error(new ActivityNotFoundException());
                        }
                        Context context = request.getContext();
                        Intent intent = request.getIntent();
                        if (context instanceof android.app.Activity) {
                            android.app.Activity activity = (android.app.Activity) context;
                            if (activity.getCallingActivity() != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                            }
                        }
                        if (context != null && context.getClass() == Context.class) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        return Observable.just(request);
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Request>>() {
                    @Override
                    public Observable<? extends Request> call(Throwable throwable) {
                        callThrowable(throwable);
                        return Observable.error(throwable);
                    }
                });
    }
}
