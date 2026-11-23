package com.gharnata.service;

import com.gharnata.entity.Utilisateur;
import com.gharnata.repository.UtilisateurRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public CustomUserDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur u = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        return User.builder()
                .username(u.getUsername())
                .password(u.getMotDePasse())
                .disabled(!u.isActif())
                .authorities(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
                .build();
    }
}
