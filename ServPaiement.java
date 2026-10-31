package com.gharnata.service;

import com.gharnata.entity.*;
import com.gharnata.enums.StatutPaiement;
import com.gharnata.repository.RePaiement;
import com.gharnata.repository.RepAffectationPaiement;
import com.gharnata.repository.RepCommande;
import com.gharnata.repository.RepCompte;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ServPaiement {

    private final RePaiement rePaiement;
    private final RepAffectationPaiement repAffectationPaiement;
    private final RepCommande repCommande;
    private final RepCompte repCompte;
    private final UtilisateurConnecteService utilisateurConnecteService;

    public ServPaiement(RePaiement rePaiement,
                        RepAffectationPaiement repAffectationPaiement,
                        RepCommande repCommande,
                        RepCompte repCompte,
                        UtilisateurConnecteService utilisateurConnecteService) {
        this.rePaiement = rePaiement;
        this.repAffectationPaiement = repAffectationPaiement;
        this.repCommande = repCommande;
        this.repCompte = repCompte;
        this.utilisateurConnecteService = utilisateurConnecteService;
    }

    /**
     * Effectue un paiement directement sur une commande.
     */
    @Transactional
    public Paiement payerCommande(Commande commande,
                                  BigDecimal montant,
                                  String modePaiement,
                                  String description) {

        verifierMontant(montant);

        if (commande == null) {
            throw new IllegalArgumentException("La commande est introuvable.");
        }

        BigDecimal reste = getResteAPayer(commande);

        if (montant.compareTo(reste) > 0) {
            throw new IllegalArgumentException(
                    "Le montant du paiement dépasse le reste à payer."
            );
        }

        Paiement paiement = new Paiement();

        paiement.setMontant(montant);
        paiement.setDatePay(
                new SimpleDateFormat("dd/MM/yyyy").format(new Date())
        );
        paiement.setModPai(modePaiement);
        paiement.setDescription(description);
        paiement.setCommande(commande);
        paiement.setClient(commande.getClient());
        paiement.setUtilisateur(this.utilisateurConnecteService.getUtilisateurConnecte());

        paiement = rePaiement.save(paiement);

        mettreAJourStatutPaiement(commande);

        return paiement;
    }

    /**
     * Crée un paiement global pour un client.
     * Le paiement n'est directement lié à aucune commande.
     */
    @Transactional
    public Paiement payerCreditClient(Client client,
                                      BigDecimal montant,
                                      Long compteId,
                                      String modePaiement,
                                      String description) {

        verifierMontant(montant);

        if (client == null) {
            throw new IllegalArgumentException("Le client est introuvable.");
        }

        BigDecimal credit = calculerCreditClient(client);

        if (montant.compareTo(credit) > 0) {
            throw new IllegalArgumentException(
                    "Le montant dépasse le crédit restant du client."
            );
        }

        Paiement paiement = new Paiement();

        paiement.setMontant(montant);
        paiement.setDatePay(
                new SimpleDateFormat("dd/MM/yyyy").format(new Date())
        );
        paiement.setModPai(modePaiement);
        paiement.setDescription(description);
        paiement.setClient(client);
        paiement.setUtilisateur(this.utilisateurConnecteService.getUtilisateurConnecte());
        Compte compte = this.repCompte.findById(compteId)
                .orElseThrow(() -> new EntityNotFoundException("Compte introuvable"));

        if (!compte.getActif()) {
            throw new IllegalStateException("Ce compte est désactivé.");
        }

        paiement.setCompte(compte);

        paiement = rePaiement.save(paiement);

        // Affectation automatique aux commandes les plus anciennes
        affecterAutomatiquement(paiement, client);

        return paiement;
    }
    /*public Paiement payerCreditClient(Client client,
                                      BigDecimal montant,
                                      String modePaiement,
                                      String description) {

        verifierMontant(montant);

        if (client == null) {
            throw new IllegalArgumentException("Le client est introuvable.");
        }

        BigDecimal credit = calculerCreditClient(client);

        if (montant > credit) {
            throw new IllegalArgumentException(
                    "Le montant dépasse le crédit restant du client."
            );
        }

        Paiement paiement = new Paiement();

        paiement.setMontant(montant);
        paiement.setDatePay(
                new SimpleDateFormat("dd/MM/yyyy").format(new Date())
        );
        paiement.setModPai(modePaiement);
        paiement.setDescription(description);
        paiement.setClient(client);

        return rePaiement.save(paiement);
    }*/

    /**
     * Affecte une partie d'un paiement global à une commande.
     */
    @Transactional
    public AffectationPaiement affecterPaiement(Paiement paiement,
                                                Commande commande,
                                                BigDecimal montant) {

        verifierMontant(montant);

        if (paiement == null) {
            throw new IllegalArgumentException("Le paiement est introuvable.");
        }

        if (commande == null) {
            throw new IllegalArgumentException("La commande est introuvable.");
        }

        if (paiement.getCommande() != null) {
            throw new IllegalArgumentException(
                    "Ce paiement est déjà directement lié à une commande."
            );
        }

        if (!Objects.equals(
                paiement.getClient().getId(),
                commande.getClient().getId())) {

            throw new IllegalArgumentException(
                    "Le paiement et la commande n'appartiennent pas au même client."
            );
        }

        BigDecimal montantDejaAffecte = getMontantAffecte(paiement);

        if (montantDejaAffecte.add(montant)
                .compareTo(paiement.getMontant()) > 0) {
            throw new IllegalArgumentException(
                    "Le montant affecté dépasse le montant du paiement."
            );
        }

        BigDecimal resteCommande = getResteAPayer(commande);

        if (montant.compareTo(resteCommande) > 0) {
            throw new IllegalArgumentException(
                    "Le montant affecté dépasse le reste à payer de la commande."
            );
        }

        AffectationPaiement affectation = new AffectationPaiement();

        affectation.setPaiement(paiement);
        affectation.setCommande(commande);
        affectation.setMontant(montant);

        affectation = repAffectationPaiement.save(affectation);

        mettreAJourStatutPaiement(commande);

        return affectation;
    }

    @Transactional
    public void affecterAutomatiquement(Paiement paiement, Client client) {

        List<Commande> commandes =
                repCommande.findAllByClientOrderByIdAsc(client);

        BigDecimal restePaiement = paiement.getMontant()
                .subtract(getMontantAffecte(paiement));

        for (Commande commande : commandes) {

            if (restePaiement.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal resteCommande = getResteAPayer(commande);

            if (resteCommande.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal montantAffectation = restePaiement.min(resteCommande);

            AffectationPaiement affectation = new AffectationPaiement();

            affectation.setPaiement(paiement);
            affectation.setCommande(commande);
            affectation.setMontant(montantAffectation);

            repAffectationPaiement.save(affectation);

            mettreAJourStatutPaiement(commande);

            restePaiement = restePaiement.subtract(montantAffectation);
        }
    }

    /**
     * Retourne le montant directement payé sur une commande.
     */
    public BigDecimal getMontantPayeDirect(Commande commande) {

        if (commande == null) {
            return BigDecimal.valueOf(0);
        }

        return rePaiement
                .findAllByCommandeOrderByIdDesc(commande)
                .stream()
                .map(Paiement::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    /**
     * Retourne le montant provenant des paiements globaux
     * affectés à cette commande.
     */
    public BigDecimal getMontantAffecte(Commande commande) {

        if (commande == null) {
            return BigDecimal.valueOf(0);
        }

        return repAffectationPaiement
                .findAllByCommande(commande)
                .stream()
                .map(AffectationPaiement::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retourne le montant total payé pour une commande.
     */
    public BigDecimal getMontantPaye(Commande commande) {

        return getMontantPayeDirect(commande).add(getMontantAffecte(commande));
    }

    /**
     * Retourne le reste à payer d'une commande.
     */
    public BigDecimal getResteAPayer(Commande commande) {

        if (commande == null) {
            return BigDecimal.valueOf(0);
        }

        BigDecimal reste = commande.getMontantHT()
                .subtract(getMontantPaye(commande));

        return reste.max(BigDecimal.ZERO);
    }

    /**
     * Calcule le crédit global restant du client.
     */
    public BigDecimal calculerCreditClient(Client client) {

        if (client == null) {
            return BigDecimal.valueOf(0);
        }

        return repCommande
                .findAllByClientOrderByIdDesc(client)
                .stream()
                .map(this::getResteAPayer)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    /**
     * Met à jour automatiquement le statut financier d'une commande.
     */
    @Transactional
    public void mettreAJourStatutPaiement(Commande commande) {

        if (commande == null) {
            return;
        }

        BigDecimal paye = getMontantPaye(commande);
        BigDecimal ttc = commande.getMontantTTC();

        if (paye.compareTo(BigDecimal.ZERO) <= 0) {
            commande.setStatutPaiement(
                    StatutPaiement.NON_PAYEE
            );
        } else if (paye.compareTo(ttc) < 0) {
            commande.setStatutPaiement(
                    StatutPaiement.PARTIELLEMENT_PAYEE
            );
        } else {
            commande.setStatutPaiement(
                    StatutPaiement.PAYEE
            );
        }

        repCommande.save(commande);
    }

    /**
     * Retourne les paiements d'une commande.
     */
    public List<Paiement> getPaiementsByCommande(Commande commande) {
        return rePaiement.findAllByCommandeOrderByIdDesc(commande);
    }

    /**
     * Retourne les paiements d'un client.
     */
    public List<Paiement> getPaymentsByClient(Client client) {
        return rePaiement.findAllByClientOrderByIdDesc(client);
    }

    /**
     * Retourne les derniers paiements d'un client.
     */
    public List<Paiement> getLimitByClient(Client client) {
        return rePaiement.findTop7ByClientOrderByIdDesc(client);
    }

    /**
     * Recherche un paiement.
     */
    public Paiement getPaiementById(Long id) {
        return rePaiement.findById(id).orElse(null);
    }

    /**
     * Retourne le montant déjà affecté d'un paiement global.
     */
    public BigDecimal getMontantAffecte(Paiement paiement) {

        if (paiement == null) {
            return BigDecimal.valueOf(0);
        }

        return repAffectationPaiement
                .findAllByPaiement(paiement)
                .stream()
                .map(AffectationPaiement::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    private void verifierMontant(BigDecimal montant) {

        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Le montant doit être supérieur à zéro."
            );
        }
    }
}
