package com.gharnata.repository;

import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RepCommande extends JpaRepository<Commande, Long> {
    List<Commande> findAllByClientOrderByIdDesc(Client client);

    List<Commande> findTop7ByClientOrderByIdDesc(Client client);

    List<Commande> findAllByClientOrderByIdAsc(Client client);
}
