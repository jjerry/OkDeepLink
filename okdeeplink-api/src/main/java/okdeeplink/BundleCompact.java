package okdeeplink;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Created by zhangqijun on 2017/4/26.
 */

public class BundleCompact {


    public static <T> T getValue(Bundle dataBundle, String key, Class<T> valueClass) {
        Object value = getValue(dataBundle, key);
        if (value != null) {
            try {
                if (value instanceof String) {
                    String strValue = (String) value;
                    return (T) parseString(strValue, valueClass);
                } else {
                    return valueClass.cast(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return (T) value;
    }

    public static Bundle getSupportBundle(Object target) {
        if (target instanceof Activity) {
            return getBundle((Activity) target);
        }
        if (target instanceof Fragment) {
            return getBundle((Fragment) target);
        }
        if (target instanceof android.support.v4.app.Fragment) {
            return getBundle((android.support.v4.app.Fragment) target);
        }
        return null;
    }


    public static Object getValue(Bundle bundle, String query) {
        if (bundle != null && !TextUtils.isEmpty(query)) {
            return bundle.get(query);
        }
        return null;
    }


    private static Bundle getBundle(Activity activity) {

        if (activity == null || activity.getIntent() == null) {
            return null;
        }
        return activity.getIntent().getExtras();
    }

    private static Bundle getBundle(Fragment fragment) {

        if (fragment == null || fragment.getArguments() == null) {
            return null;
        }
        return fragment.getArguments();

    }

    private static Bundle getBundle(android.support.v4.app.Fragment fragment) {
        if (fragment == null || fragment.getArguments() == null) {
            return null;
        }
        return fragment.getArguments();
    }

    private static <T> Object parseString(String value, Class<T> clazz) {
        if (Boolean.class == clazz || Boolean.TYPE == clazz) return Boolean.parseBoolean(value);
        if (Byte.class == clazz || Byte.TYPE == clazz) return Byte.parseByte(value);
        if (Short.class == clazz || Short.TYPE == clazz) return Short.parseShort(value);
        if (Integer.class == clazz || Integer.TYPE == clazz) return Integer.parseInt(value);
        if (Long.class == clazz || Long.TYPE == clazz) return Long.parseLong(value);
        if (Float.class == clazz || Float.TYPE == clazz) return Float.parseFloat(value);
        if (Double.class == clazz || Double.TYPE == clazz) return Double.parseDouble(value);
        return value;
    }
}
