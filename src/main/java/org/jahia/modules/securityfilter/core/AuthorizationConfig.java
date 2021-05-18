package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.ScopesHolder;
import org.jahia.modules.securityfilter.core.grants.Grant;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import javax.jcr.RepositoryException;
import java.util.*;

public class AuthorizationConfig implements ManagedServiceFactory {

    private Map<String, ScopeDefinition> scopes = new HashMap<>();
    private Map<String, ScopeDefinition> scopesByPid = new HashMap<>();

    private ScopesHolder scopesHolder = ScopesHolder.getInstance();

    @Override
    public String getName() {
        return "API Security configuration (new)";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        PropertiesManager pm = new PropertiesManager(getMap(properties));

        PropertiesValues values = pm.getValues();
        String scopeName = values.getProperty("scope");
        String scopeDescription = values.getProperty("description");
        ScopeDefinition definition = new ScopeDefinition(scopeName, scopeDescription);
        PropertiesList grantsValues = values.getList("grants");
        int size = grantsValues.getSize();
        for (int i = 0; i < size; i++) {
            PropertiesValues grantValues = grantsValues.getValues(i);
            definition.addGrant(Grant.build(grantValues));
        }

        scopes.put(scopeName, definition);
        scopesByPid.put(pid, definition);
    }

    @Override
    public void deleted(String pid) {
        ScopeDefinition definition = scopesByPid.remove(pid);
        scopes.remove(definition.getScopeName());
    }

    public boolean hasPermission(Map<String, Object> query) {
        return scopesHolder.getScopes().stream().map(scopes::get).filter(Objects::nonNull).anyMatch(p -> p.isGrantAccess(query));
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
