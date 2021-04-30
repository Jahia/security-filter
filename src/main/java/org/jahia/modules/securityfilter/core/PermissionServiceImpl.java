package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.PermissionService;
import org.jahia.modules.securityfilter.ScopesContext;
import org.jahia.modules.securityfilter.legacy.PermissionsConfig;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Collection;

public class PermissionServiceImpl implements PermissionService {
    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);
    private ScopesContext scopesContext;

    private PermissionsConfig legacyPermissionsConfig;

    @Override
    public boolean hasPermission(String apiToCheck) {
        if (apiToCheck == null) {
            throw new IllegalArgumentException("Must pass an api name");
        }

        Collection<String> scopes = scopesContext.getScopes();

        return false;
    }

    @Override
    public boolean hasPermission(String apiToCheck, Node node) throws RepositoryException {
        if (apiToCheck == null) {
            throw new IllegalArgumentException("Must pass an api name");
        }

        boolean hasPermission = legacyPermissionsConfig.hasPermission(apiToCheck, (JCRNodeWrapper) node);

        // Also look into new authorization rules

        String nodePath = node != null ? node.getPath() : "global";
        if (hasPermission) {
            logger.debug("Checking api permission '{}' for {}: GRANTED", apiToCheck, nodePath);
        } else {
            logger.debug("Checking api permission '{}' for {}: DENIED", apiToCheck, nodePath);
        }

        return hasPermission;
    }

    public void setScopesContext(ScopesContext scopesContext) {
        this.scopesContext = scopesContext;
    }

    public void setLegacyPermissionsConfig(PermissionsConfig legacyPermissionsConfig) {
        this.legacyPermissionsConfig = legacyPermissionsConfig;
    }


}
