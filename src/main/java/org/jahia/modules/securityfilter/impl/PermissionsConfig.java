/*
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
import org.jahia.modules.securityfilter.PermissionService;
import org.jahia.services.content.JCRNodeWrapper;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Permissions configuration
 * <p>
 * Bound to org.jahia.modules.api.permissions.cfg
 */
public class PermissionsConfig implements PermissionService, ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(PermissionsConfig.class);

    private PermissionsConfig() {
    }

    private List<Permission> permissions = new ArrayList<Permission>();

    private Map<String, List<Permission>> permissionsByPid = new HashMap<String, List<Permission>>();

    @Override
    public String getName() {
        return "API Security configuration";
    }

    /**
     * Configuration change - load all permissions from cfg
     *
     * @param properties The new properties
     * @throws ConfigurationException
     */
    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        List<Permission> newPermissions = new ArrayList<Permission>();
        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            Map<String, Map<String, String>> permissionConfig = new HashMap<String, Map<String, String>>();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (StringUtils.startsWith(key, "permission.")) {
                    String subKey = StringUtils.substringAfter(key, "permission.");
                    String name = StringUtils.substringBefore(subKey, ".");
                    if (!permissionConfig.containsKey(name)) {
                        permissionConfig.put(name, new HashMap<String, String>());
                    }
                    permissionConfig.get(name).put(StringUtils.substringAfter(subKey, "."), (String) properties.get(key));
                }
            }
            for (Map<String, String> map : permissionConfig.values()) {
                Permission permission = new Permission();
                permission.setAccess(map.get("access"));
                permission.setRequiredPermission(map.get("requiredPermission"));

                if (map.containsKey("nodeType")) {
                    permission.setNodeTypes(new HashSet<String>(Arrays.asList(StringUtils.split(map.get("nodeType"), ", "))));
                }
                if (map.containsKey("api")) {
                    permission.setApis(new HashSet<String>(Arrays.asList(StringUtils.split(map.get("api"), ", "))));
                }
                if (map.containsKey("pathPattern")) {
                    Set<Pattern> patterns = new HashSet<Pattern>();
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

    public void updatePermissions() {
        List<Permission> newPermissions = new ArrayList<Permission>();
        for (List<Permission> permissionList : permissionsByPid.values()) {
            newPermissions.addAll(permissionList);
        }
        Collections.sort(newPermissions, new Comparator<Permission>() {
            @Override
            public int compare(Permission o1, Permission o2) {
                return o1.getPriority() - o2.getPriority();
            }
        });
        permissions = newPermissions;

        logger.info("Security configuration reloaded");
    }

    public boolean hasPermission(String apiToCheck, Node node) throws RepositoryException {
        logger.debug("Checking api permission " + apiToCheck + " for " + node.getPath());
        for (Permission permission : permissions) {
            boolean check = true;
            if (permission.getWorkspaces() != null && !permission.getWorkspaces().isEmpty()) {
                check = permission.getWorkspaces().contains(node.getSession().getWorkspace().getName());
            }
            if (check && permission.getApis() != null && !permission.getApis().isEmpty()) {
                check = false;
                for (String api : permission.getApis()) {
                    check |= api.equals(apiToCheck) || apiToCheck.startsWith(api + ".");
                }
            }
            if (check && permission.getNodeTypes() != null && !permission.getNodeTypes().isEmpty()) {
                check = false;
                for (String nodeType : permission.getNodeTypes()) {
                    check |= node.isNodeType(nodeType);
                }
            }
            if (check && permission.getPathPatterns() != null && !permission.getPathPatterns().isEmpty()) {
                check = false;
                for (Pattern pattern : permission.getPathPatterns()) {
                    check |= pattern.matcher(node.getPath()).matches();
                }
            }
            if (check) {
                if (permission.getAccess() != null) {
                    if (permission.getAccess().equalsIgnoreCase("denied")) {
                        return false;
                    } else if (permission.getAccess().equalsIgnoreCase("restricted")) {
                        return ((JCRNodeWrapper) node).hasPermission("jcr:addChildNodes_default");
                    }
                }
                return permission.getRequiredPermission() == null || ((JCRNodeWrapper) node).hasPermission(permission.getRequiredPermission());
            }
        }
        return true;
    }
}
