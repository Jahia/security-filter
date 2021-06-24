package org.jahia.modules.securityfilter.core.apply;

import org.jahia.services.modulemanager.util.PropertiesValues;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;

public class AlwaysAutoApply implements AutoApply {

    public static AutoApply build(PropertiesValues values) {
        String value = values.getProperty("always");
        if (Boolean.parseBoolean(value)) {
            return new AlwaysAutoApply();
        }

        return null;
    }

    public AlwaysAutoApply() {
    }

    @Override
    public boolean shouldApply(HttpServletRequest request) {
        return true;
    }
}
