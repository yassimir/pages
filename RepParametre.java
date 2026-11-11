package com.gharnata.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.gharnata.entity.Parametre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepParametre extends JpaRepository<Parametre, Long> {

    // Rechercher un paramètre par sa clé
    Optional<Parametre> findByCle(String cle);

    // Vérifier si une clé existe
    boolean existsByCle(String cle);

    // Récupérer les paramètres d'une catégorie
    List<Parametre> findByCategorie(String categorie);

    // Rechercher les paramètres par catégorie, triés par clé
    List<Parametre> findByCategorieOrderByCleAsc(String categorie);

    // Rechercher les paramètres dont la clé contient un texte
    List<Parametre> findByCleContainingIgnoreCase(String texte);

    // Récupérer tous les paramètres triés par catégorie
    List<Parametre> findAllByOrderByCategorieAscCleAsc();
}
