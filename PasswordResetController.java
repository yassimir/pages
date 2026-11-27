
package com.gharnata.controller;

import com.gharnata.service.ServPasswordReset;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.gharnata.enums.ResultatReinitialisation;

@Controller
public class PasswordResetController {

    private final ServPasswordReset servPasswordReset;

    public PasswordResetController(ServPasswordReset servPasswordReset) {
        this.servPasswordReset = servPasswordReset;
    }

    // Afficher le formulaire de demande
    @GetMapping("/forgot-password")
    public String afficherFormulaireOubli() {
        return "auth/forgotPassword";
    }

    // Traiter la demande de réinitialisation
    @PostMapping("/forgot-password")
    public String demanderReinitialisation(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        long secondesRestantes =
                servPasswordReset.demanderReinitialisation(email);

        if (secondesRestantes > 0) {

            long minutes = secondesRestantes / 60;
            long secondes = secondesRestantes % 60;

            String delai;

            if (minutes > 0) {
                delai = minutes + " minute(s) et " + secondes + " seconde(s)";
            } else {
                delai = secondes + " seconde(s)";
            }

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Vous avez atteint le nombre maximum de demandes de réinitialisation. "
                            + "Veuillez réessayer dans " + delai + "."
            );

            redirectAttributes.addFlashAttribute("demandeBloquee", true);

        } else {

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Si cette adresse correspond à un compte, un lien de réinitialisation vient d'être envoyé."
            );

            redirectAttributes.addFlashAttribute("demandeBloquee", false);
        }

        return "redirect:/forgot-password";
    }

    // Afficher le formulaire de nouveau mot de passe
    @GetMapping("/reset-password")
    public String afficherFormulaireReinitialisation(
            @RequestParam(value = "token", required = false) String token,
            Model model) {

        boolean valide = servPasswordReset.verifierToken(token);

        model.addAttribute("tokenValide", valide);
        model.addAttribute("token", token);

        return "auth/resetPassword";
    }

    // Enregistrer le nouveau mot de passe
    @PostMapping("/reset-password")
    public String reinitialiserMotDePasse(
            @RequestParam("token") String token,
            @RequestParam("nouveauMotDePasse") String nouveauMotDePasse,
            @RequestParam("confirmationMotDePasse") String confirmationMotDePasse,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (nouveauMotDePasse == null
                || nouveauMotDePasse.length() < 8) {

            model.addAttribute("tokenValide",
                    servPasswordReset.verifierToken(token));
            model.addAttribute("token", token);
            model.addAttribute("erreur",
                    "Le mot de passe doit contenir au moins 8 caractères.");

            return "auth/resetPassword";
        }

        if (!nouveauMotDePasse.equals(confirmationMotDePasse)) {

            model.addAttribute("tokenValide",
                    servPasswordReset.verifierToken(token));
            model.addAttribute("token", token);
            model.addAttribute("erreur",
                    "Les deux mots de passe ne correspondent pas.");

            return "auth/resetPassword";
        }

        ResultatReinitialisation resultat =
                servPasswordReset.reinitialiserMotDePasse(
                        token,
                        nouveauMotDePasse
                );

        if (resultat == ResultatReinitialisation.COMPTE_DESACTIVE) {

            model.addAttribute("tokenValide", false);
            model.addAttribute("token", token);
            model.addAttribute("erreur",
                    "Votre compte est désactivé. Veuillez contacter l'administrateur.");

            return "auth/resetPassword";
        }

        if (resultat == ResultatReinitialisation.TOKEN_INVALIDE) {

            model.addAttribute("tokenValide", false);
            model.addAttribute("token", token);
            model.addAttribute("erreur",
                    "Ce lien est invalide, expiré ou déjà utilisé.");

            return "auth/resetPassword";
        }

        redirectAttributes.addFlashAttribute(
                "resetSuccess",
                "Votre mot de passe a été réinitialisé. Vous pouvez maintenant vous connecter."
        );

        return "redirect:/login";
    }
}
