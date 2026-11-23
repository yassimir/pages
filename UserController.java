package com.gharnata.controller;



import com.gharnata.service.ServNotification;
import com.gharnata.service.ServProduit;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@Controller
@RequestMapping("/users")
public class UserController {


    private final ServNotification servNotification;
    private final ServProduit servProduit;

    public UserController(ServNotification servNotification, ServProduit servProduit) {
        this.servNotification = servNotification;
        this.servProduit = servProduit;
    }

    @GetMapping
    public String users(Model model){
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());
        model.addAttribute("breadcrumbs", List.of(
                "Administration",
                "Utilisateurs"
        ));
        return "gest/userManagement";
    }
}
