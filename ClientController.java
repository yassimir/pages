package com.gharnata.controller;

import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import com.gharnata.entity.Paiement;
import com.gharnata.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/gharnata")
public class ClientController {
    @Autowired
    private ServClient servClient;
    @Autowired
    private ServCommande servCommande;
    @Autowired
    private ServNotification servNotification;
    @Autowired
    private ServProduit servProduit;
    @Autowired
    private ServPaiement servPaiement;

    @GetMapping("/list-clients")
    public String listClient(Model model, @RequestParam(value = "ice", required = false) String ice){
        List<Client> lC = new ArrayList<>();
        if (ice != null) {
            Client client = servClient.getClientByIce(ice);
            lC.add(client);
        }else{
            lC = this.servClient.getAllClients();
        }
        model.addAttribute("clients", lC);
        model.addAttribute("cls", this.servClient.getAllClients());
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        return "gest/listClient";
    }
    @PostMapping("/list-clients")
    public String listClient(@RequestParam("ice") String ice, RedirectAttributes attributes){
        attributes.addAttribute("ice", ice);
        return "redirect:/gharnata/list-clients";
    }
    @GetMapping("/client-details/{id}")
    public String showOneProduct(@PathVariable Long id, Model model){
        Client client = this.servClient.getClientById(id);
        List<Paiement> lP = this.servPaiement.getLimitByClient(client);
        List<Commande> lC = this.servCommande.getLimitByClient(client);
        model.addAttribute("client", client);
        model.addAttribute("commandes", lC);
        model.addAttribute("payments", lP);
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        return "gest/clientDetails";
    }
    @GetMapping("/delete-client/{id}")
    public String archiveClient(@PathVariable long id){
        this.servClient.archiveClient(id);
        return "redirect:/gharnata/list-clients";
    }

    @GetMapping("/add-client")
    public String addClient(@RequestParam(value = "cl", required = false) Client cl, Model model){
        if (cl != null) {
            model.addAttribute("cls", cl);
        }
        model.addAttribute("cls",new Client());
        return "gest/addClient";
    }
    @PostMapping("/add-client")
    public String addClient(RedirectAttributes attributes, @ModelAttribute("client") Client client){
        Long c = this.servClient.saveClient(client);
        if (c==1){
            attributes.addFlashAttribute("success", "Le client "+ client.getSte()+" a été ajouté avec succès.");
        }else {
            attributes.addFlashAttribute("error", "Le client avec ce ICE: "+ client.getIce()+" existe déjà.");
            attributes.addAttribute("cl", client);
        }
        return "redirect:/gharnata/add-client";
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
    @GetMapping("/paiements/{id}")
    public String showPaie(Model model, @PathVariable Long id){
        Client client = this.servClient.getClientById(id);
        List<Paiement> lP = this.servPaiement.getPaymentsByClient(client);
        model.addAttribute("client", client);
        model.addAttribute("payments", lP);
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        return "gest/showPayments";
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

    @GetMapping("/modify-credit")
    public String modifyCredit(Model model, @RequestParam(value = "id", required = false) Long id,  @RequestParam(value = "nouveauCredit", required = false) Double nouveauCredit, @RequestParam(value = "ice", required = false) String ice){
        System.out.println("id" + id);
        System.out.println("nouveauCredit" + nouveauCredit);
        System.out.println("ice" + ice);
        List<Client> clients = servClient.getAllClients();
        Client c;
        if (ice != null) {
            c = this.servClient.getClientByIce(ice);
            model.addAttribute("client", c);
        }else if(nouveauCredit != null) {
            c = servClient.updateClientCredit(id, nouveauCredit);
            model.addAttribute("client", c);
        }
        model.addAttribute("clients", clients);
        return "gest/modifierCredit";
    }
    @PostMapping("/modify-credit")
    public String modifierCredit(@RequestParam Long id, @RequestParam double nouveauCredit, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        attributes.addAttribute("nouveauCredit", nouveauCredit);
        return "redirect:/gharnata/modify-credit";
    }
}