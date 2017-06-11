package okdeeplink.second;

import okdeeplink.Activity;
import okdeeplink.Path;

/**
 * Created by zhangqijun on 2017/4/24.
 */

public interface SecondService {

    @Path("/second")
    @Activity(SecondActivity.class)
    void startSecondActivity();
}
