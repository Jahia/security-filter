import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import { Query } from 'react-apollo';
import { getTokensQuery } from './gqlQueries';
import { deleteToken, addToken, modifyToken, createOrModifyToken } from './gqlMutations';
import { Mutation } from 'react-apollo';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import { Delete } from "@material-ui/icons";
import { Add } from "@material-ui/icons";
import { Edit } from "@material-ui/icons";
import TokenEditor from './tokenEditor';

const styles = {
    root: {
        flexGrow: 1,
        padding: 30
    },
    textArea: {
        width: 500,
        height: 100
    }
};

class TokenManager extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            showEditCreateDialog: false
        };
        this.toggleDialog = this.toggleDialog.bind(this);
    }

    render() {
        const { dxContext, classes } = this.props;
        const path = `/modules/security-filter/${dxContext.moduleVersion}/templates/contents/security-filter-jwt/tokens`;
        return(
            <Query query={getTokensQuery} variables={{path: path}}>
            {({loading, error, data}) => {
                if (error) {
                    console.error(error);
                }

                if (!loading) {
                    const token = data.jcr.nodeByPath.children.nodes[0];
                    if (token === undefined) {
                        return this.addToken();
                    }

                    return <div className={ classes.root }>
                        <TokenEditor open={ this.state.showEditCreateDialog }
                                     close={ this.toggleDialog }
                                     { ...this.getClaims(token) }/>
                        <p>
                            <textarea className={ classes.textArea } value={token.token.value} disabled={ true }/>
                            { this.removeToken() }
                            { this.editButton() }
                        </p>
                    </div>;
                }
                return "Retrieving token ..."
            }}
        </Query>
        )
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
                    <IconButton onClick={ () => removeToken({ variables: { pathOrId: path + "/jwt-token" }}).then(() => console.log("OK!!!")) }><Delete /></IconButton>
                </Tooltip>
            }}
        </Mutation>
    }

    addToken() {
        return <div>
            <TokenEditor open={ this.state.showEditCreateDialog }
                         close={ this.toggleDialog }
                         scopes={""} referers={""} ips={""}/>
            <Tooltip title={ "Add token" } placement="top-start">
                    <IconButton onClick={ () => this.setState({showEditCreateDialog: true}) }><Add /></IconButton>
                </Tooltip>
        </div>
    }

    editButton() {
        return <IconButton onClick={() => this.toggleDialog() }><Edit/></IconButton>
    }

    toggleDialog() {
        this.setState({
            showEditCreateDialog: !this.state.showEditCreateDialog
        })
    }

    getClaims(token) {
        if (token) {
            const claims = JSON.parse(token.claims.value);
            return {
                scopes: claims.scopes.join(","),
                referers: claims.referers ? claims.referers.join(",") : "",
                ips: claims.ips ? claims.ips.join(",") : ""
            }
        }
        return {
            scopes: "",
            referers: "",
            ips: ""
        }
    }

}

export default withStyles(styles)(TokenManager);