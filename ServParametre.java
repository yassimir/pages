package com.gharnata.service;

import com.gharnata.entity.Parametre;
import com.gharnata.repository.RepParametre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ServParametre {

    private static final String CLE_PRODUITS_PAR_PAGE = "PRODUITS_PAR_PAGE";
    private static final int VALEUR_PAR_DEFAUT = 10;

    private final RepParametre repParametre;

    public ServParametre(RepParametre parametreRepository) {
        this.repParametre = parametreRepository;
    }

    public BigDecimal getTauxTva() {
        return repParametre.findByCle("TVA_TAUX")
                .map(Parametre::getValeur)
                .map(BigDecimal::new)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Le taux de TVA n'est pas configuré"
                        ));
    }

    /**
     * Récupérer le nombre global de produits par page.
     * Crée le paramètre avec la valeur 20 s'il n'existe pas.
     */
    @Transactional
    public int getProduitsParPage() {

        Parametre parametre = repParametre
                .findByCle(CLE_PRODUITS_PAR_PAGE)
                .orElseGet(() -> {
                    Parametre nouveau = new Parametre();
                    nouveau.setCle(CLE_PRODUITS_PAR_PAGE);
                    nouveau.setValeur(String.valueOf(VALEUR_PAR_DEFAUT));
                    nouveau.setCategorie("PAGINATION");
                    nouveau.setDescription(
                            "Nombre de produits affichés par page"
                    );

                    return repParametre.save(nouveau);
                });

        try {
            int valeur = Integer.parseInt(parametre.getValeur());

            if (estValeurAutorisee(valeur)) {
                return valeur;
            }

        } catch (NumberFormatException ignored) {
            // Utiliser la valeur par défaut si la valeur en base est invalide.
        }

        return VALEUR_PAR_DEFAUT;
    }

    /**
     * Modifier le nombre global de produits par page.
     */
    @Transactional
    public void modifierProduitsParPage(int valeur) {

        if (!estValeurAutorisee(valeur)) {
            throw new IllegalArgumentException(
                    "La valeur doit être 20, 40 ou 80."
            );
        }

        Parametre parametre = repParametre
                .findByCle(CLE_PRODUITS_PAR_PAGE)
                .orElseGet(() -> {
                    Parametre nouveau = new Parametre();
                    nouveau.setCle(CLE_PRODUITS_PAR_PAGE);
                    nouveau.setCategorie("PAGINATION");
                    nouveau.setDescription(
                            "Nombre de produits affichés par page"
                    );
                    return nouveau;
                });

        parametre.setValeur(String.valueOf(valeur));
        repParametre.save(parametre);
    }

    private boolean estValeurAutorisee(int valeur) {
        return valeur == 10 || valeur == 15 || valeur == 20 || valeur == 25 || valeur == 30;
    }


    public List<Parametre> getAllParams() {
        return this.repParametre.findAll();
    }

    public Optional<Parametre> getById(Long id){
        return this.repParametre.findById(id);
    }

    public void save(Parametre parametre) {
        this.repParametre.save(parametre);

    }
}
