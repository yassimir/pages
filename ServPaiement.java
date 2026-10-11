package com.gharnata.service;

import com.gharnata.entity.AffectationPaiement;
import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import com.gharnata.entity.Paiement;
import com.gharnata.enums.StatutPaiement;
import com.gharnata.repository.RePaiement;
import com.gharnata.repository.RepAffectationPaiement;
import com.gharnata.repository.RepCommande;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ServPaiement {

    private final RePaiement rePaiement;
    private final RepAffectationPaiement repAffectationPaiement;
    private final RepCommande repCommande;

    public ServPaiement(RePaiement rePaiement,
                        RepAffectationPaiement repAffectationPaiement,
                        RepCommande repCommande) {
        this.rePaiement = rePaiement;
        this.repAffectationPaiement = repAffectationPaiement;
        this.repCommande = repCommande;
    }

    /**
     * Effectue un paiement directement sur une commande.
     */
    @Transactional
    public Paiement payerCommande(Commande commande,
                                  double montant,
                                  String modePaiement,
                                  String description) {

        verifierMontant(montant);

        if (commande == null) {
            throw new IllegalArgumentException("La commande est introuvable.");
        }

        double reste = getResteAPayer(commande);

        if (montant > reste) {
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
                                      double montant,
                                      String modePaiement,
                                      String description) {

        verifierMontant(montant);

        if (client == null) {
            throw new IllegalArgumentException("Le client est introuvable.");
        }

        double credit = calculerCreditClient(client);

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

        paiement = rePaiement.save(paiement);

        // Affectation automatique aux commandes les plus anciennes
        affecterAutomatiquement(paiement, client);

        return paiement;
    }
    /*public Paiement payerCreditClient(Client client,
                                      double montant,
                                      String modePaiement,
                                      String description) {

        verifierMontant(montant);

        if (client == null) {
            throw new IllegalArgumentException("Le client est introuvable.");
        }

        double credit = calculerCreditClient(client);

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
                                                double montant) {

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

        double montantDejaAffecte =
                getMontantAffecte(paiement);

        if (montantDejaAffecte + montant > paiement.getMontant()) {
            throw new IllegalArgumentException(
                    "Le montant affecté dépasse le montant du paiement."
            );
        }

        double resteCommande = getResteAPayer(commande);

        if (montant > resteCommande) {
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

        double restePaiement =
                paiement.getMontant() - getMontantAffecte(paiement);

        for (Commande commande : commandes) {

            if (restePaiement <= 0) {
                break;
            }

            double resteCommande = getResteAPayer(commande);

            if (resteCommande <= 0) {
                continue;
            }

            double montantAffectation =
                    Math.min(restePaiement, resteCommande);

            AffectationPaiement affectation = new AffectationPaiement();

            affectation.setPaiement(paiement);
            affectation.setCommande(commande);
            affectation.setMontant(montantAffectation);

            repAffectationPaiement.save(affectation);

            mettreAJourStatutPaiement(commande);

            restePaiement -= montantAffectation;
        }
    }

    /**
     * Retourne le montant directement payé sur une commande.
     */
    public double getMontantPayeDirect(Commande commande) {

        if (commande == null) {
            return 0;
        }

        return rePaiement
                .findAllByCommandeOrderByIdDesc(commande)
                .stream()
                .mapToDouble(Paiement::getMontant)
                .sum();
    }

    /**
     * Retourne le montant provenant des paiements globaux
     * affectés à cette commande.
     */
    public double getMontantAffecte(Commande commande) {

        if (commande == null) {
            return 0;
        }

        return repAffectationPaiement
                .findAllByCommande(commande)
                .stream()
                .mapToDouble(AffectationPaiement::getMontant)
                .sum();
    }

    /**
     * Retourne le montant total payé pour une commande.
     */
    public double getMontantPaye(Commande commande) {

        return getMontantPayeDirect(commande)
                + getMontantAffecte(commande);
    }

    /**
     * Retourne le reste à payer d'une commande.
     */
    public double getResteAPayer(Commande commande) {

        if (commande == null) {
            return 0;
        }

        double reste =
                commande.getMontantTTC() - getMontantPaye(commande);

        return Math.max(0, reste);
    }

    /**
     * Calcule le crédit global restant du client.
     */
    public double calculerCreditClient(Client client) {

        if (client == null) {
            return 0;
        }

        return repCommande
                .findAllByClientOrderByIdDesc(client)
                .stream()
                .mapToDouble(this::getResteAPayer)
                .sum();
    }

    /**
     * Met à jour automatiquement le statut financier d'une commande.
     */
    @Transactional
    public void mettreAJourStatutPaiement(Commande commande) {

        if (commande == null) {
            return;
        }

        double paye = getMontantPaye(commande);
        double ttc = commande.getMontantTTC();

        if (paye <= 0) {
            commande.setStatutPaiement(
                    StatutPaiement.NON_PAYEE
            );
        } else if (paye < ttc) {
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
    public double getMontantAffecte(Paiement paiement) {

        if (paiement == null) {
            return 0;
        }

        return repAffectationPaiement
                .findAllByPaiement(paiement)
                .stream()
                .mapToDouble(AffectationPaiement::getMontant)
                .sum();
    }

    private void verifierMontant(double montant) {

        if (montant <= 0) {
            throw new IllegalArgumentException(
                    "Le montant doit être supérieur à zéro."
            );
        }
    }
}
