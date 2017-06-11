
package okdeeplink.element;

import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;

import okdeeplink.Action;
import okdeeplink.Activity;
import okdeeplink.Intercept;
import okdeeplink.Path;
import okdeeplink.Uri;
import okdeeplink.util.ElementUtils;

public final class AddressElement {

    private TypeMirror activityTypeMirror;
    List<String> deepLinkPaths = new ArrayList<>();
    List<TypeMirror> interceptorClassNames = new ArrayList<>();

    ClassName activityClassName;
    String intentAction;
    String uri;

    Element element;


    public AddressElement(Element element) {
        this.element = element;
        Path deepLinkPathAnnotation = element.getAnnotation(Path.class);
        Activity deepLinkActivityAnnotation = element.getAnnotation(Activity.class);
        Intercept interceptAnnotation = element.getAnnotation(Intercept.class);
        Action actionAnnotation = element.getAnnotation(Action.class);
        Uri uriAnnotation  = element.getAnnotation(Uri.class);
        if (deepLinkPathAnnotation != null) {
            deepLinkPaths.addAll(Arrays.asList(deepLinkPathAnnotation.value()));
        }
        if (deepLinkActivityAnnotation != null) {
            try {
                deepLinkActivityAnnotation.value();
            } catch (MirroredTypeException mte) {
                activityTypeMirror = mte.getTypeMirror();
                activityClassName = ElementUtils.getClassName(activityTypeMirror);
            }
        }
        if (actionAnnotation != null) {
            intentAction = actionAnnotation.value();
        }

        if (uriAnnotation != null) {
            uri = uriAnnotation.value();
        }

        if (interceptAnnotation != null) {
            try {
                interceptAnnotation.value();
            } catch (MirroredTypesException mte) {
                List<? extends TypeMirror> typeMirrors = mte.getTypeMirrors();
                if (!ElementUtils.isEmpty(typeMirrors)) {
                    for (TypeMirror typeMirror : typeMirrors) {
                        interceptorClassNames.add(typeMirror);
                    }
                }

            }
        }

    }

    public TypeMirror getActivityTypeMirror() {
        return activityTypeMirror;
    }

    public List<String> getDeepLinkPaths() {
        return deepLinkPaths;
    }

    public boolean isPathsEmpty() {
        return ElementUtils.isEmpty(getDeepLinkPaths());
    }

    public boolean isActionEmpty() {
        return intentAction == null || intentAction.length() == 0;
    }

    public boolean isUriEmpty() {
        return uri == null || uri.length() == 0;
    }

    public List<TypeMirror> getInterceptors() {
        return interceptorClassNames;
    }

    public ClassName getActivityClassName() {
        return activityClassName;
    }

    public Element getElement() {
        return element;
    }

    public String getIntentAction() {
        return intentAction;
    }

    public String getUri() {
        return uri;
    }


}
