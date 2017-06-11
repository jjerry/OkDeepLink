package okdeeplink.second;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import okdeeplink.Query;

/**
 * Created by zhangqijun on 2017/4/25.
 */

public class SecondActivity extends AppCompatActivity {

    @Query("key1")
    String key1;

    @Query("key2")
    Integer key2;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Toast.makeText(this, "key1:" + key1 + "\nkey2:" + key2 + "\n", Toast.LENGTH_LONG).show();
        findViewById(R.id.Button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
