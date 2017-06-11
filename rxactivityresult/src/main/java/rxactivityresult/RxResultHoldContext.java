/*
 * Copyright 2017. nekocode (nekocode.cn@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rxactivityresult;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;


/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
class RxResultHoldContext implements IActivityObservable {

    private Context context;

    public final PublishSubject<ActivityResult> resultPublisher = PublishSubject.create();
    public final BehaviorSubject<Boolean> attachedPublisher = BehaviorSubject.create(true);


    public RxResultHoldContext(Context context) {
        this.context = context;
        attachedPublisher.onNext(true);
    }

    public Observable<ActivityResult> getResultObservable() {
        return resultPublisher.asObservable();
    }

    public Observable<Boolean> getAttachedObservable() {
        return attachedPublisher.asObservable();
    }


    public void startIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        resultPublisher.onNext(new ActivityResult(Activity.RESULT_OK, null));
        resultPublisher.onCompleted();
    }
}
