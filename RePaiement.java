package com.gharnata.repository;

import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import com.gharnata.entity.Paiement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface RePaiement extends
        JpaRepository<Paiement, Long>,
        JpaSpecificationExecutor<Paiement> {

    List<Paiement> findAllByClient(Client client);

    List<Paiement> findAllByClientOrderByIdDesc(Client client);

    List<Paiement> findAllByCommande(Commande commande);

    List<Paiement> findTop7ByClientOrderByIdDesc(Client client);

    List<Paiement> findAllByCommandeOrderByIdDesc(Commande commande);

    boolean existsByCommande(Commande commande);

    @EntityGraph(attributePaths = "client")
    Page<Paiement> findAllByOrderByIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "client")
    Page<Paiement> findAll(
            Specification<Paiement> specification,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "client")
    Optional<Paiement> findWithClientById(Long id);

    @EntityGraph(attributePaths = {
            "client",
            "compte",
            "justificatif"
    })
    Optional<Paiement> findOneById(Long id);
}

/*public interface RePaiement extends JpaRepository<Paiement, Long> {
    List<Paiement> findAllByClient(Client client);

    List<Paiement> findAllByClientOrderByIdDesc(Client client);

    List<Paiement> findAllByCommande(Commande commande);

    List<Paiement> findTop7ByClientOrderByIdDesc(Client client);

    List<Paiement> findAllByCommandeOrderByIdDesc(Commande commande);

    boolean existsByCommande(Commande commande);

    @EntityGraph(attributePaths = "client")
    Page<Paiement> findAllByOrderByIdDesc(Pageable pageable);

}*/
