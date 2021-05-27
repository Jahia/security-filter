package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.AuthorizationScopesService;
import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AuthorizationConfig implements ManagedServiceFactory {

    private Collection<ScopeDefinition> scopes = new ArrayList<>();

    private AuthorizationScopesService authorizationScopesService;

    public void setAuthorizationScopesService(AuthorizationScopesService authorizationScopesService) {
        this.authorizationScopesService = authorizationScopesService;
    }

    @Override
    public String getName() {
        return "API Security configuration (new)";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        PropertiesManager pm = new PropertiesManager(getMap(properties));

        PropertiesValues values = pm.getValues();
        Set<String> keys = values.getKeys();

        for (String key : keys) {
            PropertiesValues grantValues = values.getValues(key);
            ScopeDefinition definition = new ScopeDefinition(pid, key, grantValues);

            scopes.add(definition);

            for (String context : definition.getContexts()) {
                authorizationScopesService.addScopeInContext(context, definition.getScopeName());
            }
        }

    }

    @Override
    public void deleted(String pid) {
        List<ScopeDefinition> toRemove = scopes.stream().filter(s -> s.getPid().equals(pid)).collect(Collectors.toList());
        for (ScopeDefinition definition : toRemove) {
            for (String context : definition.getContexts()) {
                authorizationScopesService.removeScopeFromContext(context, definition.getScopeName());
            }
        }
        scopes.removeAll(toRemove);
    }

    public boolean hasPermission(Map<String, Object> query) {
        Collection<String> currentScopes = authorizationScopesService.getScopes();
        return scopes.stream()
                .filter(s -> currentScopes.contains(s.getScopeName()))
                .filter(ScopeDefinition::isValidForUser)
                .anyMatch(p -> p.isGrantAccess(query));
    }

    private Map<String, String> getMap(Dictionary<String, ?> d) {
        Map<String, String> m = new HashMap<>();
        Enumeration<String> en = d.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            if (!key.startsWith("felix.") && !key.startsWith("service.")) {
                m.put(key, d.get(key).toString());
            }
        }
        return m;
    }

}
