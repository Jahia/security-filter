package org.jahia.modules.securityfilter.api.factories;

import org.glassfish.hk2.api.Factory;
import org.jahia.modules.securityfilter.api.SpringBeansAccess;
import org.jahia.services.content.JCRTemplate;

/**
 * Provider for JCRTemplate
 */
public class JCRTemplateFactory implements Factory<JCRTemplate> {

    @Override
    public JCRTemplate provide() {
        return SpringBeansAccess.getInstance().getJcrTemplate();
    }

    @Override
    public void dispose(JCRTemplate instance) {
        // nothing
    }
}
