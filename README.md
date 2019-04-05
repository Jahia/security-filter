# API Security service and filter

This module provides a service and a render filter which can be fed by different API providers to secure access. A centralized
configuration file provides rules for APIs associating an operation to a required permission.

## Permission configuration

API can be restricted based on permissions by adding `org.jahia.modules.api.permissions-*.cfg` files into `digital-factory-data/karaf/etc`.
These files contain a list of rules which will be checked for each node potentially returned by the API. Rules coming from all files are merged and sorted. Rules can be based on path, node type and/or workspace. 

A permission check is always done on a JCR node, and is associated with an API name. Different API names are provided for 
json views, the JCRest API module, or the GraphQL API.

The first rule that matches defines the permission that will be checked on the node to ensure the user is correctly allowed to use the API on it. 

A permission rule is defined by a list of entries of the following format :

```
permission.<rulename>.<property>=<value>
```

Where `property` will define the kind of access and the matching criteria for this rule. 

A rule can define at most one of the following properties : `access`, `requiredPermission`, `requiredScope`. `access` can take 2 different values : 
- `denied` : Forbidden for everybody
- `restricted` : Only allowed for users who have the `api-access` permission on the node

The check can be defined more precisely by using `requiredPermission` instead : the value is the name of the permission that will be checked if the rule matches. The user must have this permission to be granted access.

Instead of a permission, a rule can specify a `requiredScope`. Scope can be granted to a request through tokens passed in `Authorization` header (see Scope and tokens below). The access will be granted if and only if the scope is owned by the request.

By default, if none of these parameters are passed, the API is granted - no other rules will be tested, and no permission needs to be checked.

Rules can also define matching criterias with the following properties :

A rule can specify any number of matching criteria. Each of these criteria can have a single value or a comma separated list of values.
 - `api` : The names of the API, if the rule should only apply to some entry points
 - `pathPattern` : Regular expressions that will be tested on the node path.
 - `workspace` : `live` or `default`, only request on the specified workspace will match.
 - `nodeType` : Only request on nodes of these type will match.
 - `scope` : Only request claiming that scope will match. 
 - `permission` : Only request with users having this permission will match.
 
Note the difference between `requiredPermission`/`requiredScope` and `permission`/`scope` - in the "non-required" case, if the user has the permission/scope, his access will be granted (based on `access`, `requiredPermission`, `requiredScope` values), and no other rule will be checked. 
But it's not required - if he does not have it, the rule won't match, and so subsequent rules will be checked. 
In the "required" case, if the rule match, the user must have the permission/scope - no other rule will be checked. 

An optional `priority` property can be also specified - the rules with the lowest value will be executed first. Default priority is 0.

```
permission.checkFirst.priority=-10
```

A rule with no condition will always match (and thus, should have a very high value for priority so that it is checked last):
```
permission.global.access=restricted
permission.global.priority=99999
```

Rules can specify a simple path pattern : 
```
permission.users.pathPattern=/sites/[^/]+/home/.*
```

Or a combination of all criterias - if all conditions must match (including the permissionn), access will be granted
```    
permission.digitallPostsInLive.pathPattern=/sites/digitall/.*
permission.digitallPostsInLive.nodeType=jnt:post,jnt:message
permission.digitallPostsInLive.workspace=live
permission.digitallPostsInLive.permission=jcr:write
```

This rule will grant full access for all users claiming one of the scopes scope1, scope2 or scope3
```
permission.jwtAccess.scope=scope1,scope2,scope3
```

This rule will require myPermission for all graphql call on /sites/secure
```
permission.secure.api=graphql
permission.secure.pathPattern=/sites/secure/.*
permission.secure.requiredPermission=myPermission
```

### Permissions in a module

A module can package a configuration file in META-INF/configurations folder. Since DX version 7.2.2.0, all `cfg` files in this folder are deployed in `karaf/etc` at module startup. This gives the possibility for a module to provide a defaut configuration file that can be edited by the user. The files are never updated nor removed automatically. The file name can contain the name of the module : `org.jahia.modules.api.permissions-<modulename>.cfg`.

## Scope and tokens

A request can be granted a scope through the usage of tokens, passed in the `Authentication: Bearer` header. 

Security filter currently only support signed JWT token. Tokens contain a verified list of scopes, along with restriction on its usage. 
It's possible to restrict the usage of a token based on the client IP or referer header.

### Configuration

Tokens can be generated via the tools section "jwtConfiguration" - the user can specify the list of scopes that will be owned by the token, and fill in the optional restrictions. 
You must customize org.jahia.modules.jwt.token.cfg configuration file before generating any token. 
The file contains the following properties :

- `jwt.issuer` : Name of your organization, that will be included in tokens, only for informational purpose
- `jwt.audience` : The target audience is an identifier for you DX installation - audience is included in the token at generation, and only tokens with the same audience will be accepted.
- `jwt.algorithm` : Algorithm used to sign the token. Only HMAC supported.
- `jwt.secret` : Secret key used to be used with HMAC. It will be used to sign and validate tokens. You must change the secret and keep it safe - any token signed with the same secret can be accepted and will grant the associated scopes.

### JWT example

The getaway app is an example of SPA, accessing specific data through GraphQL. The code can be found on github: [getaway-dx-module](https://github.com/Jahia/getaway-dx-module) and [getaway-reactjs-app](https://github.com/Jahia/getaway-reactjs-app). 

The module first defines different types, that need to be accessible by the react SPA. The [CND file](https://github.com/Jahia/getaway-dx-module/blob/master/src/main/resources/META-INF/definitions.cnd) contains definitions for `gant:destination` , `gant:highlightedLandmarks`

A [configuration file](https://github.com/Jahia/getaway-dx-module/blob/master/src/main/resources/META-INF/configurations/org.jahia.modules.api.permissions-getaway.cfg) will give access
for nodes with these types and `jmix:image`, when they are in `/sites/getaway/contents` and `/sites/getaway/files`. They will be accessible by the `jcr.nodesByQuery` graphql endpoint 
for bearers of the scope `getaway` :

```
permission.getaway.api=graphql.Query.jcr,graphql.JCRQuery.nodesByQuery
permission.getaway.scope=getaway
permission.getaway.nodeType=gant:destination, gant:highlightedLandmarks, jmix:image
permission.getaway.pathPattern=/sites/[^/]+/contents/.*, /sites/[^/]+/files/.*
```

In order for the rule to apply and grant these access, the client will have to provide a valid token containing the corresponding `scope` claim.

The `jwtConfiguration` tool will be used to generate the token. Scope is `getaway`, and we will add more restrictions on the referer field, so that the token can only be used when being used from a site on `http://localhost` or `http://127.0.0.1` .

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

## Render filter

A render filter catches all ajax calls to `*.json` and `*.html.ajax`. The filter calls the service to check if the request is allowed or not.
The API name contains the template type and the name of the view itself : `view.<template type>.<view name>`

So for example, the following rule will apply to all requests on the tree.json view :

```
permission.tree.api=view.json.tree
```

The following rule will match all json views on pages :

```
permission.tree.api=view.json
permission.tree.nodeType=jnt:page
```

## Module

A default configuration file is provided by this module. On DX 7.2.2+ , the configuration file will be copied automatically to `karaf/etc` 
once the module is started. An existing file won't be overwritten, and the configuration is never automatically removed.
On older versions of DX, the configuration file need to be deployed and maintained manually.

