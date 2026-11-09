package com.gharnata.config;

import com.gharnata.service.ServNotificationPaiement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.jspecify.annotations.NonNull;

@Component
public class PaymentNotificationInterceptor implements HandlerInterceptor {

    private static final int LIMITE_NOTIFICATIONS = 3;

    private final ServNotificationPaiement servNotificationPaiement;

    public PaymentNotificationInterceptor(
            ServNotificationPaiement servNotificationPaiement) {

        this.servNotificationPaiement = servNotificationPaiement;
    }

    @Override
    public void postHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            ModelAndView modelAndView) {

        // Vérifier qu'une session existe.
        var session = request.getSession(false);

        if (session == null) {
            return;
        }

        // Vérifier si un administrateur vient de se connecter.
        Object afficherNotifications = session.getAttribute(
                PaymentNotificationSuccessHandler.SESSION_ATTRIBUTE
        );

        if (!Boolean.TRUE.equals(afficherNotifications)) {
            return;
        }

        // Ne pas consommer l'indicateur lors d'une redirection.
        if (modelAndView == null
                || modelAndView.getView() instanceof RedirectView
                || (modelAndView.getViewName() != null
                && modelAndView.getViewName().startsWith("redirect:"))) {
            return;
        }

        // Charger les trois premières notifications non lues.
        var notifications =
                servNotificationPaiement.getNotificationsNonLues(
                        LIMITE_NOTIFICATIONS
                );

        // Calculer le nombre de notifications restantes.
        long totalNonLues =
                servNotificationPaiement.getNombreNotificationsNonLues();

        long restantes = Math.max(
                0,
                totalNonLues - notifications.size()
        );

        // Transmettre les données à Thymeleaf.
        modelAndView.addObject(
                "notificationsPaiementConnexion",
                notifications
        );

        modelAndView.addObject(
                "nombreNotificationsPaiementRestantes",
                restantes
        );

        // Consommer l'indicateur après la première page HTML.
        session.removeAttribute(
                PaymentNotificationSuccessHandler.SESSION_ATTRIBUTE
        );
    }
}