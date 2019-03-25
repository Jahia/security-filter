import gql from "graphql-tag";

const createOrModifyToken = gql`mutation createToken($scopes: [String]!, $referer: [String], $ips:[String]) {
    jwtToken(scopes:$scopes, referer: $referer, ips: $ips) {
        token
        claims
    }
}`;

export { createOrModifyToken };