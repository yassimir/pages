package com.gharnata.service;

import com.gharnata.entity.Client;
import com.gharnata.entity.dto.ClientDTO;
import com.gharnata.repository.RepClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServClient {

    private final RepClient repClient;

    public ServClient(RepClient repClient) {
        this.repClient = repClient;
    }

    public Long saveClient(Client client){
        Client c = repClient.findByIce(client.getIce());
        if (c != null) {
            if (c.isSuprimme()){
                c.setSuprimme(false);
                c.setAdresse(client.getAdresse());
                c.setTel(client.getTel());
                c.setType(client.getType());
                this.repClient.save(c);
                return 1L;
            }else return -1L;
        }
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
            return this.repClient.save(client);
        }
        return this.repClient.findByIce(client.getIce());
    }

    public List<ClientDTO> getClientDTOs(){
        return this.repClient.findAllClientDTO();
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

    public void archiveClient(long id) {
        Client c = this.repClient.findById(id).orElse(null);
        if (c != null) {
            c.setSuprimme(true);
            this.repClient.save(c);
        }
    }

    /*public Client updateClientCredit(Long id, double nouveauCredit) {
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
    }*/

    public List<ClientDTO> searchClients(String query) {
        return repClient.searchClients(query)
                .stream()
                .map(client -> new ClientDTO(
                        client.getId(),
                        client.getSte(),
                        client.getIce()
                ))
                .toList();
    }
}
