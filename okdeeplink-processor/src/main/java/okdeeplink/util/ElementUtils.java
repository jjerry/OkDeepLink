package okdeeplink.util;

import com.squareup.javapoet.ClassName;

import java.util.List;
import java.util.Map;

import javax.lang.model.type.TypeMirror;

/**
 * Created by zhangqijun on 2017/4/24.
 */

public class ElementUtils {


    public static ClassName getClassName(TypeMirror typeMirror) {
        String fullyQualifiedClassName = typeMirror.toString();
        String simpleClassName = getSimpleName(fullyQualifiedClassName);
        String packageName = getPackageName(fullyQualifiedClassName);
        return ClassName.get(packageName, simpleClassName);
    }


    public static String getName(ClassName className) {
        return className.packageName()+"."+ className.simpleName();
    }



    private static String getPackageName(String fullyQualifiedClassName) {
        int dotIndex = fullyQualifiedClassName.lastIndexOf(".");
        return fullyQualifiedClassName.substring(0, dotIndex);
    }


    private static String getSimpleName(String fullyQualifiedClassName) {
        int dotIndex = fullyQualifiedClassName.lastIndexOf(".");
        return fullyQualifiedClassName.substring(dotIndex + 1, fullyQualifiedClassName.length());
    }

    public static <T> boolean isEmpty(List<T> list) {
        return list == null || list.size() == 0;
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.size() == 0;
    }

}
