package org.jahia.modules.securityfilter.core.constraint;

import org.jahia.services.modulemanager.util.PropertiesValues;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompoundConstraint implements UserConstraint {
    public Collection<UserConstraint> constraints;

    private static Collection<Function<PropertiesValues, UserConstraint>> constraintBuilders = Arrays.asList(
            PermissionConstraint::build,
            PrivilegedConstraint::build
    );

    public static UserConstraint build(PropertiesValues grantValues) {
        Collection<UserConstraint> constraints = constraintBuilders.stream()
                .map(builder -> builder.apply(grantValues))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new CompoundConstraint(constraints);
    }

    public CompoundConstraint(Collection<UserConstraint> grants) {
        this.constraints = grants;
    }

    public void setConstraints(Set<UserConstraint> constraints) {
        this.constraints = constraints;
    }

    @Override
    public boolean isValidForUser() {
        return constraints.isEmpty() || constraints.stream().allMatch(UserConstraint::isValidForUser);
    }
}
