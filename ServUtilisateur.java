package com.gharnata.service;

import com.gharnata.entity.Utilisateur;
import com.gharnata.enums.Role;
import com.gharnata.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServUtilisateur {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public ServUtilisateur(UtilisateurRepository utilisateurRepository,
                           PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retourne tous les utilisateurs.
     */
    public List<Utilisateur> getAllUsers() {
        return utilisateurRepository.findAll();
    }

    /**
     * Retourne le nombre d'utilisateurs actifs.
     */
    public long countActiveUsers() {
        return utilisateurRepository.findAll()
                .stream()
                .filter(Utilisateur::isActif)
                .count();
    }

    /**
     * Retourne le nombre d'administrateurs.
     */
    public long countAdminUsers() {
        return utilisateurRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .count();
    }

    /**
     * Ajoute un nouvel utilisateur.
     */
    @Transactional
    public void ajouterUtilisateur(String username,
                                   String email,
                                   String motDePasse,
                                   Role role) {

        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim();
        motDePasse = motDePasse == null ? "" : motDePasse;

        if (username.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom d'utilisateur est obligatoire."
            );
        }

        if (motDePasse.isBlank()) {
            throw new IllegalArgumentException(
                    "Le mot de passe est obligatoire."
            );
        }

        if (motDePasse.length() < 6) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins 6 caractères."
            );
        }

        if (role == null) {
            throw new IllegalArgumentException(
                    "Le rôle est obligatoire."
            );
        }

        if (utilisateurRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Ce nom d'utilisateur existe déjà."
            );
        }

        if (!email.isBlank()
                && utilisateurRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Cette adresse e-mail existe déjà."
            );
        }

        Utilisateur utilisateur = new Utilisateur();

        utilisateur.setUsername(username);
        utilisateur.setEmail(email.isBlank() ? null : email);
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateur.setRole(role);
        utilisateur.setActif(true);

        utilisateurRepository.save(utilisateur);
    }
}