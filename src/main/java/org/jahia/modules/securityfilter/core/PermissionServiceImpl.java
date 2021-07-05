package org.jahia.modules.securityfilter.core;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.jahia.modules.securityfilter.PermissionService;
import org.jahia.modules.securityfilter.ScopeDefinition;
import org.jahia.api.settings.SettingsBean;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PermissionServiceImpl implements PermissionService {
    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    private AuthorizationConfig authorizationConfig;
    private ThreadLocal<Set<ScopeDefinition>> currentScopesLocal = new ThreadLocal<>();
    private BundleContext context;
    private SettingsBean settingsBean;
    private boolean supportYaml = false;

    public Collection<ScopeDefinition> getCurrentScopes() {
        return currentScopesLocal.get() != null ? Collections.unmodifiableSet(currentScopesLocal.get()) : null;
    }

    public Collection<ScopeDefinition> getAvailableScopes() {
        return Collections.unmodifiableSet(new HashSet<>(authorizationConfig.getScopes()));
    }

    public void activate() {
        String ext = supportYaml ? "yml" : "cfg";
        String profile = settingsBean.getString("security.profile", "default");
        URL url = context.getBundle().getResource("META-INF/configuration-profiles/profile-" + profile + "." + ext);
        try {
            Path path = Paths.get(settingsBean.getJahiaVarDiskPath(), "karaf", "etc", "org.jahia.modules.api.authorization-default." + ext);
            try (InputStream input = url.openStream()) {
                List<String> lines = IOUtils.readLines(input, StandardCharsets.UTF_8);
                lines.add(0, "# Do not edit - Configuration file provided by module, any change will be lost");
                try (Writer w = new FileWriter(path.toFile())) {
                    IOUtils.writeLines(lines, null, w);
                }
                logger.info("Copied configuration file of module {} into {}", url, path);
            }
            Path otherPath = Paths.get(settingsBean.getJahiaVarDiskPath(), "karaf", "etc", "org.jahia.modules.api.authorization-default." + (supportYaml ? "cfg" : "yml"));
            if (Files.exists(otherPath)) {
                Files.delete(otherPath);
            }
        } catch (IOException e) {
            logger.error("unable to copy configuration", e);
        }
    }

    public void addScopes(Collection<String> scopes, HttpServletRequest request) {
        if (currentScopesLocal.get() == null) {
            currentScopesLocal.set(new HashSet<>());
        }
        currentScopesLocal.get().addAll(authorizationConfig.getScopes().stream()
                .filter(scope -> scopes.contains(scope.getScopeName()))
                .filter(scope -> scope.isValid(request))
                .collect(Collectors.toSet()));
    }

    public void initScopes(HttpServletRequest request) {
        Set<String> scopeNames = authorizationConfig.getScopes().stream()
                .filter(scope -> scope.shouldAutoApply(request))
                .filter(scope -> scope.isValid(request))
                .map(ScopeDefinitionImpl::getScopeName)
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

        if (currentScopes == null) {
            // initScope has not been called, bypass security check
            return true;
        }

        boolean hasPermission = authorizationConfig.getScopes().stream()
                    .filter(currentScopes::contains)
                    .anyMatch(p -> p.isGrantAccess(query));

        if (hasPermission) {
            logger.debug("Checking api permission {} : GRANTED", query);
        } else {
            logger.debug("Checking api permission {} : DENIED", query);
        }

        return hasPermission;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void setSettingsBean(SettingsBean settingsBean) {
        this.settingsBean = settingsBean;
    }

    public void setArtifactInstaller(Collection<ArtifactInstaller> installers) {
         for (ArtifactInstaller installer : installers) {
            supportYaml |= installer.canHandle(new File("config.yml"));
        }
    }

    public void setAuthorizationConfig(AuthorizationConfig authorizationConfig) {
        this.authorizationConfig = authorizationConfig;
    }
}
