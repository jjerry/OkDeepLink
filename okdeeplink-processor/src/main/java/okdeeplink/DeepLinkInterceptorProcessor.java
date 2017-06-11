
package okdeeplink;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.IOException;
import java.util.ArrayList;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import okdeeplink.element.InterceptorElement;
import okdeeplink.util.ElementUtils;
import okdeeplink.util.Logger;

import static javax.lang.model.element.ElementKind.CLASS;
import static okdeeplink.DeepLinkServiceProcessor.DEEP_LINK_CLIENT;
import static okdeeplink.DeepLinkServiceProcessor.PACKAGE_NAME;


@AutoService(Processor.class)
public class DeepLinkInterceptorProcessor extends AbstractProcessor {

    public static final String GLOBAL_INTERCEPTORS_METHOD_NAME = ElementUtils.getName(DEEP_LINK_CLIENT) + ".getGlobalInterceptors(..)";

    public static final String PATH_INTERCEPTORS_METHOD_NAME = ElementUtils.getName(DEEP_LINK_CLIENT) + ".getPathInterceptors(..)";

    public static final ClassName INTERCEPTOR = ClassName.get(PACKAGE_NAME, "Interceptor");


    private Filer filer;
    private Logger logger;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        logger = new Logger(processingEnv.getMessager());
        filer = processingEnv.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> ret = new HashSet<>();
        ret.add(Intercept.class.getCanonicalName());
        return ret;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        List<InterceptorElement> interceptorElements = generateInterceptorElements(roundEnv);
        if (ElementUtils.isEmpty(interceptorElements)) {
            return false;
        }

        for (InterceptorElement interceptorElement : interceptorElements) {
            String path = interceptorElement.getPath();
            MethodSpec.Builder methodBuilder;
            if (path == null || path.length() == 0) {
                methodBuilder = geneGlobalInterceptorsBuilder(interceptorElement);
            }else {
                methodBuilder = genePathInterceptorsBuilder(interceptorElement);
            }

            try {
                TypeSpec.Builder interceptorInitBuilder = TypeSpec.classBuilder(interceptorElement.getInitClassName())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(AnnotationSpec
                                .builder(Aspect.class)
                                .build())
                        .addMethod(methodBuilder.build());
                JavaFile.builder(interceptorElement.getPackageName(), interceptorInitBuilder.build())
                        .build()
                        .writeTo(filer);
            } catch (IOException e) {
                logger.error("Error creating matcher file", interceptorElement.getElement());
            }
        }


        return true;
    }


    private MethodSpec.Builder genePathInterceptorsBuilder(InterceptorElement interceptorElement) {
        CodeBlock.Builder builder = CodeBlock.builder();
        TypeName typeName = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), INTERCEPTOR);
        builder.add("$T result = ($T)joinPoint.proceed();\n", typeName, typeName);
        String path = interceptorElement.getPath();
        builder.add("result.put($S,new $T());\n",
                path,
                interceptorElement.getClassName());
        builder.add("return result;\n");
        return MethodSpec.methodBuilder("getPathInterceptors")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ProceedingJoinPoint.class, "joinPoint")
                .returns(typeName)
                .addException(Throwable.class)
                .addAnnotation(AnnotationSpec
                        .builder(Around.class)
                        .addMember("value", "$S", "execution(* " + PATH_INTERCEPTORS_METHOD_NAME + ")").build())
                .addCode(builder.build());
    }


    private MethodSpec.Builder geneGlobalInterceptorsBuilder(InterceptorElement interceptorElement) {
        CodeBlock.Builder builder = CodeBlock.builder();
        TypeName typeName = ParameterizedTypeName.get(ClassName.get(List.class), INTERCEPTOR);
        builder.add("$T result = ($T)joinPoint.proceed();\n", typeName, typeName);
        builder.add("result.add(new $T());\n",
                interceptorElement.getClassName());
        builder.add("return result;\n");
        return MethodSpec.methodBuilder("getGlobalInterceptors")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ProceedingJoinPoint.class, "joinPoint")
                .returns(typeName)
                .addException(Throwable.class)
                .addAnnotation(AnnotationSpec
                        .builder(Around.class)
                        .addMember("value", "$S", "execution(* " + GLOBAL_INTERCEPTORS_METHOD_NAME + ")").build())
                .addCode(builder.build());
    }


    private List<InterceptorElement> generateInterceptorElements(RoundEnvironment roundEnv) {
        Set<? extends Element> interceptorElements = roundEnv.getElementsAnnotatedWith(Intercept.class);
        List<InterceptorElement> serviceElements = new ArrayList<>();
        for (Element element : interceptorElements) {
            if (element.getKind() == CLASS) {
                InterceptorElement serviceElement = new InterceptorElement(element);
                String name = serviceElement.getClassName().simpleName();
                if (!name.endsWith("Interceptor")) {
                    logger.error(name + "must be end with Interceptor", element);
                }
                serviceElements.add(serviceElement);
            }
        }
        return serviceElements;
    }


}
