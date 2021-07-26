# API Security service and filter

This module protects specific API (graphql/rest/views, and others) from unauthorized usage, potential XSS/CSRF attacks 
and provide support for CORS requests.

It prevents API from being called from anywhere, first in a generic way through a global CORS filter, then per API configuration.

It also allows users to specify reduced scopes when using tokens. They can choose what API can be called with a token.

The module provides a service which can be used by different API providers to secure access. 

A centralized configuration file provides rules defining how the APIs can be called.

## Authorization configuration

The configuration is made up of a list of scopes. Each scope is granting some API access, based on API name, node path or types, 
or any other criteria that can be used by APIs.

Scopes are granted and associated to a request with an explicit token, or automatic rules. 
Personal API token or JWT token can hold scopes. For example you can grant a token to get server status, but not to undeploy a module.
Scopes can be automatically granted based on browser origin : some scopes are granted when called by same origin, or from a trusted origin.

They can be restricted to some specific user profiles : some scopes are available only to administrators, editors or privileged users

If the request do not hold any scope granting the requested API, access will be denied.

The configuration files are located in `digital-factory-data/karaf/etc`, with the `org.jahia.modules.api.authorization-*.(yml|cfg)` filename pattern.
Starting from Jahia 8.1.0.0 you can write configuration in yaml format - for older versions, you must use cfg format. 

The following snippet :

```yaml
graphql:
  description: Can access graphql API
  metadata:
    visible: true
  auto_apply:
    - origin: hosted
  grants:
    - api: graphql
      node: none
    - api: graphql
      node:
        withPermission: api-access
```

will be written this way when using cfg format : 
```properties
graphql.description = Can access graphql API
graphql.metadata.visible = true
graphql.auto_apply[0].origin = hosted
graphql.grants[0].api = graphql
graphql.grants[0].node = none
graphql.grants[1].api = graphql
graphql.grants[1].node.withPermission = api-access
```

Examples below are given in yaml format.

### Scope name, description and metadata

Every scope must have a unique name. The description explains what the scope is granting. 
Metadata can be freely added and used by UI or other services.

### Scope grants

A scope contains a list of grants, one for each API access. 

- `api` : The names of the API (in a comma separated list), if the rule should only apply to some entry points.
  Different API names are provided by the API services. For example : ajax views (`view.<view-type>`), the JCRest API module (`jcrestapi`), or the GraphQL API (`graphql.<gql-type>.<gql-field>`)
  
    ```yaml
       grants: 
         - api: graphql.JcrNode, graphql.JcrProperty
    ```

    You can also define `include` and `exclude` sub entries :

    ```yaml
       grants:
         - api:
             include: graphql
             exclude: graphql.GqlAdmin, graphql.JcrNode
    ```
    
- `node` : Matches the API calls related to a node. You can specify `node: none` to only match API calls that do *not* return a node. 
  To match some nodes, you can use the following sub entries :
    - `pathPattern` / `excludedPathPattern` : Regular expressions that will be tested on the node path.
    - `workspace` : `live` or `default`, only request on the specified workspace will match.
    - `nodeType` / `excludedNodeType` : Only request on nodes of these type will match.
    - `withPermission` : Only request on nodes, where the user has this permission, will match.

    ```yaml
       grants:
         - node:
             pathPattern: /,/sites(/.*)?,/modules(/.*)?,/mounts(/.*)?
             excludedPathPattern: /sites/[^/]+/users(/.*)?
    ```

### Auto-apply rules

Scope can be automatically applied to requests based on an origin. It's checked against the `Origin` and `Referer` headers. 
`hosted` or `same` mean that the rule will match if the request is coming from the same server.

```yaml
   auto_apply:
     - origin: hosted
     - origin: http://www.mysite.com
```

It's also possible to always apply the scope, whatever the request is. It can be used to have API that will always be granted.

```yaml
   auto_apply:
     - always: true
```

### User constraints

Some scopes are only usable by specific users. You can set which permission a user should have on a node :

```yaml
   constraints:
     - user_permission: manageModules
       path: /sites
       workspace: live
```

Or simply restrict the scope to privileged users :

```yaml
   constraints:
     - privileged_user: true
```

The scope will be available only to users who fulfill the constraints. It will never be applied for other users.

### Configuration profiles 

The user can choose a predefined security profile by setting a value in `security.profile`, in `org.jahia.modules.api.security.cfg` file

- "default" profile is recommended one. It will not allow any API call from external origin, and from non-privileged users.
- "compat" profile is more open and is compatible with the previous security-filter implementation. Most graphql/rest calls are allowed for any user
- "open" profile allows every call.

It's also possible to not use any profile (everything will be denied by default) - you will have to fully provide your own configuration. Without any configuration Jahia GUI will not be work.

### Legacy mode and migration report

The legacy mode can be used to keep the exact same behaviour as the previous version. It can be enabled by setting `security.legacyMode=true` into `org.jahia.modules.api.security.cfg`.
The old `org.jahia.modules.api.permissions-*.cfg` files will be used as before.

Reporting in the logs can be enabled with `security.migrationReporting=true`, to check what API call that was allowed with legacy mode, will be denied with the new configuration, or the opposite.
This is useful when migrating, if you were using API calls and you are unsure if they will still pass. 
Reporting can also be enabled when running in standard mode - it will continue to report when there's a difference between legacy and standard mode.

### Configuration in a module

