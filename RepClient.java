package com.gharnata.repository;

import com.gharnata.entity.Client;
import com.gharnata.entity.Produit;
import com.gharnata.entity.dto.ClientDTO;
import com.gharnata.entity.dto.ProductDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RepClient extends JpaRepository<Client, Long> {
    Client findByIce(String ice);



    @Query("SELECT new com.gharnata.entity.dto.ClientDTO(c.ste, c.ice) FROM Client c")
    List<ClientDTO> findAllClientDTO();




    @Query("""
    SELECT c
    FROM Client c
    WHERE LOWER(c.ste) LIKE LOWER(CONCAT('%', :query, '%'))
       OR LOWER(c.ice) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY c.ste
""")
    List<Client> searchClients(@Param("query") String query);
}
