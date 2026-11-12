package com.gharnata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
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
    private final PaymentNotificationSuccessHandler paymentNotificationSuccessHandler;

    public SecurityConfig(CustomSessionRegistry sessionRegistry,
                          PaymentNotificationSuccessHandler paymentNotificationSuccessHandler) {
        this.sessionRegistry = sessionRegistry;
        this.paymentNotificationSuccessHandler = paymentNotificationSuccessHandler;
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
                        .requestMatchers("/resources/**", "/assets/**", "/images/**", "/icons/**", "/login", "/service-worker.js", "/manifest.json").permitAll()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/payments/*/confirm").hasRole("ADMIN")
                        .requestMatchers("/notifications-paiement/**").hasRole("ADMIN")
                        .requestMatchers("/clients/**","/commands/**", "/factures/**", "/notif-stock/**",
                                         "/dashboard/**","/gharnata/**", "/panier/**", "/payments/**",
                                         "/products/**").authenticated()
                        .anyRequest().authenticated()
                )
                /*.formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/gharnata/show-products")
                        .permitAll()
                )*/
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(paymentNotificationSuccessHandler)
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
                        .sessionFixation(
                                SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId
                        )
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false) // false = la nouvelle connexion expire l'ancienne (recommandé)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/login?expired=true")
                )
                .build();
    }
}
