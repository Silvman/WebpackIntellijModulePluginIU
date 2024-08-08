package me.blep.intellij.plugin.webpackmodule

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.json.psi.JsonFile
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.io.isAncestor
import me.blep.intellij.plugin.webpackmodule.helper.MyPrettyPrinter
import java.util.regex.Pattern
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class WebpackIntellijModuleConfig private constructor() {

    val LOG = Logger.getInstance(WebpackIntellijModuleConfig::class.java)

    companion object {
        val INSTANCE: WebpackIntellijModuleConfig = WebpackIntellijModuleConfig()
        val WEBPACK_INTELLIJ_MODULE_FILE = "webpack.intellij-module.config.json"
//        val TSCONFIG_INTELLIJ_MODULE_FILE = "tsconfig.intellij-module.json"
        val WEBPACK_CONFIG_FILE = "webpack.config.js"
        val TS_CONFIG_FILE = "tsconfig.json"
    }

    private val cache: HashMap<Project, HashMap<Module, ArrayList<Pair<String, String>>>> = HashMap()

    fun updateAllProjects() {
        val projectManager = ProjectManager.getInstance()
        for (openProject in projectManager.openProjects) {
            init(openProject)
            updateProject(openProject)
        }
    }

    fun init(project: Project) {
        if (cache.containsKey(project)) return;
        val moduleToProvidedDependencyList = HashMap<Module, ArrayList<Pair<String, String>>>()
        for (module in project.modules) {
            try {
                val webpackConfigDir = findWebpackConfigDir(module)
                if (webpackConfigDir != null) {
                    val webpackPluginFile = webpackConfigDir.toNioPath().toFile().resolve(WEBPACK_INTELLIJ_MODULE_FILE)
                    if (webpackPluginFile.exists()) {
                        val readValue = ObjectMapper().readValue(webpackPluginFile, Map::class.java) // cache
                        val resolve = readValue?.get("resolve") as Map<String, *>
                        val map = resolve?.get("alias") as Map<String, String>
                        val dependencies = ArrayList<Pair<String, String>>()
                        for ((key, value) in map) {
                            dependencies.add(Pair(key, value))
                        }
                        moduleToProvidedDependencyList[module] = dependencies
                    }
                }
            } catch (ex: Exception) {
                LOG.error("failed to init module " + module.name + " of project " + project.name, ex)
            }
        }
        LOG.debug("init " + project.name + "; " + moduleToProvidedDependencyList)
        cache[project] = moduleToProvidedDependencyList
    }

    fun updateProject(project: Project, force: Boolean = false) {
        val cachedConfig = cache[project]
        val projectConfig = getProjectConfig(project)
        val equal = isEqual(cachedConfig, projectConfig)
        LOG.debug("updateProject " + project.name + "; equal=" + equal)
        if (!equal || force) {
            cache[project] = projectConfig;
            updateJson(project, cachedConfig)
        }
    }

    fun isEqual(a: HashMap<Module, ArrayList<Pair<String, String>>>?, b: HashMap<Module, ArrayList<Pair<String, String>>>?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        if (a.size != b.size) return false
        for ((key, value) in a) {
            if (!b.containsKey(key)) return false
            if (!isEqual(value, b[key])) return false
        }
        for ((key, value) in b) {
            if (!a.containsKey(key)) return false
            if (!isEqual(value, a[key])) return false
        }
        return true;
    }

    fun isEqual(a: ArrayList<Pair<String, String>>?, b: ArrayList<Pair<String, String>>?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        if (a.size != b.size) return false
        for (index in 0 until b.size) {
            val ap = a[index]
            val bp = b[index]
            if (ap.first != bp.first) return false
            if (ap.second != bp.second) return false
        }
        return true;
    }

    fun getPackageJsonData(module: Module): PackageJsonData? {
        val rootManager = ModuleRootManager.getInstance(module)
        try {
            for (contentRoot in rootManager.contentRoots) {
                val packageJson = contentRoot.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
                if (packageJson != null) {
                    return PackageJsonData.getOrCreate(packageJson)
                }
            }
        } catch (ex: Exception) {
            LOG.error("error on locating npm package in module=" + module.name + "; project=" + module.project.name, ex)
        }
        return null
    }

    fun getProjectConfig(project: Project): HashMap<Module, ArrayList<Pair<String, String>>> {
        val reverseDeps = HashMap<String, ArrayList<Module>>()
        val providedDependency = HashMap<String, Module>()

        for (module in project.modules) {
            val packageJsonData = getPackageJsonData(module)
            if (packageJsonData != null) {
                for (allDependencyEntry in packageJsonData.allDependencyEntries) {
                    reverseDeps.putIfAbsent(allDependencyEntry.key, ArrayList())
                    reverseDeps[allDependencyEntry.key]?.add(module)
                }
                if (packageJsonData.name != null) {
                    providedDependency[packageJsonData.name!!] = module
                }
            }
        }

        val moduleToProvidedDependencyList = HashMap<Module, ArrayList<Pair<String, String>>>()
        for ((packageName, providedDepModule) in providedDependency) {
            val providedDepDir = findPackageJsonDir(providedDepModule)
            val providedDepPath = providedDepDir?.toNioPath()?.absolutePathString() ?: continue
            try {
                val modules = reverseDeps[packageName];
                if (!modules.isNullOrEmpty()) {
                    for (m2 in modules) {
                        moduleToProvidedDependencyList.putIfAbsent(m2, ArrayList())
                        moduleToProvidedDependencyList[m2]?.add(Pair(packageName, providedDepPath))
                    }
                }
            } catch (ex: Exception) {
                LOG.warn("error in processing dependants of " + providedDepModule.name, ex)
            }
        }

       return moduleToProvidedDependencyList;
    }

    private val objectMapper = ObjectMapper(JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .build())

    private fun getAliases(dependencyList: ArrayList<Pair<String, String>>?): HashMap<String, String> {
        val alias = HashMap<String, String>()
        if (dependencyList != null) {
            for (t: Pair<String, String> in dependencyList) {
                alias[t.first] = t.second
            }
        }
        return alias
    }

    fun updateJson(project: Project, oldMappings: HashMap<Module, ArrayList<Pair<String, String>>>?) {
        for (module in project.modules) {
            try {
                val webpackConfigDir = findWebpackConfigDir(module)
                if (webpackConfigDir != null) {
                    val webpackPluginFile = webpackConfigDir.toNioPath().toFile().resolve(WEBPACK_INTELLIJ_MODULE_FILE)
                    val tsConfigFile = webpackConfigDir.toNioPath().toFile().resolve(TS_CONFIG_FILE)

                    var indent = "    "
                    if (tsConfigFile.exists()) {
                        val compile = Pattern.compile("^([\\s]+)")
                        for (readLine in tsConfigFile.readLines()) {
                            val matcher = compile.matcher(readLine)
                            val find = matcher.find();
                            if (find) {
                                indent = matcher.group(1)
                                LOG.debug("${module.name}: found indent \"$indent\" in line \"$readLine\"")
                                break;
                            }
                        }
                    }

                    val writer = objectMapper.writer(MyPrettyPrinter(indent))
                    val alias = getAliases(cache[project]?.get(module))
                    val oldAlias = getAliases(oldMappings?.get(module))

                    try {
                        if (tsConfigFile.exists()) {
                            val jsonNode = objectMapper.readTree(tsConfigFile)
                            if (jsonNode.isObject) {
                                val topObj = jsonNode as ObjectNode
                                if (!topObj.has("compilerOptions")) {
                                    topObj.set("compilerOptions", objectMapper.createObjectNode()) as ObjectNode
                                }
                                val compilerOptions = topObj.get("compilerOptions") as ObjectNode
                                if (!compilerOptions.has("paths")) {
                                    compilerOptions.set("paths", objectMapper.createObjectNode()) as ObjectNode
                                }
                                val paths = compilerOptions.get("paths") as ObjectNode
                                for ((key) in oldAlias) {
                                    paths.remove(key)
                                }
                                for ((key, value) in alias) {
                                    val arrayNode = objectMapper.createArrayNode()
                                    arrayNode.add(value)
                                    arrayNode.add("./node_modules/$key")
                                    paths.set(key, arrayNode) as ObjectNode
                                }
                                if (paths.isEmpty) {
                                    compilerOptions.remove("paths")
                                }
                            }
                            writer.writeValue(tsConfigFile, jsonNode)
                        }
                    } catch (a: Exception) {
                        LOG.error("${module.name}: error on update $TS_CONFIG_FILE", a)
                    }

                    try {
                        if (alias.isNotEmpty() || webpackPluginFile.exists()) {
                            val fixedAlias = HashMap<String, String>()

                            for (alias1 in alias) {
                                val packageName = alias1.key
                                val path = alias1.value
                                val moduleDir = VirtualFileManager.getInstance().findFileByNioPath(Path(path))
                                val packageJsonFile = moduleDir?.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
                                val packageJsonData =  PackageJsonData.getOrCreate(packageJsonFile!!)
                                for (allDependencyEntry in packageJsonData.allDependencyEntries) {
                                    val fileByRelativePath = moduleDir.findChild(NodeModuleNamesUtil.MODULES)?.findFileByRelativePath(allDependencyEntry.key)
                                    if (fileByRelativePath != null) {
                                        fixedAlias[allDependencyEntry.key] = fileByRelativePath.toNioPath().absolutePathString()
                                    }
                                }
                                fixedAlias[packageName] = path
                            }

                            val resolve = HashMap<String, Any>()
                            resolve["alias"] = fixedAlias
                            val file = HashMap<String, Any>()
                            file["resolve"] = resolve
                            if (!webpackPluginFile.exists()) {
                                webpackPluginFile.createNewFile()
                            }
                            writer.writeValue(webpackPluginFile, file)
                        }
                    } catch (a: Exception) {
                        LOG.error("${module.name}: error on update $WEBPACK_INTELLIJ_MODULE_FILE", a)
                    }
                }
            } catch (ex: Exception) {
                LOG.error("${module.name}: error processing", ex)
            }
        }
    }

    fun findPackageJsonDir(module: Module): VirtualFile? {
        val dependencyRootManager = ModuleRootManager.getInstance(module)
        for (contentRoot in dependencyRootManager.contentRoots) {
            if (contentRoot.findChild(NodeModuleNamesUtil.PACKAGE_JSON) != null) {
                return contentRoot;
            }
        }
        return null;
    }

    fun findWebpackConfigDir(module: Module): VirtualFile? {
        val dependencyRootManager = ModuleRootManager.getInstance(module)
        for (contentRoot in dependencyRootManager.contentRoots) {
            if (contentRoot.findChild(WEBPACK_CONFIG_FILE) != null) {
                return contentRoot;
            }
        }
        return null;
    }

    /* for reference contributors */

    fun useWebpackIntellijModule(host: PsiElement): Boolean {
        return findWebpackModuleConf(host) != null;
    }

    private fun findWebpackModuleConf(host: PsiElement): VirtualFile? {
        val resolveModule = resolveModule(host) ?: return null
        val rootManager = ModuleRootManager.getInstance(resolveModule)
        for (contentRoot in rootManager.contentRoots) {
            val findChild = contentRoot.findChild(WEBPACK_INTELLIJ_MODULE_FILE)
            if (findChild != null) {
                return findChild
            }
        }
        return null
    }

    private fun readWebpackModuleConf(host: PsiElement): PsiFile? {
        val wmc = findWebpackModuleConf(host) ?: return null;
        return PsiUtilCore.getPsiFile(host.project, wmc);
    }

    fun readWebpackModuleConfDependencyPath(host: PsiElement, dependency: String): String? {
        val moduleConf = readWebpackModuleConf(host)
        if (moduleConf != null) {
            val jsonFile = moduleConf as JsonFile
            val text = LoadTextUtil.loadText(jsonFile.virtualFile)
            val readValue = ObjectMapper().readValue(text.toString(), Map::class.java) // cache
            val resolve = readValue?.get("resolve") as Map<String, *>
            val map = resolve?.get("alias") as Map<String, String>
            return map[dependency];
        }
        return null
    }

    fun resolveModule(host: PsiElement): Module? {
        val moduleManager = ModuleManager.getInstance(host.project)
        for (module in moduleManager.modules) {
            for (contentRoot in ModuleRootManager.getInstance(module).contentRoots) {
                val ancestor = contentRoot.toNioPath().isAncestor(host.containingFile.virtualFile.toNioPath())
                if (ancestor) {
                    return module
                }
            }
        }
        return null
    }

}