package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.core.apply.AutoApply;
import org.jahia.modules.securityfilter.core.constraint.Constraint;
import org.jahia.modules.securityfilter.core.grant.Grant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

public class ScopeDefinition {
    private static final Logger logger = LoggerFactory.getLogger(ScopeDefinition.class);

    private String pid;
    private String scopeName;
    private String description;
    private Collection<AutoApply> apply;
    private Collection<Constraint> constraints;
    private Collection<Grant> grants;

    public ScopeDefinition(String pid, String scopeName, String description, Collection<AutoApply> apply, Collection<Constraint> constraints, Collection<Grant> grants) {
        this.pid = pid;
        this.scopeName = scopeName;
        this.description = description;
        this.apply = apply;
        this.constraints = constraints;
        this.grants = grants;
    }

    public String getPid() {
        return pid;
    }

    public String getScopeName() {
        return scopeName;
    }

    public String getDescription() {
        return description;
    }

    public Collection<AutoApply> getApply() {
        return apply;
    }

    public Collection<Constraint> getConstraints() {
        return constraints;
    }

    public Collection<Grant> getGrants() {
        return grants;
    }

    public boolean shouldAutoApply(HttpServletRequest request) {
        return apply.stream().anyMatch(a -> a.shouldApply(request));
    }

    public boolean isValid(HttpServletRequest request) {
        return constraints.isEmpty() || constraints.stream().allMatch(c -> c.isValid(request));
    }

    public boolean isGrantAccess(Map<String, Object> query) {
        boolean match = grants.stream().anyMatch(grant -> grant.matches(query));
        if (match) {
            logger.debug("Access granted for {} by scope {}", query, scopeName);
        }
        return match;
    }

}
