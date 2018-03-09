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
import org.osgi.service.cm.ManagedService;
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
public class PermissionsConfig implements PermissionService, ManagedService {
    private static final Logger logger = LoggerFactory.getLogger(PermissionsConfig.class);
    private final static PermissionsConfig INSTANCE = new PermissionsConfig();

    private PermissionsConfig() {
    }

    private List<Permission> permissions = new ArrayList<Permission>();

    /**
     * Configuration change - load all permissions from cfg
     *
     * @param properties The new properties
     * @throws ConfigurationException
     */
    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
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
                permission.setRequiredPermission(map.get("requiredPermission"));
                permission.setNodeType(map.get("nodeType"));
                permission.setApi(map.get("api"));
                if (map.containsKey("pathPattern")) {
                    permission.setPathPattern(Pattern.compile(map.get("pathPattern")));
                }
                permission.setWorkspace(map.get("workspace"));
                if (map.containsKey("priority")) {
                    permission.setPriority(Integer.parseInt(map.get("priority")));
                }
                if (permission.getRequiredPermission() != null) {
                    newPermissions.add(permission);
                } else {
                    logger.warn("No required permission set : " + map);
                }
            }
        }
        Collections.sort(newPermissions, new Comparator<Permission>() {
            @Override
            public int compare(Permission o1, Permission o2) {
                return o1.getPriority() - o2.getPriority();
            }
        });
        permissions = newPermissions;
    }

    public boolean hasPermission(String api, Node node) throws RepositoryException {
        logger.debug("Checking api permission " + api + " for " + node.getPath());
        for (Permission permission : permissions) {
            boolean check = true;
            if (permission.getWorkspace() != null) {
                check = permission.getWorkspace().contains(node.getSession().getWorkspace().getName());
            }
            if (check && permission.getApi() != null) {
                check = permission.getApi().equals(api) || api.startsWith(permission.getApi() + ".");
            }
            if (check && permission.getNodeType() != null) {
                check = node.isNodeType(permission.getNodeType());
            }
            if (check && permission.getPathPattern() != null) {
                check = permission.getPathPattern().matcher(node.getPath()).matches();
            }
            if (check) {
                return ((JCRNodeWrapper) node).hasPermission(permission.getRequiredPermission());
            }
        }
        return true;
    }
}
