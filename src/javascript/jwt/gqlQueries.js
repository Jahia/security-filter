import gql from "graphql-tag";

//"/modules/security-filter/1.0.2-SNAPSHOT/templates/contents/security-filter-jwt/tokens"
const getTokensQuery = gql`query getTokens($path: String!) {
    jcr {
    nodeByPath(path:$path) {
      children {
        nodes {
          token:property(name:"token") {
            value
          }
          claims:property(name:"claims") {
            value
          }
          name:property(name:"j:nodename") {
            value
          }
        }
      }
    }
  }
}`;
export { getTokensQuery }