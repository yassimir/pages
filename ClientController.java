package com.gharnata.controller;

import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import com.gharnata.entity.Paiement;
import com.gharnata.entity.dto.ClientDTO;
import com.gharnata.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/gharnata")
public class ClientController {

    private final ServClient servClient;

    private final ServCommande servCommande;

    private final ServNotification servNotification;

    private final ServProduit servProduit;

    private final ServPaiement servPaiement;

    public ClientController(ServClient servClient, ServCommande servCommande, ServNotification servNotification,
                            ServProduit servProduit, ServPaiement servPaiement){
        this.servClient = servClient;
        this.servCommande = servCommande;
        this.servNotification = servNotification;
        this.servProduit = servProduit;
        this.servPaiement = servPaiement;
    }

    @GetMapping("/api/clients/search")
    @ResponseBody
    public List<ClientDTO> searchProducts(
            @RequestParam("q") String query) {

        return servClient.searchClients(query);
    }

    @GetMapping("/list-clients")
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
        return "gest/listClient";
    }

    @GetMapping("/client-details/{id}")
    public String showOneProduct(@PathVariable Long id, Model model, RedirectAttributes attributes){
        Client client = this.servClient.getClientById(id);
        if (client == null) {
            attributes.addFlashAttribute("error", "Ce client n'existe pas.");
            return "redirect:/gharnata/list-clients";
        }
        List<Paiement> lP = this.servPaiement.getLimitByClient(client);
        List<Commande> lC = this.servCommande.getLimitByClient(client);
        double credit = servPaiement.calculerCreditClient(client);
        model.addAttribute("client", client);
        model.addAttribute("commandes", lC);
        model.addAttribute("payments", lP);
        model.addAttribute("credit", credit);
        model.addAttribute("notif", servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        return "gest/clientDetails";
    }

    @PostMapping("/delete-client/{id}")
    public String archiveClient(@PathVariable long id){
        this.servClient.archiveClient(id);
        return "redirect:/gharnata/list-clients";
    }

    @GetMapping("/clients/check-ice")
    public String checkIce(@RequestParam String ice, Model model) {
        Client client = this.servClient.getClientByIce(ice);
        boolean existe = client != null;

        model.addAttribute("existe", existe);
        if ( client != null){
            model.addAttribute("name", client.getSte());
        }
        return "fragments/ice-feedback :: feedback";
    }

    @PostMapping("/add-client")
    public String addClient(RedirectAttributes attributes, @ModelAttribute("client") Client client){
        Long c = this.servClient.saveClient(client);
        if (c == 1L){
            attributes.addFlashAttribute("success", "Le client "+ client.getSte() +" a été ajouté avec succès.");
            return "redirect:/gharnata/add-client";
        } else {
            attributes.addFlashAttribute("error", "Le client avec ce ICE : "+ client.getIce() +" existe déjà.");
            attributes.addFlashAttribute("cls", client); // conserve la saisie
            return "redirect:/gharnata/add-client";
        }
    }

    @GetMapping("/add-client")
    public String addClient(Model model){
        if (!model.containsAttribute("cls")) {
            model.addAttribute("cls", new Client());
        }
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        return "gest/addClient";
    }

    @GetMapping("/modify-client/{id}")
    public String modifyClient(Model model, @PathVariable Long id){
        Client client = this.servClient.getClientById(id);
        model.addAttribute("cls", client);
        return "gest/modifyClient";
    }
    @PostMapping("/modify-client")
    public String modifyClient(@ModelAttribute("client") Client client, RedirectAttributes attributes){
        System.out.println("le type de client est : "+client.getType());
        System.out.println("ICE : "+client.getIce());
        Long c = this.servClient.modifyClient(client);
        if (c==1){
            attributes.addFlashAttribute("success", "Le client "+ client.getSte()+" a été modifié avec succès.");
        }else {
            attributes.addFlashAttribute("error", "Une erreur s'est produite lors de la modification du client : "+ client.getSte());
        }
        return "redirect:/gharnata/modify-client/"+client.getId();
    }

    @GetMapping("/commandes/{id}")
    public String showComm(Model model, @PathVariable Long id){
        Client client = this.servClient.getClientById(id);
        List<Commande> lC = this.servCommande.getCommandsByClient(client);
        model.addAttribute("client", client);
        model.addAttribute("commands", lC);
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        return "gest/showClientCommands";
    }

    @PostMapping("/modify-credit")
    public String modifierCredit(@RequestParam Long id, @RequestParam double nouveauCredit, RedirectAttributes attributes) {
        servClient.updateClientCredit(id, nouveauCredit);
        attributes.addFlashAttribute("success", "Crédit mis à jour.");
        return "redirect:/gharnata/modify-credit?id=" + id;
    }

    @GetMapping("/modify-credit")
    public String modifyCredit(Model model, @RequestParam(value = "id", required = false) Long id, @RequestParam(value = "ice", required = false) String ice){
        Client c = null;
        if (ice != null) c = servClient.getClientByIce(ice);
        else if (id != null) c = servClient.getClientById(id);
        if (c != null) model.addAttribute("client", c);
        model.addAttribute("clients", servClient.getAllClients());
        return "gest/modifierCredit";
    }

}