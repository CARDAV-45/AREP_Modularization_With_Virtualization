package com.eci.arep.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class WebApplicationTests {

    @Test
    void shouldRegisterGetMappingRoutesFromControllerAnnotation() {
        ControllerRegistry registry = new ControllerRegistry();
        registry.loadFromClassName("com.eci.arep.web.HelloController");

        assertNotNull(registry.getHandler("/"));
        assertNotNull(registry.getHandler("/pi"));
        assertNotNull(registry.getHandler("/hello"));
        assertNotNull(registry.getHandler("/greeting"));
    }

    @Test
    void shouldResolveRequestParamWithDefaultValue() {
        ControllerRegistry registry = new ControllerRegistry();
        registry.loadFromClassName("com.eci.arep.web.HelloController");

        RouteHandler handler = registry.getHandler("/greeting");
        String response = handler.invoke(new Request("/greeting", null));

        assertEquals("Hola World", response);
    }

    @Test
    void shouldResolveRequestParamFromQueryString() {
        ControllerRegistry registry = new ControllerRegistry();
        registry.loadFromClassName("com.eci.arep.web.HelloController");

        RouteHandler handler = registry.getHandler("/greeting");
        String response = handler.invoke(new Request("/greeting", "name=David"));

        assertEquals("Hola David", response);
    }
}
