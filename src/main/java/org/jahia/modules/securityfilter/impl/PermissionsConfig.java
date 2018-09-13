/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.securityfilter.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.security.JahiaPrivilegeRegistry;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.securityfilter.PermissionService;
import org.jahia.modules.securityfilter.impl.Permission.AccessType;
import org.jahia.modules.securityfilter.jwt.impl.JWTFilter;
import org.jahia.modules.securityfilter.jwt.impl.TokenVerificationResult;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Access permission configuration service.
 * <p>
 * Bound to org.jahia.modules.api.permissions.cfg
 */
public class PermissionsConfig implements PermissionService, ManagedServiceFactory, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(PermissionsConfig.class);

    private static final Comparator<Permission> PERMISSION_COMPARATOR = new Comparator<Permission>() {
        @Override
        public int compare(Permission o1, Permission o2) {
            return o1.getPriority() - o2.getPriority();
        }
    };

    private static boolean apiMatches(String apiToCheck, Permission permission) {
        if (permission.getApis().isEmpty()) {
            return true;
        }

        for (String api : permission.getApis()) {
            if (api.equals(apiToCheck) || apiToCheck.startsWith(api + ".")) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkPermissionExists(final String permissionName) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {

                @Override
                public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    String name = JCRContentUtils.getExpandedName(permissionName,
                            session.getWorkspace().getNamespaceRegistry());
                    for (Privilege p : JahiaPrivilegeRegistry.getRegisteredPrivileges()) {
                        if (p.getName().equals(name)) {
                            return Boolean.TRUE;
                        }
                    }
                    return Boolean.FALSE;
                }
            });
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(
                    "Unable to check the presence of the configured permission for the restricted API access: "
                            + permissionName,
                    e);
        }
    }

    private static boolean nodeTypeMatches(Node node, Permission permission) throws RepositoryException {
        if (permission.getNodeTypes().isEmpty()) {
            return true;
        }

        for (String nodeType : permission.getNodeTypes()) {
            if (node.isNodeType(nodeType)) {
                return true;
            }
        }

        return false;
    }

    private static boolean pathMatches(String nodePath, Permission permission) {
        if (permission.getPathPatterns().isEmpty()) {
            return true;
        }

        for (Pattern pattern : permission.getPathPatterns()) {
            if (pattern.matcher(nodePath).matches()) {
                return true;
            }
        }

        return false;
    }

    private static boolean workspaceMatches(Node node, Permission permission) throws RepositoryException {
        return permission.getWorkspaces().isEmpty()
                || permission.getWorkspaces().contains(node.getSession().getWorkspace().getName());
    }

    private static boolean tokenMatches(Permission permission) {
        TokenVerificationResult verificationResult = JWTFilter.getJWTTokenVerificationStatus();

        //Ignore rule if JWT filter didn't run - TODO investigate this
        if (verificationResult == null) {
            return false;
        }

        //Permission is not using jwt and user is not trying to access resource with jwt
        if (permission.getScopes().isEmpty() && verificationResult.getVerificationStatusCode() == TokenVerificationResult.VerificationStatus.NOT_FOUND) {
            return true;
        }

        //Failed to verify token signature
        if (verificationResult.getVerificationStatusCode() == TokenVerificationResult.VerificationStatus.REJECTED) {
            return false;
        }

        //Token contains required scope, allow access
        if (verificationResult.getToken() != null) {
            List<String> tokenScopes = verificationResult.getToken().getClaim("scopes").asList(String.class);
            for (String scope : permission.getScopes()) {
                if (tokenScopes.contains(scope)) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<Permission> permissions = new ArrayList<Permission>();

    private Map<String, List<Permission>> permissionsByPid = new HashMap<String, List<Permission>>();

    private String restrictedAccessPermissionFallbackName; 

    private String restrictedAccessPermissionName; 

    private PermissionsConfig() {
        super();
    }

    @Override
    public String getName() {
        return "API Security configuration";
    }

    /**
     * Configuration change - load all permissions from cfg file.
     *
     * @param properties The new properties
     */
    @Override
    public void updated(String pid, Dictionary<String, ?> properties) {
        List<Permission> newPermissions = new ArrayList<Permission>();
        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            Map<String, Map<String, String>> permissionConfig = new LinkedHashMap<String, Map<String, String>>();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (StringUtils.startsWith(key, "permission.")) {
                    String subKey = StringUtils.substringAfter(key, "permission.");
                    String name = StringUtils.substringBefore(subKey, ".");
                    if (!permissionConfig.containsKey(name)) {
                        permissionConfig.put(name, new LinkedHashMap<String, String>());
                    }
                    permissionConfig.get(name).put(StringUtils.substringAfter(subKey, "."), (String) properties.get(key));
                }
            }
            for (Map<String, String> map : permissionConfig.values()) {
                Permission permission = new Permission();
                permission.setAccess(map.get("access"));
                permission.setRequiredPermission(map.get("requiredPermission"));
                permission.setPermission(map.get("permission"));

                if (map.containsKey("nodeType")) {
                    permission.setNodeTypes(new LinkedHashSet<String>(Arrays.asList(StringUtils.split(map.get("nodeType"), ", "))));
                }
                if (map.containsKey("api")) {
                    permission.setApis(new LinkedHashSet<String>(Arrays.asList(StringUtils.split(map.get("api"), ", "))));
                }
                if (map.containsKey("pathPattern")) {
                    Set<Pattern> patterns = new LinkedHashSet<Pattern>();
                    for (String exp : StringUtils.split(map.get("pathPattern"), ", ")) {
                        patterns.add(Pattern.compile(exp));
                    }
                    permission.setPathPatterns(patterns);
                }
                if (map.containsKey("workspace")) {
                    permission.setWorkspaces(new HashSet<String>(Arrays.asList(StringUtils.split(map.get("workspace"), ", "))));
                }
                if (map.containsKey("priority")) {
                    permission.setPriority(Integer.parseInt(map.get("priority")));
                }
                if (map.containsKey("scope")) {
                    permission.setScopes(new LinkedHashSet<String>(Arrays.asList(StringUtils.split(map.get("scope"), ", "))));
                }
                newPermissions.add(permission);
            }
        }

        permissionsByPid.put(pid, newPermissions);

        updatePermissions();
    }

    @Override
    public void deleted(String pid) {
        permissionsByPid.remove(pid);

        updatePermissions();
    }

    private void updatePermissions() {
        List<Permission> newPermissions = new ArrayList<Permission>();
        for (List<Permission> permissionList : permissionsByPid.values()) {
            newPermissions.addAll(permissionList);
        }
        Collections.sort(newPermissions, PERMISSION_COMPARATOR);
        permissions = newPermissions;

        logger.info("Security configuration reloaded");
    }

    @Override
    public boolean hasPermission(String apiToCheck, Node node) throws RepositoryException {
        String nodePath = node.getPath();

        boolean hasPermission = hasPermissionInternal(apiToCheck, nodePath, node);

        if (hasPermission) {
            logger.debug("Checking api permission '{}' for {}: GRANTED", apiToCheck, nodePath);
        } else {
            logger.debug("Checking api permission '{}' for {}: DENIED", apiToCheck, nodePath);
        }

        return hasPermission;
    }

    private boolean hasPermissionInternal(String apiToCheck, String nodePath, Node node) throws RepositoryException {
        for (Permission permission : permissions) {
            if (!workspaceMatches(node, permission) || !apiMatches(apiToCheck, permission)
                    || !pathMatches(nodePath, permission) || !nodeTypeMatches(node, permission)
                    || !tokenMatches(permission)) {
                continue;
            }
            if (permission.getAccess() != null) {
                if (permission.getAccess() == AccessType.denied) {
                    return false;
                } else if (permission.getAccess() == AccessType.restricted) {
                    JCRNodeWrapper jcrNode = (JCRNodeWrapper) node;
                    return jcrNode.hasPermission(getRestrictedPermissionName(jcrNode));
                }
            }
            if (permission.getPermission() != null && ((JCRNodeWrapper) node).hasPermission(permission.getPermission())) {
                return true;
            } else {
                return permission.getRequiredPermission() == null
                        || ((JCRNodeWrapper) node).hasPermission(permission.getRequiredPermission());
            }
        }
        //Make sure if no permissions are matched the request is denied
//        return false;
        return true;
    }

    private String getRestrictedPermissionName(JCRNodeWrapper node) {
        return node.getProvider().isDefault() ? restrictedAccessPermissionName : restrictedAccessPermissionFallbackName;
    }

    public void setRestrictedAccessPermissionFallbackName(String restrictedAccessPermissionFallbackName) {
        this.restrictedAccessPermissionFallbackName = restrictedAccessPermissionFallbackName;
    }

    public void setRestrictedAccessPermissionName(String restrictedAccessPermissionName) {
        this.restrictedAccessPermissionName = restrictedAccessPermissionName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!restrictedAccessPermissionFallbackName.equals(restrictedAccessPermissionName)
                && !checkPermissionExists(restrictedAccessPermissionName)) {
            restrictedAccessPermissionName = restrictedAccessPermissionFallbackName;
        }

        logger.info("Using {} permission for restricted access", this.restrictedAccessPermissionName);
    }
}
