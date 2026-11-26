
package com.gharnata.service;

import com.gharnata.entity.PasswordResetToken;
import com.gharnata.entity.Utilisateur;
import com.gharnata.repository.RepPasswordResetToken;
import com.gharnata.repository.UtilisateurRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class ServPasswordReset {

    private final UtilisateurRepository utilisateurRepository;
    private final RepPasswordResetToken tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url}")
    private String baseUrl;

    public ServPasswordReset(
            UtilisateurRepository utilisateurRepository,
            RepPasswordResetToken tokenRepository,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder) {

        this.utilisateurRepository = utilisateurRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void demanderReinitialisation(String email) {

        if (email == null || email.isBlank()) {
            return;
        }

        utilisateurRepository.findByEmail(email.trim())
                .filter(Utilisateur::isActif)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .ifPresent(utilisateur -> {

                    tokenRepository.deleteByUtilisateur_Id(utilisateur.getId());

                    byte[] randomBytes = new byte[32];
                    secureRandom.nextBytes(randomBytes);

                    String token = Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(randomBytes);

                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setTokenHash(hashToken(token));
                    resetToken.setUtilisateur(utilisateur);
                    resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
                    resetToken.setUsed(false);

                    tokenRepository.save(resetToken);

                    String lien = baseUrl
                            + "/reset-password?token=" + token;

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(utilisateur.getEmail());
                    message.setSubject("Réinitialisation de votre mot de passe");
                    message.setText(
                            "Bonjour " + utilisateur.getUsername() + ",\n\n"
                                    + "Vous avez demandé la réinitialisation de votre mot de passe.\n\n"
                                    + "Cliquez sur le lien suivant pour choisir un nouveau mot de passe :\n"
                                    + lien + "\n\n"
                                    + "Ce lien expire dans 30 minutes et ne peut être utilisé qu'une seule fois.\n\n"
                                    + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email."
                    );

                    mailSender.send(message);
                });
    }

    public boolean verifierToken(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        return tokenRepository.findByTokenHash(hashToken(token))
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public boolean reinitialiserMotDePasse(
            String token,
            String nouveauMotDePasse) {

        if (token == null || token.isBlank()
                || nouveauMotDePasse == null
                || nouveauMotDePasse.length() < 8) {
            return false;
        }

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHash(hashToken(token))
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(null);

        if (resetToken == null) {
            return false;
        }

        Utilisateur utilisateur = resetToken.getUtilisateur();
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));

        resetToken.setUsed(true);

        return true;
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible de calculer le hash du jeton.", e
            );
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void nettoyerJetonsInutilisables() {

        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        tokenRepository.deleteByUsedTrue();
    }
}
