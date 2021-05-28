package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.core.apply.AutoApply;
import org.jahia.modules.securityfilter.core.apply.AutoApplyByOrigin;
import org.jahia.modules.securityfilter.core.constraint.Constraint;
import org.jahia.modules.securityfilter.core.constraint.PermissionConstraint;
import org.jahia.modules.securityfilter.core.constraint.PrivilegedConstraint;
import org.jahia.modules.securityfilter.core.grant.ApiGrant;
import org.jahia.modules.securityfilter.core.grant.Grant;
import org.jahia.modules.securityfilter.core.grant.NodeGrant;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuthorizationConfig implements ManagedServiceFactory {

    private final Collection<Function<PropertiesValues, AutoApply>> applyBuilders;
    private final Collection<Function<PropertiesValues, Constraint>> constraintBuilders;
    private final Collection<Function<PropertiesValues, Grant>> grantBuilders;
    private Collection<ScopeDefinition> scopes = new ArrayList<>();

    public AuthorizationConfig() {
        // Should be configurable/extendable
        applyBuilders = Arrays.asList(AutoApplyByOrigin::build);
        constraintBuilders = Arrays.asList(PermissionConstraint::build, PrivilegedConstraint::build);
        grantBuilders = Arrays.asList(ApiGrant::build, NodeGrant::build);
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
            PropertiesValues scopeValues = values.getValues(key);
            String description = scopeValues.getProperty("description");
            Collection<Constraint> constraints = getList(scopeValues.getList("constraints"), constraintBuilders);
            Collection<AutoApply> apply = getList(scopeValues.getList("auto_apply"), applyBuilders);
            Collection<Grant> grants = getList(scopeValues.getList("grants"), Collections.singleton(this::buildCompoundGrant));
            ScopeDefinition definition = new ScopeDefinition(pid, key, description, apply, constraints, grants);
            scopes.add(definition);
        }
    }

    private <T> Collection<T> getList(PropertiesList list, Collection<Function<PropertiesValues, T>> builders) {
        Collection<T> s = new ArrayList<>();
        int size = list.getSize();
        for (int i = 0; i < size; i++) {
            PropertiesValues values = list.getValues(i);
            builders.stream()
                    .map(builder -> builder.apply(values))
                    .filter(Objects::nonNull).findFirst().ifPresent(s::add);
        }
        return s;
    }

    @Override
    public void deleted(String pid) {
        scopes.removeAll(scopes.stream().filter(s -> s.getPid().equals(pid)).collect(Collectors.toList()));
    }

    public Collection<ScopeDefinition> getScopes() {
        return scopes;
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

    private CompoundGrant buildCompoundGrant(PropertiesValues grantValues) {
        Collection<Grant> grants = grantBuilders.stream()
                .map(builder -> builder.apply(grantValues))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new CompoundGrant(grants);
    }

    private static class CompoundGrant implements Grant {
        public Collection<Grant> grants;

        public CompoundGrant(Collection<Grant> grants) {
            this.grants = grants;
        }

        public void setGrants(Set<Grant> grants) {
            this.grants = grants;
        }

        @Override
        public boolean matches(Map<String, Object> query) {
            return !grants.isEmpty() && grants.stream().allMatch(g -> g.matches(query));
        }
    }

}
