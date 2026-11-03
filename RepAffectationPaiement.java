package com.gharnata.repository;

import com.gharnata.entity.AffectationPaiement;
import com.gharnata.entity.Commande;
import com.gharnata.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepAffectationPaiement
        extends JpaRepository<AffectationPaiement, Long> {

    List<AffectationPaiement> findAllByCommande(Commande commande);

    List<AffectationPaiement> findAllByPaiement(Paiement paiement);

    @Query("""
        SELECT COALESCE(SUM(a.montant), 0)
        FROM AffectationPaiement a
        WHERE a.commande = :commande
    """)
    double sumMontantByCommande(@Param("commande") Commande commande);
    boolean existsByCommande(Commande commande);
}
