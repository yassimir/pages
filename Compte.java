package com.gharnata.entity;

import com.gharnata.enums.TypeCompte;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compte")
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private TypeCompte type;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    public Compte() {
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public TypeCompte getType() {
        return type;
    }

    public void setType(TypeCompte type) {
        this.type = type;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}