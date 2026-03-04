package com.evalx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    /**
     * SPA Routing: Forward specific non-API paths to index.html so the client-side router can take over.
     */
    @GetMapping({"/admin", "/upload", "/result", "/stages", "/years"})
    public String forward() {
        return "forward:/index.html";
    }
}
