package com.gharnata.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

        @GetMapping({"","/"})
        public String redirect() {
                return "redirect:/dashboard";
        }
        /*@GetMapping("/login")
        public String login(CsrfToken token) {
                token.getToken(); // force la génération + la sauvegarde en session MAINTENANT, avant tout rendu
                return "auth/loginP";
        }*/
        @GetMapping("/login")
        public String login(
                CsrfToken token,
                Authentication authentication) {

                token.getToken();

                if (authentication != null &&
                        authentication.isAuthenticated() &&
                        !(authentication instanceof AnonymousAuthenticationToken)) {

                        return "redirect:/dashboard";
                }
                return "auth/loginP";
        }
}
