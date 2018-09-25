import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import { Query } from 'react-apollo';
import { getTokensQuery, isAuthorizedQuery } from './gqlQueries';
import { deleteToken, addToken, modifyToken, createOrModifyToken } from './gqlMutations';
import { Mutation } from 'react-apollo';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import { Delete } from "@material-ui/icons";
import { Add } from "@material-ui/icons";
import { Edit } from "@material-ui/icons";
import TokenEditor from './tokenEditor';
import {lodash as _} from 'lodash';

const styles = {
    root: {
        flexGrow: 1,
        padding: 30
    },
    textArea: {
        width: 500,
        height: 100
    },
    loginContainer: {
        display: "flex",
        alignItems: "center",
        margin:"20px;"
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
            <Query query={isAuthorizedQuery} variables={{path: path}}>
            {({loading, error, data}) => {
                if (error) {
                    console.error(error);
                }

                if (!loading) {
                    if (data.isAuthorized) {
                        return this.requestToken();
                    } else {
                        return <div className={classes.loginContainer}>
                            <Typography variant={"title"}>Unauthorized</Typography>
                            <Button style={{marginLeft:"10px"}} onClick={() => {window.location.href = "/cms/login"}}>Login</Button>
                        </div>
                    }
                }
                return "Retrieving token ..."
            }}
        </Query>
        )
    }

    requestToken() {
        const { dxContext, classes } = this.props;
        const path = `/modules/security-filter/${dxContext.moduleVersion}/templates/contents/security-filter-jwt/tokens`;
        return <Query query={getTokensQuery} variables={{path: path}}>
            {({loading, error, data}) => {
                if (error) {
                    console.error(error);
                }

                if (!loading) {
                    console.log(data);
                    if (_.isEmpty(data.existingJWTToken)) {
                        return this.addToken();
                    }
                    const claims = data.existingJWTToken.claims;
                    return <div className={ classes.root }>
                        <TokenEditor open={ this.state.showEditCreateDialog }
                                     close={ this.toggleDialog }
                                     { ...this.getClaims(claims) }/>
                        <p>
                            <textarea className={ classes.textArea } value={data.existingJWTToken.token} disabled={ true }/>
                            { this.removeToken() }
                            { this.editButton() }
                        </p>
                    </div>;
                }
                return "Retrieving token ..."
            }}
        </Query>
    }
    removeToken() {
        const { dxContext } = this.props;console.log(dxContext.moduleVersion);
        const path = `/modules/security-filter/${dxContext.moduleVersion}/templates/contents/security-filter-jwt/tokens`;
        return <Mutation
            mutation={ deleteToken }
            refetchQueries={[{
                query: getTokensQuery,
                variables: {path: path}
            }]}>
            {(removeToken) => {
                return <Tooltip title={ "Remove token" } placement="top-start">
                    <IconButton onClick={ () => removeToken({ variables: { path: path + "/jwt-token" }}).then(() => console.log("OK!!!")) }><Delete /></IconButton>
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

    getClaims(claimsJson) {
        if (claimsJson) {
            const claims = JSON.parse(claimsJson);
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