const BaseWebpack = require('./webpack.base.config');
const { merge } = require('webpack-merge');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

module.exports = merge(BaseWebpack, {
  mode: 'development',
  plugins: [
    new MiniCssExtractPlugin({filename: '[name].css'})
  ]
});
