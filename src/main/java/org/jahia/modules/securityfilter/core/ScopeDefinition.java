package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.core.constraint.CompoundConstraint;
import org.jahia.modules.securityfilter.core.constraint.UserConstraint;
import org.jahia.modules.securityfilter.core.grant.CompoundGrant;
import org.jahia.modules.securityfilter.core.grant.Grant;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScopeDefinition {
    private static final Logger logger = LoggerFactory.getLogger(ScopeDefinition.class);

    private String scopeName;
    private String description;
    private Set<String> contexts;

    private UserConstraint userConstraint;

    private Set<Grant> grants = new HashSet<>();

    public ScopeDefinition(String key, PropertiesValues values) {
        scopeName = key;
        description = values.getProperty("description");
        contexts = ParserHelper.buildSet(values, "contexts");
        userConstraint = CompoundConstraint.build(values.getValues("user"));

        PropertiesList grantsValues = values.getList("grants");
        int size = grantsValues.getSize();
        for (int i = 0; i < size; i++) {
            PropertiesValues grantValues = grantsValues.getValues(i);
            grants.add(CompoundGrant.build(grantValues));
        }
    }

    public boolean isValidForUser() {
        return userConstraint == null || userConstraint.isValidForUser();
    }

    public String getScopeName() {
        return scopeName;
    }

    public String getDescription() {
        return description;
    }

    public Collection<String> getContexts() {
        return contexts;
    }

    public boolean isGrantAccess(Map<String, Object> query) {
        boolean match = grants.stream().anyMatch(grant -> grant.matches(query));
        if (match) {
            logger.debug("Access granted for {} by scope {}", query, scopeName);
        }
        return match;
    }

}
