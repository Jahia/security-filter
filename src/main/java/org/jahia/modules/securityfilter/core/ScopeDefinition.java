package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.core.grants.Grant;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScopeDefinition {
    private String scopeName;
    private String description;

    private Set<Grant> grants = new HashSet<>();

    public ScopeDefinition(String scopeName, String description) {
        this.scopeName = scopeName;
        this.description = description;
    }

    public String getScopeName() {
        return scopeName;
    }

    public String getDescription() {
        return description;
    }

    public void addGrant(Grant grant) {
        grants.add(grant);
    }

    public boolean isGrantAccess(Map<String, Object> query) {
        return grants.stream().anyMatch(grant -> grant.matches(query));
    }

}
