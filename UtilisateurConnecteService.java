package com.gharnata.service;

import com.gharnata.entity.Utilisateur;
import com.gharnata.repository.UtilisateurRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurConnecteService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurConnecteService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public Utilisateur getUtilisateurConnecte() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Utilisateur non authentifié");
        }

        return utilisateurRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new EntityNotFoundException("Utilisateur introuvable"));
    }
}
