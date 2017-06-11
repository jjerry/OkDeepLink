package okdeeplink.sample;

import android.content.Intent;
import android.provider.MediaStore;

import okdeeplink.Action;
import okdeeplink.Activity;
import okdeeplink.Path;
import okdeeplink.Query;
import okdeeplink.Response;
import okdeeplink.Uri;
import okdeeplink.UriReplace;
import rx.Observable;

/**
 * Created by zhangqijun on 2017/4/24.
 */
public interface SampleService {


    @Path("/main")
    @Activity(MainActivity.class)
    void startMainActivity(@Query("key") String key);

    @Path("/second")
    void startSecondActivity(@Query("key1") String value1, @Query("key2") int value2);


    @Action(MediaStore.ACTION_IMAGE_CAPTURE)
    Observable<Response> startImageCapture();


    @Action(Intent.ACTION_DIAL)
    @Uri("tel:{phone}")
    void startTel(@UriReplace("phone") String phone);
}
