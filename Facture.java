package com.gharnata.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "facture",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_facture_numero",
                columnNames = "numero"
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "date")
    private String date;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "client_ste")
    private String destinataireNom;

    @Column(name = "client_ice")
    private String destinataireIce;

    @Column(name = "client_adresse")
    private String destinataireAdresse;

    @Column(name = "montant_ht", precision = 12, scale = 2)
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 12, scale = 2, nullable = false)
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 12, scale = 2)
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Column(name = "mtl")
    private String mtl;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paiement_id", nullable = false, unique = true)
    private Paiement paiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id")
    private Client destinataire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getDate() {
        return date;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getDestinataireNom() {
        return destinataireNom;
    }

    public void setDestinataireNom(String destinataireNom) {
        this.destinataireNom = destinataireNom;
    }

    public String getDestinataireIce() {
        return destinataireIce;
    }

    public void setDestinataireIce(String destinataireIce) {
        this.destinataireIce = destinataireIce;
    }

    public String getDestinataireAdresse() {
        return destinataireAdresse;
    }

    public void setDestinataireAdresse(String destinataireAdresse) {
        this.destinataireAdresse = destinataireAdresse;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getMtl() {
        return mtl;
    }

    public void setMtl(String mtl) {
        this.mtl = mtl;
    }

    public Paiement getPaiement() {
        return paiement;
    }

    public void setPaiement(Paiement paiement) {
        this.paiement = paiement;
    }

    public Client getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(Client destinataire) {
        this.destinataire = destinataire;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }
}
