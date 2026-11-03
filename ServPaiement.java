package com.gharnata.service;

import com.gharnata.entity.*;
import com.gharnata.enums.StatutPaiement;
import com.gharnata.repository.*;
import com.gharnata.storage.StorageException;
import com.gharnata.storage.StorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Logger;


import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ServPaiement {

    private static final long TAILLE_MAX_JUSTIFICATIF = 5L * 1024 * 1024;

    private final RePaiement rePaiement;
    private final RepAffectationPaiement repAffectationPaiement;
    private final RepCommande repCommande;
    private final RepCompte repCompte;
    private final UtilisateurConnecteService utilisateurConnecteService;
    private final StorageService storageService;
    private final RepImage repImage;

    public ServPaiement(RePaiement rePaiement,
                        RepAffectationPaiement repAffectationPaiement,
                        RepCommande repCommande,
                        RepCompte repCompte,
                        UtilisateurConnecteService utilisateurConnecteService,
                        StorageService storageService,
                        RepImage repImage) {
        this.rePaiement = rePaiement;
        this.repAffectationPaiement = repAffectationPaiement;
        this.repCommande = repCommande;
        this.repCompte = repCompte;
        this.utilisateurConnecteService = utilisateurConnecteService;
        this.storageService = storageService;
        this.repImage = repImage;
    }


    private static final Logger LOGGER =
            Logger.getLogger(ServPaiement.class.getName());


    public Paiement getPaiementAvecDetails(Long id) {
        return rePaiement.findOneById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Paiement introuvable"));
    }

    private String validerJustificatif(MultipartFile fichier) {

        if (fichier == null || fichier.isEmpty()) {
            return null;
        }

        if (fichier.getSize() > TAILLE_MAX_JUSTIFICATIF) {
            throw new IllegalArgumentException(
                    "Le justificatif ne doit pas dépasser 5 Mo."
            );
        }

        try {
            byte[] signature;

            try (var input = fichier.getInputStream()) {
                signature = input.readNBytes(8);
            }

            if (signature.length >= 5
                    && signature[0] == 0x25
                    && signature[1] == 0x50
                    && signature[2] == 0x44
                    && signature[3] == 0x46
                    && signature[4] == 0x2D) {
                return "application/pdf";
            }

            if (signature.length >= 3
                    && (signature[0] & 0xFF) == 0xFF
                    && (signature[1] & 0xFF) == 0xD8
                    && (signature[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }

            if (signature.length == 8
                    && Arrays.equals(signature, new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A
            })) {
                return "image/png";
            }

            throw new IllegalArgumentException(
                    "Format invalide. Formats autorisés : PDF, JPG et PNG."
            );

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Impossible de lire le justificatif."
            );
        }
    }

    private void enregistrerJustificatif(
            Paiement paiement,
            MultipartFile fichier) {

        String typeMime = validerJustificatif(fichier);

        // Aucun justificatif fourni
        if (typeMime == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "L'enregistrement du justificatif nécessite une transaction."
            );
        }

        // Enregistrement physique du fichier
        String chemin;

        try (InputStream input = fichier.getInputStream()) {

            chemin = storageService.store(
                    input,
                    "paiements",
                    typeMime
            );

        } catch (IOException e) {

            throw new StorageException(
                    "Impossible de lire le justificatif envoyé.",
                    e
            );
        }

        // Nettoyage automatique si la transaction est annulée
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status == STATUS_ROLLED_BACK) {

                            try {
                                storageService.delete(chemin);

                            } catch (RuntimeException e) {

                                LOGGER.severe(
                                        "Impossible de supprimer le fichier orphelin : "
                                                + chemin + " - " + e.getMessage()
                                );
                            }
                        }
                    }
                }
        );

        // Création des métadonnées
        Image image = new Image();

        image.setCheminFichier(chemin);
        image.setTypeMime(typeMime);
        image.setTaille(fichier.getSize());

        String nomOriginal = fichier.getOriginalFilename();

        if (nomOriginal != null) {

            // Éviter de conserver un chemin fourni par le navigateur
            nomOriginal = nomOriginal.replace("\\", "/");

            nomOriginal = nomOriginal.substring(
                    nomOriginal.lastIndexOf('/') + 1
            ).trim();

            if (nomOriginal.length() > 255) {
                nomOriginal = nomOriginal.substring(0, 255);
            }
        }

        image.setNomOriginal(nomOriginal);

        image = repImage.save(image);

        // Association avec le paiement
        paiement.setJustificatif(image);

        rePaiement.save(paiement);
    }

    /**
     * Retourne les paiements avec pagination.
     * Les plus récents apparaissent en premier.
     */
    public Page<Paiement> getPaiementsPagines(Pageable pageable) {
        return rePaiement.findAllByOrderByIdDesc(pageable);
    }
    public Paiement getPaiementAvecClient(Long id) {
        return rePaiement.findWithClientById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Paiement introuvable"));
    }
    public Page<Paiement> rechercherPaiements(
            String recherche,
            String statut,
            String methode,
            Pageable pageable) {

        Specification<Paiement> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Recherche générale : client, description ou identifiant
            if (recherche != null && !recherche.isBlank()) {
                String valeur = "%" + recherche.trim().toLowerCase(Locale.ROOT) + "%";

                Join<Paiement, Client> client =
                        root.join("client", JoinType.LEFT);

                predicates.add(cb.or(
                        cb.like(cb.lower(client.get("ste")), valeur),
                        cb.like(cb.lower(root.get("description")), valeur),
                        cb.like(root.get("id").as(String.class), valeur)
                ));
            }

            // Filtre par statut
            if (statut != null && !statut.isBlank()) {
                if ("confirme".equalsIgnoreCase(statut)) {
                    predicates.add(cb.isTrue(root.get("confirmed")));
                } else if ("attente".equalsIgnoreCase(statut)) {
                    predicates.add(cb.isFalse(root.get("confirmed")));
                }
            }

            // Filtre par mode de paiement
            if (methode != null && !methode.isBlank()) {

                String valeur = "%" +
                        methode.trim().toLowerCase(Locale.ROOT) +
                        "%";

                predicates.add(
                        cb.like(
                                cb.lower(root.get("modPai")),
                                valeur
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return rePaiement.findAll(specification, pageable);
    }



    /**
     * Effectue un paiement directement sur une commande.
     */
    @Transactional
    public Paiement payerCommande(Commande commande,
                                  BigDecimal montant,
                                  String modePaiement,
                                  String description,
                                  MultipartFile justificatif) {

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

        // Enregistrement facultatif du justificatif
        enregistrerJustificatif(paiement, justificatif);

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
                                      String description,
                                      MultipartFile justificatif) {

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

        // Enregistrement facultatif du justificatif
        enregistrerJustificatif(paiement, justificatif);

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
