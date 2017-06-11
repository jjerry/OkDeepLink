# OkDeepLink
[![Build Status](https://travis-ci.org/HongJun2046/OkDeepLink.svg?branch=master)](https://travis-ci.org/HongJun2046/OkDeepLink)
[ ![Download](https://api.bintray.com/packages/zmanchina/maven/okdeeplink-gradle/images/download.svg) ](https://bintray.com/zmanchina/maven/okdeeplink-gradle/_latestVersion)

OkDeepLink provides a annotation-based api to manipulate app deep links.

- register deep links  with annotation `@Path`、`@Activity`
- start  deep links by service which  inject to Activity with annotation `@Service`
- url or bundle parameters auto inject to activity , restore  when activity recreate  by annotation `@Query`
- async intercept deep links in ui  thread  with annotation `@Intercept`
- support activity result  with `rxJava`


### Config
**In Root Gradle**

```groovy
repositories {
   jcenter()
}
dependencies {
    classpath 'com.hongjun:okdeeplink-gradle:1.0.0'
}
```

**In App Or Lib Gradle**

```groovy
apply plugin: 'okdeeplink.plugin'
```
if use multiple apt lib, you must use

```groovy
packagingOptions {
        exclude 'META-INF/services/javax.annotation.processing.Processor'
}
```

### Example

If you want define `old://app/main?key=value` uri, you can do like this

#### Define Host And Scheme

you can define dispatch activity which receive deep links in your `AndroidManifest.xml` file (using `odl` as an example):

**In App AndroidManifest**
```xml
 <activity
            android:name="okdeeplink.DeepLinkActivity"
            android:theme="@android:style/Theme.Translucent.NoTitleBar">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <data
                    android:host="app"
                    android:scheme="odl" />
            </intent-filter>
 </activity>
```
In the future, I will use annotation `@AppLink` which define scheme and host  instead of `AndroidManifest.xml`. I already have a simple way.

**In Service File**

```java
public interface SampleService {


    @Path("/main")
    @Activity(MainActivity.class)
    void startMainActivity(@Query("key") String key);
}
```
path must start with "/"
when app receive uri like `old://app/main?key=value`, `DeepLinkActivity` will start ,then dispatch the deep link to the appropriate `Activity`.


**In Activity Or Fragment File**

You also start MainActivity by service in app.
```java
public class SecondActivity extends AppCompatActivity {

       @Service
       SampleService sampleService;
       @Query("key")
       String key;


       sampleService.startMainActivity("value");
}

```

### Intercept
If you want Intercept `old://app/second`, you can use `@Intercept`

```java
@Intercept(path = "/second")
public class SecondInterceptor extends Interceptor {
    @Override
    public void intercept(final Call call) {

        Request request = call.getRequest();
        final Intent intent = request.getIntent();
        Context context = request.getContext();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("拦截到你想打开\n");
        stringBuffer.append("URL: " + request.getUrl() + "\n");

        Map<String, Object> query = request.getQuery();
        for (String key : query.keySet()) {
            stringBuffer.append(key + ":" + query.get(key) + "\n");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("提醒");
        builder.setMessage(stringBuffer);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                call.cancel();
            }
        });
        builder.setPositiveButton("加点盐", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                intent.putExtra("key1", "value3");
                call.proceed();
            }
        });
        builder.show();
    }
}
```

![intercept`old://app/second` ](https://raw.githubusercontent.com/HongJun2046/OkDeepLink/master/snapshot/intercept_preview.png)


### Log
I define `LogInterceptor`, this can log deep links which start、notFound、error , log tag is `OkDeepLink`

### Other Way
You can start  intent action by service

```java
public interface SampleService {

    @Action(MediaStore.ACTION_IMAGE_CAPTURE)
    Observable<Response> startImageCapture();
}

```
```java
    public void startImageCapture(){
        sampleService
                .startImageCapture()
                .subscribe(new Action1<Response>() {
                    @Override
                    public void call(Response response) {
                        Intent data = response.getData();
                        int resultCode = response.getResultCode();
                        if (resultCode == RESULT_OK) {
                            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                            ImageView imageView = (ImageView) findViewById(R.id.ImageView_Capture_Image);
                            imageView.setImageBitmap(imageBitmap);
                        }
                    }
                });
    }
```

### Words

It is so simple，then you just do it like `sample`


