
package okdeeplink;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import okdeeplink.element.AddressElement;
import okdeeplink.util.ElementUtils;
import okdeeplink.util.Logger;
import okdeeplink.util.MethodGenerator;

import static okdeeplink.DeepLinkInterceptorProcessor.INTERCEPTOR;


@AutoService(Processor.class)
public class DeepLinkServiceProcessor extends AbstractProcessor {

    private static final String INTERNAL_HOST = "app://deeplink";

    private static final ClassName INTENT = ClassName.get("android.content", "Intent");
    private static final ClassName URI = ClassName.get("android.net", "Uri");


    public static final String PACKAGE_NAME = DeepLinkServiceProcessor.class.getPackage().getName();

    private static final ClassName DEEP_LINK_ENTRY = ClassName.get(PACKAGE_NAME, "Address");

    public static final ClassName DEEP_LINK_CLIENT = ClassName.get(PACKAGE_NAME, "DeepLinkClient");

    public static final String INIT_METHOD_NAME = ElementUtils.getName(DEEP_LINK_CLIENT) + ".init(..)";


    public static final String BUILD_METHOD_NAME = ElementUtils.getName(DEEP_LINK_CLIENT) + ".build(..)";

    public static final String BUILD_REQUEST_METHOD_NAME = "buildRequest";

    private static final String PROVIDER_SUFFIX = "$$Provider";


    private static final ClassName DEEP_LINK_REQUEST = ClassName.get(PACKAGE_NAME, "Request");

    private static final TypeName DEEP_LINK_OBSERVABLE = ParameterizedTypeName.get(ClassName.get("rx", "Observable"), ClassName.get(PACKAGE_NAME, "Response"));


    private static final List<TypeName> SUPPORT_RETURN_TYPE = new ArrayList<>();

    private static final String ACTIVITY = "android.app.Activity";


    static {
        SUPPORT_RETURN_TYPE.add(DEEP_LINK_REQUEST);
        SUPPORT_RETURN_TYPE.add(TypeName.VOID);
        SUPPORT_RETURN_TYPE.add(DEEP_LINK_OBSERVABLE);
    }


    private List<String> addresses = new ArrayList<>();

    private Filer filer;
    private Logger logger;
    private Types types;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        logger = new Logger(processingEnv.getMessager());
        filer = processingEnv.getFiler();
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> ret = new HashSet<>();
        ret.add(Path.class.getCanonicalName());
        ret.add(Action.class.getCanonicalName());
        ret.add(Activity.class.getCanonicalName());
        ret.add(Uri.class.getCanonicalName());
        return ret;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {


        List<AddressElement> addressElements = generateAddressElements(roundEnv);

        if (ElementUtils.isEmpty(addressElements)) {
            return false;
        }
        Map<TypeElement, List<AddressElement>> serviceElements = findServiceElements(addressElements);
        if (ElementUtils.isEmpty(serviceElements)) {
            return false;
        }
        for (Map.Entry<TypeElement, List<AddressElement>> serviceElementEntrySet : serviceElements.entrySet()) {
            TypeElement serviceElement = serviceElementEntrySet.getKey();
            List<AddressElement> serviceAddressElements = serviceElementEntrySet.getValue();
            if (ElementUtils.isEmpty(serviceAddressElements)) {
                break;
            }
            try {
                generateDeepLinkService(serviceElement, serviceAddressElements);
            } catch (IOException e) {
                logger.error("Error creating matcher file", serviceElement);
            }
        }

        return true;
    }


