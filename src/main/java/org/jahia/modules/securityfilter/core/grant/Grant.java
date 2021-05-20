package org.jahia.modules.securityfilter.core.grant;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.modulemanager.util.PropertiesValues;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Grant {

    boolean matches(Map<String, Object> query);
}
