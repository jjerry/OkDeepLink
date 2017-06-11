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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;

import rx.Observable;
import rx.subjects.PublishSubject;


/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class RxResultHoldFragment extends Fragment implements IActivityObservable {

    public static final String TAG = RxResultHoldFragment.class.getName();
    private ActivityResult activityResult;
    public final PublishSubject<ActivityResult> resultPublisher = PublishSubject.create();
    public final PublishSubject<Boolean> attachedPublisher = PublishSubject.create();

    public RxResultHoldFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public Observable<ActivityResult> getResultObservable() {
        return resultPublisher.asObservable();
    }

    public Observable<Boolean> getAttachedObservable() {
        return attachedPublisher.asObservable();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachToContext(context);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < 23) {
            onAttachToContext(activity);
        }
    }


    private void onAttachToContext(Context context) {
        attachedPublisher.onNext(true);
    }


    public void startIntent(Intent intent) {
        startActivityForResult(intent, 100);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        activityResult = new ActivityResult(resultCode, data);


    }

    @Override
    public void onResume() {
        super.onResume();
        if (activityResult != null) {
            resultPublisher.onNext(activityResult);
            activityResult = null;
            resultPublisher.onCompleted();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.remove(this);
            transaction.commit();
        }
    }

    public static RxResultHoldFragment getHoldFragment(FragmentManager fragmentManager) {
        RxResultHoldFragment holdFragment = new RxResultHoldFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(holdFragment, RxResultHoldFragment.TAG);
        transaction.commit();
        return holdFragment;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
