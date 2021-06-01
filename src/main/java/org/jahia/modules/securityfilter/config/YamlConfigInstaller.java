package org.jahia.modules.securityfilter.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.internal.DirectoryWatcher;
import org.apache.tika.io.IOUtils;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

@Component(service = {ConfigurationListener.class, ArtifactInstaller.class, ArtifactListener.class})
public class YamlConfigInstaller implements ArtifactInstaller, ConfigurationListener {
    private static Logger logger = LoggerFactory.getLogger(YamlConfigInstaller.class);
    private final Map<String, String> pidToFile = new HashMap<>();
    private BundleContext context;
    private ConfigurationAdmin configAdmin;
    private YAMLMapper yamlMapper = new YAMLMapper();


    @Reference
    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    @Activate
    public void activate() {
        try {
            Configuration[] configs = configAdmin.listConfigurations(null);
            if (configs != null) {
                for (Configuration config : configs) {
                    Dictionary dict = config.getProperties();
                    String fileName = dict != null ? (String) dict.get(DirectoryWatcher.FILENAME) : null;
                    if (fileName != null) {
                        pidToFile.put(config.getPid(), fileName);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Unable to initialize configurations list", e);
        }
    }

    public boolean canHandle(File artifact) {
        return artifact.getName().endsWith(".yaml") || artifact.getName().endsWith(".yml");
    }

    public void install(File artifact) throws Exception {
        setConfig(artifact);
    }

    public void update(File artifact) throws Exception {
        setConfig(artifact);
    }

    public void uninstall(File artifact) throws Exception {
        deleteConfig(artifact);
    }

    public void configurationEvent(final ConfigurationEvent configurationEvent) {
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged(
                    new PrivilegedAction<Void>() {
                        public Void run() {
                            doConfigurationEvent(configurationEvent);
                            return null;
                        }
                    }
            );
        } else {
            doConfigurationEvent(configurationEvent);
        }
    }

    public void doConfigurationEvent(ConfigurationEvent configurationEvent) {
        // Check if writing back configurations has been disabled.
        if (!shouldSaveConfig()) {
            return;
        }

        if (configurationEvent.getType() == ConfigurationEvent.CM_UPDATED) {
            try {
                Configuration config = getConfigurationAdmin().getConfiguration(configurationEvent.getPid(), "?");
                Dictionary dict = config.getProperties();
                String fileName = dict != null ? (String) dict.get(DirectoryWatcher.FILENAME) : null;
                File file = fileName != null ? fromConfigKey(fileName) : null;
                if (file != null && file.isFile() && canHandle(file)) {
                    pidToFile.put(config.getPid(), fileName);
                    Map<String, Object> previousValues;
                    StringWriter previousContent = new StringWriter();
                    IOUtils.copy(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), previousContent);
                    // read YAML file
                    try (Reader r = new StringReader(previousContent.getBuffer().toString())) {
                        previousValues = yamlMapper.readValue(r, new TypeReference<Map<String, Object>>() {
                        });
                    }

                    PropertiesManager propertiesManager = new PropertiesManager(getMap(dict));
                    PropertiesValues v = propertiesManager.getValues();

                    Map<String, Object> newValues = new LinkedHashMap<>();
                    convert(newValues, v, previousValues);
                    if (!previousValues.equals(newValues)) {
                        StringWriter newContent = new StringWriter();
                        yamlMapper.writeValue(newContent, newValues);

                        try (Writer fw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                            IOUtils.copy(new StringReader(newContent.getBuffer().toString()), fw);
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("Unable to save configuration", e);
            }
        }

        if (configurationEvent.getType() == ConfigurationEvent.CM_DELETED) {
            try {
                String fileName = pidToFile.remove(configurationEvent.getPid());
                File file = fileName != null ? fromConfigKey(fileName) : null;
                if (file != null && file.isFile()) {
                    if (!file.delete()) {
                        throw new IOException("Unable to delete file: " + file);
                    }
                }
            } catch (Exception e) {
                logger.info("Unable to delete configuration file", e);
            }
        }
    }

    private void convert(Map<String, Object> map, PropertiesValues propertiesValues, Map<String, Object> previous) {
        map.clear();

        List<String> keys = new ArrayList<>(propertiesValues.getKeys());
        if (previous != null) {
            List<String> previousKeys = new ArrayList<>(previous.keySet());
            keys.sort(Comparator.comparing(previousKeys::indexOf));
        }

        for (String key : keys) {
            if (propertiesValues.getProperty(key) != null) {
                map.put(key, propertiesValues.getProperty(key));
            } else if (propertiesValues.getList(key).getSize() > 0) {
                ArrayList<Object> item = new ArrayList<>();
                convert(item, propertiesValues.getList(key), previous != null ? (List<Object>) previous.get(key) : null);
                map.put(key, item);
            } else if (!propertiesValues.getValues(key).getKeys().isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                convert(item, propertiesValues.getValues(key), previous != null ? (Map<String, Object>) previous.get(key) : null);
                map.put(key, item);
            }
        }
    }

    private void convert(List<Object> list, PropertiesList propertiesList, List<Object> previousList) {
        list.clear();
        for (int i = 0; i < propertiesList.getSize(); i++) {
            if (propertiesList.getProperty(i) != null) {
                list.add(propertiesList.getProperty(i));
            } else if (propertiesList.getList(i).getSize() > 0) {
                ArrayList<Object> item = new ArrayList<>();
                convert(item, propertiesList.getList(i), null);
                list.add(item);
            } else if (!propertiesList.getValues(i).getKeys().isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                convert(item, propertiesList.getValues(i), null);
                list.add(item);
            }
        }
    }

    boolean shouldSaveConfig() {
        String str = this.context.getProperty(DirectoryWatcher.ENABLE_CONFIG_SAVE);
        if (str == null) {
            str = this.context.getProperty(DirectoryWatcher.DISABLE_CONFIG_SAVE);
        }
        if (str != null) {
            return Boolean.parseBoolean(str);
        }
        return true;
    }

    ConfigurationAdmin getConfigurationAdmin() {
        return configAdmin;
    }

    /**
     * Set the configuration based on the config file.
     *
     * @param f Configuration file
     * @return <code>true</code> if the configuration has been updated
     * @throws Exception
     */
    boolean setConfig(final File f) throws Exception {
        final Hashtable<String, Object> ht = new Hashtable<>();
        try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            Map<String, Object> m = yamlMapper.readValue(in, new TypeReference<Map<String, Object>>() {
            });
            flatten(ht, "", m);
        }

        String[] pid = parsePid(f.getName());
        Configuration config = getConfiguration(toConfigKey(f), pid[0], pid[1]);

        Dictionary<String, Object> props = config.getProperties();
        Hashtable<String, Object> old = props != null ? new Hashtable<>(getMap(props)) : null;
        if (old != null) {
            old.remove(DirectoryWatcher.FILENAME);
            old.remove(Constants.SERVICE_PID);
            old.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
        }

        if (!ht.equals(old)) {
            ht.put(DirectoryWatcher.FILENAME, toConfigKey(f));
            if (old == null) {
                logger.info("Creating configuration from " + pid[0] + (pid[1] == null ? "" : "-" + pid[1]) + ".yml");
            } else {
                logger.info("Updating configuration from " + pid[0] + (pid[1] == null ? "" : "-" + pid[1]) + ".yml");
            }
            config.update(ht);
            return true;
        } else {
            return false;
        }
    }

    private void flatten(Map<String, Object> builder, String key, Map<String, ?> m) {
        for (Map.Entry<String, ?> entry : m.entrySet()) {
            flatten(builder, (key.isEmpty() ? key : (key + '.')) + entry.getKey(), entry.getValue());
        }
    }

    private void flatten(Map<String, Object> builder, String key, List<?> m) {
        int i = 0;
        for (Object value : m) {
            flatten(builder, key + '[' + (i++) + ']', value);
        }
    }

    private void flatten(Map<String, Object> builder, String key, Object value) {
        if (value instanceof Map) {
            flatten(builder, key, (Map<String, ?>) value);
        } else if (value instanceof List) {
            flatten(builder, key, (List<?>) value);
        } else if (value != null) {
            builder.put(key, value.toString());
        }
    }

    private Map<String, String> getMap(Dictionary<String, ?> d) {
        Map<String, String> m = new HashMap<>();
        Enumeration<String> en = d.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            if (!key.startsWith("felix.") && !key.startsWith("service.")) {
                m.put(key, d.get(key).toString());
            }
        }
        return m;
    }

    /**
     * Remove the configuration.
     *
     * @param f File where the configuration in was defined.
     * @return <code>true</code>
     * @throws Exception
     */
    boolean deleteConfig(File f) throws Exception {
        String[] pid = parsePid(f.getName());
        logger.info("Deleting configuration from " + pid[0] + (pid[1] == null ? "" : "-" + pid[1]) + ".yml");
        Configuration config = getConfiguration(toConfigKey(f), pid[0], pid[1]);
        config.delete();
        return true;
    }

    String toConfigKey(File f) {
        return f.getAbsoluteFile().toURI().toString();
    }

    File fromConfigKey(String key) {
        return new File(URI.create(key));
    }

    String[] parsePid(String path) {
        String pid = path.substring(0, path.lastIndexOf('.'));
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]{pid, factoryPid};
        } else {
            return new String[]{pid, null};
        }
    }

    Configuration getConfiguration(String fileName, String pid, String factoryPid)
            throws Exception {
        Configuration oldConfiguration = findExistingConfiguration(fileName);
        if (oldConfiguration != null) {
            return oldConfiguration;
        } else {
            Configuration newConfiguration;
            if (factoryPid != null) {
                newConfiguration = getConfigurationAdmin().createFactoryConfiguration(pid, "?");
            } else {
                newConfiguration = getConfigurationAdmin().getConfiguration(pid, "?");
            }
            return newConfiguration;
        }
    }

    Configuration findExistingConfiguration(String fileName) throws Exception {
        String filter = "(" + DirectoryWatcher.FILENAME + "=" + escapeFilterValue(fileName) + ")";
        Configuration[] configurations = getConfigurationAdmin().listConfigurations(filter);
        if (configurations != null && configurations.length > 0) {
            return configurations[0];
        } else {
            return null;
        }
    }

    private String escapeFilterValue(String s) {
        return s.replaceAll("[(]", "\\\\(").
                replaceAll("[)]", "\\\\)").
                replaceAll("[=]", "\\\\=").
                replaceAll("[\\*]", "\\\\*");
    }

}
