package org.jahia.modules.securityfilter.core.grant;

import org.jahia.services.modulemanager.util.PropertiesValues;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompoundGrant implements Grant {
    public Collection<Grant> grants;

    private static Collection<Function<PropertiesValues, Grant>> grantBuilders = Arrays.asList(
            ApiGrant::build,
            NodeGrant::build
    );

    public static Grant build(PropertiesValues grantValues) {
        Collection<Grant> grants = grantBuilders.stream()
                .map(builder -> builder.apply(grantValues))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new CompoundGrant(grants);
    }

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
