package org.jahia.modules.securityfilter.core;

import org.jahia.modules.securityfilter.PermissionService;
import org.jahia.modules.securityfilter.legacy.PermissionsConfig;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

public class PermissionServiceImpl implements PermissionService {
    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    private AuthorizationConfig authorizationConfig;
    private PermissionsConfig permissionsConfig;

    private Map<String, Collection<String>> contexts = new HashMap<>();
    private ThreadLocal<Set<ScopeDefinition>> currentScopesLocal = ThreadLocal.withInitial(HashSet::new);

    public Collection<ScopeDefinition> getCurrentScopes() {
        return Collections.unmodifiableSet(currentScopesLocal.get());
    }

    public void addScopes(Collection<String> scopes, HttpServletRequest request) {
        currentScopesLocal.get().addAll(authorizationConfig.getScopes().stream()
                .filter(scope -> scopes.contains(scope.getScopeName()))
                .filter(scope -> scope.isValid(request))
                .collect(Collectors.toSet()));
    }

    public void initScopes(HttpServletRequest request) {
        Set<String> scopeNames = authorizationConfig.getScopes().stream()
                .filter(scope -> scope.shouldAutoApply(request))
                .filter(scope -> scope.isValid(request))
                .map(ScopeDefinition::getScopeName)
                .collect(Collectors.toSet());
        logger.debug("Auto apply following scopes : {}", scopeNames);
        addScopes(scopeNames, request);
    }

    public void resetScopes() {
        currentScopesLocal.remove();
    }

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

        Collection<ScopeDefinition> currentScopes = getCurrentScopes();

        boolean hasPermission = authorizationConfig.getScopes().stream()
                    .filter(currentScopes::contains)
                    .anyMatch(p -> p.isGrantAccess(query));

        try {
            // Legacy permissions check
            hasPermission = hasPermission || permissionsConfig.hasPermission((String) query.get("api"), (JCRNodeWrapper) query.get("node"));
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        if (hasPermission) {
            logger.debug("Checking api permission {} : GRANTED", query);
        } else {
            logger.debug("Checking api permission {} : DENIED", query);
        }

        return hasPermission;
    }

    public void setPermissionsConfig(PermissionsConfig permissionsConfig) {
        this.permissionsConfig = permissionsConfig;
    }

    public void setAuthorizationConfig(AuthorizationConfig authorizationConfig) {
        this.authorizationConfig = authorizationConfig;
    }
}
