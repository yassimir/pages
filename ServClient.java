package com.gharnata.service;

import com.gharnata.entity.Client;
import com.gharnata.entity.Compte;
import com.gharnata.repository.RepClient;
import com.gharnata.repository.RepCompte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServClient {
    @Autowired
    private RepClient repClient;
    @Autowired
    private RepCompte repCompte;

    public Long saveClient(Client client){
        Client c = repClient.findByIce(client.getIce());
        if (c != null) {
            if (c.isSuprimme()){
                Compte cmp = this.repCompte.findById(c.getCompte().getId()).orElse(null);
                if (cmp != null) {
                    cmp.setCredit(0);
                }else{
                    cmp = new Compte();
                    cmp.setCredit(0);
                }
                this.repCompte.save(cmp);
                c.setSuprimme(false);
                c.setAdresse(client.getAdresse());
                c.setTel(client.getTel());
                c.setType(client.getType());
                this.repClient.save(c);
                return 1L;
            }else return -1L;
        }
        Compte cp = new Compte();
        cp.setCredit(0);
        client.setCompte(this.repCompte.save(cp));
        this.repClient.save(client);
        return 1L;
    }

    public Long modifyClient(Client client){
        Client c = repClient.findById(client.getId()).orElse(null);
        if(c != null){
            c.setSte(client.getSte());
            c.setIce(client.getIce());
            c.setAdresse(client.getAdresse());
            c.setTel(client.getTel());
            c.setType(client.getType());
            this.repClient.save(c);
            return 1L;
        }
        return 0L;
    }

    public Client checkClientExis(Client client) {
        if (this.repClient.findByIce(client.getIce()) ==null){
            Compte compte = new Compte();
            client.setCompte(compte);
            this.repCompte.save(compte);
            return this.repClient.save(client);
        }
        return this.repClient.findByIce(client.getIce());
    }

    public List<Client> getAllClients() {
        return this.repClient.findAll();
    }

    public Client getClientById(Long id) {
        return this.repClient.findById(id).orElse(null);
    }

    public Client getClientByIce(String ice) {
        return this.repClient.findByIce(ice);
    }
    public Compte getCmptById(long id){
        return this.repCompte.findById(id).orElse(null);
    }

    public void saveCmpt(Compte c) {
        this.repCompte.save(c);
    }

    public void archiveClient(long id) {
        Client c = this.repClient.findById(id).orElse(null);
        if (c != null) {
            c.setSuprimme(true);
            this.repClient.save(c);
        }
    }

    public Client updateClientCredit(Long id, double nouveauCredit) {
        Client cl = this.repClient.findById(id).orElse(null);
        if (cl != null) {
            Compte cmp = this.repCompte.findById(cl.getCompte().getId()).orElse(null);
            if (cmp != null) {
                cmp.setCredit(cmp.getCredit() + nouveauCredit);
                this.repCompte.save(cmp);
                return cl;
            }
            return cl;
        }
        return null;
    }
}
