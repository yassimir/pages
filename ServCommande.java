package com.gharnata.service;


import com.gharnata.entity.*;
import com.gharnata.entity.dto.ProdQte;
import com.gharnata.enums.StatutPaiement;
import com.gharnata.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ServCommande {

    private final RepCommande repCommande;
    private final RepLigneCommand repLigneCommand;
    private final RePaiement rePayments;
    private final RepProduit repProduit;
    private final RepAffectationPaiement repAffectationPaiement;
    private final ServPaiement servPaiement;
    private final RepParametre repParametres;
    private final UtilisateurConnecteService utilisateurConnecteService;

    public ServCommande(RepCommande repCommande,
                        RepLigneCommand repLigneCommand,
                        RepProduit repProduit,
                        RePaiement rePayments,
                        RepAffectationPaiement repAffectationPaiement,
                        ServPaiement servPaiement,
                        RepParametre repParametres,
                        UtilisateurConnecteService utilisateurConnecteService) {

        this.repCommande = repCommande;
        this.repLigneCommand = repLigneCommand;
        this.repProduit = repProduit;
        this.rePayments = rePayments;
        this.repAffectationPaiement = repAffectationPaiement;
        this.servPaiement = servPaiement;
        this.repParametres = repParametres;
        this.utilisateurConnecteService = utilisateurConnecteService;
    }
    public Long getCommandeIdByLigneId(long ligneId) {

        LigneCommande ligne =
                getLigneComandById(ligneId);

        if (ligne == null
                || ligne.getCommande() == null) {
            return null;
        }

        return ligne.getCommande().getId();
    }

    @Transactional
    public Commande saveCommand(Client client) {

        if (client == null) {
            throw new IllegalArgumentException(
                    "Le client est obligatoire."
            );
        }
        Utilisateur utilisateur = utilisateurConnecteService.getUtilisateurConnecte();
        Commande commande = new Commande();

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        commande.setDate(sdf.format(date));
        commande.setClient(client);
        commande.setStatutPaiement(StatutPaiement.NON_PAYEE);
        commande.setConfirme(false);

        commande.setTva(BigDecimal.ZERO);
        commande.setMontantHT(BigDecimal.ZERO);
        commande.setMontantTVA(BigDecimal.ZERO);
        commande.setMontantTTC(BigDecimal.ZERO);
        commande.setUtilisateur(utilisateur);

        return repCommande.save(commande);
    }

    public Commande saveCommand(Commande commande) {

        if (commande == null) {
            throw new IllegalArgumentException(
                    "La commande est obligatoire."
            );
        }

        return repCommande.save(commande);
    }

    public Commande findCommandById(Long id) {

        if (id == null) {
            return null;
        }

        return repCommande.findById(id).orElse(null);
    }

    public List<Commande> getAllCommands() {

        Sort sort = Sort.by(
                Sort.Order.desc("id")
        );

        return repCommande.findAll(sort);
    }

    public List<LigneCommande> getLignesByCommande(
            Commande commande) {

        if (commande == null) {
            return List.of();
        }

        return repLigneCommand.findAllByCommande(commande);
    }

    public LigneCommande getLigneComandById(long id) {

        return repLigneCommand
                .findById(id)
                .orElse(null);
    }

    public List<Commande> getLimitByClient(Client client) {

        if (client == null) {
            return List.of();
        }

        return repCommande.findTop7ByClientOrderByIdDesc(client);
    }

    public List<Commande> getCommandsByClient(Client client) {

        if (client == null) {
            return List.of();
        }

        return repCommande.findAllByClientOrderByIdDesc(client);
    }

    @Transactional
    public int saveLCom(
            Commande commande,
            List<PanierItems> panierItems) {

        if (commande == null) {
            throw new IllegalArgumentException(
                    "La commande est obligatoire."
            );
        }

        if (panierItems == null || panierItems.isEmpty()) {
            return 0;
        }

        int nombreLignes = 0;
        BigDecimal montant = BigDecimal.ZERO;

        for (PanierItems panierItem : panierItems) {

            if (panierItem == null
                    || panierItem.getProduit() == null) {
                continue;
            }

            if (panierItem.getQuantite() <= 0) {
                continue;
            }

            Produit produit = panierItem.getProduit();

            LigneCommande lc = new LigneCommande();

            lc.setCommande(commande);
            lc.setQuantity(panierItem.getQuantite());

            // Snapshot historique du produit
            lc.setProductId(produit.getId());
            lc.setProductName(produit.getName());
            lc.setRef(produit.getRef());
            lc.setPrixVente(produit.getPrixVente());

            BigDecimal montantLigne = produit.getPrixVente().multiply(BigDecimal.valueOf(panierItem.getQuantite()));

            lc.setMontant(montantLigne);

            repLigneCommand.save(lc);

            nombreLignes++;
            montant = montant.add(montantLigne);
        }

        recalculerTotaux(commande, montant);

        return nombreLignes;
    }

    private void recalculerTotaux(
            Commande commande,
            BigDecimal montantHT) {

        commande.setMontantHT(montantHT);
        BigDecimal tauxTva = this.repParametres.findByCle("TVA_TAUX")
                .map(parametre -> new BigDecimal(parametre.getValeur()))
                .orElseThrow(() -> new IllegalStateException(
                        "Le taux de TVA n'est pas configuré"
                ));

        BigDecimal montantTVA = montantHT
                .multiply(tauxTva)
                .setScale(2, RoundingMode.HALF_UP);

        //commande.setMTVA(montantTVA);
        commande.setTva(tauxTva);
        commande.setMontantTVA(montantTVA);

        BigDecimal montantTTC = montantHT.add(montantTVA);

        commande.setMontantTTC(montantTTC);

        /*int dirhams = (int) montantTTC;

        int centimes =
                (int) Math.round(
                        (montantTTC - dirhams) * 100
                );

        String mtl =
                NombreEnLettres.convertir(dirhams)
                        + " dirhams";

        if (centimes > 0) {

            mtl += " et "
                    + NombreEnLettres.convertir(centimes)
                    + " centimes";
        }*/

        //commande.setMtl(mtl);

        repCommande.save(commande);
    }

    @Transactional
    public long deleteLigneComand(long id) {

        LigneCommande ligne =
                getLigneComandById(id);

        if (ligne == null
                || ligne.getCommande() == null) {

            throw new IllegalArgumentException(
                    "La ligne de commande est introuvable."
            );
        }

        Commande commande = ligne.getCommande();

        /*
         * On utilise ServPaiement afin de prendre en compte :
         *
         * 1. les paiements directement liés à la commande
         * 2. les paiements globaux affectés à la commande
         */
        BigDecimal montantPaye = servPaiement.getMontantPaye(commande);

        BigDecimal nouveauMontantHT = commande.getMontantHT().subtract(ligne.getMontant());

        if (nouveauMontantHT.compareTo(BigDecimal.ZERO) < 0) {
            nouveauMontantHT = BigDecimal.ZERO;
        }
        BigDecimal tauxTva = this.repParametres.findByCle("TVA_TAUX")
                .map(parametre -> new BigDecimal(parametre.getValeur()))
                .orElseThrow(() -> new IllegalStateException(
                        "Le taux de TVA n'est pas configuré"
                ));

        BigDecimal nouveauMontantTTC = nouveauMontantHT.add(nouveauMontantHT.multiply(tauxTva).setScale(2, RoundingMode.HALF_UP));

        /*
         * On ne peut pas diminuer le montant de la commande
         * sous le montant déjà payé.
         */
        if (montantPaye.compareTo(nouveauMontantTTC) > 0) {

            throw new IllegalStateException(
                    "Cette ligne ne peut pas être supprimée "
                            + "car le montant déjà payé dépasse "
                            + "le nouveau total de la commande."
            );
        }

        repLigneCommand.delete(ligne);

        recalculerTotaux(commande, nouveauMontantHT);

        /*
         * Le statut peut changer après la suppression.
         *
         * Exemple :
         * commande = 1200 DH
         * payé = 1000 DH
         * suppression d'une ligne de 200 DH
         * nouveau TTC = 1000 DH
         *
         * => commande PAYEE
         */
        servPaiement.mettreAJourStatutPaiement(commande);

        return commande.getId();
    }

    @Transactional
    public void deleteComand(long id) {

        Commande commande =
                findCommandById((long) id);

        if (commande == null) {

            throw new IllegalArgumentException(
                    "La commande est introuvable."
            );
        }

        /*
         * Paiement directement lié à la commande.
         */
        if (rePayments.existsByCommande(commande)) {

            throw new IllegalStateException(
                    "Cette commande ne peut pas être supprimée "
                            + "car elle possède déjà un paiement."
            );
        }

        /*
         * Paiement global affecté à cette commande.
         */
        if (repAffectationPaiement.existsByCommande(commande)) {

            throw new IllegalStateException(
                    "Cette commande ne peut pas être supprimée "
                            + "car elle possède déjà une affectation de paiement."
            );
        }

        List<LigneCommande> lignes =
                repLigneCommand.findAllByCommande(commande);

        repLigneCommand.deleteAll(lignes);

        repCommande.delete(commande);
    }

    @Transactional
    public void addOneLCom(
            Commande commande,
            Produit produit,
            int quantite) {

        if (commande == null || produit == null) {

            throw new IllegalArgumentException(
                    "Commande ou produit invalide."
            );
        }

        if (quantite <= 0) {

            throw new IllegalArgumentException(
                    "La quantité doit être supérieure à zéro."
            );
        }

        /*
         * Une commande totalement payée est verrouillée.
         */
        if (commande.getStatutPaiement()
                == StatutPaiement.PAYEE) {

            throw new IllegalStateException(
                    "Une commande entièrement payée "
                            + "ne peut plus être modifiée."
            );
        }

        List<LigneCommande> lignes =
                repLigneCommand.findAllByCommande(commande);

        for (LigneCommande ligne : lignes) {

            if (Objects.equals(
                    ligne.getRef(),
                    produit.getRef())) {

                return;
            }
        }

        LigneCommande ligne =
                new LigneCommande();

        ligne.setQuantity(quantite);
        ligne.setCommande(commande);

        // Snapshot historique
        ligne.setRef(produit.getRef());
        ligne.setProductName(produit.getName());
        ligne.setProductId(produit.getId());
        ligne.setPrixVente(produit.getPrixVente());

        BigDecimal montant = produit.getPrixVente().multiply(BigDecimal.valueOf(quantite));

        ligne.setMontant(montant);

        repLigneCommand.save(ligne);

        BigDecimal nouveauHT = commande.getMontantHT().add(montant);

        recalculerTotaux(commande, nouveauHT);

        /*
         * Si la commande était partiellement payée,
         * elle le reste après ajout d'une ligne.
         */
        servPaiement.mettreAJourStatutPaiement(commande);
    }

    // ---------------------------------------------------------
    // Gestion du stock / anciennes fonctions
    // ---------------------------------------------------------

    private List<LigneCommande>
    getLigneCommandesByProduit(Produit produit) {

        if (produit == null || produit.getId() == null) {
            return List.of();
        }

        return repLigneCommand
                .findAllByProductId(produit.getId());
    }

    public int checkProductInStock(Produit produit) {

        if (produit == null) {
            return 0;
        }

        List<LigneCommande> lLC =
                getLigneCommandesByProduit(produit);

        int qteResrv = 0;

        for (LigneCommande lc : lLC) {

            if (lc.getCommande() != null
                    && !lc.getCommande().isConfirme()) {

                qteResrv += lc.getQuantity();
            }
        }

        return produit.getQuantite() - qteResrv;
    }

    public List<Commande> findCommandProduitEx(
            Produit produit) {

        if (produit == null
                || produit.getId() == null) {

            return List.of();
        }

        List<LigneCommande> lLC =
                repLigneCommand.findAllByProductId(
                        produit.getId()
                );

        List<Commande> commandId =
                new ArrayList<>();

        for (LigneCommande lc : lLC) {

            if (lc.getCommande() != null
                    && !lc.getCommande().isConfirme()) {

                commandId.add(lc.getCommande());
            }
        }

        return commandId;
    }
}