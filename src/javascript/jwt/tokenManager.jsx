import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import { Query } from 'react-apollo';
import { getTokensQuery } from './gqlQueries';
import { deleteToken, addToken, modifyToken} from './gqlMutations';
import DxContext from '../DxContext';
import { Mutation } from 'react-apollo';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import { Delete } from "@material-ui/icons";
import { Add } from "@material-ui/icons";

const styles = {
    root: {
        flexGrow: 1,
    },
};

class TokenManager extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        const { dxContext } = this.props;
        const path = `/modules/security-filter/${dxContext.moduleVersion}/templates/contents/security-filter-jwt/tokens`;
        return <Query query={getTokensQuery} variables={{path: path}}>
            {({loading, error, data}) => {
                if (error) {
                    console.error(error);
                }

                if (!loading) {
                    const token = data.jcr.nodeByPath.children.nodes[0];
                    if (token === undefined) {
                        return this.addToken();
                    }

                    return <div>
                        <p>
                            <em>{ token.token.value }</em>
                            { this.removeToken() }
                        </p>
                    </div>;
                }
                return "Retrieving token ..."
            }}
        </Query>

    }

    removeToken() {
        const { dxContext } = this.props;
        const path = `/modules/security-filter/${dxContext.moduleVersion}/templates/contents/security-filter-jwt/tokens`;
        return <Mutation
            mutation={ deleteToken }
            refetchQueries={[{
                query: getTokensQuery,
                variables: {path: path}
            }]}>
            {(removeToken) => {
                return <Tooltip title={ "Remove token" } placement="top-start">
                    <IconButton onClick={ () => removeToken({ variables: { pathOrId: path + "/jwt-token" }}) }><Delete /></IconButton>
                </Tooltip>
            }}
        </Mutation>
    }

    addToken() {
        const { dxContext } = this.props;
        const path = `/modules/security-filter/${dxContext.moduleVersion}/templates/contents/security-filter-jwt/tokens`;
        return <Mutation
            mutation={ addToken }
            refetchQueries={[{
                query: getTokensQuery,
                variables: {path: path}
            }]}>
            {(addToken) => {
                return <Tooltip title={ "Add token" } placement="top-start">
                    <IconButton onClick={ () => addToken({ variables: { pathOrId: path + "/jwt-token" }}) }><Add /></IconButton>
                </Tooltip>
            }}
        </Mutation>
    }
}

export default withStyles(styles)(TokenManager);