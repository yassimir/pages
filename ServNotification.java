package com.gharnata.service;

import com.gharnata.entity.Notification;
import com.gharnata.entity.PanierItems;
import com.gharnata.entity.Produit;
import com.gharnata.repository.RepNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServNotification {

    private final RepNotification repNotification;

    public ServNotification(RepNotification repNotification) {
        this.repNotification = repNotification;
    }

    public long nbNotif(){
        return this.repNotification.count();
    }

    public List<Notification> getAll() {
        return this.repNotification.findAll();
    }

    public void save(Notification notification) {
        this.repNotification.save(notification);
    }
    public void deleteAll(){
        this.repNotification.deleteAll();
    }

    public void deletEnr(Produit pt) {
        Notification nt = this.repNotification.findByProduit(pt);
        if(nt!= null){
            this.repNotification.deleteById(nt.getId());
        }
    }

    public void checkProductExi(Produit produit) {
        Notification notification = this.repNotification.findByProduit(produit);
        if(notification != null){
            this.repNotification.delete(notification);
        }
    }
    public void updateNotif(Produit p){
        Notification n =this.repNotification.findByProduit(p);
        if (n!=null){
            this.repNotification.delete(n);
        }
        if (p.getQuantite()<=5){
            Notification notification = new Notification();
            notification.setProduit(p);
            this.save(notification);
        }
    }


    public void checkProductsExi(List<Produit> prods) {
            List<Notification> lPI = this.repNotification.findByProduitIn(prods);
            if (!lPI.isEmpty()){
                this.repNotification.deleteAll(lPI);
            }
    }
}
