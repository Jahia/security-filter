import React from 'react';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import TextField from '@material-ui/core/TextField';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import { Mutation } from 'react-apollo';

import { withStyles } from '@material-ui/core/styles';
import {createOrModifyToken} from "./gqlMutations";

const styles = theme => ({
    root: {
        flexGrow: 1,
    },
    container: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    textField: {
        marginLeft: theme.spacing.unit,
        marginRight: theme.spacing.unit,
        width: "100%",
    }
});

const initState = {};

class TokenEditor extends React.Component {

    constructor(props) {
        super(props);
        this.state = initState;
    }


    render() {
        const { classes, open, close, scopes, referer, ips } = this.props;

        return (
                <Dialog
                    open={open}
                    aria-labelledby="alert-dialog-title"
                    aria-describedby="alert-dialog-description"
                >
                    <DialogTitle id="alert-dialog-title">{"Edit Token Parameters"}</DialogTitle>
                    <DialogContent>
                        <DialogContentText id="alert-dialog-description">
                            Enter comma separated parameter values below. Leave empty to ignore parameter. Refer will match itself and any subpath
                            You need at least one scope value.
                        </DialogContentText>
                        <form ref={ this.form } className={classes.container} noValidate autoComplete="off">
                            <TextField
                                id="scopes"
                                label="Scopes"
                                multiline
                                rowsMax="4"
                                className={classes.textField}
                                defaultValue={ scopes }
                                onChange={(e) => this.handleChange(e, "scopes")}
                                margin="normal"
                            />
                            <TextField
                                id="referer"
                                label="Referer"
                                multiline
                                rowsMax="4"
                                className={classes.textField}
                                defaultValue={ referer }
                                onChange={(e) => this.handleChange(e, "referer")}
                                margin="normal"
                            />
                            <TextField
                                id="ips"
                                label="IPs"
                                multiline
                                rowsMax="4"
                                className={classes.textField}
                                defaultValue={ ips }
                                onChange={(e) => this.handleChange(e, "ips")}
                                margin="normal"
                            />
                        </form>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={ () => this.reset() }color="primary">
                            Cancel
                        </Button>
                        { this.saveTokenButton() }
                    </DialogActions>
                </Dialog>
        );
    }

    handleChange(e, name) {
        this.setState({
            [name] : e.target.value
        })
    }

    persistToken(mutation) {
        let {onTokenCreation} = this.props;
        const values = {
            scopes: this.props.scopes,
            referer: this.props.referer,
            ips: this.props.ips
        };
        if (this.state.scopes !== undefined) {
            values.scopes = this.state.scopes
        }
        if (this.state.referer !== undefined) {
            values.referer = this.state.referer
        }
        if (this.state.ips !== undefined) {
            values.ips = this.state.ips
        }

        for (const prop of Object.keys(values)) {
            if (values[prop] === "" || values[prop] === undefined) {
                delete values[prop];
            }
            else {
                values[prop] = values[prop].split(",").map(function(item) {
                    return item.trim();
                });
            }
        }

        mutation({variables: { ...values }}).then((res) => {
            if (res.data && res.data.jwtToken){
                onTokenCreation(res.data.jwtToken);
            }
        });
        this.reset();
    }

    saveTokenButton() {
        return <Mutation
            mutation={ createOrModifyToken }>
            {(createOrModifyToken) => {
                return <Button onClick={ () => this.persistToken(createOrModifyToken) } color="primary" autoFocus>
                    Save
                </Button>
            }}
        </Mutation>
    }

    reset() {
        const state = this.state;
        for (const prop of Object.keys(state)) {
            delete state[prop];
        }
        this.setState(state);
        this.props.close();
    }
}

export default withStyles(styles)(TokenEditor);