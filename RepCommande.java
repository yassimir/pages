package com.gharnata.repository;

import com.gharnata.entity.Client;
import com.gharnata.entity.Commande;
import com.gharnata.enums.StatutPaiement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RepCommande extends JpaRepository<Commande, Long> {

    List<Commande> findAllByClientOrderByIdDesc(Client client);

    List<Commande> findTop7ByClientOrderByIdDesc(Client client);

    List<Commande> findAllByClientOrderByIdAsc(Client client);

    @EntityGraph(attributePaths = {"client"})
    @Query(
            value = """
                SELECT c
                FROM Commande c
                LEFT JOIN c.client cl
                WHERE (
                    :query IS NULL
                    OR :query = ''
                    OR LOWER(COALESCE(cl.ste, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(cl.ice, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(cl.tel, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                )
                AND (
                    :statutPaiement IS NULL
                    OR c.statutPaiement = :statutPaiement
                )
                AND (
                    :confirme IS NULL
                    OR c.confirme = :confirme
                )
                AND (
                    :dateRecherche IS NULL
                    OR :dateRecherche = ''
                    OR COALESCE(c.date, '') LIKE CONCAT('%', :dateRecherche, '%')
                )
                ORDER BY c.id DESC
                """,
            countQuery = """
                SELECT COUNT(c)
                FROM Commande c
                LEFT JOIN c.client cl
                WHERE (
                    :query IS NULL
                    OR :query = ''
                    OR LOWER(COALESCE(cl.ste, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(cl.ice, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(cl.tel, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                )
                AND (
                    :statutPaiement IS NULL
                    OR c.statutPaiement = :statutPaiement
                )
                AND (
                    :confirme IS NULL
                    OR c.confirme = :confirme
                )
                AND (
                    :dateRecherche IS NULL
                    OR :dateRecherche = ''
                    OR COALESCE(c.date, '') LIKE CONCAT('%', :dateRecherche, '%')
                )
                """
    )
    Page<Commande> findCommandesPage(
            @Param("query") String query,
            @Param("statutPaiement") StatutPaiement statutPaiement,
            @Param("confirme") Boolean confirme,
            @Param("dateRecherche") String dateRecherche,
            Pageable pageable
    );
}

/*public interface RepCommande extends JpaRepository<Commande, Long> {
    List<Commande> findAllByClientOrderByIdDesc(Client client);

    List<Commande> findTop7ByClientOrderByIdDesc(Client client);

    List<Commande> findAllByClientOrderByIdAsc(Client client);
}*/
