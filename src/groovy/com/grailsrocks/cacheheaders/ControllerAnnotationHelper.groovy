package com.grailsrocks.cacheheaders

import org.codehaus.groovy.grails.commons.ApplicationHolder as ApplicationHolder

import org.apache.commons.lang.WordUtils

//http://burtbeckwith.com/blog/?p=80
class ControllerAnnotationHelper {
    private static Map<String, Map<String, List<Class>>> _actionMap = [:]
    private static Map<String, Class> _controllerAnnotationMap = [:]

    /**
     * Find controller annotation information.
     */
    static void init() {
        ApplicationHolder.application.controllerClasses.each { controllerClass ->
            def clazz = controllerClass.clazz
            String controllerName = WordUtils.uncapitalize(controllerClass.name)
            mapClassAnnotation clazz, NoCaching, controllerName

            Map<String, List<Class>> annotatedClosures = findAnnotatedClosures(
                    clazz, NoCaching)
            if (annotatedClosures) {
                _actionMap[controllerName] = annotatedClosures
            }
        }
    }

    public static boolean requiresAnnotation(Class annotationClass,
                                             String controllerName, String actionName) {

        // see if the controller has the annotation
        def annotations = _controllerAnnotationMap[controllerName]
        if (annotations && annotations.contains(annotationClass)) {
            return true
        }

        // otherwise check the action
        Map<String, List<Class>> controllerClosureAnnotations =
            _actionMap[controllerName] ?: [:]
        List<Class> annotationClasses = controllerClosureAnnotations[actionName]
        return annotationClasses && annotationClasses.contains(annotationClass)
    }

    private static void mapClassAnnotation(clazz, annotationClass, controllerName) {
        if (clazz.isAnnotationPresent(annotationClass)) {
            def list = _controllerAnnotationMap[controllerName] ?: []
            list << annotationClass
            _controllerAnnotationMap[controllerName] = list
        }
    }

    private static Map<String, List<Class>> findAnnotatedClosures(
            Class clazz, Class... annotationClasses) {

        // since action closures are defined as "def foo = ..." they're
        // fields, but they end up private
        def map = [:]
        for (field in clazz.declaredFields) {
            def fieldAnnotations = []
            for (annotationClass in annotationClasses) {
                if (field.isAnnotationPresent(annotationClass)) {
                    fieldAnnotations << annotationClass
                }
            }
            if (fieldAnnotations) {
                map[field.name] = fieldAnnotations
            }
        }

        return map
    }
}
