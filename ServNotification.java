package com.gharnata.service;

import com.gharnata.entity.Notification;
import com.gharnata.entity.PanierItems;
import com.gharnata.entity.Parametre;
import com.gharnata.entity.Produit;
import com.gharnata.repository.RepNotification;
import com.gharnata.repository.RepParametre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServNotification {

    private static final int SEUIL_DEFAUT = 5;
    private static final String CLE_SEUIL_STOCK = "STOCK_SEUIL_ALERTE";

    private final RepNotification repNotification;
    private final RepParametre repParametre;

    public ServNotification(
            RepNotification repNotification,
            RepParametre repParametre) {
        this.repNotification = repNotification;
        this.repParametre = repParametre;
    }

    public long nbNotif() {
        return repNotification.count();
    }

    public List<Notification> getAll() {
        return repNotification.findAll();
    }

    public List<Notification> getDernieresNotifications() {
        return repNotification.findAllByOrderByDateCreationDesc();
    }

    public void save(Notification notification) {
        repNotification.save(notification);
    }

    public void deleteAll() {
        repNotification.deleteAll();
    }

    public void deletEnr(Produit produit) {
        Notification notification = repNotification.findByProduit(produit);

        if (notification != null) {
            repNotification.delete(notification);
        }
    }

    public void checkProductExi(Produit produit) {
        Notification notification = repNotification.findByProduit(produit);

        if (notification != null) {
            repNotification.delete(notification);
        }
    }

    /**
     * Vérifie le stock d'un produit et actualise sa notification.
     */
    @Transactional
    public void updateNotif(Produit produit) {

        int seuil = getSeuilStock();
        Notification notification = repNotification.findByProduit(produit);

        // Le stock est redevenu normal : supprimer la notification active.
        if (produit.getQuantite() > seuil) {
            if (notification != null) {
                repNotification.delete(notification);
            }
            return;
        }

        // Créer une notification uniquement si elle n'existe pas.
        if (notification == null) {
            notification = new Notification();
            notification.setProduit(produit);
        }

        // Déterminer le niveau de notification.
        if (produit.getQuantite() <= 0) {
            notification.setNiveau(
                    Notification.NiveauNotification.RUPTURE
            );
        } else {
            notification.setNiveau(
                    Notification.NiveauNotification.STOCK_FAIBLE
            );
        }

        repNotification.save(notification);
    }

    public void checkProductsExi(List<Produit> produits) {
        List<Notification> notifications =
                repNotification.findByProduitIn(produits);

        if (!notifications.isEmpty()) {
            repNotification.deleteAll(notifications);
        }
    }

    /**
     * Récupère le seuil configuré dans la table Parametre.
     */
    private int getSeuilStock() {

        return repParametre.findByCle(CLE_SEUIL_STOCK)
                .map(Parametre::getValeur)
                .map(valeur -> {
                    try {
                        return Integer.parseInt(valeur);
                    } catch (NumberFormatException e) {
                        return SEUIL_DEFAUT;
                    }
                })
                .orElse(SEUIL_DEFAUT);
    }
}