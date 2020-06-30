package org.jahia.modules.securityfilter.api;



import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.jahia.modules.securityfilter.api.factories.JCRTemplateFactory;
import org.jahia.services.content.JCRTemplate;


public class SecurityFilterApiApplication extends ResourceConfig{
    public SecurityFilterApiApplication() {
        this(JCRTemplateFactory.class);
    }

    SecurityFilterApiApplication(final Class<? extends Factory<JCRTemplate>> jcrTemplateFactoryClass) {
        super(SecurityFilterAPI.class,
                jcrTemplateFactoryClass);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(jcrTemplateFactoryClass).to(JCRTemplate.class);
            }
        });
    }
}
