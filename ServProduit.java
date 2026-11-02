package com.gharnata.service;

import com.gharnata.entity.*;
import com.gharnata.entity.dto.ProductDTO;
import com.gharnata.repository.RepCategory;
import com.gharnata.repository.RepCommande;
import com.gharnata.repository.RepProduit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class ServProduit {
    private RepProduit repProduit ;
    private RepCategory repCategory;
    private ServNotification servNotification;
    private RepCommande repCommande;
    private ServClient servClient;


    public ServProduit(RepProduit repProduit, ServClient servClient,
                       ServNotification servNotification, RepCategory repCategory,
                       RepCommande repCommande) {
        this.repProduit = repProduit;
        this.servClient = servClient;
        this.servNotification = servNotification;
        this.repCategory = repCategory;
        this.repCommande = repCommande;
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

    public void deletePrById(Long id) {
        this.repProduit.deleteById(id);
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

    public void deleteAllById(List<Long> ids) {
        this.repProduit.deleteAllById(ids);
    }

    public List<Produit> getAllByIds(List<Long> ids) {
        return this.repProduit.findAllById(ids);
    }
}
