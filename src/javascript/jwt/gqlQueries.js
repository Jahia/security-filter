import gql from "graphql-tag";

//"/modules/security-filter/1.0.2-SNAPSHOT/templates/contents/security-filter-jwt/tokens"
const getTokensQuery = gql`query getTokens($path: String!) {
    existingJWTToken(path: $path) {
        token
        claims
    }
}`;

const isAuthorizedQuery = gql`query isAuthorized {
    isAuthorized:authorized
}`;
export { getTokensQuery, isAuthorizedQuery}