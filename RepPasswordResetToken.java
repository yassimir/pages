
package com.gharnata.repository;

import com.gharnata.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RepPasswordResetToken
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUtilisateur_Id(Long utilisateurId);

    void deleteByExpiresAtBefore(LocalDateTime date);

    void deleteByUsedTrue();
}
