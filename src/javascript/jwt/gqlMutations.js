import gql from "graphql-tag";

const addToken = gql`mutation addToken($pathOrId: String!, $tokenName: String!, $token: String!, $claims: String!) {
  jcr {
    addNode(parentPathOrId:$pathOrId, name:$tokenName, primaryNodeType:"sfnt:token") {
      token:mutateProperty(name:"token") {
        setValue(value:$token)
      }
      claims:mutateProperty(name:"claims") {
        setValue(value:$claims)
      }
    }
  }
}`;

const deleteToken = gql`mutation deleteToken($path: String!) {
    deleteJWTToken (path:$path) 
}`;

const modifyToken = gql`mutation modifyToken($pathOrId: String!, $token: String!, $claims: String!) {
    jcr {
        mutateNode(pathOrId:$pathOrId) {
          token:mutateProperty(name:"token") {
            setValue(value:$token)
          }
          claims:mutateProperty(name:"claims") {
            setValue(value:$claims)
          }
        }
    }
}`;

const createOrModifyToken = gql`mutation createToken($scopes: [String]!, $referers: [String], $ips:[String]) {
    jwtToken(scopes:$scopes, referers: $referers, ips: $ips) {
        token
    }
}`;

export { addToken, deleteToken, modifyToken, createOrModifyToken };