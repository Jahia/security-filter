package org.jahia.modules.securityfilter.core.constraint;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.modulemanager.util.PropertiesValues;
import pl.touk.throwing.exception.WrappedException;

import javax.jcr.RepositoryException;

/**
 * Check for a permission on a specific node
 */
public class PermissionConstraint implements UserConstraint {
    private String nodePath;
    private String workspace;
    private String permission;

    public PermissionConstraint(String nodePath, String workspace, String permission) {
        this.nodePath = nodePath;
        this.workspace = workspace;
        this.permission = permission;
    }

    public static UserConstraint build(PropertiesValues grantValues) {
        PropertiesValues nodeValues = grantValues.getValues("permission");
        if (nodeValues.getKeys().contains("name") && nodeValues.getKeys().contains("path")) {
            return new PermissionConstraint(nodeValues.getProperty("path"), nodeValues.getProperty("workspace"), nodeValues.getProperty("name"));
        }

        return null;
    }

    @Override
    public boolean isValidForUser() {
        try {
            JCRNodeWrapper node = JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(nodePath);
            return node.hasPermission(permission);
        } catch (WrappedException | RepositoryException e) {
            return false;
        }
    }
}
