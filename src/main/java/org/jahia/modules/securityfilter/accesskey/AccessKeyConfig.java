package org.jahia.modules.securityfilter.accesskey;

import com.google.common.collect.Sets;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaUnauthorizedException;
import org.jahia.modules.securityfilter.impl.Permission;
import org.jahia.services.content.*;
import org.jahia.services.usermanager.DefaultJahiaUserSplittingRuleImpl;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;

public class AccessKeyConfig implements AccessKeyService, ManagedService, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyConfig.class);

    //TODO syncronize methods
    private List<AccessKey> keys = Collections.emptyList();
    private JCRTemplate jcrTemplate;
    private Map<String, String> accessKeyConfig = new HashMap<>();

    private static DefaultJahiaUserSplittingRuleImpl splittingRule;

    static {
        splittingRule = new DefaultJahiaUserSplittingRuleImpl();
        splittingRule.setUsersRootNode("/a");
        splittingRule.setNonSplittedUsers(Collections.<String>emptyList());
    }

    @Override
    public Permission getPermission(AccessKey accessKey) {
        Permission permission = new Permission();
        permission.setApis(Sets.newHashSet(accessKey.getApis()));
        permission.setNodeTypes(Sets.newHashSet(accessKey.getNodeTypes()));
        permission.setWorkspaces(Sets.newHashSet(accessKey.getWorkspaces()));
        permission.setScopes(Sets.newHashSet(accessKey.getScopes()));
        permission.setPathPatterns(accessKey.getPaths().stream().map(Pattern::compile).collect(Collectors.toSet()));

        return permission;
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (StringUtils.startsWith(key, "accessKey.")) {
                    String subKey = StringUtils.substringAfter(key, "accessKey.");
                    String name = StringUtils.substringBefore(subKey, ".");
                    if (!accessKeyConfig.containsKey(name)) {
                        accessKeyConfig.put(name, (String) properties.get(key));
                    }
                }
            }
            logger.info("Access Key configuration reloaded");

            // TODO remove
            try {
                System.out.println(bytesToString(encrypt("adb5cc7b358345a1be511b22a2feb587", "//.*liveedit")));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //Do nothing
        jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                keys.clear();
                QueryManager qm = session.getWorkspace().getQueryManager();
                StringBuilder query = new StringBuilder("SELECT * FROM [sfnt:accessKey] as key");
                query.append(" ORDER BY [jcr:lastModified] DESC");
                Query q = qm.createQuery(query.toString(), Query.JCR_SQL2);
                NodeIterator ni = q.execute().getNodes();


                while (ni.hasNext()) {
                    JCRNodeWrapper key = (JCRNodeWrapper) ni.next();
                    AccessKey accessKey = new AccessKey();
                    accessKey.setAccessId(key.getPropertyAsString("accessId"));
                    accessKey.setKey(key.getPropertyAsString("accessKey"));
                    accessKey.setActive(key.getProperty("active").getBoolean());
                    accessKey.setSuperkey(key.getProperty("superkey").getBoolean());
                    accessKey.setType(key.getPropertyAsString("type"));
                    accessKey.setUserKey(key.getPropertyAsString("userKey"));
                    accessKey.setWorkspaces(getValues(key.getProperty("workspaces")));

                    if (key.hasProperty("paths")) {
                        accessKey.setPaths(getValues(key.getProperty("paths")));
                    }

                    if (key.hasProperty("ips")) {
                        accessKey.setIps(getValues(key.getProperty("ips")));
                    }

                    if (key.hasProperty("referrers")) {
                        accessKey.setReferrers(getValues(key.getProperty("referrers")));
                    }

                    if (key.hasProperty("apis")) {
                        accessKey.setApis(getValues(key.getProperty("apis")));
                    }

                    if (key.hasProperty("nodeTypes")) {
                        accessKey.setNodeTypes(getValues(key.getProperty("nodeTypes")));
                    }

                    if (keys.isEmpty()) {
                        keys = new ArrayList<>();
                    }

                    keys.add(accessKey);
                }
                return null;
            }
        });

        System.out.println("****************** Init access keys ********************  " + keys.size());
    }

    @Override
    public AccessKey createAccessKey(AccessKey accessKey) throws IOException, RepositoryException, JahiaUnauthorizedException {
        if (!checkPermission(accessKey)) {
            throw new JahiaUnauthorizedException("You do not have the right to perform this operation");
        }

        JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser();
        String accessId = RandomStringUtils.randomAlphabetic(16);
        accessKey.setAccessId(accessId);
        accessKey.setScopes(Arrays.asList(accessId));
        accessKey.setSuperkey(false);
        String key = RandomStringUtils.randomAlphabetic(48);

        //Store in jcr
        jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper accessKeys;

                if (!session.nodeExists("/access-keys")) {
                    accessKeys = session.getNode("/").addNode("access-keys", "sfnt:accessKeys");
                } else {
                    accessKeys = session.getNode("/access-keys");
                }

                //TODO split path
//                JCRNodeWrapper entry = accessKeys.addNode(splittingRule.getRelativePathForUsername(accessId), "sfnt:accessKey");
                JCRNodeWrapper entry = accessKeys.addNode("key-" + accessId, "sfnt:accessKey");
                entry.setProperty("type", accessKey.getType());
                entry.setProperty("accessId", accessId);
                entry.setProperty("superkey", accessKey.isSuperkey());
                entry.setProperty("scopes", new String[]{accessId});
                entry.setProperty("active", true);
                entry.setProperty("user", user.getProperty("jcr:uuid"));
                entry.setProperty("userKey", user.getUsername());
                entry.setProperty("paths", accessKey.getPaths().toArray(new String[0]));
                entry.setProperty("workspaces", accessKey.getWorkspaces().toArray(new String[0]));

                if (!accessKey.getIps().isEmpty()) entry.setProperty("ips", accessKey.getIps().toArray(new String[0]));
                if (!accessKey.getApis().isEmpty()) entry.setProperty("apis", accessKey.getApis().toArray(new String[0]));
                if (!accessKey.getReferrers().isEmpty()) entry.setProperty("referrers", accessKey.getReferrers().toArray(new String[0]));
                if (!accessKey.getNodeTypes().isEmpty()) entry.setProperty("nodeTypes", accessKey.getNodeTypes().toArray(new String[0]));

                try {
                    byte[] hash = encrypt(key, getSecretSuffix(accessKey));
                    String encrypted = bytesToString(hash);
                    entry.setProperty("accessKey", encrypted);

                    AccessKey storedKey = accessKey.clone();
                    storedKey.setAccessId(accessId);
                    storedKey.setKey(encrypted);
                    storedKey.setUser(user.getProperty("jcr:uuid"));
                    storedKey.setUserKey(user.getUsername());
                    storedKey.setScopes(Arrays.asList(accessId));
                    storedKey.setActive(true);

                    keys.add(storedKey);
                } catch (Exception e) {
                    logger.error("Failed to hash password", e);
                }

                session.save();

                return null;
            }
        });

        accessKey.setActive(true);
        accessKey.setAccessId(accessId);
        accessKey.setKey(key);

        return accessKey;
    }

    @Override
    public AccessKey createRootAccessKey(AccessKey accessKey) throws IOException, RepositoryException {

        Predicate<AccessKey> predicate = AccessKey::isSuperkey;

        if (findAccessKey(predicate) != null) {
            return null;
        }

        accessKey.setAccessId(RandomStringUtils.randomAlphabetic(16));

        jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper accessKeys;

                if (!session.nodeExists("/access-keys")) {
                    accessKeys = session.getNode("/").addNode("access-keys", "sfnt:accessKeys");
                } else {
                    accessKeys = session.getNode("/access-keys");
                }

                //TODO split path
//                JCRNodeWrapper entry = accessKeys.addNode(splittingRule.getRelativePathForUsername(accessId), "sfnt:accessKey");
                JCRNodeWrapper entry = accessKeys.addNode("key-" + accessKey.getAccessId(), "sfnt:accessKey");
                entry.setProperty("type", accessKey.getType());
                entry.setProperty("accessId", accessKey.getAccessId());
                entry.setProperty("superkey", true);
                entry.setProperty("scopes", new String[]{accessKey.getAccessId()});
                entry.setProperty("active", true);
                entry.setProperty("user", JahiaUserManagerService.getInstance().lookup("root").getIdentifier());
                entry.setProperty("userKey", "root");
                entry.setProperty("paths", new String[]{"/", "/.*"});
                entry.setProperty("workspaces", new String[]{"live", "edit"});

                try {
                    entry.setProperty("accessKey", accessKey.getKey());

                    AccessKey storedKey = accessKey.clone();
                    storedKey.setUser(JahiaUserManagerService.getInstance().lookup("root").getIdentifier());
                    storedKey.setUserKey("root");
                    storedKey.setScopes(Arrays.asList(accessKey.getAccessId()));
                    storedKey.setPaths(Arrays.asList("/", "/.*"));
                    storedKey.setWorkspaces(Arrays.asList("live", "edit"));
                    storedKey.setSuperkey(true);
                    storedKey.setActive(true);

                    keys.add(storedKey);
                } catch (Exception e) {
                    logger.error("Failed to hash password", e);
                }

                session.save();
                return null;
            }
        });

        accessKey.setActive(true);
        accessKey.setKey(null);
        return accessKey;
    }

    @Override
    public boolean verifyAccessKey(AccessKey accessKey) throws RepositoryException {
        return jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Boolean>() {
            @Override
            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                accessKey.setVerified(false);
                Predicate<AccessKey> predicate = key -> accessKey.getAccessId().equals(key.getAccessId());
                AccessKey storedKey = findAccessKey(predicate);

                if (storedKey == null) {
                    return false;
                }

                if (!storedKey.isActive()) {
                    return false;
                }

                String accessKeyHash = storedKey.getKey();

                try {
                    byte[] hash = encrypt(accessKey.getKey(), getSecretSuffix(storedKey));
                    if (accessKeyHash.equals(bytesToString(hash))) {
                        accessKey.setIps(storedKey.getIps());
                        accessKey.setReferrers(storedKey.getReferrers());
                        accessKey.setScopes(storedKey.getScopes());
                        accessKey.setPaths(storedKey.getPaths());
                        accessKey.setNodeTypes(storedKey.getNodeTypes());
                        accessKey.setApis(storedKey.getApis());

                        accessKey.setVerified(true);
                        accessKey.setUserKey(storedKey.getUserKey());
                        accessKey.setWorkspaces(storedKey.getWorkspaces());
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("Failed to hash password during verification", e);
                }
                return false;
            }
        });
    }

    @Override
    public boolean deactivateAccessKey(AccessKey accessKey) throws RepositoryException {
        Predicate<AccessKey> predicate = key -> accessKey.getAccessId().equals(key.getAccessId());
        AccessKey storedKey = findAccessKey(predicate);

        if (storedKey != null) {
            storedKey.setActive(false);
        }

        jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper accessKeys;

                if (!session.nodeExists("/access-keys")) {
                    return null;
                } else {
                    accessKeys = session.getNode("/access-keys");
                }

                JCRNodeWrapper entry = accessKeys.getNode("key-" + accessKey.getAccessId());
                entry.setProperty("active", false);
                session.save();
                return null;
            }
        });

        return true;
    }

    @Override
    public boolean activateAccessKey(AccessKey accessKey) throws RepositoryException {
        Predicate<AccessKey> predicate = key -> accessKey.getAccessId().equals(key.getAccessId());
        AccessKey storedKey = findAccessKey(predicate);

        if (storedKey != null) {
            storedKey.setActive(true);
        }

        jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper accessKeys;

                if (!session.nodeExists("/access-keys")) {
                    return null;
                } else {
                    accessKeys = session.getNode("/access-keys");
                }

                JCRNodeWrapper entry = accessKeys.getNode("key-" + accessKey.getAccessId());
                entry.setProperty("active", true);
                session.save();
                return null;
            }
        });

        return true;
    }

    @Override
    public boolean destroyAccessKey(AccessKey accessKey) throws RepositoryException {
        keys.removeIf(key -> key.getAccessId().equals(accessKey.getAccessId()));
        jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper accessKeys;

                if (!session.nodeExists("/access-keys")) {
                    return null;
                } else {
                    accessKeys = session.getNode("/access-keys");
                }

                JCRNodeWrapper entry = accessKeys.getNode("key-" + accessKey.getAccessId());
                entry.remove();
                session.save();
                return null;
            }
        });

        return true;
    }

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    private byte[] encrypt(String key, String secretSufix) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String secret = accessKeyConfig.get("secret").concat(secretSufix);
        KeySpec spec = new PBEKeySpec(key.toCharArray(), secret.getBytes(), 1000, 264);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return factory.generateSecret(spec).getEncoded();
    }

    private String bytesToString(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    private boolean checkPermission(AccessKey accessKey) throws RepositoryException {
        JCRSessionWrapper userSession = JCRSessionFactory.getInstance().getCurrentUserSession();

        BiPredicate<List<String>, String> hasPermissionOnAllPaths = (List<String> paths, String type) ->
            paths.stream().allMatch(path -> {
                path = path.replace("/.*", "");
                try {
                    return userSession.getNode(path).hasPermission(type.equals("read") ? "jcr:read_default" : "jcr:write_default") &&
                            userSession.getNode(path).hasPermission(type.equals("read") ? "jcr:read_live" : "jcr:write_live");
                } catch (RepositoryException e) {
                    logger.error("Failed permission check", e);
                }

                return false;
            });

        if ("write".equals(accessKey.getType())) {
            return hasPermissionOnAllPaths.test(accessKey.getPaths(), "write");
        }

        return hasPermissionOnAllPaths.test(accessKey.getPaths(), "read");
    }

    private List<String> getValues(JCRPropertyWrapper prop) throws RepositoryException {
        return Arrays.stream(prop.getValues()).map(v -> {
            try {
                return v.getString();
            } catch (Exception e) {
                //
            }
            return null;
        }).collect(Collectors.toList());
    }

    private AccessKey findAccessKey(Predicate<AccessKey> predicate) {
        List<AccessKey> list = keys.stream().filter(predicate).collect(Collectors.toList());

        if (!list.isEmpty()) {
            return list.get(0);
        }

        return null;
    }

    private String getSecretSuffix(AccessKey accessKey) {
        List<String> list = new ArrayList<>();
        list.addAll(accessKey.getPaths());
        list.addAll(accessKey.getApis());
        list.addAll(accessKey.getNodeTypes());
        list.addAll(accessKey.getWorkspaces());
        list.addAll(accessKey.getIps());
        list.addAll(accessKey.getReferrers());
        return String.join("", list);
    }
}
