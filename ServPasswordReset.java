
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
import java.time.Duration;


import java.util.Locale;
import com.gharnata.entity.PasswordResetAttempt;
import com.gharnata.repository.RepPasswordResetAttempt;

@Service
public class ServPasswordReset {

    private final UtilisateurRepository utilisateurRepository;
    private final RepPasswordResetToken tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final RepPasswordResetAttempt attemptRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    /*private final Map<String, List<LocalDateTime>> demandesParEmail =
            new HashMap<>();*/

    @Value("${app.base-url}")
    private String baseUrl;

    public ServPasswordReset(
            UtilisateurRepository utilisateurRepository,
            RepPasswordResetToken tokenRepository,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            RepPasswordResetAttempt attemptRepository) {

        this.utilisateurRepository = utilisateurRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.attemptRepository = attemptRepository;
    }



    /*@Transactional
    public void demanderReinitialisation(String email) {

        if (email == null || email.isBlank()) {
            return;
        }

        String emailNormalise = email.trim().toLowerCase(Locale.ROOT);

        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.minusMinutes(15);

        // Vérifier le nombre de demandes durant les 15 dernières minutes
        long nombreDemandes =
                attemptRepository.countByEmailAndRequestedAtAfter(
                        emailNormalise,
                        limite
                );

        // Maximum 3 demandes en 15 minutes
        if (nombreDemandes >= 3) {
            return;
        }

        // Enregistrer la demande dans la base de données
        PasswordResetAttempt attempt = new PasswordResetAttempt();
        attempt.setEmail(emailNormalise);
        attempt.setRequestedAt(maintenant);

        attemptRepository.save(attempt);

        // Rechercher le compte correspondant à l'adresse email
        utilisateurRepository.findByEmail(emailNormalise)
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
    }*/

    @Transactional
    public long demanderReinitialisation(String email) {

        if (email == null || email.isBlank()) {
            return 0;
        }

        String emailNormalise = email.trim().toLowerCase(Locale.ROOT);

        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.minusMinutes(15);

        // Compter les demandes durant les 15 dernières minutes
        long nombreDemandes =
                attemptRepository.countByEmailAndRequestedAtAfter(
                        emailNormalise,
                        limite
                );

        // Maximum 3 demandes en 15 minutes
        if (nombreDemandes >= 3) {

            PasswordResetAttempt premiereDemande =
                    attemptRepository
                            .findFirstByEmailAndRequestedAtAfterOrderByRequestedAtAsc(
                                    emailNormalise,
                                    limite
                            )
                            .orElse(null);

            if (premiereDemande == null) {
                return 0;
            }

            LocalDateTime prochaineTentative =
                    premiereDemande.getRequestedAt().plusMinutes(15);

            long millisecondesRestantes =
                    Duration.between(maintenant, prochaineTentative)
                            .toMillis();

            return Math.max(1, (millisecondesRestantes + 999) / 1000);
        }

        // Enregistrer la demande dans la base de données
        PasswordResetAttempt attempt = new PasswordResetAttempt();
        attempt.setEmail(emailNormalise);
        attempt.setRequestedAt(maintenant);

        attemptRepository.save(attempt);

        // Rechercher le compte correspondant à l'adresse email
        utilisateurRepository.findByEmail(emailNormalise)
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

        return 0;
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
                .findByTokenHashForUpdate(hashToken(token))
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(null);

        if (resetToken == null) {
            return false;
        }

        Utilisateur utilisateur = resetToken.getUtilisateur();
        if (!utilisateur.isActif()) {
            return false;
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));

        resetToken.setUsed(true);

        return true;
    }

    /*private synchronized boolean autoriserDemande(String email) {

        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.minusMinutes(15);

        // Supprimer les anciennes demandes
        Iterator<Map.Entry<String, List<LocalDateTime>>> iterator =
                demandesParEmail.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, List<LocalDateTime>> entry =
                    iterator.next();

            entry.getValue().removeIf(date -> !date.isAfter(limite));

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }

        // Récupérer les demandes de cette adresse
        List<LocalDateTime> demandes =
                demandesParEmail.computeIfAbsent(
                        email,
                        k -> new ArrayList<>()
                );

        // Maximum 3 demandes en 15 minutes
        if (demandes.size() >= 3) {
            return false;
        }

        demandes.add(maintenant);

        return true;
    }*/

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

        attemptRepository.deleteByRequestedAtBefore(
                LocalDateTime.now().minusDays(30)
        );
    }
}
