package org.jahia.modules.securityfilter.accesskey.api.graphql.mutations;

import graphql.annotations.annotationTypes.*;
import org.jahia.exceptions.JahiaUnauthorizedException;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.securityfilter.accesskey.AccessKey;
import org.jahia.modules.securityfilter.accesskey.AccessKeyService;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLMutation
@GraphQLDescription("Create/Remove/Update access key")
public class AccessKeyMutationExtension {
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyMutationExtension.class);

    @GraphQLField
    @GraphQLDescription("Generate a new access key")
    public static AccessKey createAccessKey(@GraphQLName("apis") @GraphQLDescription("") List<String> apis,
                                            @GraphQLName("referer") @GraphQLDescription("") List<String> referer,
                                            @GraphQLName("ips") @GraphQLDescription("") List<String> ips,
                                            @GraphQLName("nodeTypes") @GraphQLDescription("") List<String> nodeTypes,
                                            @GraphQLName("paths") @GraphQLDescription("") List<String> paths,
                                            @GraphQLName("workspaces") @GraphQLDescription("") List<String> workspaces,
                                            @GraphQLNonNull @GraphQLName("type") @GraphQLDescription("") String type) throws RepositoryException, IOException {
        System.out.println("Create");

        AccessKeyService accessKeyService = BundleUtils.getOsgiService(AccessKeyService.class, null);
        AccessKey accessKey = new AccessKey();

        if (!apis.isEmpty()) accessKey.setApis(apis);
        if (!referer.isEmpty()) accessKey.setReferrers(referer);
        if (!ips.isEmpty()) accessKey.setIps(ips);
        if (!nodeTypes.isEmpty()) accessKey.setNodeTypes(nodeTypes);
        if (!paths.isEmpty()) accessKey.setPaths(paths);

        accessKey.setType(type);
        accessKey.setWorkspaces(workspaces);

        return accessKeyService.createAccessKey(accessKey);
    }

    @GraphQLField
    @GraphQLDescription("Generate a root access key")
    public static AccessKey createRootAccessKey(@GraphQLName("key") @GraphQLDescription("") String key,
                                            @GraphQLNonNull @GraphQLName("type") @GraphQLDescription("") String type) throws RepositoryException, IOException {
        System.out.println("Create root");

        AccessKeyService accessKeyService = BundleUtils.getOsgiService(AccessKeyService.class, null);
        AccessKey accessKey = new AccessKey();
        accessKey.setType(type);
        accessKey.setKey(key);
        AccessKey k = accessKeyService.createRootAccessKey(accessKey);

        if (k == null) {
            throw new DataFetchingException("Root key already exists");
        }

        return  k;
    }

    @GraphQLField
    @GraphQLDescription("Remove access key")
    public static Boolean removeAccessKey(@GraphQLNonNull @GraphQLName("accessId") @GraphQLDescription("") String accessId) throws RepositoryException {
        System.out.println("Remove");
        AccessKeyService accessKeyService = BundleUtils.getOsgiService(AccessKeyService.class, null);
        AccessKey accessKey = new AccessKey();
        accessKey.setAccessId(accessId);
        return accessKeyService.destroyAccessKey(accessKey);
    }

    @GraphQLField
    @GraphQLDescription("Activate access key")
    public static Boolean activateAccessKey(@GraphQLNonNull @GraphQLName("accessId") @GraphQLDescription("") String accessId) throws RepositoryException {
        System.out.println("Activate");
        AccessKeyService accessKeyService = BundleUtils.getOsgiService(AccessKeyService.class, null);
        AccessKey accessKey = new AccessKey();
        accessKey.setAccessId(accessId);
        return accessKeyService.activateAccessKey(accessKey);
    }

    @GraphQLField
    @GraphQLDescription("Deactivate access key")
    public static Boolean deactivateAccessKey(@GraphQLNonNull @GraphQLName("accessId") @GraphQLDescription("") String accessId) throws RepositoryException {
        System.out.println("Deactivate");
        AccessKeyService accessKeyService = BundleUtils.getOsgiService(AccessKeyService.class, null);
        AccessKey accessKey = new AccessKey();
        accessKey.setAccessId(accessId);
        return accessKeyService.deactivateAccessKey(accessKey);
    }
}
