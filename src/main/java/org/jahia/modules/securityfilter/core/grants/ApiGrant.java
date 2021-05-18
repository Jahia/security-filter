package org.jahia.modules.securityfilter.core.grants;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.modulemanager.util.PropertiesValues;

import java.util.*;

public class ApiGrant implements Grant {
    private Set<String> apis = Collections.emptySet();

    public static Grant build(PropertiesValues grantValues) {
        String api = grantValues.getProperty("api");
        if (api == null) {
            return null;
        }

        ApiGrant apiGrant = new ApiGrant();
        apiGrant.setApis(new LinkedHashSet<>(Arrays.asList(StringUtils.split(api, ", "))));
        return apiGrant;
    }


    public void setApis(Set<String> apis) {
        this.apis = apis;
    }

    @Override
    public boolean matches(Map<String, Object> query) {
        String apiToCheck = (String) query.get("api");
        if (apiToCheck == null) {
            return false;
        }
        for (String api : apis) {
            if (api.equals(apiToCheck) || apiToCheck.startsWith(api + ".")) {
                return true;
            }
        }

        return false;
    }
}
