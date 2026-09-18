package com.gharnata.repository;

import com.gharnata.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepClient extends JpaRepository<Client, Long> {
    Client findByIce(String ice);
}
