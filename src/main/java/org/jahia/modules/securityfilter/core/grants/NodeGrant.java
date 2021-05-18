package org.jahia.modules.securityfilter.core.grants;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.modulemanager.util.PropertiesValues;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.*;
import java.util.regex.Pattern;

public class NodeGrant implements Grant {
    private String permission;
    private String permissionTargetNode;
    private Set<String> workspaces = Collections.emptySet();
    private Set<Pattern> pathPatterns = Collections.emptySet();
    private Set<String> nodeTypes = Collections.emptySet();

    public static Grant build(PropertiesValues grantValues) {
        PropertiesValues nodeValues = grantValues.getValues("node");
        if (nodeValues.getKeys().isEmpty()) {
            return null;
        }
        NodeGrant nodeGrant = new NodeGrant();

        nodeGrant.setPermission(nodeValues.getProperty("permission"));
        nodeGrant.setPermissionTargetNode(nodeValues.getProperty("permissionTargetNode"));
        if (nodeValues.getProperty("nodeType") != null) {
            nodeGrant.setNodeTypes(new LinkedHashSet<String>(Arrays.asList(StringUtils.split(nodeValues.getProperty("nodeType"), ", "))));
        }
        if (nodeValues.getProperty("pathPattern") != null) {
            Set<Pattern> patterns = new LinkedHashSet<Pattern>();
            for (String exp : StringUtils.split(nodeValues.getProperty("pathPattern"), ", ")) {
                patterns.add(Pattern.compile(exp));
            }
            nodeGrant.setPathPatterns(patterns);
        }
        if (nodeValues.getProperty("workspace") != null) {
            nodeGrant.setWorkspaces(new HashSet<String>(Arrays.asList(StringUtils.split(nodeValues.getProperty("workspace"), ", "))));
        }
        return nodeGrant;
    }


    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setPermissionTargetNode(String permissionTargetNode) {
        this.permissionTargetNode = permissionTargetNode;
    }

    public void setWorkspaces(Set<String> workspaces) {
        this.workspaces = workspaces;
    }

    public void setPathPatterns(Set<Pattern> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public void setNodeTypes(Set<String> nodeTypes) {
        this.nodeTypes = nodeTypes;
    }

    @Override
    public boolean matches(Map<String, Object> query) {
        JCRNodeWrapper node = (JCRNodeWrapper) query.get("node");
        if (node == null) {
            return false;
        }

        try {
            return permissionMatches(node) && nodeTypeMatches(node) && pathMatches(node) && workspaceMatches(node);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private boolean permissionMatches(JCRNodeWrapper node) {
        if (permission == null) {
            return true;
        }
        try {
            if (permissionTargetNode != null) {
                node = node.getSession().getNode(permissionTargetNode);
            }
            return node.hasPermission(permission);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private boolean nodeTypeMatches(Node node) throws RepositoryException {
        if (nodeTypes.isEmpty()) {
            return true;
        }

        for (String nodeType : nodeTypes) {
            if (node.isNodeType(nodeType)) {
                return true;
            }
        }

        return false;
    }

    private boolean pathMatches(Node node) throws RepositoryException {
        if (pathPatterns.isEmpty()) {
            return true;
        }
        String nodePath = node.getPath();
        for (Pattern pattern : pathPatterns) {
            if (pattern.matcher(nodePath).matches()) {
                return true;
            }
        }

        return false;
    }

    private boolean workspaceMatches(Node node) throws RepositoryException {
        return workspaces.isEmpty() || workspaces.contains(node.getSession().getWorkspace().getName());
    }
}
