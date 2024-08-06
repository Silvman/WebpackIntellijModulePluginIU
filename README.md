# Webpack Intellij Module Plugin for Intellij Idea Ultimate

The plugin allows you to add an NPM package as a module to your Idea project to develop it together.

This plugin will search your project module directories for NPM packages and their usages according 
to `package.json` files and provide changes to `tsconfig.json` and webpack config. Your project modules 
will be used in webpack build instead of the ones in `node_modules` directory.

#### Requirements

- Webpack 5 (is tested, but may be working with Webpack 4)
- Intellij Idea Ultimate >= 2022.2.5
- TypeScript type resolving supported only if your tsconfig file is named `tsconfig.json`

#### Installation and usage

1. Build and install plugin

2. Add companion Webpack plugin `webpack-intellij-module-plugin` in your project
- `package.json`:
```    
 "webpack-intellij-module-plugin": "^0.0.5"
```
- `webpack.config.js`:
```
const WebpackIntellijModulePlugin = require("webpack-intellij-module-plugin");

module.exports = {
    ...
    resolve: {
        ...
        plugins: [new WebpackIntellijModulePlugin()],
    },
}
```

3. Add your NPM package source directory as a project module
- `File` -> `Project Structure` -> `Modules` -> `Add` -> `Import Module` 
-> `[locate your package dir]` -> `Create Module` -> `Next` -> `Next` -> `Create`

Package source folder will appear in the Project View.
At this point plugin will create file `webpack.intellij-module.config.json` and modify `tsconfig.json`
providing the necessary resolving paths

4. Run both your project and NPM package build in watch mode, check if your project rebuilds after change in NPM package source code