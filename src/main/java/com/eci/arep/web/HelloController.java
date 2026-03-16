package com.eci.arep.web;

@RestController
public class HelloController {

    @GetMapping("/pi")
    public String getPI() {
        return "PI= " + Math.PI;
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello world";
    }

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hola " + name;
    }
}
    