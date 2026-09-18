package com.gharnata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client")
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "ste")
    private String ste;
    @Column(name = "ice")
    private String ice;
    @Column(name = "adresse")
    private String adresse = "";
    @Column(name = "tel")
    private String tel = "";
    @Column(name = "type")
    private String type;
    @Column(name = "suprimme",columnDefinition = "boolean default false")
    private boolean suprimme;
    @OneToOne
    @JoinColumn(name = "id_compte")
    private Compte compte;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSte() {
        return ste;
    }

    public void setSte(String ste) {
        this.ste = ste;
    }

    public String getIce() {
        return ice;
    }

    public void setIce(String ice) {
        this.ice = ice;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSuprimme() {
        return suprimme;
    }

    public void setSuprimme(boolean suprimme) {
        this.suprimme = suprimme;
    }

    public Compte getCompte() {
        return compte;
    }

    public void setCompte(Compte compte) {
        this.compte = compte;
    }
}
