package com.gharnata.repository;

import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import com.gharnata.entity.Paiement;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Paiement p WHERE p.id = :id")
    Optional<Paiement> findByIdForUpdate(@Param("id") Long id);

    @Query("""
    SELECT p
    FROM Paiement p
    WHERE p.actif = true
      AND p.confirmed = false
      AND p.dateEcheance IS NOT NULL
      AND p.dateEcheance <= :date
      AND (
            p.modPai = 'Chèque'
            OR p.modPai LIKE 'Chèque -%'
            OR p.modPai = 'Effet'
            OR p.modPai LIKE 'Effet -%'
      )
    """)
    List<Paiement> findPaiementsArrivesAEcheance(
            @Param("date") LocalDate date
    );
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
