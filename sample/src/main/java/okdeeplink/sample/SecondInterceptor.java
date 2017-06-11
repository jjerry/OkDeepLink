package okdeeplink.sample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import java.util.Map;

import okdeeplink.Intercept;
import okdeeplink.Interceptor;
import okdeeplink.Request;

/**
 * Created by zhangqijun on 2017/5/6.
 */

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
        AlertDialog.Builder builder = new AlertDialog.Builder(context,R.style.Theme_AppCompat_Dialog_Alert);
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
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                call.cancel();
            }
        });
        builder.show();
    }
}