A module can package a configuration file in META-INF/configurations folder. Since DX version 7.2.2.0, all  files in this folder are deployed in `karaf/etc` at module startup.

### Extending existing scope

It's possible to extends an existing scope in another configuration file, in order to add grants or auto-apply rules.
You just need to redeclare the scope, and the list of grants/rules you want to add :

```yaml
graphql:
  auto_apply:
    - origin: http://www.mytrusted-origin.com
```

## Personal API token

Personal API token can hold scopes and can be used to make an API call. More documentation can be found at [personal-api-tokens](`https://github.com/Jahia/personal-api-tokens`)

## JWT tokens

A request can be granted a scope through the usage of JWT tokens, passed in the `Authentication: Bearer` header. 

JWT Tokens contain a verified list of scopes, along with restriction on its usage. 
It's possible to restrict the usage of a token based on the client IP or referer header.

### Configuration

Tokens can be generated via the tools section "jwtConfiguration" - the user can specify the list of scopes that will be owned by the token, and fill in the optional restrictions. 
You must customize `org.jahia.modules.jwt.token.cfg` configuration file before generating any token. 
The file contains the following properties :

- `jwt.issuer` : Name of your organization, that will be included in tokens, only for informational purpose
- `jwt.audience` : The target audience is an identifier for you DX installation - audience is included in the token at generation, and only tokens with the same audience will be accepted.
- `jwt.algorithm` : Algorithm used to sign the token. Only HMAC supported.
- `jwt.secret` : Secret key used to be used with HMAC. It will be used to sign and validate tokens. You must change the secret and keep it safe - any token signed with the same secret can be accepted and will grant the associated scopes.

### JWT example

A module can expose a scope, that will be granted with JWT token. 
In order for the scope to be applied, the client will have to provide a valid token containing the corresponding `scope` claim.

The `jwtConfiguration` tool will be used to generate the token. In this example, scope is `getaway`, and we will add more restrictions on the referer field, so that the token can only be used when being used from a site on `http://localhost` or `http://127.0.0.1` .

Generated token will look like that :
`eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwOi8vamFoaWEuY29tIiwic3ViIjoiYXBpIHZlcmlmaWNhdGlvbiIsInJlZmVyZXIiOlsiaHR0cDovLzEyNy4wLjAuMSIsImh0dHA6Ly9sb2NhbGhvc3QiXSwiaXNzIjoiZHgiLCJzY29wZXMiOlsiZ2V0YXdheSJdLCJpYXQiOjE1Mzg0NjU3NjQsImp0aSI6ImJiNjUyYmI2LTVlOGUtNGRmZC1hYjI3LWRlYzY4NWQxZmVmYiJ9.YolJyuSXGlvIN9_hL4eH6D9_oFHKwt005y3vfCuR2ZU`

The content of the token can be verified on [jwt.io](https://jwt.io/) :

```json
{
  "aud": "http://jahia.com",
  "sub": "api verification",
  "referer": [
    "http://127.0.0.1",
    "http://localhost"
  ],
  "iss": "dx",
  "scopes": [
    "getaway"
  ],
  "iat": 1538465764,
  "jti": "bb652bb6-5e8e-4dfd-ab27-dec685d1fefb"
}
```

The claims `aud` and `iss` are coming from the configuration file. You can also check the signature on [jwt.io](https://jwt.io/) - here the token is signed with the default key `my super secret secret`. It must match the secret in the configuration file.
`iat` is the date of issue, and `jti` is a unique token identifier. They could be used to set an expiration time or manually revoke a specific token, although the current implementation does not support it yet.

Finally, the application will add the token to its `Authentication: Bearer` header, as in [index.js](https://github.com/Jahia/getaway-reactjs-app/blob/master/src/index.js) . 

## Checking API authorization

### Graphql

Graphql provider use the security-filter service to check every field access. 
The API name is built from the graphql type and the requested field : `graphql.<gql-type>.<gql-field>`.

When a graphql field returns a JCR node or a list of JCR nodes, it filters the result based on API authorization on these nodes.

### JCRest API 

JCRest API filters all result based on security-filter configuration. API name is `jcrestapi.<query-type>`.

### Views

A render filter catches all ajax calls to `*.json` and `*.html.ajax`. The filter calls the service to check if the request is allowed or not.
The API name contains the template type and the name of the view itself : `view.<template type>.<view name>`

So for example, the following rule will apply to all requests on the tree.json view :

```yaml
 - api: view.json.tree
```

The following rule will match all json views on pages :

```yaml
 - api: view.json
   node: 
     nodeType: jnt:page
```

### Adding API checks to your API

The module exposes an OSGi service implementing `org.jahia.modules.securityfilter.PermissionService`. 
In order to check an API call, you should call the `hasPermission` method, with a `query` map parameter. 
The query map contains information that describes your API call, and will be tested against the different `grants` :

- It must at least contain the `api` entry, with a string describing it in a dot-separated fashion : `my-api.type.sub-type`. It is tested by the `ApiGrant` class.
- It can optionally contains a `node` entry, with a `JCRNodeWrapper` value. This one is tested by the `NodeGrant` class.

Other `Grant` implementations may check other entries.

## CORS Filter

Security-filter module embeds a global CORS filter. It is based on tomcat implementation, and can use all configuration settings described here :
[CORS Filter](https://tomcat.apache.org/tomcat-9.0-doc/config/filter.html#CORS_Filter). 
These settings must be set in the `org.jahia.modules.api.security.cfg` file. 
