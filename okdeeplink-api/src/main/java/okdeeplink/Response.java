package okdeeplink;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by zhangqijun on 2017/5/2.
 */

public class Response {

    private Request request;
    private int resultCode = Activity.RESULT_CANCELED;
    private Intent data;


    public Response(Request request, int resultCode, Intent data) {
        this.request = request;
        this.resultCode = resultCode;
        this.data = data;
    }

    public Request getRequest() {
        return request;
    }

    public int getResultCode() {
        return resultCode;
    }


    public Intent getData() {
        return data;
    }
}
