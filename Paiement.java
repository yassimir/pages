package com.gharnata.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "montant")
    private BigDecimal montant;

    @Column(name = "date_pay")
    private String datePay;

    @Column(name = "description")
    private String description;

    @Column(name = "mod_pai")
    private String modPai;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "confirmed", nullable = false)
    @ColumnDefault("false")
    private boolean confirmed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "justificatif_id", unique = true)
    private Image justificatif;

    @OneToOne(mappedBy = "paiement", fetch = FetchType.LAZY)
    private Facture facture;

    @OneToMany(mappedBy = "paiement",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<AffectationPaiement> affectations = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id")
    private Compte compte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "actif", nullable = false)
    @ColumnDefault("true")
    private boolean actif = true;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }



    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getDatePay() {
        return datePay;
    }

    public void setDatePay(String datePay) {
        this.datePay = datePay;
    }

    public String getModPai() {
        return modPai;
    }

    public void setModPai(String modPai) {
        this.modPai = modPai;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Facture getFacture() {
        return facture;
    }

    public void setFacture(Facture facture) {
        this.facture = facture;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<AffectationPaiement> getAffectations() {
        return affectations;
    }

    public void setAffectations(List<AffectationPaiement> affectations) {
        this.affectations = affectations;
    }

    public Compte getCompte() {
        return compte;
    }

    public void setCompte(Compte compte) {
        this.compte = compte;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Image getJustificatif() {
        return justificatif;
    }

    public void setJustificatif(Image justificatif) {
        this.justificatif = justificatif;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }
}
