package com.gharnata.entity;

import com.gharnata.enums.StatutPaiement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commande")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "date")
    private String date;

    @Column(name = "tva", precision = 5, scale = 4, nullable = false)
    private BigDecimal tva;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false)
    private StatutPaiement statutPaiement = StatutPaiement.NON_PAYEE;

    @Column(name = "montant_ht", precision = 12, scale = 2, nullable = false)
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 12, scale = 2, nullable = false)
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 12, scale = 2, nullable = false)
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Column(name = "confirme",columnDefinition = "boolean default false")
    private boolean confirme;

    @Column(name = "date_confirmation")
    private String dateConfirmation;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @OneToMany(mappedBy = "commande")
    private List<Paiement> paiements = new ArrayList<>();

    @OneToMany(mappedBy = "commande")
    private List<AffectationPaiement> affectations = new ArrayList<>();

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    private List<LigneCommande> lignes;

    @OneToOne(mappedBy = "commande", fetch = FetchType.LAZY)
    private Bon bon;

    /*@OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    private List<Facture> factures;*/
    /*@OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    private List<Paiement> paiements;*/
    /*@Column(name = "paye", columnDefinition = "boolean default false")
    private boolean paye;*/

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }



    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getTva() {
        return tva;
    }

    public void setTva(BigDecimal tva) {
        this.tva = tva;
    }

    public BigDecimal getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getMontantTVA() {
        return montantTVA;
    }

    public void setMontantTVA(BigDecimal montantTVA) {
        this.montantTVA = montantTVA;
    }

    public BigDecimal getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public boolean isConfirme() {
        return confirme;
    }

    public void setConfirme(boolean confirme) {
        this.confirme = confirme;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getDateConfirmation() {
        return dateConfirmation;
    }

    public void setDateConfirmation(String dateConfirmation) {
        this.dateConfirmation = dateConfirmation;
    }

    public List<Paiement> getPaiements() {
        return paiements;
    }

    public void setPaiements(List<Paiement> paiements) {
        this.paiements = paiements;
    }

    public List<AffectationPaiement> getAffectations() {
        return affectations;
    }

    public void setAffectations(List<AffectationPaiement> affectations) {
        this.affectations = affectations;
    }

    public StatutPaiement getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(StatutPaiement statutPaiement) {
        this.statutPaiement = statutPaiement;
    }

    public List<LigneCommande> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneCommande> lignes) {
        this.lignes = lignes;
    }

    public Bon getBon() {
        return bon;
    }

    public void setBon(Bon bon) {
        this.bon = bon;
    }
}
