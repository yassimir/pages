package com.gharnata.repository;

import com.gharnata.entity.Notification;
import com.gharnata.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepNotification extends JpaRepository<Notification, Long> {
    Notification findByProduit(Produit pt);

    List<Notification> findByProduitIn(List<Produit> prods);
}
