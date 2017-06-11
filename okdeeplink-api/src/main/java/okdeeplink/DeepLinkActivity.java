package okdeeplink;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import rx.Subscriber;

public class DeepLinkActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dispatchFrom(getIntent());
    }


    public void dispatchFrom(Intent intent) {
        new DeepLinkClient(this)
                .buildRequest(intent)
                .dispatch()
                .subscribe(new Subscriber<Request>() {
                    @Override
                    public void onCompleted() {
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        finish();
                    }

                    @Override
                    public void onNext(Request request) {
                        Intent dispatchIntent = request.getIntent();
                        startActivity(dispatchIntent);
                    }
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        dispatchFrom(intent);
    }


}