    private List<AddressElement> generateAddressElements(RoundEnvironment roundEnv) {
        List<Element> annotationElements = new ArrayList<>();
        for (String annotationType : getSupportedAnnotationTypes()) {
            TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(annotationType);
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(typeElement);
            for (Element annotatedElement : annotatedElements) {
                if (!annotationElements.contains(annotatedElement)) {
                    annotationElements.add(annotatedElement);
                }
            }
        }

        List<AddressElement> serviceElements = new ArrayList<>();
        for (Element element : annotationElements) {
            ElementKind kind = element.getKind();
            if (kind != ElementKind.METHOD) {
                logger.error("Only classes and methods can be  with " + getSupportedAnnotationTypes(), element);
            }
            if (!isSupportReturnType((ExecutableElement) element)) {
                logger.error("method only support return type is " + SUPPORT_RETURN_TYPE.toString(), element);
            }

            Element enclosingElement = element.getEnclosingElement();
            String name = enclosingElement.getSimpleName().toString();
            if (!name.endsWith("Service")) {
                logger.error(name + "this class must be in end with Service", enclosingElement);
            }

            if (enclosingElement.getKind() != ElementKind.INTERFACE) {

                logger.error(name + "this class must be interface", enclosingElement);
            }
            serviceElements.add(new AddressElement(element));
        }
        return serviceElements;
    }


    private Map<TypeElement, List<AddressElement>> findServiceElements(List<AddressElement> queryInjectElements) {

        Map<TypeElement, List<AddressElement>> map = new HashMap<>();

        for (AddressElement queryInjectElement : queryInjectElements) {
            TypeElement enclosingElement = (TypeElement) queryInjectElement.getElement().getEnclosingElement();
            List<AddressElement> builder = map.get(enclosingElement);
            if (builder == null) {
                builder = new ArrayList<>();
                map.put(enclosingElement, builder);
            }
            builder.add(queryInjectElement);
        }
        return map;
    }


    private void generateDeepLinkService(TypeElement serviceElements,
                                         List<AddressElement> deepLinkMatchElements)
            throws IOException {

        ClassName providerClassName = getServiceProviderClassName(serviceElements);

        MethodSpec initMethod = generateInitMethod(deepLinkMatchElements);


        FieldSpec activity = FieldSpec
                .builder(DEEP_LINK_CLIENT, "deepLinkClient",
                        Modifier.PUBLIC)
                .build();


        MethodSpec activityConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(DEEP_LINK_CLIENT, "deepLinkClient")
                .addCode("this.deepLinkClient= deepLinkClient;\n")
                .build();
        TypeSpec.Builder serviceProviderBuilder = TypeSpec.classBuilder(providerClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AnnotationSpec.builder(Aspect.class).build())
                .addSuperinterface(ClassName.get(serviceElements))
                .addField(activity)
                .addMethod(activityConstructor)
                .addMethod(initMethod);


        for (AddressElement matchElement : deepLinkMatchElements) {
            ExecutableElement element = (ExecutableElement) matchElement.getElement();
            serviceProviderBuilder.addMethod(geneOverServiceMethod(element));
        }

        MethodSpec buildMethodSpec = generateBuildMethod(serviceElements);
        serviceProviderBuilder.addMethod(buildMethodSpec);
        JavaFile.builder(ClassName.get(serviceElements).packageName(), serviceProviderBuilder.build())
                .build()
                .writeTo(filer);
    }


    private MethodSpec generateInitMethod(List<AddressElement> addressElements) {

        CodeBlock.Builder initializer = CodeBlock.builder();
        for (AddressElement element : addressElements) {
            List<String> deepLinkPaths = element.getDeepLinkPaths();
            if (!element.isPathsEmpty()) {
                for (String deepLinkPath : deepLinkPaths) {
                    TypeMirror activityTypeMirror = element.getActivityTypeMirror();
                    TypeMirror supportTypeMirror = elements.getTypeElement(ACTIVITY).asType();
                    if (activityTypeMirror != null) {
                        if (!types.isSubtype(activityTypeMirror, supportTypeMirror)) {
                            logger.error(Activity.class.getName() + " only support class which extends " + ACTIVITY, element.getElement());
                        }
                    }
                    ClassName activityClassName = element.getActivityClassName();
                    if (activityClassName != null) {
                        initializer.add("$T.addAddress(new $T($S, $T.class));\n",
                                DEEP_LINK_CLIENT, DEEP_LINK_ENTRY, deepLinkPath, activityClassName);
                    }
                }
            }

        }
        MethodSpec.Builder initMethod = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(After.class).addMember("value", "$S", "execution(* " + INIT_METHOD_NAME + ")").build())
                .addCode(initializer.build());

