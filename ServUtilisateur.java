
package com.gharnata.service;

import com.gharnata.entity.Utilisateur;
import com.gharnata.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServUtilisateur {

    private final UtilisateurRepository utilisateurRepository;

    public ServUtilisateur(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
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
                .filter(user -> user.getRole() != null
                        && user.getRole().name().equals("ADMIN"))
                .count();
    }
}
