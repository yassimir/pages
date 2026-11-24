package com.gharnata.controller;



import com.gharnata.enums.Role;
import com.gharnata.service.ServNotification;
import com.gharnata.service.ServProduit;
import com.gharnata.service.ServUtilisateur;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
@RequestMapping("/users")
public class UserController {


    private final ServNotification servNotification;
    private final ServProduit servProduit;
    private final ServUtilisateur servUtilisateur;

    public UserController(ServNotification servNotification, ServProduit servProduit,
                          ServUtilisateur servUtilisateur) {
        this.servNotification = servNotification;
        this.servProduit = servProduit;
        this.servUtilisateur = servUtilisateur;
    }

    @GetMapping
    public String users(Model model){
        model.addAttribute("notif",servNotification.nbNotif());
        model.addAttribute("cats", this.servProduit.getAllCats());

        model.addAttribute("roles", Role.values());
        model.addAttribute("users", servUtilisateur.getAllUsers());
        model.addAttribute("activeUsers", servUtilisateur.countActiveUsers());
        model.addAttribute("adminUsers", servUtilisateur.countAdminUsers());
        model.addAttribute("breadcrumbs", List.of(
                "Administration",
                "Utilisateurs"
        ));
        return "gest/userManagement";
    }

    @PostMapping("/add")
    public String ajouterUtilisateur(
            @RequestParam String username,
            @RequestParam(required = false) String email,
            @RequestParam String password,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        try {
            Role roleUtilisateur = Role.valueOf(role);

            servUtilisateur.ajouterUtilisateur(
                    username,
                    email,
                    password,
                    roleUtilisateur
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Utilisateur ajouté avec succès."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/users";
    }

    @PostMapping("/edit")
    public String modifierUtilisateur(
            @RequestParam Long id,
            @RequestParam String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String password,
            @RequestParam String role,
            @RequestParam boolean actif,
            RedirectAttributes redirectAttributes) {

        try {
            Role roleUtilisateur = Role.valueOf(role);

            servUtilisateur.modifierUtilisateur(
                    id,
                    username,
                    email,
                    password,
                    roleUtilisateur,
                    actif
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Utilisateur modifié avec succès."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/users";
    }
}
