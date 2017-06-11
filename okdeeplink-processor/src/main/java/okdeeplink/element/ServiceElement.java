
package okdeeplink.element;

import com.squareup.javapoet.ClassName;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

public final class ServiceElement {


    ClassName serviceClassName;
    String packageName;
    ClassName providerClassName;
    List<? extends Element> enclosedElements;
    Element element;

    public ServiceElement(Element serviceElement) {
        element = serviceElement;
        String className = serviceElement.getSimpleName().toString();
        PackageElement packageElement = (PackageElement) serviceElement.getEnclosingElement();
        packageName = packageElement.getQualifiedName().toString();
        serviceClassName = ClassName.get(packageName, className);
        enclosedElements = serviceElement.getEnclosedElements();
        String serviceProviderName = className +"$$Provider";
        providerClassName = ClassName.get(packageName, serviceProviderName);

    }


    public String getPackageName() {
        return packageName;
    }

    public ClassName getProviderClassName() {
        return providerClassName;
    }

    public ClassName getClassName() {
        return serviceClassName;
    }

    public List<? extends Element> getEnclosedElements() {
        return enclosedElements;
    }

    public Element getElement() {
        return element;
    }
}
