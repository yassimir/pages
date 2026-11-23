package com.gharnata.config;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
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
}
