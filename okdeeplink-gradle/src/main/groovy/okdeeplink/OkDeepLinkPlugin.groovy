package okdeeplink

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by zhangqijun on 2017/5/16.
 */

public class OkDeepLinkPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {


        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)
        def hasApt = project.plugins.hasPlugin('com.neenbedankt.android-apt')
        def usesAndroidAspectJxPlugin = project.plugins.hasPlugin('android-aspectjx')
        if (hasApp || hasLib) {
            if (hasApp && !usesAndroidAspectJxPlugin) {
                project.pluginManager.apply(com.hujiang.gradle.plugin.android.aspectjx.AndroidAspectJXPlugin);
            }
            project.dependencies {
                compile 'org.aspectj:aspectjrt:1.8.9'
                compile 'com.hongjun:okdeeplink-api:1.0.0'
                if (hasApt){
                    apt 'com.hongjun:okdeeplink-processor:1.0.0'
                }else {
                    annotationProcessor 'com.hongjun:okdeeplink-processor:1.0.0'
                }

            }
        }
        project.configurations.all {
            resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
        }
    }

}