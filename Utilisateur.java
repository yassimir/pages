package com.gharnata.entity;

import com.gharnata.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String motDePasse; // stocké encodé (BCrypt), jamais en clair

    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean actif = true;

    @OneToMany(mappedBy = "utilisateur", fetch = FetchType.LAZY)
    private List<Paiement> paiements = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur", fetch = FetchType.LAZY)
    private List<Facture> factures = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur", fetch = FetchType.LAZY)
    private List<Commande> commands = new ArrayList<>();

    @OneToOne(
            mappedBy = "utilisateur",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Panier panier;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public List<Paiement> getPaiements() {
        return paiements;
    }

    public void setPaiements(List<Paiement> paiements) {
        this.paiements = paiements;
    }

    public Panier getPanier() {
        return panier;
    }

    public void setPanier(Panier panier) {
        this.panier = panier;
    }


    public List<Commande> getCommands() {
        return commands;
    }

    public void setCommands(List<Commande> commands) {
        this.commands = commands;
    }

    public List<Facture> getFactures() {
        return factures;
    }

    public void setFactures(List<Facture> factures) {
        this.factures = factures;
    }
}
