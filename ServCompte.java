package com.gharnata.service;

import com.gharnata.entity.Compte;
import com.gharnata.repository.RepCompte;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServCompte {

    private final RepCompte repCompte;

    public ServCompte(RepCompte repCompte) {
        this.repCompte = repCompte;
    }

    public Compte trouverCompteActif(Long id) {
        return repCompte.findById(id)
                .filter(Compte::getActif)
                .orElseThrow(() ->
                        new EntityNotFoundException("Compte introuvable ou désactivé"));
    }

    public List<Compte> getComptesActives() {
        return repCompte.findByActifTrue();
    }
}