package com.gharnata.repository;

import com.gharnata.entity.Facture;
import com.gharnata.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepFacture extends JpaRepository<Facture, Long> {

    Optional<Facture> findByPaiement(Paiement paiement);
}
