package com.gharnata.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

        @GetMapping({"","/", "/gharnata", "/gharnata/"})
        public String redirect() {
                return "redirect:/gharnata/show-products";
        }
        @GetMapping("/login")
        public String login(CsrfToken token) {
                token.getToken(); // force la génération + la sauvegarde en session MAINTENANT, avant tout rendu
                return "auth/loginP";
        }
}
