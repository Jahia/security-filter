package org.jahia.modules.securityfilter.accesskey.api.graphql.queries;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.securityfilter.accesskey.AccessKey;
import org.jahia.services.content.*;
import org.jahia.services.usermanager.JahiaUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLMutation
@GraphQLDescription("Get access keys")
public class AccessKeyQueryExtension {
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyQueryExtension.class);

    @GraphQLField
    @GraphQLDescription("Get access keys")
    public static List<AccessKey> getAccessKeys() throws RepositoryException {
        System.out.println("Get");

        JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser();

        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<List<AccessKey>>() {
            @Override
            public List<AccessKey> doInJCR(JCRSessionWrapper session) throws RepositoryException {
                List<AccessKey> list = new ArrayList<>();
                QueryManager qm = session.getWorkspace().getQueryManager();
                StringBuilder query = new StringBuilder("SELECT * FROM [sfnt:accessKey] as key");

                if (!"root".equals(user.getUsername())) {
                    query.append( " WHERE key.[user] = '" + user.getProperty("jcr:uuid") + "'");
                }

                query.append(" ORDER BY [jcr:lastModified] DESC");
                Query q = qm.createQuery(query.toString(), Query.JCR_SQL2);
                NodeIterator ni = q.execute().getNodes();


                while (ni.hasNext()) {
                    JCRNodeWrapper key = (JCRNodeWrapper) ni.next();
                    AccessKey accessKey = new AccessKey();
                    accessKey.setAccessId(key.getPropertyAsString("accessId"));
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

                    list.add(accessKey);
                }

                return list;
            }
        });
    }

    private static List<String> getValues(JCRPropertyWrapper prop) throws RepositoryException {
        return Arrays.stream(prop.getValues()).map(v -> {
                    try {
                        return v.getString();
                    } catch (Exception e) {
                        //
                    }
                    return null;
                }).collect(Collectors.toList());
    }
}
