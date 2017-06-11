package rxactivityresult;

import android.content.Intent;

import rx.Observable;

/**
 * Created by zhangqijun on 2017/6/9.
 */

interface IActivityObservable {

    Observable<Boolean> getAttachedObservable();

    Observable<ActivityResult> getResultObservable();

    void startIntent(Intent intent);

}
