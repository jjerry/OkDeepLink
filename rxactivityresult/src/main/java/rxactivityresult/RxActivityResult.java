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
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import java.lang.ref.WeakReference;

import rx.Observable;
import rx.functions.Func1;


public final class RxActivityResult {


    private RxActivityResult() {
    }


    public static <T extends Context> RxActivityResult.Builder<T> on(T context) {
        return new RxActivityResult.Builder<>(context);
    }

    public static <T extends Fragment> RxActivityResult.Builder<T> on(T fragment) {
        return new RxActivityResult.Builder<>(fragment);
    }

    public static <T extends android.support.v4.app.Fragment> RxActivityResult.Builder<T> on(T fragment) {
        return new RxActivityResult.Builder<>(fragment);
    }


    public static class Builder<T> {

        WeakReference<T> targetWeak;

        private Builder(T t) {
            targetWeak = new WeakReference(t);
        }


        public Observable<ActivityResult> startIntent(final Intent intent) {
            IActivityObservable activityObservable = buildActivityObservable();
            Observable<IActivityObservable> intentObservable = startActivity(activityObservable, intent);
            Observable.Transformer transformer = getActivityResultTransformer();
            return intentObservable.compose(transformer);
        }



        private Observable<IActivityObservable> startActivity(final IActivityObservable activityObservable, final Intent intent) {
            return activityObservable
                    .getAttachedObservable()
                    .filter(new Func1<Boolean, Boolean>() {
                        @Override
                        public Boolean call(Boolean attach) {
                            return attach;
                        }
                    })
                    .take(1)
                    .map(new Func1<Boolean, IActivityObservable>() {
                        @Override
                        public IActivityObservable call(Boolean aBoolean) {
                            activityObservable.startIntent(intent);
                            return activityObservable;
                        }
                    });
        }

        private IActivityObservable buildActivityObservable() {

            T target = targetWeak.get();

            if (target instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) target;
                android.support.v4.app.FragmentManager fragmentManager = activity.getSupportFragmentManager();
                IActivityObservable activityObservable = RxResultHoldFragmentV4.getHoldFragment(fragmentManager);
                return activityObservable;
            }

            if (target instanceof Activity) {
                Activity activity = (Activity) target;
                FragmentManager fragmentManager = activity.getFragmentManager();
                IActivityObservable activityObservable = RxResultHoldFragment.getHoldFragment(fragmentManager);
                return activityObservable;
            }
            if (target instanceof Context) {
                final Context context = (Context) target;
                IActivityObservable activityObservable = new RxResultHoldContext(context);
                return activityObservable;
            }

            if (target instanceof Fragment) {
                Fragment fragment = (Fragment) target;
                FragmentManager fragmentManager = fragment.getFragmentManager();
                if (fragmentManager != null) {
                    IActivityObservable activityObservable = RxResultHoldFragment.getHoldFragment(fragmentManager);
                    return activityObservable;
                }
            }
            if (target instanceof android.support.v4.app.Fragment) {
                android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) target;
                android.support.v4.app.FragmentManager fragmentManager = fragment.getFragmentManager();
                if (fragmentManager != null) {
                    IActivityObservable activityObservable = RxResultHoldFragmentV4.getHoldFragment(fragmentManager);
                    return activityObservable;
                }
            }
            return new RxResultHoldEmpty();
        }


        public Observable.Transformer<IActivityObservable, ActivityResult> getActivityResultTransformer() {
            return new Observable.Transformer<IActivityObservable, ActivityResult>() {
                @Override
                public Observable<ActivityResult> call(Observable<IActivityObservable> intentObservable) {
                    return intentObservable
                            .flatMap(new Func1<IActivityObservable, Observable<ActivityResult>>() {
                                @Override
                                public Observable<ActivityResult> call(IActivityObservable activityObservable) {
                                    return activityObservable.getResultObservable();
                                }
                            })
                            .filter(new Func1<ActivityResult, Boolean>() {
                                @Override
                                public Boolean call(ActivityResult result) {
                                    T target = targetWeak.get();
                                    return target != null;
                                }
                            });
                }
            };
        }


    }


}
