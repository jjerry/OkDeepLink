
package okdeeplink.sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import okdeeplink.Query;
import okdeeplink.Response;
import okdeeplink.Service;
import rx.functions.Action1;


public class MainActivity extends AppCompatActivity {

    @Service
    SampleService sampleService;
    @Query("key")
    String key;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.Second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleService.startSecondActivity("value1",2);
            }
        });

        findViewById(R.id.Capture_Image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });

        findViewById(R.id.Tel_Phone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleService.startTel("10000");
            }
        });
    }
}
