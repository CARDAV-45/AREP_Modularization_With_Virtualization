package com.eci.arep.web;

import java.util.Arrays;

public class WebApplication {

    public static void main(String[] args) {
        ControllerRegistry registry = new ControllerRegistry();

        if (args.length > 0) {
            Arrays.stream(args).forEach(registry::loadFromClassName);
        } else {
            registry.scanPackage("com.eci.arep");
        }

        System.out.println("Loaded routes: " + registry.getRoutes().keySet());

        HttpWebServer server = new HttpWebServer(registry, 35000);
        server.start();
    }
}
