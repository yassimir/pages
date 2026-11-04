package com.gharnata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomSessionRegistry sessionRegistry;

    public SecurityConfig(CustomSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http
                //.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                /*.csrf(Customizer.withDefaults())*/
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/resources/**", "/assets/**", "/login").permitAll()
                        .requestMatchers("/gharnata/users/**").hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/gharnata/payments/*/confirm"
                        ).hasRole("ADMIN")
                        .requestMatchers("/gharnata/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/gharnata/show-products")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/error")
                )
                .rememberMe(remember -> remember
                        .key("gharnata-uniquekey")
                        .tokenValiditySeconds(14 * 24 * 60 * 60) // 14 jours
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false) // false = la nouvelle connexion expire l'ancienne (recommandé)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/login?expired=true")
                )
                .build();
    }
}
