package com.gharnata.repository;

import com.gharnata.entity.Categorie;
import com.gharnata.entity.Produit;
import com.gharnata.entity.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface RepProduit extends JpaRepository<Produit, Long> {
    Produit findByRef(String ref);

    List<Produit> findAllByCategorie(Categorie c);
    @Query("SELECT new com.gharnata.entity.dto.ProductDTO(p.name, p.ref) FROM Produit p")
    List<ProductDTO> findAllProductDTO();

    List<Produit> findTop18ByOrderByIdDesc();

    List<Produit> findByPrixVenteLessThanEqual(BigDecimal prixVente);

    /*@Query("""
    SELECT p
    FROM Produit p
    WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
       OR LOWER(p.ref) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY p.name
""")
    List<Produit> searchProducts(@Param("query") String query);*/

    @Query("""
    SELECT p
    FROM Produit p
    WHERE LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
       OR LOWER(COALESCE(p.ref, '')) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY p.name
    """)
    List<Produit> searchProducts(@Param("query") String query);

    List<Produit> findByCategorie_IdInAndPrixVenteLessThanEqual(
            List<Integer> categorieIds,
            BigDecimal prixMax
    );

    @EntityGraph(
            attributePaths = {"categorie"},
            type = EntityGraph.EntityGraphType.FETCH
    )
    @Query(
            value = """
            SELECT p
            FROM Produit p
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(p.ref, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            ORDER BY p.name ASC
            """,
            countQuery = """
            SELECT COUNT(p)
            FROM Produit p
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(p.ref, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """
    )
    Page<Produit> findStockPage(
            @Param("search") String search,
            Pageable pageable
    );

    /*
    @Query(
            value = """
                SELECT p FROM Produit p
                WHERE (:search IS NULL OR :search = ''
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.ref) LIKE LOWER(CONCAT('%', :search, '%')))
                """,
            countQuery = """
                SELECT COUNT(p) FROM Produit p
                WHERE (:search IS NULL OR :search = ''
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.ref) LIKE LOWER(CONCAT('%', :search, '%')))
                """
    )
    Page<Produit> findStockPage(
            @Param("search") String search,
            Pageable pageable
    );*/



}
