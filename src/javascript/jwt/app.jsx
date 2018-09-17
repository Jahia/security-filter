import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import {ApolloProvider} from "react-apollo";
import {client} from "@jahia/apollo-dx";
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import DxContext from '../DxContext';
import TokenManager from './tokenManager';

const styles = {
    root: {
        flexGrow: 1,
    },
};

function App(props) {
    const { classes, dxContext } = props;
    return (
        <div className={classes.root}>
            <AppBar position="static" color="default">
                <Toolbar>
                    <Typography variant="title" color="inherit">
                        Configure Json Web Token
                    </Typography>
                </Toolbar>
            </AppBar>
            <ApolloProvider client={client({contextPath: dxContext.context})}>
                <DxContext.Provider value={dxContext}>
                    <TokenManager dxContext={dxContext}/>
                </DxContext.Provider>
            </ApolloProvider>
        </div>
    );
}

App.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(App);