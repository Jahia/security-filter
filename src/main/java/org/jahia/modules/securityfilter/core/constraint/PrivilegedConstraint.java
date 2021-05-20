package org.jahia.modules.securityfilter.core.constraint;

import org.jahia.services.modulemanager.util.PropertiesValues;

/**
 * Check for a permission on a specific node
 */
public class PrivilegedConstraint extends PermissionConstraint {

    public PrivilegedConstraint() {
        super("/sites", null, "jcr:read_default");
    }

    public static UserConstraint build(PropertiesValues grantValues) {
        Boolean v = grantValues.getBooleanProperty("privileged");
        if (v != null && v) {
            return new PrivilegedConstraint();
        }

        return null;
    }
}
