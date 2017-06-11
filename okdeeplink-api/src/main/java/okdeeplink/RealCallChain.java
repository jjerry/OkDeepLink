package okdeeplink;

import android.app.PendingIntent;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;

/**
 * Created by zhangqijun on 2017/5/5.
 */

public class RealCallChain implements Interceptor.Call {


    private final List<Interceptor> interceptors;
    private int index;
    private Request request;

    public final BehaviorSubject<Interceptor> interceptorSubject = BehaviorSubject.create();

    public final AsyncSubject<Request> requestSubject = AsyncSubject.create();

    private int timeout = 0;

    public RealCallChain(List<Interceptor> interceptors, int index, Request request) {
        this.interceptors = interceptors;
        this.index = index;
        this.request = request;
    }

    public RealCallChain setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public void call() {

        Observable.just(1)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        proceed();
                    }
                });


        Observable<Interceptor> observable = interceptorSubject;
        if (timeout > 0) {
            observable = interceptorSubject.timeout(timeout, TimeUnit.SECONDS);
        }
        observable.subscribe(new Subscriber<Interceptor>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                        requestSubject.onError(new TimeoutException());
                    }

                    @Override
                    public void onNext(Interceptor interceptor) {

                    }
                });
    }


    @Override
    public Request getRequest() {
        return request;
    }


    @Override
    public void proceed(Request request) {
        this.request = request;
        proceed();
    }

    public Observable<Request> getRequestObservable() {
        return requestSubject
                .asObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void realCall() {
        requestSubject.onNext(request);
        requestSubject.onCompleted();
    }

    @Override
    public void proceed() {


        if (index >= interceptors.size()) {
            realCall();
            return;
        }
        final Interceptor interceptor = interceptors.get(index);
        Observable
                .just(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        interceptor.intercept(RealCallChain.this);
                    }
                });

        interceptorSubject.onNext(interceptor);
        index = index + 1;
    }

    @Override
    public void cancel() {
        requestSubject.onError(new PendingIntent.CanceledException());
    }
}
