package com.gharnata.controller;

import com.gharnata.service.ServCommande;
import com.gharnata.service.ServNotification;
import com.gharnata.service.ServPaiement;
import com.gharnata.service.ServProduit;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final ServPaiement servPaiement;
    private final ServProduit servProduit;
    private final ServCommande servCommande;
    private final ServNotification servNotification;

    public DashboardController(ServPaiement servPaiement, ServNotification servNotification,
                               ServProduit servProduit, ServCommande servCommande) {
        this.servPaiement = servPaiement;
        this.servNotification = servNotification;
        this.servProduit = servProduit;
        this.servCommande = servCommande;
    }

    @GetMapping
    public String showDashboard(Model model) {

        // Statistiques des paiements
        Map<String, Object> statistiques =
                servPaiement.obtenirStatistiquesPaiements();

        model.addAttribute(
                "currentMonthTotal",
                statistiques.get("currentMonthTotal")
        );

        model.addAttribute(
                "pendingPayments",
                statistiques.get("pendingTotal")
        );

        // Statistiques du stock et des commandes
        model.addAttribute(
                "lowStockCount",
                servProduit.compterStockCritique()
        );

        model.addAttribute(
                "pendingOrdersCount",
                servCommande.compterCommandesEnAttente()
        );

        // Les 5 derniers paiements, sans condition
        model.addAttribute(
                "payments",
                servPaiement.getCinqDerniersPaiements()
        );

        model.addAttribute(
                "lowStockProducts",
                servProduit.getProduitsStockCritique()
        );

        model.addAttribute(
                "pendingOrders",
                servCommande.getCinqDernieresCommandesAConfirmer()
        );

        model.addAttribute("breadcrumbs",
                List.of(
                        "Espace de travail",
                        "Dashboard"
                ));
        model.addAttribute("notif",servNotification.nbNotif());

        return "gest/dashboard";
    }
}