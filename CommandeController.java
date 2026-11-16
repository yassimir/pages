package com.gharnata.controller;
import com.gharnata.entity.*;
import com.gharnata.entity.dto.ProductDTO;
import com.gharnata.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Controller
@RequestMapping("/commands")
public class CommandeController {

    private final ServClient servClient;
    private final ServPanier servPanier;
    private final ServProduit servProduit;
    private final ServCommande servCommande;
    private final ServNotification servNotification;
    private final ServPaiement servPaiement;
    private final ServCompte servCompte;

    public CommandeController(
            ServClient servClient,
            ServPanier servPanier,
            ServProduit servProduit,
            ServCommande servCommande,
            ServNotification servNotification,
            ServPaiement servPaiement,
            ServCompte servCompte) {

        this.servClient = servClient;
        this.servPanier = servPanier;
        this.servProduit = servProduit;
        this.servCommande = servCommande;
        this.servNotification = servNotification;
        this.servPaiement = servPaiement;
        this.servCompte = servCompte;
    }

    @GetMapping
    public String showCommande(Model model) {
        System.out.println("here1");
        List<Commande> commandeList = servCommande.getAllCommands();
        System.out.println("here2");
        model.addAttribute("commands", commandeList);
        model.addAttribute("notif",
                servNotification.nbNotif());
        model.addAttribute("cats",
                servProduit.getAllCats());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Commandes"
        ));

        return "gest/showCommandes";
    }

    @GetMapping("/{id}/details")
    public String commandeDetails(
            @PathVariable("id") Long id,
            Model model) {

        Commande commande =
                servCommande.findCommandById(id);

        if (commande == null) {
            return "redirect:/error";
        }

        List<LigneCommande> lignes =
                servCommande.getLignesByCommande(commande);

        /*
         * Le montant restant doit être calculé à partir
         * du TTC et de tous les paiements :
         *
         * paiements directs
         * +
         * affectations de paiements globaux
         */
        BigDecimal reste = servPaiement.getResteAPayer(commande);

        model.addAttribute("commande", commande);
        model.addAttribute("lignes", lignes);

        model.addAttribute("reste", reste);

        model.addAttribute("notif",
                servNotification.nbNotif());

        model.addAttribute("prodInfo",
                servProduit.getProductDTOs());

        model.addAttribute("cats",
                servProduit.getAllCats());

        model.addAttribute("clients",
                servClient.getAllClients());
        model.addAttribute("comptes", servCompte.getComptesActives());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Commandes",
                "Commande nº" + commande.getId()
        ));

        return "gest/commandDetails";
    }



    @GetMapping("/bind-c-c/{id}")
    public String bindC(
            @PathVariable("id") Long id,
            RedirectAttributes attributes) {

        Client client =
                servClient.getClientById(id);

        if (client == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Le client est introuvable."
            );

            return "redirect:/clients";
        }

        attributes.addFlashAttribute(
                "cls",
                client
        );

        return "redirect:/commands/pass-commande";
    }

    @PostMapping("/pass-commande")
    public String passCommande(
            @ModelAttribute Client client,
            RedirectAttributes attributes) {

        List<PanierItems> panierItems =
                servPanier.getPanierItemsByAdm();

        if (panierItems.isEmpty()) {

            attributes.addFlashAttribute(
                    "error",
                    "Aucun produit n'existe dans le panier."
            );

            return "redirect:/commands/pass-commande";
        }

        try {

            Client clientExistant =
                    servClient.checkClientExis(client);

            if (clientExistant == null) {

                attributes.addFlashAttribute(
                        "error",
                        "Le client est introuvable."
                );

                return "redirect:/commands/pass-commande";
            }

            Commande commande =
                    servCommande.saveCommand(
                            clientExistant
                    );

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

                return "redirect:/commands/pass-commande";
            }

            servPanier.clearCart();

            return "redirect:/commands";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            attributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/commands/pass-commande";
        }
    }

    @GetMapping("/delete-ligne-commande/{id}")
    public String deleteLigneCommande(
            @PathVariable("id") Long id,
            RedirectAttributes attributes) {

        Long commandeId =
                servCommande.getCommandeIdByLigneId(id);

        if (commandeId == null) {
            attributes.addFlashAttribute(
                    "error",
                    "La ligne de commande est introuvable."
            );

            return "redirect:/commands";
        }

        try {

            servCommande.deleteLigneComand(id);

            return "redirect:/commands/" + commandeId +"/details";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            attributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/commands/" + commandeId + "/details";
        }
    }

    @GetMapping("/delete/{id}")
    public String suppCom(
            @PathVariable("id") long id,
            RedirectAttributes attributes) {

        try {

            servCommande.deleteComand(id);

            attributes.addFlashAttribute(
                    "success",
                    "La commande a été supprimée."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            attributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/commands";
    }

    @PostMapping("/add-prd-to-command")
    public String addPToCmd(
            @RequestParam String ref,
            @RequestParam long id,
            @RequestParam int qte,
            RedirectAttributes attributes) {

        if (qte <= 0) {

            attributes.addFlashAttribute(
                    "error",
                    "Veuillez choisir une quantité."
            );

            return "redirect:/commands/"+id+"/details";
        }

        Produit produit =
                servProduit.getProductByRef(ref);

        Commande commande =
                servCommande.findCommandById(id);

        if (commande == null) {

            attributes.addFlashAttribute(
                    "error",
                    "La commande est introuvable."
            );

            return "redirect:/commands";
        }

        if (produit == null) {

            attributes.addFlashAttribute(
                    "error",
                    "Le produit est introuvable."
            );

            return "redirect:/commands/"+id+"/details";
        }

        try {

            int qteDispo =
                    servCommande.checkProductInStock(produit);

            System.out.println(
                    "la quantité disponible est : "
                            + qteDispo
            );

            if (qteDispo <= 0) {

                attributes.addFlashAttribute(
                        "error",
                        "Le produit "
                                + produit.getRef()
                                + " est actuellement en rupture de stock."
                );

                attributes.addFlashAttribute(
                        "qteEnHold",
                        0
                );

                return "redirect:/commands/"+id+"/details";
            }

            if (qte > qteDispo) {

                int qteEnHold =
                        produit.getQuantite()
                                - qteDispo;

                List<Commande> commandesProduit =
                        servCommande.findCommandProduitEx(
                                produit
                        );

                attributes.addFlashAttribute(
                        "error",
                        "Il existe juste "
                                + qteDispo
                                + " Pièce(s) de cette référence : "
                                + produit.getRef()
                                + " en stock."
                );

                attributes.addFlashAttribute(
                        "qteEnHold",
                        qteEnHold
                );

                attributes.addFlashAttribute(
                        "lcP",
                        commandesProduit
                );

                return "redirect:/commands/"+id+"/details";
            }

            servCommande.addOneLCom(
                    commande,
                    produit,
                    qte
            );

            return "redirect:/commands/"+id+"/details";

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            attributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/commands/"+id+"/details";
        }
    }

    @GetMapping("/confirme-cmd/{p}/{id}")
    public String valideCmd(
            @PathVariable("id") long id,
            @PathVariable("p") int p,
            RedirectAttributes attributes) {

        Commande commande = servCommande.findCommandById(id);

        if (commande == null) {
            attributes.addFlashAttribute(
                    "error",
                    "La commande est introuvable."
            );

            return "redirect:/commands";
        }
        List<LigneCommande> lignes =
                servCommande.getLignesByCommande(
                        commande
                );
        servProduit.confirmeCmd(
                commande,
                lignes
        );

        if (p == 0) {
            return "redirect:/commands";
        }

        return "redirect:/commands/"+id+"/details";
    }

    @GetMapping("/new")
    public String passMass(Model model){
        List<PanierItems> panierItems = this.servPanier.getPanierItemsByAdm();
        List<ProductDTO> lPI = this.servProduit.getProductDTOs();
        List<Integer> indexs =new ArrayList<>();
        for (int i=1; i<=panierItems.size();i++){
            indexs.add(i);
        }
        model.addAttribute("prodInfos", lPI);
        model.addAttribute("items",panierItems);
        model.addAttribute("mht", this.servPanier.calculPanierMss(panierItems));
        model.addAttribute("idxs", indexs);
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("breadcrumbs", List.of(
                "Gestion Commerciale",
                "Commandes",
                "Nouvelle commande"
        ));
        return "gest/passMasse";
    }

    /*
     * À remplacer plus tard par une vraie méthode
     * de récupération de la commande depuis la ligne.
     *
     * Pour l'instant, utilisé uniquement pour conserver
     * le comportement de redirection en cas d'erreur.
     */
    private long getCommandeIdFromLigne(long ligneId) {

        LigneCommande ligne =
                servCommande.getLigneComandById(ligneId);

        if (ligne == null
                || ligne.getCommande() == null) {

            return 0;
        }

        return ligne.getCommande().getId();
    }
}
