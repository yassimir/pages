package com.gharnata.controller;

import com.gharnata.entity.*;
import com.gharnata.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;


@Controller
@RequestMapping("/gharnata")
public class PaymentController {

    private static final Set<String> MODES_PAIEMENT =
            Set.of("Virement", "Chèque", "Espèces", "Effet");


    private final ServClient servClient;
    private final ServPaiement servPaiement;
    private final ServNotification servNotification;
    private final ServProduit servProduit;
    private final ServFacture servFacture;
    private final ServCommande servCommande;
    private final ServCompte servCompte;

    public PaymentController(
            ServClient servClient,
            ServPaiement servPaiement,
            ServNotification servNotification,
            ServProduit servProduit,
            ServFacture servFacture,
            ServCommande servCommande,
            ServCompte servCompte) {

        this.servClient = servClient;
        this.servPaiement = servPaiement;
        this.servNotification = servNotification;
        this.servProduit = servProduit;
        this.servFacture = servFacture;
        this.servCommande = servCommande;
        this.servCompte = servCompte;
    }

    @GetMapping("/show-payments")
    public String showPayments(Model model) {
        model.addAttribute("notif", servNotification.nbNotif());
        return "gest/paymentManagement";
    }

    @GetMapping("/paiements/{id}")
    public String showPaie(
            Model model,
            @PathVariable Long id) {

        Client client = servClient.getClientById(id);
        BigDecimal credit = servPaiement.calculerCreditClient(client);

        if (client == null) {
            throw new IllegalArgumentException("Client introuvable.");
        }

        List<Paiement> paiements =
                servPaiement.getPaymentsByClient(client);

        model.addAttribute("client", client);
        model.addAttribute("clients", this.servClient.getAllClients());
        model.addAttribute("payments", paiements);
        model.addAttribute("notif", servNotification.nbNotif());
        model.addAttribute("cats", servProduit.getAllCats());
        model.addAttribute("credit", credit);
        model.addAttribute("comptes", servCompte.getComptesActives());

        return "gest/showClientPayments";
    }

    /*
     * Paiement global du crédit d'un client.
     *
     * Le paiement n'est associé directement à aucune commande.
     * Il pourra ensuite être affecté à une ou plusieurs commandes.
     */

    @PostMapping("/payment")
    public String payment(@RequestParam("montant") BigDecimal montant,
                          @RequestParam("idC") long idC,
                          @RequestParam("compteId") Long compteId,
                          @RequestParam("modPai") String modePaiement,
                          @RequestParam("pInfo") String pInfo,
                          RedirectAttributes attributes) {

        Client client = servClient.getClientById(idC);
        if (client == null) {
            attributes.addFlashAttribute("error", "Client introuvable.");
            return "redirect:/gharnata/list-clients";
        }

        String retour = "redirect:/gharnata/client-details/" + client.getId();

        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            attributes.addFlashAttribute(
                    "error",
                    "Le montant doit être supérieur à zéro."
            );
            return retour;
        }
        if (!MODES_PAIEMENT.contains(modePaiement)) {
            attributes.addFlashAttribute("error", "Mode de paiement invalide.");
            return retour;
        }
        if (pInfo != null && pInfo.length() > 150) {
            attributes.addFlashAttribute("error", "Les détails du paiement sont trop longs (150 caractères maximum).");
            return retour;
        }
        String mdP = modePaiement + ((pInfo == null || pInfo.isBlank()) ? "" : " - " + pInfo.trim());;
        String description = "Paiement global - " + mdP;

        try {
            servPaiement.payerCreditClient(client, montant,compteId, mdP, description);
            attributes.addFlashAttribute("success", "Paiement enregistré avec succès.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return retour;
    }


    @PostMapping("/pay-cmd")
    public String paycmd(
            @RequestParam("mt") BigDecimal montant,
            @RequestParam("id") long idCommande,
            @RequestParam("client_id") Long clientId,
            @RequestParam("pInfo") String modePaiement,
            RedirectAttributes attributes) {

        Commande commande = servCommande.findCommandById(idCommande);

        if (commande == null) {
            attributes.addFlashAttribute("error", "Commande introuvable.");
            return "redirect:/gharnata/client-details/" + clientId;
        }

        try {
            if (commande.getClient() == null
                    || !commande.getClient().getId().equals(clientId)) {
                throw new IllegalArgumentException(
                        "La commande n'appartient pas à ce client."
                );
            }

            servPaiement.payerCommande(
                    commande,
                    montant,
                    modePaiement,
                    "Paiement de la commande n°" + commande.getId()
                            + " - " + commande.getClient().getSte()
            );

            attributes.addFlashAttribute(
                    "success",
                    "Paiement enregistré avec succès. Aucune facture n'a été générée."
            );

            return "redirect:/gharnata/command-details/" + commande.getId();

        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());

            return "redirect:/gharnata/command-details/" + commande.getId();
        }
    }


    @PostMapping("/paiements/{id}/facture")   // POST : la facture est créée en base
    public String genererFacturePaiement(@PathVariable Long id,
                                         @RequestParam(value = "categories", required = false) List<Long> categories,
                                         @RequestParam(value = "tmp", required = false) String tmp,
                                         HttpServletResponse response,
                                         RedirectAttributes attributes) throws Exception {
        Paiement paiement = servPaiement.getPaiementById(id);
        if (paiement == null) {
            attributes.addFlashAttribute("error", "Paiement introuvable.");
            return "redirect:/gharnata/list-clients";
        }
        String retour = "redirect:/gharnata/paiements/" + paiement.getClient().getId();

        if (paiement.getFacture() != null) {
            attributes.addFlashAttribute("error", "Une facture existe déjà pour ce paiement.");
            return retour;
        }

        try {
            Facture facture;
            if (paiement.getCommande() != null) {
                facture = servFacture.creerFactureDepuisCommande(paiement, paiement.getCommande());
            } else {
                if (categories == null || categories.isEmpty()) {
                    attributes.addFlashAttribute("error", "Sélectionnez au moins une catégorie.");
                    return retour;
                }
                facture = servFacture.creerFactureGlobale(paiement, categories); // à écrire selon ta réponse
            }
            servFacture.genererFacture(facture, tmp, response);
            return null;
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return retour;
        }
    }
}
