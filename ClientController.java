package com.gharnata.controller;

import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import com.gharnata.entity.Paiement;
import com.gharnata.entity.PanierItems;
import com.gharnata.entity.dto.ClientDTO;
import com.gharnata.repository.RepCompte;
import com.gharnata.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ServClient servClient;
    private final ServCommande servCommande;
    private final ServNotification servNotification;
    private final ServProduit servProduit;
    private final ServPaiement servPaiement;
    private final ServCompte servCompte;
    private final ServPanier servPanier;

    public ClientController(ServClient servClient, ServCommande servCommande, ServNotification servNotification,
                            ServProduit servProduit, ServPaiement servPaiement, ServCompte servCompte, ServPanier servPanier) {
        this.servClient = servClient;
        this.servCommande = servCommande;
        this.servNotification = servNotification;
        this.servProduit = servProduit;
        this.servPaiement = servPaiement;
        this.servCompte = servCompte;
        this.servPanier = servPanier;
    }

    @GetMapping("/api/search")
    @ResponseBody
    public List<ClientDTO> searchProducts(
            @RequestParam("q") String query) {

        return servClient.searchClients(query);
    }

    @GetMapping
    public String listClient(Model model, @RequestParam(value = "clientId", required = false) Long clientId){
        List<Client> lC ;
        if (clientId != null) {
            Client client = servClient.getClientById(clientId);
            if (client == null) {
                model.addAttribute("error", "Ce Client n'existe pas.");
                lC = this.servClient.getAllClients();
            } else {
                lC = List.of(client);
            }
        }else{
            lC = this.servClient.getAllClients();
        }
        model.addAttribute("clients", lC);
        //model.addAttribute("cls", this.servClient.getClientDTOs());
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Clients"
        ));
        return "gest/listClient";
    }

    @GetMapping("/{id}/details")
    public String showOneClient(@PathVariable Long id, Model model, RedirectAttributes attributes){
        Client client = this.servClient.getClientById(id);
        if (client == null) {
            attributes.addFlashAttribute("error", "Ce client n'existe pas.");
            return "redirect:/clients";
        }
        List<Paiement> lP = this.servPaiement.getLimitByClient(client);
        List<Commande> lC = this.servCommande.getLimitByClient(client);
        BigDecimal credit = servPaiement.calculerCreditClient(client);
        model.addAttribute("client", client);
        model.addAttribute("commandes", lC);
        model.addAttribute("payments", lP);
        model.addAttribute("credit", credit);
        model.addAttribute("notif", servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("comptes", servCompte.getComptesActives());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Clients",
                client.getSte()
        ));
        return "gest/clientDetails";
    }

    @PostMapping("/{id}/delete")
    public String archiveClient(@PathVariable long id){
        this.servClient.archiveClient(id);
        return "redirect:/clients";
    }

    @GetMapping("/check-ice")
    public String checkIce(@RequestParam String ice, Model model) {
        Client client = this.servClient.getClientByIce(ice);
        boolean existe = client != null;

        model.addAttribute("existe", existe);
        if ( client != null){
            model.addAttribute("name", client.getSte());
        }
        return "fragments/ice-feedback :: feedback";
    }

    @PostMapping("/add")
    public String addClient(RedirectAttributes attributes, @ModelAttribute("client") Client client){
        Long c = this.servClient.saveClient(client);
        if (c == 1L){
            attributes.addFlashAttribute("success", "Le client "+ client.getSte() +" a été ajouté avec succès.");
            return "redirect:/clients/add";
        } else {
            attributes.addFlashAttribute("error", "Le client avec ce ICE : "+ client.getIce() +" existe déjà.");
            attributes.addFlashAttribute("cls", client); // conserve la saisie
            return "redirect:/clients/add";
        }
    }

    @GetMapping("/add")
    public String addClient(Model model){
        if (!model.containsAttribute("cls")) {
            model.addAttribute("cls", new Client());
        }
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Clients",
                "Ajouter un client"
        ));
        return "gest/addClient";
    }

    @GetMapping("/{id}/modify")
    public String modifyClient(Model model, @PathVariable Long id){
        Client client = this.servClient.getClientById(id);
        model.addAttribute("cls", client);
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Clients",
                client.getSte()
        ));
        return "gest/modifyClient";
    }
    @PostMapping("/modify")
    public String modifyClient(@ModelAttribute("client") Client client, RedirectAttributes attributes){
        System.out.println("le type de client est : "+client.getType());
        System.out.println("ICE : "+client.getIce());
        Long c = this.servClient.modifyClient(client);
        if (c==1){
            attributes.addFlashAttribute("success", "Le client "+ client.getSte()+" a été modifié avec succès.");
        }else {
            attributes.addFlashAttribute("error", "Une erreur s'est produite lors de la modification du client : "+ client.getSte());
        }
        return "redirect:/clients/modify/"+client.getId();
    }

    @GetMapping("/{id}/commands")
    public String showComm(Model model, @PathVariable Long id){
        Client client = this.servClient.getClientById(id);
        List<Commande> lC = this.servCommande.getCommandsByClient(client);
        BigDecimal credit = servPaiement.calculerCreditClient(client);
        model.addAttribute("client", client);
        model.addAttribute("commands", lC);
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("credit", credit);
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Clients",
                client.getSte(),
                "Commandes"
        ));
        return "gest/showClientCommands";
    }
    @GetMapping("/{id}/payments")
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
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Clients",
                client.getSte(),
                "Paiements"
        ));

        return "gest/showClientPayments";
    }

    /*@PostMapping("/modify-credit")
    public String modifierCredit(@RequestParam Long id, @RequestParam double nouveauCredit, RedirectAttributes attributes) {
        servClient.updateClientCredit(id, nouveauCredit);
        attributes.addFlashAttribute("success", "Crédit mis à jour.");
        return "redirect:/gharnata/modify-credit?id=" + id;
    }*/

    @GetMapping("/modify-credit")
    public String modifyCredit(Model model, @RequestParam(value = "id", required = false) Long id, @RequestParam(value = "ice", required = false) String ice){
        Client c = null;
        if (ice != null) c = servClient.getClientByIce(ice);
        else if (id != null) c = servClient.getClientById(id);
        if (c != null) model.addAttribute("client", c);
        model.addAttribute("clients", servClient.getAllClients());
        return "gest/modifierCredit";
    }

    @GetMapping("/{id}/commands/new")
    public String passCommande(
            Model model,
            @PathVariable("id") Long id,
            RedirectAttributes attributes) {

        List<PanierItems> panierItems =
                servPanier.getPanierItemsByAdm();

        Client client =
                servClient.getClientById(id);

        if (client == null) {
            attributes.addFlashAttribute(
                    "error",
                    "Le client est introuvable."
            );

            return "redirect:/clients";
        }

        if (panierItems.isEmpty()) {

            attributes.addFlashAttribute(
                    "error",
                    "Aucun produit n'existe dans le panier."
            );

            return "redirect:/clients";
        }

        try {

            Commande commande =
                    servCommande.saveCommand(client);

            int nombreLignes =
                    servCommande.saveLCom(
                            commande,
                            panierItems
                    );

            if (nombreLignes <= 0) {

                attributes.addFlashAttribute(
                        "error",
                        "Aucune ligne de commande valide n'a été créée."
                );

                return "redirect:/clients";
            }

            servPanier.clearCart();

            return "redirect:/commands/" +commande.getId()+"/details";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            attributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/clients";
        }
    }

}