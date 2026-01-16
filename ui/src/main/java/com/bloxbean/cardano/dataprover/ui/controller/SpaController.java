package com.bloxbean.cardano.dataprover.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving the SPA (Single Page Application).
 * Forwards all UI routes to index.html for client-side routing.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
            "/ui",
            "/ui/",
            "/ui/dashboard",
            "/ui/dashboard/**",
            "/ui/merkle",
            "/ui/merkle/**",
            "/ui/proofs",
            "/ui/proofs/**",
            "/ui/cache",
            "/ui/cache/**",
            "/ui/settings",
            "/ui/settings/**"
    })
    public String forwardToIndex() {
        return "forward:/ui/index.html";
    }
}
