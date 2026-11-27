package com.gharnata.config;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomSessionRegistry extends SessionRegistryImpl {

    @Override
    public List<Object> getAllPrincipals() {
        // Implémentez la logique personnalisée si nécessaire
        return super.getAllPrincipals();
    }

    @Override
    public List<SessionInformation> getAllSessions(Object principal, boolean includeExpiredSessions) {
        // Implémentez la logique personnalisée si nécessaire
        return super.getAllSessions(principal, includeExpiredSessions);
    }

    /**
     * Invalide toutes les sessions d'un utilisateur.
     */
    public void invaliderSessionsUtilisateur(String username) {

        if (username == null || username.isBlank()) {
            return;
        }

        List<Object> principals = getAllPrincipals();

        for (Object principal : principals) {

            if (principal instanceof UserDetails userDetails
                    && username.equals(userDetails.getUsername())) {

                List<SessionInformation> sessions =
                        getAllSessions(principal, false);

                for (SessionInformation session : sessions) {
                    session.expireNow();
                }
            }
        }
    }
}
