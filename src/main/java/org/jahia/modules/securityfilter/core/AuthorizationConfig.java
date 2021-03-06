package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.PermissionService;
import org.jahia.modules.securityfilter.core.apply.AlwaysAutoApply;
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
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuthorizationConfig implements ManagedServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationConfig.class);

    private final Collection<Function<PropertiesValues, AutoApply>> applyBuilders;
    private final Collection<Function<PropertiesValues, Constraint>> constraintBuilders;
    private final Collection<Function<PropertiesValues, Grant>> grantBuilders;
    private Collection<ScopeDefinitionImpl> scopeDefinitions = new ArrayList<>();
    private Collection<ScopeDefinitionImpl> aggregatedScopes = new ArrayList<>();

    public AuthorizationConfig() {
        // Should be configurable/extendable
        applyBuilders = Arrays.asList(AutoApplyByOrigin::build, AlwaysAutoApply::build);
        constraintBuilders = Arrays.asList(PermissionConstraint::build, PrivilegedConstraint::build);
        grantBuilders = Arrays.asList(ApiGrant::build, NodeGrant::build);
    }

    @Override
    public String getName() {
        return "API Security configuration (new)";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) {
        scopeDefinitions.removeAll(scopeDefinitions.stream().filter(s -> s.getPid().equals(pid)).collect(Collectors.toList()));

        if (properties != null) {
            PropertiesManager pm = new PropertiesManager(ConfigUtil.getMap(properties));
            PropertiesValues values = pm.getValues();
            Set<String> keys = values.getKeys();

            for (String key : keys) {
                PropertiesValues scopeValues = values.getValues(key);
                String description = scopeValues.getProperty("description");
                Collection<Constraint> constraints = getList(scopeValues.getList("constraints"), constraintBuilders);
                Collection<AutoApply> apply = getList(scopeValues.getList("auto_apply"), applyBuilders);
                Collection<Grant> grants = getList(scopeValues.getList("grants"), Collections.singleton(this::buildCompoundGrant));
                PropertiesValues metadataValues = scopeValues.getValues("metadata");
                Map<String, String> metadata = metadataValues.getKeys().stream().collect(Collectors.toMap(s -> s, metadataValues::getProperty));
                ScopeDefinitionImpl definition = new ScopeDefinitionImpl(pid, key, description, apply, constraints, grants, metadata);
                scopeDefinitions.add(definition);
            }
        }

        aggregateScopes();
    }

    private void aggregateScopes() {
        aggregatedScopes = scopeDefinitions.stream().collect(Collectors.groupingBy(ScopeDefinitionImpl::getScopeName)).entrySet().stream().map(entry ->
            new ScopeDefinitionImpl(null, entry.getKey(),
                    entry.getValue().stream().map(ScopeDefinitionImpl::getDescription).filter(Objects::nonNull).findFirst().orElse(null),
                    entry.getValue().stream().flatMap(s -> s.getApply().stream()).collect(Collectors.toList()),
                    entry.getValue().stream().flatMap(s -> s.getConstraints().stream()).collect(Collectors.toList()),
                    entry.getValue().stream().flatMap(s -> s.getGrants().stream()).collect(Collectors.toList()),
                    entry.getValue().stream().flatMap(s -> s.getMetadata().entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1,v2) -> {
                        if (!v1.equals(v2)) {
                            logger.warn("Different metadata values, using {}, ignoring {}", v1, v2);
                        }
                        return v1;
                    }))
            )
        ).collect(Collectors.toList());
    }

    public Set<String> getAllOrigins() {
        return scopeDefinitions.stream()
                .flatMap(scope -> scope.getApply().stream()
                        .filter(AutoApplyByOrigin.class::isInstance)
                        .map(AutoApplyByOrigin.class::cast)
                        .map(AutoApplyByOrigin::getOrigin))
                .collect(Collectors.toSet());
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
        updated(pid, null);
    }

    public Collection<ScopeDefinitionImpl> getScopes() {
        return aggregatedScopes;
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
