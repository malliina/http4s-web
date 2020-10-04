const ScalaJS = require('./scalajs.webpack.config');
const { merge } = require('webpack-merge');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
// path is crossTarget.in(npmUpdate)
const path = require('path');
const rootDir = path.resolve(__dirname, '../../../../src/main/resources');
const cssDir = path.resolve(rootDir, 'css');

const WebApp = merge(ScalaJS, {
  entry: {
    styles: [path.resolve(cssDir, './styles.js')],
    // vendors: [path.resolve(cssDir, './vendors.js')],
    // fonts: [path.resolve(cssDir, './fonts.js')]
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: [
          MiniCssExtractPlugin.loader,
          { loader: 'css-loader', options: { importLoaders: 1, url: true } },
          'postcss-loader'
        ]
      },
      {
        test: /\.(png|woff|woff2|eot|ttf|svg)$/,
        use: [
          { loader: 'url-loader' }
        ],
        exclude: /node_modules/
      },
      {
        test: /\.less$/,
        use: [
          MiniCssExtractPlugin.loader,
          { loader: 'css-loader', options: { importLoaders: 1 } },
          'postcss-loader',
          'less-loader'
        ]
      }
    ]
  }
});

module.exports = WebApp;
