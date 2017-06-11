
package okdeeplink.element;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

import okdeeplink.Intercept;

public final class InterceptorElement {


    private final ClassName initClassName;
    ClassName interceptorClassName;
    String packageName;
    Element element;
    String path;

    public InterceptorElement(Element serviceElement) {
        element = serviceElement;
        String className = serviceElement.getSimpleName().toString();
        PackageElement packageElement = (PackageElement) serviceElement.getEnclosingElement();
        packageName = packageElement.getQualifiedName().toString();
        interceptorClassName = ClassName.get(packageName, className);
        String serviceProviderName = className + "$$Injector";
        initClassName = ClassName.get(packageName, serviceProviderName);
        Intercept intercept = serviceElement.getAnnotation(Intercept.class);
        if (intercept != null) {
            path = intercept.path();
        }
    }


    public ClassName getInitClassName() {
        return initClassName;
    }

    public String getPackageName() {
        return packageName;
    }


    public ClassName getClassName() {
        return interceptorClassName;
    }

    public Element getElement() {
        return element;
    }

    public String getPath() {
        return path;
    }
}
