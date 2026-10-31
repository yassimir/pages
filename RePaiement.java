package com.gharnata.repository;

import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import com.gharnata.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RePaiement extends JpaRepository<Paiement, Long> {
    List<Paiement> findAllByClient(Client client);

    List<Paiement> findAllByClientOrderByIdDesc(Client client);

    List<Paiement> findAllByCommande(Commande commande);

    List<Paiement> findTop7ByClientOrderByIdDesc(Client client);

    List<Paiement> findAllByCommandeOrderByIdDesc(Commande commande);

    boolean existsByCommande(Commande commande);

}
