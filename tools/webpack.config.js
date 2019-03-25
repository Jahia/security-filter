var path = require('path');
var env = process.env.WEBPACK_ENV || 'development';

module.exports = {
    entry: path.resolve(__dirname, 'src/javascript', 'index.js'),
    output: {
        path: path.resolve(__dirname, 'src/main/resources/javascript/apps/'),
        filename: 'security-filter-jwt.js'
    },
    resolve: {
        mainFields: ['module', 'main'],
        extensions: ['.mjs', '.js', '.jsx', 'json']
    },
    module: {
        rules: [
            {
                test: /\.mjs$/,
                include: /node_modules/,
                type: "javascript/auto",
            },
            {
                test: /\.jsx?$/,
                include: [path.join(__dirname, "src")],
                loader: 'babel-loader',

                query: {
                    presets: [['env', {modules: false}], 'react', 'stage-2'],
                    plugins: [
                        "lodash"
                    ]
                }
            }
        ]
    },
    mode: env,
    devtool: 'source-map'
};