package okdeeplink;


import android.net.Uri;

public final class Address {

    private final Class<?> activityClass;
    private final String path;

    public Address(String path, Class<?> activityClass) {
        this.path = path;
        this.activityClass = activityClass;

    }

    public Class<?> getActivityClass() {
        return activityClass;
    }


    public boolean isUri(){
        boolean isUri = false;
        try {
            Uri uri = Uri.parse(path);
            isUri = true;
            return isUri;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  isUri;

    }

    public String getPath() {
        return path;
    }
}
