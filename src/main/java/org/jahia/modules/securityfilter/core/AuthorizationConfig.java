package org.jahia.modules.securityfilter.core;

import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.*;

public class AuthorizationConfig implements ManagedServiceFactory {

    @Override
    public String getName() {
        return "API Security configuration (new)";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        PropertiesManager pm = new PropertiesManager(getMap(properties));
        // read the rules
        System.out.println(pm.getValues());
    }

    @Override
    public void deleted(String pid) {

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

}
