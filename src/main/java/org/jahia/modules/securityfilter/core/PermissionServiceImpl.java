package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.PermissionService;
import org.jahia.modules.securityfilter.ScopesHolder;
import org.jahia.modules.securityfilter.legacy.PermissionsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PermissionServiceImpl implements PermissionService {
    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);
    private ScopesHolder scopesContext;

    private AuthorizationConfig authorizationConfig;
    private PermissionsConfig legacyPermissionsConfig;

    @Override
    public boolean hasPermission(String apiToCheck) {
        if (apiToCheck == null) {
            throw new IllegalArgumentException("Must pass an api name");
        }

        return hasPermission(Collections.singletonMap("api", apiToCheck));
    }

    @Override
    public boolean hasPermission(String apiToCheck, Node node) {
        if (apiToCheck == null) {
            throw new IllegalArgumentException("Must pass an api name");
        }

        Map<String, Object> query = new HashMap();
        query.put("api", apiToCheck);
        query.put("node", node);

        return hasPermission(query);
    }

    @Override
    public boolean hasPermission(Map<String, Object> query) {
        if (query == null) {
            throw new IllegalArgumentException("Must pass a valid api query");
        }

        boolean hasPermission = false;
        //legacyPermissionsConfig.hasPermission(apiToCheck, (JCRNodeWrapper) node);

        // Also look into new authorization rules
        hasPermission = authorizationConfig.hasPermission(query);

        if (hasPermission) {
            logger.debug("Checking api permission '{}' for {}: GRANTED", query);
        } else {
            logger.debug("Checking api permission '{}' for {}: DENIED", query);
        }

        return hasPermission;
    }

    public void setScopesContext(ScopesHolder scopesContext) {
        this.scopesContext = scopesContext;
    }

    public void setLegacyPermissionsConfig(PermissionsConfig legacyPermissionsConfig) {
        this.legacyPermissionsConfig = legacyPermissionsConfig;
    }

    public void setAuthorizationConfig(AuthorizationConfig authorizationConfig) {
        this.authorizationConfig = authorizationConfig;
    }
}
