package org.jahia.modules.securityfilter.core.grants;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CompoundGrant implements Grant {
    public Collection<Grant> grants;

    public CompoundGrant(Collection<Grant> grants) {
        this.grants = grants;
    }

    public void setGrants(Set<Grant> grants) {
        this.grants = grants;
    }

    @Override
    public boolean matches(Map<String, Object> query) {
        return grants.stream().allMatch(g -> g.matches(query));
    }
}
