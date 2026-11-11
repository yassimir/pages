package com.gharnata.service;

import com.gharnata.entity.*;
import com.gharnata.entity.dto.ProductDTO;
import com.gharnata.repository.RepCategory;
import com.gharnata.repository.RepCommande;
import com.gharnata.repository.RepImage;
import com.gharnata.repository.RepProduit;
import com.gharnata.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class ServProduit {
    private RepProduit repProduit ;
    private RepCategory repCategory;
    private ServNotification servNotification;
    private RepCommande repCommande;
    private final RepImage repImage;
    private final StorageService storageService;

    private static final Logger LOGGER = Logger.getLogger(ServProduit.class.getName());


    public ServProduit(RepProduit repProduit, ServNotification servNotification,
                       RepCategory repCategory, RepCommande repCommande,
                       RepImage repImage, StorageService storageService) {
        this.repProduit = repProduit;
        this.servNotification = servNotification;
        this.repCategory = repCategory;
        this.repCommande = repCommande;
        this.repImage = repImage;
        this.storageService = storageService;
    }

    public List<ProductDTO> searchProducts(String query) {

        return repProduit.searchProducts(query)
                .stream()
                .map(product -> new ProductDTO(
                        product.getId(),
                        product.getName(),
                        product.getRef()
                ))
                .toList();
    }

    public Produit addProduit(Produit produit){
        return this.repProduit.save(produit);
    }
    public List<Produit> getProducts(){
        return this.repProduit.findAll();
    }
    public List<Produit> getProductsLimit(){
        return this.repProduit.findTop18ByOrderByIdDesc();
    }
    public List<ProductDTO> getProductDTOs(){
        return this.repProduit.findAllProductDTO();
    }
    public Produit getProductById(Long id){
        return this.repProduit.findById(id).orElse(null);
    }

    public Produit getProductByRef(String ref) {
        return this.repProduit.findByRef(ref);
    }
    public List<Produit> getProductsByCat(int idCat){
        Categorie c = this.repCategory.findById(idCat).orElse(null);
        return this.repProduit.findAllByCategorie(c);
    }

    @Transactional
    public void deletePrById(Long id) {

        Produit produit = repProduit.findById(id)
                .orElse(null);

        if (produit == null) {
            return;
        }

        Image image = produit.getPic();

        String chemin = image != null
                ? image.getCheminFichier()
                : null;

        // Supprimer d'abord le produit
        repProduit.delete(produit);
        repProduit.flush();

        // Supprimer ensuite les métadonnées de l'image
        if (image != null) {
            repImage.delete(image);
            repImage.flush();
        }

        // Supprimer le fichier physique après validation
        programmerSuppressionFichier(chemin);
    }

    public Categorie getCatById(int id){
        return this.repCategory.findById(id).orElse(null);
    }


    public List<Categorie> getAllCats() {
        return this.repCategory.findAll();
    }

    public void addCat(String cat) {
        Categorie categorie = this.repCategory.findByLib(cat);
        if (categorie==null){
            categorie = new Categorie();
            categorie.setLib(cat);
            this.repCategory.save(categorie);
        }
    }

    public void confirmeCmd(Commande c, List<LigneCommande> lLC) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        int i= 0;
        for (LigneCommande lc : lLC){
            System.out.println("produit " + i + " = " + lc.getProductName());
            System.out.println("profuit " + i + " = "+ lc.getProductId());
            i++;
        }
        for (LigneCommande lc : lLC){
            System.out.println("here : "+ i);
            Produit p = this.getProductById(lc.getProductId());
            p.setQuantite(p.getQuantite()-lc.getQuantity());
            this.servNotification.updateNotif(this.addProduit(p));
            i++;
        }
        c.setConfirme(true);
        c.setDateConfirmation(sdf.format(new Date()));
        this.repCommande.save(c);
        /*Compte cmpt = this.servClient.getCmptById(c.getClient().getCompte().getId());
        cmpt.setCredit(cmpt.getCredit()+c.getMontantHT());
        this.servClient.saveCmpt(cmpt);*/

    }

    @Transactional
    public void deleteAllById(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Produit> produits = repProduit.findAllById(ids);

        if (produits.isEmpty()) {
            return;
        }

        List<Image> images = produits.stream()
                .map(Produit::getPic)
                .filter(image -> image != null)
                .toList();

        Set<String> chemins = new LinkedHashSet<>();

        for (Image image : images) {
            if (image.getCheminFichier() != null) {
                chemins.add(image.getCheminFichier());
            }
        }

        // Suppression des produits
        repProduit.deleteAll(produits);
        repProduit.flush();

        // Suppression des métadonnées
        repImage.deleteAll(images);
        repImage.flush();

        // Suppression physique après validation de la transaction
        for (String chemin : chemins) {
            programmerSuppressionFichier(chemin);
        }
    }

    private void programmerSuppressionFichier(String chemin) {

        if (chemin == null
                || chemin.isBlank()
                || chemin.equals("produits/defaut.jpg")) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        try {
                            storageService.delete(chemin);

                        } catch (RuntimeException e) {

                            LOGGER.severe(
                                    "Impossible de supprimer le fichier : "
                                            + chemin + " - " + e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    public List<Produit> getAllByIds(List<Long> ids) {
        return this.repProduit.findAllById(ids);
    }
}
