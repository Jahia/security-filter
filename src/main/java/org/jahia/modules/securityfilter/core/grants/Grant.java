package org.jahia.modules.securityfilter.core.grants;

import org.jahia.services.modulemanager.util.PropertiesValues;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Grant {

    Collection<Function<PropertiesValues, Grant>> grantBuilders = Arrays.asList(
            ApiGrant::build,
            NodeGrant::build
    );

    static Grant build(PropertiesValues grantValues) {
        Collection<Grant> grants = grantBuilders.stream()
                .map(builder -> builder.apply(grantValues))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return grants.size() == 1 ? grants.iterator().next() : new CompoundGrant(grants);
    }

    boolean matches(Map<String, Object> query);
}
