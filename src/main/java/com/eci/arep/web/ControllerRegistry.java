package com.eci.arep.web;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class ControllerRegistry {

    private final Map<String, RouteHandler> routes = new HashMap<>();

    public void loadFromClassName(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            registerController(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Controller class not found: " + className, e);
        }
    }

    public void scanPackage(String basePackage) {
        for (Class<?> clazz : findClasses(basePackage)) {
            registerController(clazz);
        }
    }

    public RouteHandler getHandler(String path) {
        return routes.get(path);
    }

    public Map<String, RouteHandler> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    private void registerController(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(RestController.class)) {
            return;
        }

        Object instance = createInstance(clazz);
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(GetMapping.class)) {
                continue;
            }
            if (!String.class.equals(method.getReturnType())) {
                throw new IllegalArgumentException("Only String return type is supported for @GetMapping: " + method.getName());
            }
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            routes.put(mapping.value(), new RouteHandler(instance, method));
        }
    }

    private Object createInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to instantiate controller: " + clazz.getName(), e);
        }
    }

    private Set<Class<?>> findClasses(String basePackage) {
        Set<Class<?>> classes = new HashSet<>();
        String packagePath = basePackage.replace('.', '/');

        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    scanDirectory(basePackage, Paths.get(resource.toURI()), classes);
                } else if ("jar".equals(protocol)) {
                    scanJar(basePackage, resource, classes);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to scan package: " + basePackage, e);
        }

        return classes;
    }

    private void scanDirectory(String basePackage, Path packageDir, Set<Class<?>> classes) throws IOException {
        if (!Files.exists(packageDir)) {
            return;
        }

        try (Stream<Path> pathStream = Files.walk(packageDir)) {
            pathStream
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".class"))
                .forEach(path -> {
                    String relative = packageDir.relativize(path).toString().replace('\\', '.').replace('/', '.');
                    String className = basePackage + "." + relative.substring(0, relative.length() - 6);
                    loadClass(className, classes);
                });
        }
    }

    private void scanJar(String basePackage, URL resource, Set<Class<?>> classes) throws IOException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        try (JarFile jarFile = connection.getJarFile()) {
            String packagePath = basePackage.replace('.', '/');
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(packagePath) || !name.endsWith(".class") || name.contains("$")) {
                    continue;
                }
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                loadClass(className, classes);
            }
        }
    }

    private void loadClass(String className, Set<Class<?>> classes) {
        if (className.contains("$")) {
            return;
        }
        try {
            classes.add(Class.forName(className));
        } catch (ClassNotFoundException ignored) {
        }
    }
}
