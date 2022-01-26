package at.schrottner.gradle.testhelper;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

public class MappedParameterContext implements ParameterContext {

    private final int index;
    private final Parameter parameter;
    private final Optional<Object> target;

    public MappedParameterContext(int index, Parameter parameter, Optional<Object> target) {
        this.index = index;
        this.parameter = parameter;
        this.target = target;
    }

    @Override
    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Optional<Object> getTarget() {
        return target;
    }

    @Override
    public boolean isAnnotated(Class<? extends Annotation> annotationType) {
        return AnnotationUtils.isAnnotated(parameter, annotationType);
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(parameter, annotationType);
    }

    @Override
    public <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
        return AnnotationUtils.findRepeatableAnnotations(parameter, annotationType);
    }
}