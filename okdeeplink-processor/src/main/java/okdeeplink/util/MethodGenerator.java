package okdeeplink.util;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Created by zhangqijun on 2017/4/28.
 */

public class MethodGenerator {

    private ExecutableElement executableElement;

    public MethodGenerator(ExecutableElement executableElement) {
        this.executableElement = executableElement;
    }

    public MethodSpec.Builder overMethod(String name) {


        MethodSpec.Builder builder = MethodSpec.methodBuilder(getValidMethodName(name));

        addModifiers(builder);

        addAnnotation(builder);

        checkParameters(executableElement, builder);

        addReturnType(executableElement, builder);

        addExceptions(executableElement, builder);

        return builder;
    }



    MethodGenerator addModifiers(MethodSpec.Builder builder) {
        builder.addModifiers(Modifier.PUBLIC);
        return this;
    }

    void addAnnotation(MethodSpec.Builder builder) {
        builder.addAnnotation(AnnotationSpec.builder(Override.class).build());
    }

    String getValidMethodName(String name) {
        return name.replace(' ', '_');
    }

    void addExceptions(ExecutableElement annotatedMtd, MethodSpec.Builder builder) {
        List<? extends TypeMirror> exceptions = annotatedMtd.getThrownTypes();
        if (exceptions != null){
            for (TypeMirror exc : exceptions) {
                builder.addException(ClassName.get(exc));
            }
        }
    }

    void addReturnType(ExecutableElement annotatedMtd, MethodSpec.Builder builder) {
        builder.returns(ClassName.get(annotatedMtd.getReturnType()));
    }

    void checkParameters(ExecutableElement annotatedMtd, MethodSpec.Builder builder) {
        List<? extends VariableElement> params = annotatedMtd.getParameters();
        if (params != null){
            for (int i = 0; i < params.size(); i++) {
                VariableElement elem = params.get(i);
                builder.addParameter(ClassName.get(elem.asType()), elem.getSimpleName().toString());
            }
        }

    }
}
