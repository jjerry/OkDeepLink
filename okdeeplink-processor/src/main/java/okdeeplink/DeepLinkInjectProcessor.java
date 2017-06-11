
package okdeeplink;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.aspectj.lang.JoinPoint;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import okdeeplink.util.ElementUtils;
import okdeeplink.util.Logger;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
public class DeepLinkInjectProcessor extends AbstractProcessor {

    private static final String PACKAGE_NAME = DeepLinkInjectProcessor.class.getPackage().getName();

    private static final ClassName BUNDLE = ClassName.get("android.os", "Bundle");
    private static final ClassName INTENT = ClassName.get("android.content", "Intent");
    private static final ClassName BUNDLE_COMPACT = ClassName.get(PACKAGE_NAME, "BundleCompact");

    private static final String ACTIVITY = "android.app.Activity";


    private static final String INJECTOR_SUFFIX = "$$Injector";

    private static final List<String> SUPPORT_INJECT = new ArrayList<>();

    static {
        SUPPORT_INJECT.add(ACTIVITY);
        SUPPORT_INJECT.add("android.app.Fragment");
        SUPPORT_INJECT.add("android.support.v4.app.Fragment");
    }


    private Filer filer;
    private Logger logger;

    private Map<TypeElement, List<Element>> targetInjectElements;
    private Elements elements;
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        logger = new Logger(processingEnv.getMessager());
        filer = processingEnv.getFiler();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> ret = new HashSet<>();
        ret.add(Query.class.getCanonicalName());
        ret.add(Service.class.getCanonicalName());
        return ret;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        List<Element> injectElements = new ArrayList<>();
        List<Element> queryElements = generateQueryElements(roundEnv);
        List<Element> serviceElements = generateServiceElements(roundEnv);
        injectElements.addAll(queryElements);
        injectElements.addAll(serviceElements);
        if (ElementUtils.isEmpty(injectElements)) {
            return false;
        }
        targetInjectElements = findInjectElements(injectElements);
        if (ElementUtils.isEmpty(targetInjectElements)) {
            return false;
        }
        for (Map.Entry<TypeElement, List<Element>> injectElementEntrySet : targetInjectElements.entrySet()) {
            TypeElement targetElement = injectElementEntrySet.getKey();
            List<Element> fieldElements = injectElementEntrySet.getValue();

            MethodSpec injectQueryMethod = geneOnCreateQueryMethod(targetElement, fieldElements);

            MethodSpec injectServiceMethod = geneInjectServiceMethod(targetElement, fieldElements);

            MethodSpec saveInstanceMethod = geneSaveInstanceMethod(targetElement, fieldElements);


            MethodSpec newIntentMethod = geneOnNewIntentQueryMethod(targetElement, fieldElements);

            String fileName = targetElement.getSimpleName() + INJECTOR_SUFFIX;
            TypeSpec.Builder helper = TypeSpec.classBuilder(fileName)
                    .addModifiers(PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(Aspect.class).build())
                    .addMethod(injectQueryMethod)
                    .addMethod(injectServiceMethod)
                    .addMethod(saveInstanceMethod);

            TypeMirror typeMirror = elements.getTypeElement(ACTIVITY).asType();
            if (types.isSubtype(targetElement.asType(), typeMirror)) {
                helper.addMethod(newIntentMethod);
            }

            try {
                PackageElement packageElement = (PackageElement) targetElement.getEnclosingElement();
                JavaFile.builder(packageElement.getQualifiedName().toString(), helper.build()).build().writeTo(filer);
            } catch (IOException e) {
                logger.error("Error creating inject file", targetElement);
            }

        }
        return true;
    }

    private MethodSpec geneOnCreateQueryMethod(TypeElement targetElement, List<Element> fieldElements) {

        MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder("onCreate")
                .addModifiers(PUBLIC)
                .addException(Throwable.class)
                .addAnnotation(AnnotationSpec.builder(Around.class).addMember("value", "$S", "execution(* " + targetElement.getQualifiedName() + ".onCreate(..))").build())
                .addParameter(ProceedingJoinPoint.class, "joinPoint");

        CodeBlock.Builder injectQueryCodeBuilder = geneOnCreateCodeBuilder(targetElement);

        List<String> queryKeys = new ArrayList<>();

        for (Element queryElement : fieldElements) {
            if (queryElement.getAnnotation(Query.class) != null) {
                String queryKey = queryElement.getAnnotation(Query.class).value();
                if (queryKeys.contains(queryKey)) {
                    logger.error("The inject query key cannot be Duplicate" + Query.class.getCanonicalName(), queryElement);
                }
                queryKeys.add(queryKey);

                injectQueryCodeBuilder
                        .beginControlFlow("try")
                        .add("target.$L= $T.getValue(dataBundle,$S,$T.class);\n", queryElement, BUNDLE_COMPACT, queryKey, queryElement)
                        .nextControlFlow("catch ($T e)", Exception.class)
                        .addStatement("e.printStackTrace()")
                        .endControlFlow();

            }
        }
        injectQueryCodeBuilder.add("joinPoint.proceed();\n");
        injectMethodBuilder.addCode(injectQueryCodeBuilder.build());

        return injectMethodBuilder.build();
    }


    private MethodSpec geneOnNewIntentQueryMethod(TypeElement targetElement, List<Element> fieldElements) {

        MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder("onNewIntent")
                .addModifiers(PUBLIC)
                .addException(Throwable.class)
                .addAnnotation(AnnotationSpec.builder(Around.class).addMember("value", "$S", "execution(* " + targetElement.getQualifiedName() + ".onNewIntent(..))").build())
                .addParameter(ProceedingJoinPoint.class, "joinPoint");

        CodeBlock.Builder injectQueryCodeBuilder = geneOnNewIntentCodeBuilder(targetElement);

        List<String> queryKeys = new ArrayList<>();

        for (Element queryElement : fieldElements) {
            if (queryElement.getAnnotation(Query.class) != null) {
                String queryKey = queryElement.getAnnotation(Query.class).value();
                if (queryKeys.contains(queryKey)) {
                    logger.error("The inject query key cannot be Duplicate" + Query.class.getCanonicalName(), queryElement);
                }
                queryKeys.add(queryKey);

                injectQueryCodeBuilder
                        .beginControlFlow("try")
                        .add("target.$L= $T.getValue(dataBundle,$S,$T.class);\n", queryElement, BUNDLE_COMPACT, queryKey, queryElement)
                        .nextControlFlow("catch ($T e)", Exception.class)
                        .addStatement("e.printStackTrace()")
                        .endControlFlow();

            }
        }
        injectQueryCodeBuilder.add("joinPoint.proceed();\n");
        injectMethodBuilder.addCode(injectQueryCodeBuilder.build());

        return injectMethodBuilder.build();
    }


    private MethodSpec geneInjectServiceMethod(TypeElement targetElement, List<Element> fieldElements) {

        MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder("onCreateService")
                .addModifiers(PUBLIC)
                .addException(Throwable.class)
                .addAnnotation(AnnotationSpec.builder(Around.class).addMember("value", "$S", "execution(* " + targetElement.getQualifiedName() + ".onCreate(..))").build())
                .addParameter(ProceedingJoinPoint.class, "joinPoint");

        CodeBlock.Builder injectServiceCodeBuilder = CodeBlock.builder();
        injectServiceCodeBuilder.add("$T target = ($T)joinPoint.getTarget();\n", targetElement, targetElement);
        List<TypeName> serviceTypeNames = new ArrayList<>();

        for (Element queryElement : fieldElements) {
            if (queryElement.getAnnotation(Service.class) != null) {
                TypeName className = TypeName.get(queryElement.asType());
                if (serviceTypeNames.contains(className)) {
                    logger.error("The inject " + className + " cannot be Duplicate", queryElement);
                }
                serviceTypeNames.add(className);
                injectServiceCodeBuilder.add("target.$L=  new $T(target).build($T.class);\n ", queryElement, DeepLinkServiceProcessor.DEEP_LINK_CLIENT, queryElement);

            }
        }
        injectServiceCodeBuilder.add("joinPoint.proceed();\n");
        injectMethodBuilder.addCode(injectServiceCodeBuilder.build());

        return injectMethodBuilder.build();
    }


    private MethodSpec geneSaveInstanceMethod(TypeElement targetElement, List<Element> fieldElements) {

        MethodSpec.Builder saveInstanceMethodBuilder = methodBuilder("onSaveInstanceState")
                .addModifiers(PUBLIC)
                .addException(Throwable.class)
                .addAnnotation(AnnotationSpec
                        .builder(After.class)
                        .addMember("value", "$S", "execution(* " + targetElement.getQualifiedName() + ".onSaveInstanceState(..))")
                        .build())
                .addParameter(JoinPoint.class, "joinPoint");

        CodeBlock.Builder saveInstanceCodeBuilder = geneSaveInstanceCodeBuilder(targetElement);

        List<String> queryKeys = new ArrayList<>();

        for (Element queryElement : fieldElements) {
            if (queryElement.getAnnotation(Query.class) != null) {

                String queryKey = queryElement.getAnnotation(Query.class).value();
                if (queryKeys.contains(queryKey)) {
                    logger.error("The inject query key cannot be Duplicate" + Query.class.getCanonicalName(), queryElement);
                }
                queryKeys.add(queryKey);
                saveInstanceCodeBuilder.add("intent.putExtra($S,target.$L);\n", queryKey, queryElement);
            }
        }

        saveInstanceCodeBuilder.add("saveBundle.putAll(intent.getExtras());\n");

        saveInstanceMethodBuilder.addCode(saveInstanceCodeBuilder.build());

        return saveInstanceMethodBuilder.build();
    }

    private CodeBlock.Builder geneSaveInstanceCodeBuilder(TypeElement targetElement) {
        CodeBlock.Builder blockBuilderSave = CodeBlock.builder();
        blockBuilderSave.add("$T target = ($T)joinPoint.getTarget();\n", targetElement, targetElement);
        blockBuilderSave.add("$T saveBundle = ($T)joinPoint.getArgs()[0];\n", BUNDLE, BUNDLE);
        blockBuilderSave.add("$T intent = new $T();\n", INTENT, INTENT);
        return blockBuilderSave;
    }

    private CodeBlock.Builder geneOnCreateCodeBuilder(TypeElement targetElement) {
        CodeBlock.Builder injectQueryCodeBuilder = CodeBlock.builder();
        injectQueryCodeBuilder.add("$T target = ($T)joinPoint.getTarget();\n", targetElement, targetElement);
        injectQueryCodeBuilder.add("$T dataBundle = new $T();\n", BUNDLE, BUNDLE);
        injectQueryCodeBuilder.add("$T saveBundle = ($T)joinPoint.getArgs()[0];\n", BUNDLE, BUNDLE);
        injectQueryCodeBuilder.add("$T targetBundle = $T.getSupportBundle(target);\n", BUNDLE, BUNDLE_COMPACT);
        injectQueryCodeBuilder.beginControlFlow("if(targetBundle != null)");
        injectQueryCodeBuilder.add("dataBundle.putAll(targetBundle);\n");
        injectQueryCodeBuilder.endControlFlow();
        injectQueryCodeBuilder.beginControlFlow("if(saveBundle != null)");
        injectQueryCodeBuilder.add("dataBundle.putAll(saveBundle);\n");
        injectQueryCodeBuilder.endControlFlow();
        return injectQueryCodeBuilder;
    }

    private CodeBlock.Builder geneOnNewIntentCodeBuilder(TypeElement targetElement) {
        CodeBlock.Builder injectQueryCodeBuilder = CodeBlock.builder();
        injectQueryCodeBuilder.add("$T target = ($T)joinPoint.getTarget();\n", targetElement, targetElement);
        injectQueryCodeBuilder.add("$T targetIntent = ($T)joinPoint.getArgs()[0];\n", INTENT, INTENT);
        injectQueryCodeBuilder.add("$T dataBundle = targetIntent.getExtras();\n", BUNDLE);

        return injectQueryCodeBuilder;
    }


    private List<Element> generateQueryElements(RoundEnvironment roundEnv) {
        Set<? extends Element> deepLinkPathElements = roundEnv.getElementsAnnotatedWith(Query.class);
        List<Element> queryElements = new ArrayList<>();
        for (Element element : deepLinkPathElements) {
            Query deepLinkPathAnnotation = element.getAnnotation(Query.class);
            ElementKind kind = element.getKind();
            if (kind != ElementKind.PARAMETER && kind != ElementKind.FIELD) {
                logger.error("Only classes and methods can be annotated with @" + Query.class.getCanonicalName(), element);
            }
            String queryKey = deepLinkPathAnnotation.value();
            if (queryKey == null || queryKey.length() == 0) {
                logger.error("The inject query cannot be null @" + Query.class.getCanonicalName(), element);
            }

            if (kind == ElementKind.FIELD) {
                Element enclosingElement = element.getEnclosingElement();
                if (enclosingElement.getKind() != CLASS) {
                    logger.error("@" + Query.class.getCanonicalName() + "only be contained in classes", element);
                }
                TypeElement typeElement = (TypeElement) enclosingElement;
                boolean support = isSupportInject(typeElement);
                if (!support) {
                    logger.error("@" + Query.class.getCanonicalName() + "only support inject in activity or fragment", element);
                }
                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    logger.error("The inject query fields can not be private, please check field @" + Query.class.getCanonicalName() + "in class" + typeElement.getQualifiedName(), element);
                }
                queryElements.add(element);
            }
        }
        return queryElements;
    }


    private List<Element> generateServiceElements(RoundEnvironment roundEnv) {
        Set<? extends Element> deepLinkServiceElements = roundEnv.getElementsAnnotatedWith(Service.class);
        List<Element> serviceElements = new ArrayList<>();
        for (Element element : deepLinkServiceElements) {
            serviceElements.add(element);
        }
        return serviceElements;
    }

    private Map<TypeElement, List<Element>> findInjectElements(List<Element> queryInjectElements) {

        Map<TypeElement, List<Element>> map = new HashMap<>();

        for (Element queryInjectElement : queryInjectElements) {
            TypeElement enclosingElement = (TypeElement) queryInjectElement.getEnclosingElement();
            List<Element> builder = map.get(enclosingElement);
            if (builder == null) {
                builder = new ArrayList<>();
                map.put(enclosingElement, builder);
            }
            builder.add(queryInjectElement);
        }
        return map;
    }


    private boolean isSupportInject(TypeElement typeElement) {
        for (String supportClass : SUPPORT_INJECT) {
            TypeMirror typeMirror = elements.getTypeElement(supportClass).asType();
            if (types.isSubtype(typeElement.asType(), typeMirror)) {
                return true;
            }
        }
        return false;
    }


}