        return initMethod.build();
    }


    private MethodSpec geneOverServiceMethod(ExecutableElement element) {

        TypeName returnType = ClassName.get(element.getReturnType());

        MethodSpec.Builder serviceMethodBuilder = new MethodGenerator(element)
                .overMethod(element.getSimpleName().toString());
        CodeBlock.Builder serviceStartMethodBuilder = CodeBlock
                .builder()
                .add("$T intent = new $T();\n", INTENT, INTENT);


        List<? extends VariableElement> params = element.getParameters();

        String requestCode = null;

        Map<String, VariableElement> uriReplaces = new HashMap<>();

        if (params != null) {
            for (int i = 0; i < params.size(); i++) {
                VariableElement elem = params.get(i);
                Query query = elem.getAnnotation(Query.class);
                RequestCode requestCodeAnnotation = elem.getAnnotation(RequestCode.class);
                UriReplace uriReplaceAnnotation = elem.getAnnotation(UriReplace.class);
                if (uriReplaceAnnotation != null) {

                    if (!ClassName.get(elem.asType()).equals(ClassName.get(String.class))) {
                        logger.error(UriReplace.class + "must be annotation " + String.class, elem);
                    }
                    uriReplaces.put(uriReplaceAnnotation.value(), elem);
                }
                if (query != null) {
                    serviceStartMethodBuilder.add("intent.putExtra($S,$L);\n", query.value(), elem);
                } else if (requestCodeAnnotation != null) {
                    if (requestCode != null) {
                        logger.error("Method Must have only one  @" + RequestCode.class, elem);
                    }
                    TypeName requestCodeTypeName = TypeName.get(elem.asType());
                    if (TypeName.INT.equals(requestCodeTypeName)) {
                        requestCode = elem.getSimpleName().toString();
                    } else {
                        logger.error("@" + RequestCode.class + " must be annotation int variable", elem);
                    }
                }
            }
        }

        AddressElement addressElement = new AddressElement(element);

        List<String> pathList = addressElement.getDeepLinkPaths();
        List<TypeMirror> interceptorList = addressElement.getInterceptors();

        if (!addressElement.isPathsEmpty()) {
            if (!addressElement.isUriEmpty()) {
                logger.error("path or uri not use in same time ", element);
            }
            for (String path : pathList) {
                if (!path.startsWith("/")) {
                    if (addressElement.getActivityClassName() == null) {
                        logger.error("if you what start activity may be use  " + Uri.class + " replace " + Path.class, element);
                    }
                    logger.error("path must be start with / ", element);
                }
                if (addresses.contains(path)) {
                    logger.error("Duplicate path: " + path, element);
                }
            }
            addresses.addAll(pathList);
            String path = pathList.get(0);
            String url = path.startsWith("/") ? INTERNAL_HOST + path : path;
            serviceStartMethodBuilder.add(String.format("intent.setData($T.parse(\"%s\"));\n", url), URI);
        }
        if (!addressElement.isUriEmpty()) {
            if (addressElement.getActivityClassName() != null) {
                logger.error("uri not need  activity annotation", element);
            }
            serviceStartMethodBuilder.add("$T uri = $S;\n", String.class, addressElement.getUri());
            for (String uriReplace : uriReplaces.keySet()) {
                serviceStartMethodBuilder.add("uri = uri.replace($S,$L);\n", "{" + uriReplace + "}", uriReplaces.get(uriReplace));
            }
            serviceStartMethodBuilder.add("intent.setData($T.parse(uri));\n", URI);
        }


        if (!addressElement.isActionEmpty()) {
            serviceStartMethodBuilder.add("intent.setAction($S);\n", addressElement.getIntentAction());
        }
        serviceStartMethodBuilder.add("$T request = deepLinkClient.$L(intent);\n", DEEP_LINK_REQUEST, BUILD_REQUEST_METHOD_NAME);
        serviceStartMethodBuilder.beginControlFlow("if (request != null)");
        if (requestCode != null) {
            serviceStartMethodBuilder.add("request.setRequestCode($L);\n", requestCode);
        }
        if (!ElementUtils.isEmpty(interceptorList)) {
            for (TypeMirror interceptorTypeMirror : interceptorList) {
                TypeMirror supportTypeMirror = elements.getTypeElement(ElementUtils.getName(INTERCEPTOR)).asType();
                TypeName typeName = ElementUtils.getClassName(interceptorTypeMirror);
                if (!types.isSubtype(interceptorTypeMirror, supportTypeMirror) || typeName.equals(TypeName.OBJECT)) {
                    logger.error(Intercept.class.getName() + " only support class which extends " + INTERCEPTOR, element);
                }
                serviceStartMethodBuilder.add("request.addInterceptor(new $T());\n", ElementUtils.getClassName(interceptorTypeMirror));
            }
        }
        if (DEEP_LINK_REQUEST.equals(returnType)) {
            serviceStartMethodBuilder.endControlFlow();
            serviceStartMethodBuilder.add("return request;\n");


        } else if (TypeName.VOID.equals(returnType)) {
            serviceStartMethodBuilder.add("request.start();\n");
            serviceStartMethodBuilder.endControlFlow();

        } else if (DEEP_LINK_OBSERVABLE.equals(returnType)) {

            if (requestCode != null) {
                logger.error(returnType + " not need " + RequestCode.class, element);
            }
            serviceStartMethodBuilder.endControlFlow();
            serviceStartMethodBuilder.add("return request.createResponse();\n");
        }


        serviceMethodBuilder.addCode(serviceStartMethodBuilder.build());
        return serviceMethodBuilder.build();
    }

    private ClassName getServiceProviderClassName(TypeElement serviceElements) {
        String className = serviceElements.getSimpleName().toString();
        String packageName = ClassName.get(serviceElements).packageName();
        String serviceProviderName = className + PROVIDER_SUFFIX;
        return ClassName.get(packageName, serviceProviderName);
    }


    private MethodSpec generateBuildMethod(TypeElement deepLinkServiceElement) {

        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
        codeBlockBuilder.add("$T target = ($T)joinPoint.getTarget();\n", DEEP_LINK_CLIENT, DEEP_LINK_CLIENT);
        codeBlockBuilder.beginControlFlow("if (joinPoint.getArgs() == null || joinPoint.getArgs().length != 1)");
        codeBlockBuilder.add("return joinPoint.proceed();\n");
        codeBlockBuilder.endControlFlow();
        codeBlockBuilder.add("$T arg = joinPoint.getArgs()[0];\n", Object.class);
        codeBlockBuilder.beginControlFlow("if (arg instanceof Class)");
        codeBlockBuilder.add("$T buildClass = ($T) arg;\n", Class.class, Class.class);
        codeBlockBuilder.beginControlFlow("if (buildClass.isAssignableFrom(getClass()))");
        codeBlockBuilder.add("return new $T(target);\n", getServiceProviderClassName(deepLinkServiceElement));
        codeBlockBuilder.endControlFlow();
        codeBlockBuilder.endControlFlow();
        codeBlockBuilder.add("return joinPoint.proceed();\n");

        MethodSpec.Builder initMethod = MethodSpec.methodBuilder("aroundBuildMethod")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ProceedingJoinPoint.class, "joinPoint")
                .returns(Object.class)
                .addException(Throwable.class)
                .addAnnotation(AnnotationSpec.builder(Around.class).addMember("value", "$S", "execution(* " + BUILD_METHOD_NAME + ")").build())
                .addCode(codeBlockBuilder.build());

        return initMethod.build();
    }


    private boolean isSupportReturnType(ExecutableElement executableElement) {
        if (executableElement == null) {
            return false;
        }
        TypeName matchReturnType = ClassName.get(executableElement.getReturnType());
        for (TypeName supportClass : SUPPORT_RETURN_TYPE) {
            if (supportClass.equals(matchReturnType)) {
                return true;
            }
        }
        return false;
    }

}
