package org.jahia.modules.securityfilter.accesskey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@GraphQLName("AccessKey")
public class AccessKey {
    private String accessId;
    private String key;
    private String user;
    private String userKey;
    private String type;
    private List<String> ips = Collections.emptyList();
    private List<String> referrers = Collections.emptyList();
    private List<String> scopes = Collections.emptyList();
    private List<String> paths = Collections.emptyList();
    private List<String> apis = Collections.emptyList();
    private List<String> nodeTypes = Collections.emptyList();
    private List<String> workspaces = Collections.emptyList();
    private boolean active;
    private boolean verified;
    private boolean superkey;


    @GraphQLField
    public String getAccessId() {
        return accessId;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    @GraphQLField
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUser() {
        return user;
    }

    @JsonIgnore
    public void setUser(String user) {
        this.user = user;
    }

    @GraphQLField
    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    @GraphQLField
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @GraphQLField
    public List<String> getIps() {
        return ips;
    }

    public void setIps(List<String> ips) {
        this.ips = ips;
    }

    @GraphQLField
    public List<String> getReferrers() {
        return referrers;
    }

    public void setReferrers(List<String> referrers) {
        this.referrers = referrers;
    }

    public List<String> getScopes() {
        return scopes;
    }

    @JsonIgnore
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @GraphQLField
    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    @GraphQLField
    public List<String> getApis() {
        return apis;
    }

    public void setApis(List<String> apis) {
        this.apis = apis;
    }

    @GraphQLField
    public List<String> getNodeTypes() {
        return nodeTypes;
    }

    public void setNodeTypes(List<String> nodeTypes) {
        this.nodeTypes = nodeTypes;
    }

    @GraphQLField
    public List<String> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(List<String> workspaces) {
        this.workspaces = workspaces;
    }

    @GraphQLField
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @GraphQLField
    public boolean isSuperkey() {
        return superkey;
    }

    public void setSuperkey(boolean superkey) {
        this.superkey = superkey;
    }

    public Properties toProperties() {
        Properties props = new Properties();

        if (ips != null) {
            props.put("permission." + accessId + ".ip", String.join(",", ips));
        }

        if (referrers != null) {
            props.put("permission." + accessId + ".referer", String.join(",", referrers));
        }

        if (scopes != null) {
            props.put("permission." + accessId + ".scope", String.join(",", scopes));
        }

        if (paths != null) {
            props.put("permission." + accessId + ".pathPattern", String.join(",", paths));
        }

        if (apis != null) {
            props.put("permission." + accessId + ".api", String.join(",", apis));
        }

        if (nodeTypes != null) {
            props.put("permission." + accessId + ".nodeType", String.join(",", nodeTypes));
        }

        if (workspaces != null) {
            props.put("permission." + accessId + ".workspace", String.join(",", workspaces));
        }

        return props;
    }

    @Override
    public AccessKey clone() {
        AccessKey newKey = new AccessKey();
        newKey.setAccessId(accessId);
        newKey.setKey(key);
        newKey.setUser(user);
        newKey.setUserKey(userKey);
        newKey.setType(type);
        newKey.setActive(active);
        newKey.setVerified(verified);
        newKey.setSuperkey(superkey);
        newKey.setIps(new ArrayList<>(ips));
        newKey.setReferrers(new ArrayList<>(referrers));
        newKey.setScopes(new ArrayList<>(scopes));
        newKey.setPaths(new ArrayList<>(paths));
        newKey.setApis(new ArrayList<>(apis));
        newKey.setNodeTypes(new ArrayList<>(nodeTypes));
        newKey.setWorkspaces(new ArrayList<>(workspaces));

        return newKey;
    }
}
