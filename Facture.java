package com.gharnata.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "montant_ht", columnDefinition = "double default 0.0")
    private double montantHT;

    @Column(name = "montant_ttc", columnDefinition = "double default 0.0")
    private double montantTTC;

    @Column(name = "mtva", columnDefinition = "double default 0.0")
    private double mTVA;

    @Column(name = "mtl")
    private String mtl;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paiement_id", nullable = false, unique = true)
    private Paiement paiement;


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

    public void setDate(String date) {
        this.date = date;
    }

    public double getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(double montantHT) {
        this.montantHT = montantHT;
    }

    public double getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(double montantTTC) {
        this.montantTTC = montantTTC;
    }

    public String getMtl() {
        return mtl;
    }

    public void setMtl(String mtl) {
        this.mtl = mtl;
    }

    public double getmTVA() {
        return mTVA;
    }

    public void setmTVA(double mTVA) {
        this.mTVA = mTVA;
    }

    public Paiement getPaiement() {
        return paiement;
    }

    public void setPaiement(Paiement paiement) {
        this.paiement = paiement;
    }
}
