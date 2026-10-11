package com.gharnata.controller;

import com.gharnata.entity.*;
import com.gharnata.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;


@Controller
@RequestMapping("/gharnata")
public class PaymentController {

    private final ServClient servClient;
    private final ServPaiement servPaiement;
    private final ServNotification servNotification;
    private final ServProduit servProduit;
    private final ServFacture servFacture;
    private final ServCommande servCommande;

    public PaymentController(
            ServClient servClient,
            ServPaiement servPaiement,
            ServNotification servNotification,
            ServProduit servProduit,
            ServFacture servFacture,
            ServCommande servCommande) {

        this.servClient = servClient;
        this.servPaiement = servPaiement;
        this.servNotification = servNotification;
        this.servProduit = servProduit;
        this.servFacture = servFacture;
        this.servCommande = servCommande;
    }

    @GetMapping("/paiements/{id}")
    public String showPaie(
            Model model,
            @PathVariable Long id) {

        Client client = servClient.getClientById(id);

        if (client == null) {
            throw new IllegalArgumentException("Client introuvable.");
        }

        List<Paiement> paiements =
                servPaiement.getPaymentsByClient(client);

        model.addAttribute("client", client);
        model.addAttribute("payments", paiements);
        model.addAttribute("notif", servNotification.nbNotif());
        model.addAttribute("cats", servProduit.getAllCats());

        return "gest/showPayments";
    }

    /**
     * Paiement global du crédit d'un client.
     *
     * Le paiement n'est associé directement à aucune commande.
     * Il pourra ensuite être affecté à une ou plusieurs commandes.
     */
    @PostMapping("/payment")
    public String payment(
            @RequestParam("montant") double montant,
            @RequestParam("idC") long idC,
            @RequestParam("modPai") String modePaiement,
            RedirectAttributes attributes) {

        Client client = servClient.getClientById(idC);

        if (client == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Client introuvable."
            );

            return "redirect:/gharnata/client-details/" + idC;
        }

        try {

            Paiement paiement = servPaiement.payerCreditClient(
                    client,
                    montant,
                    modePaiement,
                    "Paiement global d'un montant de : " + montant
            );

            /*
             * Le paiement est automatiquement affecté aux commandes
             * les plus anciennes, jusqu'à épuisement du montant.
             * L'affectation est réalisée dans ServPaiement.
             */

            attributes.addFlashAttribute(
                    "success",
                    "Paiement enregistré avec succès."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {

            attributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/gharnata/client-details/" + client.getId();
    }

    /**
     * Paiement direct d'une commande.
     */
    /*@PostMapping("/pay-cmd")
    public String paycmd(
            @RequestParam("mt") double montant,
            @RequestParam("id") long idCommande,
            @RequestParam("client_id") Long clientId,
            HttpServletResponse response,
            @RequestParam(value = "tmp", required = false) String tmp,
            @RequestParam("pInfo") String modePaiement,
            RedirectAttributes attributes) throws Exception {

        Commande commande = servCommande.findCommandById(idCommande);

        if (commande == null) {
            attributes.addFlashAttribute(
                    "message",
                    "Commande introuvable."
            );

            return "redirect:/gharnata/client-details/" + clientId;
        }

        try {

            if (commande.getClient() == null
                    || !commande.getClient().getId().equals(clientId)) {

                throw new IllegalArgumentException(
                        "La commande n'appartient pas à ce client."
                );
            }

            Paiement paiement = servPaiement.payerCommande(
                    commande,
                    montant,
                    modePaiement,
                    "Paiement de la commande n°"
                            + commande.getId()
                            + " - "
                            + commande.getClient().getSte()
            );

            Facture facture = servFacture.creeFacture(paiement);

            servFacture.persFactItems(
                    facture,
                    commande
            );

            servFacture.genererFacture(
                    facture,
                    tmp,
                    response
            );

            return null;

        } catch (IllegalArgumentException | IllegalStateException e) {

            attributes.addFlashAttribute(
                    "message",
                    e.getMessage()
            );

            return "redirect:/gharnata/command-details/"
                    + commande.getId();
        }
    }*/


    @PostMapping("/pay-cmd")
    public String paycmd(
            @RequestParam("mt") double montant,
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

    @GetMapping("/paiements/{id}/facture")
    public String genererFacturePaiement(
            @PathVariable Long id,
            HttpServletResponse response,
            RedirectAttributes attributes) throws Exception {
        System.out.println("here1");
        Paiement paiement = servPaiement.getPaiementById(id);
        System.out.println("here2");
        if (paiement == null) {
            System.out.println("here3");
            attributes.addFlashAttribute(
                    "error", "Paiement introuvable."
            );
            return "redirect:/gharnata/list-clients";
        }

        if (paiement.getFacture() != null) {
            System.out.println("here4");
            attributes.addFlashAttribute(
                    "error", "Une facture existe déjà pour ce paiement."
            );
            return "redirect:/gharnata/paiements/"
                    + paiement.getClient().getId();
        }
        System.out.println("here5");
        Commande commande = paiement.getCommande();
        System.out.println("here6");
        if (commande == null) {
            System.out.println("here7");
            attributes.addFlashAttribute(
                    "error",
                    "Ce paiement global nécessite de sélectionner les catégories "
                            + "de produits pour composer la facture."
            );
            return "redirect:/gharnata/paiements/"
                    + paiement.getClient().getId();
        }

        try {
            System.out.println("here8");
            Facture facture = servFacture.creerFactureDepuisCommande(paiement, commande);
            System.out.println("here9");
            servFacture.genererFacture(facture, null, response);
            System.out.println("here10");
            return null;

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("here11");
            attributes.addFlashAttribute("error", e.getMessage());

            return "redirect:/gharnata/paiements/"
                    + paiement.getClient().getId();
        }
    }
}
