package com.eci.arep.web;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class RouteHandler {

    private final Object instance;
    private final Method method;

    public RouteHandler(Object instance, Method method) {
        this.instance = instance;
        this.method = method;
    }

    public String invoke(Request request) {
        try {
            Object[] args = buildArguments(request);
            Object result = method.invoke(instance, args);
            return result == null ? "" : result.toString();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error invoking route method: " + method.getName(), e);
        }
    }

    private Object[] buildArguments(Request request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam == null) {
                throw new IllegalArgumentException("All parameters must use @RequestParam in method: " + method.getName());
            }
            if (!String.class.equals(parameter.getType())) {
                throw new IllegalArgumentException("Only String parameters are supported in method: " + method.getName());
            }
            String value = request.getQueryParam(requestParam.value());
            if (value == null || value.isBlank()) {
                value = requestParam.defaultValue();
            }
            args[i] = value;
        }

        return args;
    }
}
