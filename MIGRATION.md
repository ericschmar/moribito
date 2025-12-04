# Moribito: Go to Kotlin Migration Plan

## 🎯 Migration Progress Tracker

### ✅ COMPLETED: Stage 1 - Project Structure Setup
**Completed:** December 3, 2025  
**Branch:** `kotlin`

#### What Was Built
- ✅ Kotlin Multiplatform project structure (5 modules)
- ✅ Gradle 8.5 build system with Kotlin DSL
- ✅ **Java 21** target (upgraded from planned Java 17)
- ✅ All dependencies configured and verified
- ✅ Shadow JAR packaging for both TUI and GUI
- ✅ Working executable JARs for both applications
- ✅ Gradle wrapper with auto-provisioning toolchain

#### Build Status
```bash
✅ ./gradlew build              # SUCCESS
✅ app-tui/build/libs/moribito-tui-2.0.0.jar   # WORKING
✅ app-gui/build/libs/moribito-gui-2.0.0.jar   # WORKING
```

### 🔄 NEXT: Stage 2 - LDAP Core Implementation
**Status:** Ready to Start  
**Estimated:** Weeks 3-4

#### Upcoming Tasks
- [ ] LdapClient wrapper implementation
- [ ] SSL/TLS connection management
- [ ] Retry logic with exponential backoff
- [ ] Data models (Entry, TreeNode, SearchPage)
- [ ] YAML configuration management
- [ ] Unit tests + embedded LDAP integration tests

---

## Executive Summary

This plan details the migration of moribito LDAP explorer from Go to Kotlin Multiplatform, including development of both TUI and GUI interfaces. The project currently consists of ~36 Go files with comprehensive test coverage (24 test files), implementing an interactive LDAP browser with tree navigation, record viewing, and custom queries.

**Current Stack:**
- Language: Go 1.24.6
- TUI: BubbleTea (Elm Architecture)
- LDAP: go-ldap/ldap v3.4.11
- Build: Go modules, Makefile, GoReleaser
- Platforms: Linux, macOS, Windows

**Target Stack:**
- Language: Kotlin Multiplatform (JVM target)
- TUI: Mosaic
- GUI: Doodle framework
- LDAP: Ldaptive
- Build: Gradle with Kotlin DSL
- Platforms: Linux, macOS, Windows (JVM-based)

---

## Part 1: Migration to Kotlin Multiplatform

### 1.1 Project Structure Setup

#### Kotlin Multiplatform Module Organization

```
moribito-kotlin/
├── gradle/
│   └── wrapper/
├── buildSrc/
│   └── src/main/kotlin/
│       └── Dependencies.kt
├── core/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/
│       │   └── com/moribito/
│       │       ├── ldap/          # LDAP client & models
│       │       ├── config/        # Configuration management
│       │       └── version/       # Version info
│       └── commonTest/kotlin/
├── tui/
│   ├── build.gradle.kts
│   └── src/
│       ├── jvmMain/kotlin/
│       │   └── com/moribito/tui/
│       │       ├── views/         # Start, Tree, Record, Query views
│       │       ├── model/         # Application state model
│       │       └── components/    # Reusable TUI components
│       └── jvmTest/kotlin/
├── gui/
│   ├── build.gradle.kts
│   └── src/
│       ├── jvmMain/kotlin/
│       │   └── com/moribito/gui/
│       │       ├── views/         # GUI view implementations
│       │       ├── components/    # Doodle components
│       │       └── theme/         # Styling & theming
│       └── jvmTest/kotlin/
├── app-tui/
│   ├── build.gradle.kts           # TUI application entry point
│   └── src/
│       └── jvmMain/kotlin/
│           └── com/moribito/
│               └── Main.kt
├── app-gui/
│   ├── build.gradle.kts           # GUI application entry point
│   └── src/
│       └── jvmMain/kotlin/
│           └── com/moribito/
│               └── MainGui.kt
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

**Rationale:**
- Separate `core` module for shared business logic (LDAP, config)
- Dedicated `tui` and `gui` modules for UI-specific code
- Application modules (`app-tui`, `app-gui`) for entry points
- Enables code sharing while maintaining UI separation

#### Critical Decision: Build System

**Choice: Gradle with Kotlin DSL**

Reasons:
1. Native Kotlin Multiplatform support
2. Better IDE integration (IntelliJ IDEA)
3. Flexible dependency management
4. Plugin ecosystem for native builds

### 1.2 Dependency Mapping

#### Go → Kotlin Library Mapping

| Go Library | Kotlin Alternative | Notes |
|------------|-------------------|-------|
| go-ldap/ldap v3.4.11 | Ldaptive | Enterprise-grade, comprehensive LDAP support |
| gopkg.in/yaml.v3 | kaml or kotlinx-serialization-yaml | YAML config parsing |
| github.com/charmbracelet/bubbletea | Mosaic | TUI framework (Part 2) |
| github.com/charmbracelet/lipgloss | Mosaic styling | Terminal styling |
| github.com/atotto/clipboard | java.awt.Toolkit + kotlinx-coroutines | Clipboard access |
| golang.org/x/crypto | Java Crypto API | SSL/TLS support |

### 1.3 Core Functionality Migration Order

**Phase 1: Foundation (Week 1-2)**
1. ✅ Project structure & build setup
2. ✅ Version info module
3. ✅ Configuration module (YAML parsing, multi-connection support)
4. ✅ Basic tests for config loading

**Phase 2: LDAP Core (Week 3-4)**
5. ✅ LDAP client wrapper
   - Connection management (SSL/TLS)
   - Authentication (bind)
   - Basic search operations
6. ✅ Data models (Entry, TreeNode, SearchPage, SavedConnection)
7. ✅ Pagination support
8. ✅ Retry logic with exponential backoff
9. ✅ Comprehensive LDAP tests (mock server or embedded LDAP)

**Phase 3: Business Logic (Week 5)**
10. ✅ Tree building & lazy loading
11. ✅ Custom query execution
12. ✅ Update checker (GitHub API integration)
13. ✅ Integration tests

### 1.4 Architecture Translation

#### BubbleTea's Elm Architecture → Kotlin Pattern

**Go Pattern (BubbleTea):**
```go
type Model struct {
    // State
}

func (m Model) Init() tea.Cmd { }
func (m Model) Update(msg tea.Msg) (tea.Model, tea.Cmd) { }
func (m Model) View() string { }
```

**Kotlin Pattern (State Machine + Coroutines):**
```kotlin
// State-based architecture
sealed class AppState {
    data class Start(val config: Config) : AppState()
    data class Tree(val root: TreeNode, val cursor: Int) : AppState()
    data class Record(val entry: Entry) : AppState()
    data class Query(val results: List<Entry>) : AppState()
}

sealed class AppEvent {
    data class KeyPressed(val key: Key) : AppEvent()
    data class NodeLoaded(val node: TreeNode) : AppEvent()
    data class QueryResult(val entries: List<Entry>) : AppEvent()
}

class AppModel {
    private val _state = MutableStateFlow<AppState>(AppState.Start(config))
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    suspend fun handleEvent(event: AppEvent) {
        // State transitions
    }
    
    fun render(): String {
        // View rendering based on state
    }
}
```

#### Asynchronous Operation Translation

**Go (Goroutines + Channels):**
```go
return func() tea.Msg {
    root, err := tv.client.BuildTree()
    if err != nil {
        return ErrorMsg{Err: err}
    }
    return RootNodeLoadedMsg{Node: root}
}
```

**Kotlin (Coroutines + Flow):**
```kotlin
viewModelScope.launch {
    try {
        val root = ldapClient.buildTree()
        _state.emit(AppState.TreeLoaded(root))
    } catch (e: Exception) {
        _errors.emit(ErrorEvent(e))
    }
}
```

### 1.5 Key Components to Migrate

#### 1. LDAP Client (`internal/ldap/client.go` → `core/src/commonMain/kotlin/ldap/LdapClient.kt`)

**Critical Features:**
- SSL/TLS connection support
- Context-based timeouts
- Retry logic with exponential backoff
- Paginated search with cookies
- Lazy tree loading

**Kotlin Implementation Strategy:**
```kotlin
class LdapClient(private val config: LdapConfig) {
    private var connection: LDAPConnection? = null
    
    suspend fun connect() = withContext(Dispatchers.IO) {
        val socketFactory = if (config.useSSL) {
            SSLUtil(TrustAllTrustManager()).createSSLSocketFactory()
        } else null
        
        connection = LDAPConnection(socketFactory, config.host, config.port).apply {
            if (config.useTLS) startTLS()
            bind(config.bindUser, config.bindPass)
        }
    }
    
    suspend fun search(
        baseDN: String,
        filter: String,
        scope: SearchScope
    ): List<Entry> = withRetry {
        withTimeout(5000) {
            connection?.search(baseDN, scope, filter)
                ?.searchEntries
                ?.map { it.toEntry() }
                ?: emptyList()
        }
    }
    
    private suspend fun <T> withRetry(
        maxAttempts: Int = config.maxRetries,
        block: suspend () -> T
    ): T {
        // Exponential backoff implementation
    }
}
```

#### 2. Configuration (`internal/config/config.go` → `core/src/commonMain/kotlin/config/Config.kt`)

**Migration Strategy:**
```kotlin
@Serializable
data class Config(
    val ldap: LdapConfig,
    val pagination: PaginationConfig,
    val retry: RetryConfig
) {
    companion object {
        suspend fun load(path: String? = null): Config {
            val configPath = path ?: findConfigFile()
            val yaml = Yaml(configuration = YamlConfiguration(
                strictMode = false
            ))
            return File(configPath).readText()
                .let { yaml.decodeFromString(serializer(), it) }
        }
        
        private fun findConfigFile(): String {
            // OS-specific config path detection
            val osConfigPaths = when (Platform.osFamily) {
                OsFamily.WINDOWS -> listOf(
                    "${System.getenv("APPDATA")}/moribito/config.yaml"
                )
                OsFamily.MACOSX -> listOf(
                    "${System.getProperty("user.home")}/.moribito/config.yaml",
                    "${System.getProperty("user.home")}/Library/Application Support/moribito/config.yaml"
                )
                else -> listOf(
                    "${System.getenv("XDG_CONFIG_HOME") ?: "${System.getProperty("user.home")}/.config"}/moribito/config.yaml"
                )
            }
            // Search logic
        }
    }
}

@Serializable
data class SavedConnection(
    val name: String,
    val host: String,
    val port: Int,
    val baseDN: String,
    val useSSL: Boolean,
    val useTLS: Boolean,
    val bindUser: String,
    val bindPass: String
)
```

#### 3. Data Models

**Entry Model:**
```kotlin
data class Entry(
    val dn: String,
    val attributes: Map<String, List<String>>
)

// Extension function for UnboundID SearchResultEntry
fun SearchResultEntry.toEntry() = Entry(
    dn = dn,
    attributes = attributes.associate { attr ->
        attr.name to attr.values.toList()
    }
)
```

**TreeNode Model:**
```kotlin
data class TreeNode(
    val dn: String,
    val name: String,
    var children: List<TreeNode>? = null,
    var isLoaded: Boolean = false
)
```

### 1.6 Testing Strategy Migration

#### Test Structure

**Go Test Pattern:**
```go
func TestConfigLoad(t *testing.T) {
    cfg, err := config.Load("testdata/config.yaml")
    if err != nil {
        t.Fatalf("Failed to load config: %v", err)
    }
    // assertions
}
```

**Kotlin Test Pattern (Kotest):**
```kotlin
class ConfigTest : FunSpec({
    test("load config from YAML file") {
        val config = Config.load("testdata/config.yaml")
        
        config.ldap.host shouldBe "ldap.example.com"
        config.ldap.port shouldBe 389
        config.pagination.pageSize shouldBe 50
    }
    
    test("handle missing config file gracefully") {
        shouldThrow<FileNotFoundException> {
            Config.load("nonexistent.yaml")
        }
    }
})
```

#### Test Coverage Goals

- **Unit Tests:** 80%+ coverage for core logic
- **Integration Tests:** LDAP operations with embedded LDAP server (UnboundID In-Memory Directory Server)
- **Mock Tests:** UI interactions using MockK

#### Example LDAP Integration Test

```kotlin
class LdapClientIntegrationTest : FunSpec({
    lateinit var ldapServer: InMemoryDirectoryServer
    lateinit var client: LdapClient
    
    beforeTest {
        ldapServer = InMemoryDirectoryServer(
            InMemoryDirectoryServerConfig("dc=example,dc=com").apply {
                setListenerConfigs(
                    InMemoryListenerConfig.createLDAPConfig("test", 11389)
                )
            }
        )
        ldapServer.startListening()
        
        client = LdapClient(LdapConfig(
            host = "localhost",
            port = 11389,
            baseDN = "dc=example,dc=com"
        ))
    }
    
    afterTest {
        ldapServer.shutDown(true)
    }
    
    test("search returns entries") {
        ldapServer.add(
            "dn: dc=example,dc=com",
            "objectClass: domain",
            "dc: example"
        )
        
        val results = client.search(
            "dc=example,dc=com",
            "(objectClass=*)",
            SearchScope.SUB
        )
        
        results shouldHaveSize 1
        results.first().dn shouldBe "dc=example,dc=com"
    }
})
```

### 1.7 Build System Migration

#### Root build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

allprojects {
    group = "com.moribito"
    version = "2.0.0"
    
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
```

#### Core Module build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.unboundid:unboundid-ldapsdk:6.0.11")
                implementation("com.charleskorn.kaml:kaml:0.55.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-runner-junit5:5.8.0")
                implementation("io.kotest:kotest-assertions-core:5.8.0")
            }
        }
    }
}
```

### 1.8 CI/CD Pipeline Updates

#### GitHub Actions Workflow (.github/workflows/build.yml)

```yaml
name: Build and Test

on:
  push:
    branches: [ main, develop, kotlin ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        java-version: [17, 21]
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java-version }}
          distribution: 'temurin'
      
      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
      
      - name: Build with Gradle
        run: ./gradlew build
      
      - name: Run tests
        run: ./gradlew test
      
      - name: Upload test reports
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports-${{ matrix.os }}-java${{ matrix.java-version }}
          path: '**/build/reports/tests/'
```

#### Release Workflow (.github/workflows/release.yml)

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build-and-release:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build distributions
        run: ./gradlew :app-tui:shadowJar :app-gui:shadowJar
      
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            app-tui/build/libs/moribito-tui-*.jar
            app-gui/build/libs/moribito-gui-*.jar
          draft: false
          prerelease: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 1.9 Version Injection

**Go Approach (ldflags):**
```go
// Set at build time via -ldflags
var (
    Version = "dev"
    Commit  = "unknown"
    Date    = "unknown"
)
```

**Kotlin Approach (buildSrc + generated code):**

**buildSrc/src/main/kotlin/VersionInfo.kt:**
```kotlin
object VersionInfo {
    fun generate(outputDir: File, version: String) {
        val commit = "git rev-parse --short HEAD".runCommand() ?: "unknown"
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date())
        
        val versionFile = File(outputDir, "Version.kt")
        versionFile.writeText("""
            package com.moribito.version
            
            object Version {
                const val VERSION = "$version"
                const val COMMIT = "$commit"
                const val DATE = "$date"
                
                fun fullVersion() = "${'$'}VERSION (${'$'}COMMIT) built on ${'$'}DATE"
            }
        """.trimIndent())
    }
}
```

### 1.10 Potential Challenges & Mitigations

| Challenge | Impact | Mitigation |
|-----------|--------|------------|
| **JVM startup time** | TUI feels slower than native Go binary | Use GraalVM native-image for native compilation (future optimization) |
| **UnboundID API differences** | Different pagination approach than go-ldap | Wrapper abstraction layer to match Go client API |
| **YAML library variations** | Config parsing edge cases | Comprehensive test suite with real config files |
| **Coroutine context management** | Complex async patterns | Use structured concurrency, clear scope definitions |
| **Platform-specific paths** | Config file location detection | Kotlin expect/actual for platform-specific code |
| **Clipboard access** | JVM requires AWT, not available on all platforms | Fallback to terminal escape sequences, graceful degradation |

---

## Part 2: TUI Development with Kotlin

### 2.1 TUI Framework Selection

#### Framework Comparison

| Framework | Pros | Cons | Verdict |
|-----------|------|------|---------|
| **Mordant** | - Mature, actively maintained<br>- Rich styling (colors, markdown)<br>- Excellent table support<br>- Kotlin-first API | - Less interactive features<br>- No built-in state management | ✅ **RECOMMENDED** for static/simple TUI |
| **Mosaic** | - Jetpack Compose for Terminal<br>- Reactive UI paradigm<br>- State management built-in | - Experimental<br>- Less documentation<br>- Heavier dependency | ⚠️ Consider for complex interactions |
| **Lanterna** | - Comprehensive terminal features<br>- Swing-like API | - Java-centric API<br>- Verbose for Kotlin<br>- Older design patterns | ❌ Not recommended |
| **Kotter** | - BubbleTea-inspired<br>- Kotlin DSL | - Early stage<br>- Limited features | ⚠️ Watch for future |

**Decision: Use Mordant with custom state management**

Reasons:
1. Mature library with excellent styling
2. Similar terminal capabilities to lipgloss
3. Good table/list rendering
4. Can implement BubbleTea-like architecture on top

### 2.2 TUI Architecture Adaptation

#### BubbleTea → Kotlin State Machine Pattern

**Implementation Strategy:**

```kotlin
// State definitions
sealed class ViewState {
    data class StartView(
        val config: Config,
        val cursor: Int,
        val editing: Boolean,
        val editingField: Int
    ) : ViewState()
    
    data class TreeView(
        val root: TreeNode,
        val flattenedTree: List<TreeItem>,
        val cursor: Int,
        val viewport: Int,
        val loading: Boolean
    ) : ViewState()
    
    data class RecordView(
        val entry: Entry?,
        val cursor: Int
    ) : ViewState()
    
    data class QueryView(
        val query: String,
        val results: List<Entry>,
        val inputMode: Boolean,
        val loading: Boolean
    ) : ViewState()
}

// Event system
sealed class TuiEvent {
    data class KeyPress(val key: KeyEvent) : TuiEvent()
    data class MouseClick(val x: Int, val y: Int) : TuiEvent()
    data class NodeLoaded(val node: TreeNode) : TuiEvent()
    data class QueryResults(val entries: List<Entry>) : TuiEvent()
    data class Error(val error: Exception) : TuiEvent()
}

// Main TUI controller
class TuiController(
    private val ldapClient: LdapClient,
    private val config: Config
) {
    private val terminal = Terminal()
    private val _state = MutableStateFlow<ViewState>(ViewState.StartView(config, 0, false, 0))
    val state: StateFlow<ViewState> = _state.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    suspend fun start() {
        terminal.enterRawMode()
        
        scope.launch {
            state.collect { state ->
                render(state)
            }
        }
        
        // Event loop
        while (true) {
            val event = readEvent()
            handleEvent(event)
        }
    }
    
    private suspend fun handleEvent(event: TuiEvent) {
        val currentState = _state.value
        val newState = when (event) {
            is TuiEvent.KeyPress -> handleKeyPress(currentState, event.key)
            is TuiEvent.NodeLoaded -> handleNodeLoaded(currentState, event.node)
            // ... other events
        }
        newState?.let { _state.emit(it) }
    }
    
    private fun render(state: ViewState) {
        val content = when (state) {
            is ViewState.StartView -> renderStartView(state)
            is ViewState.TreeView -> renderTreeView(state)
            is ViewState.RecordView -> renderRecordView(state)
            is ViewState.QueryView -> renderQueryView(state)
        }
        
        terminal.print(content)
    }
}
```

### 2.3 View Implementation Strategy

#### Start View (Configuration Screen)

**Go Component:** `internal/tui/start.go` (826 lines)

**Kotlin Implementation:**

```kotlin
class StartViewRenderer(private val terminal: Terminal) {
    fun render(state: ViewState.StartView): String {
        return terminal.render {
            h1("Configure LDAP Connection")
            
            table {
                header {
                    row("Field", "Value")
                }
                
                body {
                    renderConfigField("Host", state.config.ldap.host, 0, state)
                    renderConfigField("Port", state.config.ldap.port.toString(), 1, state)
                    renderConfigField("Base DN", state.config.ldap.baseDN, 2, state)
                    renderBooleanField("Use SSL", state.config.ldap.useSSL, 3, state)
                    renderBooleanField("Use TLS", state.config.ldap.useTLS, 4, state)
                    renderConfigField("Bind User", state.config.ldap.bindUser, 5, state)
                    renderPasswordField("Password", state.config.ldap.bindPass, 6, state)
                }
            }
            
            if (state.config.ldap.savedConnections.isNotEmpty()) {
                h2("Saved Connections")
                renderSavedConnections(state)
            }
            
            text {
                style(dim = true, italic = true)
                +"Press [↑↓] to navigate • [Enter] to edit • [Tab] to switch views"
            }
        }
    }
    
    private fun renderConfigField(
        label: String,
        value: String,
        index: Int,
        state: ViewState.StartView
    ) {
        val isSelected = state.cursor == index
        val isEditing = state.editing && state.editingField == index
        
        val style = when {
            isEditing -> Style(bgColor = Color.YELLOW, fgColor = Color.BLACK, bold = true)
            isSelected -> Style(bgColor = Color.BLUE, fgColor = Color.WHITE)
            else -> Style()
        }
        
        row {
            cell(label, style = style)
            cell(if (isEditing) "> $value <" else value, style = style)
        }
    }
}
```

#### Tree View (LDAP Directory Browser)

**Key Features to Implement:**
- Lazy loading of tree nodes
- Expandable/collapsible nodes
- Viewport scrolling
- Loading indicators with timer
- Mouse click support (if terminal supports it)

**Kotlin Implementation:**

```kotlin
class TreeViewRenderer(
    private val terminal: Terminal,
    private val ldapClient: LdapClient
) {
    fun render(state: ViewState.TreeView): String {
        if (state.loading) {
            return terminal.render {
                text {
                    align = Align.CENTER
                    +"Loading LDAP tree..."
                }
            }
        }
        
        return terminal.render {
            val (width, height) = terminal.size
            val contentHeight = height - 5 // Reserve for UI chrome
            
            // Render visible portion of tree
            val visibleStart = state.viewport
            val visibleEnd = min(visibleStart + contentHeight, state.flattenedTree.size)
            
            state.flattenedTree.subList(visibleStart, visibleEnd).forEachIndexed { idx, item ->
                val absoluteIndex = visibleStart + idx
                val isSelected = absoluteIndex == state.cursor
                
                renderTreeItem(item, isSelected)
            }
            
            // Pagination info
            if (state.flattenedTree.size > contentHeight) {
                br()
                text {
                    style(dim = true, italic = true)
                    +"Showing ${visibleStart + 1}-$visibleEnd of ${state.flattenedTree.size}"
                }
            }
        }
    }
    
    private fun RenderContext.renderTreeItem(item: TreeItem, isSelected: Boolean) {
        val indent = "  ".repeat(item.level)
        val prefix = when {
            item.node.children != null && item.node.children!!.isNotEmpty() -> "[-] "
            item.node.isLoaded -> "[·] "
            else -> "[+] "
        }
        
        val style = if (isSelected) {
            Style(bgColor = Color.rgb("#0066CC"), fgColor = Color.WHITE)
        } else {
            Style()
        }
        
        text(indent + prefix + item.node.name, style = style)
    }
    
    suspend fun loadNode(node: TreeNode): TreeNode {
        return withContext(Dispatchers.IO) {
            val children = ldapClient.getChildren(node.dn)
            node.copy(children = children, isLoaded = true)
        }
    }
}
```

#### Record View (Entry Details)

**Features:**
- Table rendering with attributes
- Multi-value attribute display
- Clipboard copy support
- Color gradients for rows
- Scrollable viewport

**Kotlin Implementation:**

```kotlin
class RecordViewRenderer(private val terminal: Terminal) {
    fun render(state: ViewState.RecordView): String {
        if (state.entry == null) {
            return terminal.render {
                text {
                    align = Align.CENTER
                    +"No record selected"
                }
            }
        }
        
        return terminal.render {
            // DN Header
            h2(state.entry.dn) {
                style(bgColor = Color.rgb("#333333"), fgColor = Color.rgb("#00CCFF"))
            }
            
            br()
            
            // Attributes table
            table {
                header {
                    row("Attribute", "Value(s)")
                }
                
                body {
                    state.entry.attributes.entries
                        .sortedBy { it.key }
                        .forEachIndexed { idx, (name, values) ->
                            val isSelected = idx == state.cursor
                            val bgColor = if (isSelected) {
                                getGradientColor(0.3)
                            } else {
                                Color.TRANSPARENT
                            }
                            
                            val valueText = if (values.size == 1) {
                                values.first()
                            } else {
                                values.joinToString(" • ", prefix = "• ")
                            }
                            
                            row {
                                cell(name, style = Style(bgColor = bgColor))
                                cell(valueText, style = Style(bgColor = bgColor))
                            }
                        }
                }
            }
            
            br()
            text {
                style(dim = true, italic = true)
                +"Press [C] to copy value • [↑↓] to navigate"
            }
        }
    }
    
    private fun getGradientColor(position: Double): Color {
        // Blue to teal gradient
        val blue = Color.rgb("#0066CC")
        val teal = Color.rgb("#008080")
        return blue.blend(teal, position)
    }
}
```

#### Query View (Custom LDAP Search)

**Features:**
- Multi-line text input
- Query formatting (auto-indent LDAP filters)
- Paginated results
- Input mode vs browse mode
- Loading states

**Kotlin Implementation:**

```kotlin
class QueryViewRenderer(
    private val terminal: Terminal,
    private val ldapClient: LdapClient
) {
    fun render(state: ViewState.QueryView): String {
        return terminal.render {
            h2("LDAP Query")
            
            // Query input area
            box {
                border = BoxBorder.ROUNDED
                padding = Padding(1)
                
                if (state.inputMode) {
                    text(state.query, style = Style(bgColor = Color.BLACK))
                    text("█") // Cursor
                } else {
                    text(state.query)
                }
            }
            
            br()
            
            // Status/loading
            when {
                state.loading -> {
                    text("⏳ Executing query...", style = Style(italic = true))
                }
                state.results.isNotEmpty() -> {
                    h3("Results:")
                    
                    table {
                        header {
                            row("DN", "Summary")
                        }
                        
                        body {
                            state.results.forEach { entry ->
                                val summary = entry.attributes.entries
                                    .take(3)
                                    .joinToString(" | ") { (k, v) -> 
                                        "$k: ${v.firstOrNull() ?: ""}"
                                    }
                                
                                row(entry.dn, summary)
                            }
                        }
                    }
                }
            }
            
            br()
            val instructions = if (state.inputMode) {
                "[Enter] to execute • [Esc] to clear • [Ctrl+F] to format"
            } else {
                "[↑↓] to navigate • [Enter] to view record • [Esc] to edit"
            }
            text(instructions, style = Style(dim = true, italic = true))
        }
    }
    
    suspend fun executeQuery(query: String, pageSize: Int): List<Entry> {
        return ldapClient.search(
            baseDN = ldapClient.config.baseDN,
            filter = query,
            scope = SearchScope.SUB
        )
    }
}
```

### 2.4 Input Handling and Navigation

**Keyboard Event Handling:**

```kotlin
class InputHandler(private val controller: TuiController) {
    suspend fun handleKey(key: KeyEvent, state: ViewState): ViewState? {
        return when (key) {
            is KeyEvent.Tab -> controller.switchView()
            is KeyEvent.Number -> handleNumberKey(key.digit, state)
            is KeyEvent.Char -> handleCharKey(key.char, state)
            is KeyEvent.Special -> handleSpecialKey(key, state)
            else -> null
        }
    }
    
    private fun handleSpecialKey(key: KeyEvent.Special, state: ViewState): ViewState? {
        return when (key) {
            KeyEvent.Up -> handleNavigationUp(state)
            KeyEvent.Down -> handleNavigationDown(state)
            KeyEvent.Left -> handleNavigationLeft(state)
            KeyEvent.Right -> handleNavigationRight(state)
            KeyEvent.Enter -> handleEnter(state)
            KeyEvent.Escape -> handleEscape(state)
            KeyEvent.PageUp -> handlePageUp(state)
            KeyEvent.PageDown -> handlePageDown(state)
            else -> null
        }
    }
    
    private fun handleNavigationUp(state: ViewState): ViewState? {
        return when (state) {
            is ViewState.StartView -> state.copy(cursor = max(0, state.cursor - 1))
            is ViewState.TreeView -> {
                val newCursor = max(0, state.cursor - 1)
                val newViewport = adjustViewport(newCursor, state.viewport, state.flattenedTree.size)
                state.copy(cursor = newCursor, viewport = newViewport)
            }
            is ViewState.RecordView -> state.copy(cursor = max(0, state.cursor - 1))
            else -> null
        }
    }
    
    private fun adjustViewport(cursor: Int, viewport: Int, totalItems: Int): Int {
        val terminalHeight = terminal.size.height - 5
        return when {
            cursor < viewport -> cursor
            cursor >= viewport + terminalHeight -> cursor - terminalHeight + 1
            else -> viewport
        }.coerceIn(0, max(0, totalItems - terminalHeight))
    }
}
```

**Mouse Support (if available):**

```kotlin
class MouseHandler(private val controller: TuiController) {
    fun handleClick(x: Int, y: Int, state: ViewState): ViewState? {
        return when (state) {
            is ViewState.TreeView -> handleTreeClick(x, y, state)
            is ViewState.RecordView -> handleRecordClick(x, y, state)
            is ViewState.StartView -> handleStartClick(x, y, state)
            else -> null
        }
    }
    
    private fun handleTreeClick(x: Int, y: Int, state: ViewState.TreeView): ViewState? {
        val clickedIndex = state.viewport + y - 1 // Adjust for header
        if (clickedIndex in state.flattenedTree.indices) {
            return state.copy(cursor = clickedIndex)
        }
        return null
    }
}
```

### 2.5 Terminal Rendering Approach

**Mordant Rendering Pipeline:**

```kotlin
class TerminalRenderer(private val terminal: Terminal) {
    private var lastFrame: String = ""
    
    fun render(content: String) {
        // Diff-based rendering to minimize terminal writes
        if (content != lastFrame) {
            terminal.cursor.move {
                setPosition(0, 0)
            }
            terminal.print(content)
            lastFrame = content
        }
    }
    
    fun clearScreen() {
        terminal.print("\u001b[2J\u001b[H")
        lastFrame = ""
    }
    
    fun enterAlternateScreen() {
        terminal.print("\u001b[?1049h")
    }
    
    fun exitAlternateScreen() {
        terminal.print("\u001b[?1049l")
    }
}
```

### 2.6 TUI Module Structure

```
tui/src/jvmMain/kotlin/com/moribito/tui/
├── TuiApplication.kt           # Main TUI app
├── controller/
│   ├── TuiController.kt        # Main controller
│   ├── InputHandler.kt         # Keyboard input
│   └── MouseHandler.kt         # Mouse input
├── views/
│   ├── StartViewRenderer.kt    # Start screen
│   ├── TreeViewRenderer.kt     # Tree browser
│   ├── RecordViewRenderer.kt   # Record details
│   └── QueryViewRenderer.kt    # Query interface
├── state/
│   ├── ViewState.kt            # State definitions
│   ├── TuiEvent.kt             # Event definitions
│   └── StateManager.kt         # State management
├── components/
│   ├── TabBar.kt               # Tab navigation
│   ├── StatusBar.kt            # Status display
│   ├── HelpBar.kt              # Help text
│   └── LoadingSpinner.kt       # Loading indicator
└── utils/
    ├── ColorUtils.kt           # Color gradients
    ├── ViewportUtils.kt        # Scrolling logic
    └── TerminalRenderer.kt     # Rendering engine
```

---

## Part 3: GUI Development with Doodle

### 3.1 Doodle Framework Integration

#### Dependencies (gui/build.gradle.kts)

```kotlin
plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.0"
}

dependencies {
    // Doodle framework
    implementation("io.nacular.doodle:core:0.9.1")
    implementation("io.nacular.doodle:browser:0.9.1")
    implementation("io.nacular.doodle:controls:0.9.1")
    implementation("io.nacular.doodle:themes:0.9.1")
    implementation("io.nacular.doodle:animation:0.9.1")
    
    // Core module
    implementation(project(":core"))
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    
    // Dependency injection (optional)
    implementation("io.insert-koin:koin-core:3.5.3")
}
```

#### Doodle Overview

Doodle is a pure Kotlin UI framework that renders to:
- **Browser:** Canvas/SVG rendering
- **Desktop:** Java2D/Swing backend

**Key Features:**
- Vector-based rendering
- Flexible layout system
- Rich theming support
- Built-in controls (tables, trees, text fields)
- Animation support

### 3.2 GUI Architecture

**MVVM Pattern with Doodle:**

```kotlin
// ViewModel layer
class MainViewModel(
    private val ldapClient: LdapClient,
    private val config: Config
) {
    private val _currentView = MutableStateFlow<GuiView>(GuiView.Start)
    val currentView: StateFlow<GuiView> = _currentView.asStateFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _treeData = MutableStateFlow<TreeNode?>(null)
    val treeData: StateFlow<TreeNode?> = _treeData.asStateFlow()
    
    private val _selectedEntry = MutableStateFlow<Entry?>(null)
    val selectedEntry: StateFlow<Entry?> = _selectedEntry.asStateFlow()
    
    suspend fun connect() {
        _connectionState.value = ConnectionState.Connecting
        try {
            ldapClient.connect()
            _connectionState.value = ConnectionState.Connected
            loadTree()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
        }
    }
    
    private suspend fun loadTree() {
        val root = ldapClient.buildTree()
        _treeData.value = root
    }
}

sealed class GuiView {
    object Start : GuiView()
    object Tree : GuiView()
    object Record : GuiView()
    object Query : GuiView()
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
```

### 3.3 UI Component Mapping

#### Tree → Doodle Tree Control

**Go BubbleTea Tree:**
- Custom ASCII rendering
- Keyboard navigation
- Lazy loading

**Doodle Tree:**
```kotlin
import io.nacular.doodle.controls.tree.Tree
import io.nacular.doodle.controls.tree.TreeModel

class LdapTreeView(
    private val viewModel: MainViewModel,
    private val theme: Theme
) : View() {
    private val tree: Tree<TreeNode, TreeNodeModel> = Tree(
        model = TreeNodeModel(viewModel.treeData.value),
        itemVisualizer = treeItemVisualizer()
    ).apply {
        selectionChanged += { _, removed, added ->
            added.firstOrNull()?.let { node ->
                viewModel.selectNode(node)
            }
        }
    }
    
    init {
        children += tree
        
        // Observe tree data changes
        viewModel.treeData.onEach { root ->
            root?.let {
                tree.model = TreeNodeModel(it)
            }
        }.launchIn(viewModelScope)
    }
    
    private fun treeItemVisualizer() = object : ItemVisualizer<TreeNode> {
        override fun invoke(item: TreeNode, previous: View?, context: ItemContext): View {
            return container {
                layout = constrain(it) { parent ->
                    it.width = parent.width
                    it.height = 30.0
                }
                
                children += text(item.name) {
                    font = Font(size = 14)
                    foregroundColor = if (context.selected) {
                        Color.White
                    } else {
                        Color.Black
                    }
                }
            }
        }
    }
}

class TreeNodeModel(private val root: TreeNode?) : TreeModel<TreeNode> {
    override fun child(parent: TreeNode?, index: Int): TreeNode? {
        return when {
            parent == null -> root
            parent.children != null -> parent.children?.getOrNull(index)
            else -> null
        }
    }
    
    override fun children(parent: TreeNode?): Iterator<TreeNode> {
        return when {
            parent == null && root != null -> listOf(root).iterator()
            parent?.children != null -> parent.children!!.iterator()
            else -> emptyList<TreeNode>().iterator()
        }
    }
    
    override fun isLeaf(node: TreeNode): Boolean {
        return node.children?.isEmpty() ?: true
    }
}
```

#### Table → Doodle Table Control

**Record View Table:**

```kotlin
import io.nacular.doodle.controls.table.Table
import io.nacular.doodle.controls.table.TableModel

class RecordTableView(
    private val entry: Entry,
    private val theme: Theme
) : View() {
    
    data class AttributeRow(
        val name: String,
        val values: List<String>
    )
    
    private val tableModel = object : TableModel<AttributeRow> {
        private val rows = entry.attributes.map { (name, values) ->
            AttributeRow(name, values)
        }.sortedBy { it.name }
        
        override val rowCount: Int = rows.size
        override val columnCount: Int = 2
        
        override fun get(row: Int): AttributeRow = rows[row]
        override fun get(row: Int, column: Int): Any? {
            return when (column) {
                0 -> rows[row].name
                1 -> rows[row].values.joinToString(", ")
                else -> null
            }
        }
    }
    
    private val table = Table(
        model = tableModel,
        selectionModel = SingleItemSelectionModel()
    ).apply {
        // Configure columns
        columns[0].header = "Attribute"
        columns[0].width = 200.0
        columns[1].header = "Value(s)"
        columns[1].preferredWidth = 400.0
        
        // Styling
        cellRenderer = attributeCellRenderer()
    }
    
    init {
        layout = constrain(table) { parent ->
            it.top = parent.top
            it.left = parent.left
            it.right = parent.right
            it.bottom = parent.bottom
        }
        
        children += table
    }
    
    private fun attributeCellRenderer() = object : CellRenderer<AttributeRow> {
        override fun invoke(
            row: AttributeRow,
            column: Int,
            index: Int,
            selected: Boolean
        ): View {
            val text = when (column) {
                0 -> row.name
                1 -> row.values.joinToString(" • ", prefix = "• ")
                else -> ""
            }
            
            return container {
                backgroundColor = if (selected) {
                    getGradientColor(0.3)
                } else {
                    Color.Transparent
                }
                
                children += text(text) {
                    font = Font(size = 13)
                    foregroundColor = if (selected) Color.White else Color.Black
                }
            }
        }
    }
}
```

#### Forms → Doodle TextField & Controls

**Start View Configuration Form:**

```kotlin
class ConfigurationView(
    private val viewModel: MainViewModel,
    private val theme: Theme
) : View() {
    
    private val hostField = TextField().apply {
        placeholder = "ldap.example.com"
        text = viewModel.config.ldap.host
    }
    
    private val portField = TextField().apply {
        placeholder = "389"
        text = viewModel.config.ldap.port.toString()
    }
    
    private val baseDnField = TextField().apply {
        placeholder = "dc=example,dc=com"
        text = viewModel.config.ldap.baseDN
    }
    
    private val useSSLCheckbox = CheckBox().apply {
        selected = viewModel.config.ldap.useSSL
    }
    
    private val useTLSCheckbox = CheckBox().apply {
        selected = viewModel.config.ldap.useTLS
    }
    
    private val bindUserField = TextField().apply {
        placeholder = "cn=admin,dc=example,dc=com"
        text = viewModel.config.ldap.bindUser
    }
    
    private val bindPassField = TextField().apply {
        placeholder = "Password"
        text = viewModel.config.ldap.bindPass
        inputType = TextField.InputType.Password
    }
    
    private val connectButton = PushButton("Connect").apply {
        fired += {
            // Update config from form
            viewModel.updateConfig(
                host = hostField.text,
                port = portField.text.toIntOrNull() ?: 389,
                baseDN = baseDnField.text,
                useSSL = useSSLCheckbox.selected,
                useTLS = useTLSCheckbox.selected,
                bindUser = bindUserField.text,
                bindPass = bindPassField.text
            )
            
            // Attempt connection
            viewModelScope.launch {
                viewModel.connect()
            }
        }
    }
    
    init {
        layout = VerticalLayout(spacing = 10.0, padding = 20.0)
        
        children += listOf(
            formRow("Host:", hostField),
            formRow("Port:", portField),
            formRow("Base DN:", baseDnField),
            formRow("Use SSL:", useSSLCheckbox),
            formRow("Use TLS:", useTLSCheckbox),
            formRow("Bind User:", bindUserField),
            formRow("Password:", bindPassField),
            connectButton
        )
    }
    
    private fun formRow(label: String, control: View): View {
        return container {
            layout = HorizontalLayout(spacing = 10.0)
            
            children += text(label) {
                font = Font(size = 14, weight = FontWeight.Bold)
                width = 120.0
            }
            
            children += control.apply {
                width = 300.0
            }
        }
    }
}
```

### 3.4 Layout Strategy

#### Main Application Layout

```kotlin
class MainApplication(
    private val viewModel: MainViewModel,
    private val theme: Theme
) : Application {
    
    override fun run(display: Display) {
        val mainView = container {
            layout = BorderLayout()
            backgroundColor = Color.rgb(0xF5F5F5)
            
            // Tab bar at top
            children += TabBar(viewModel).apply {
                constraints = BorderLayout.Constraints.Top
                height = 50.0
            }
            
            // Content area in center
            children += ContentArea(viewModel, theme).apply {
                constraints = BorderLayout.Constraints.Center
            }
            
            // Status bar at bottom
            children += StatusBar(viewModel).apply {
                constraints = BorderLayout.Constraints.Bottom
                height = 30.0
            }
        }
        
        display += mainView
    }
}

class TabBar(private val viewModel: MainViewModel) : View() {
    init {
        layout = HorizontalLayout(spacing = 5.0, padding = 10.0)
        backgroundColor = Color.rgb(0x333333)
        
        val tabs = listOf(
            "Start" to GuiView.Start,
            "Tree" to GuiView.Tree,
            "Record" to GuiView.Record,
            "Query" to GuiView.Query
        )
        
        tabs.forEach { (label, view) ->
            children += TabButton(label, view, viewModel)
        }
    }
}

class TabButton(
    private val label: String,
    private val view: GuiView,
    private val viewModel: MainViewModel
) : PushButton(label) {
    
    init {
        // Observe current view
        viewModel.currentView.onEach { currentView ->
            backgroundColor = if (currentView == view) {
                Color.rgb(0x0066CC) // Active blue
            } else {
                Color.rgb(0x555555) // Inactive gray
            }
        }.launchIn(viewModelScope)
        
        fired += {
            viewModel.navigateTo(view)
        }
    }
}

class ContentArea(
    private val viewModel: MainViewModel,
    private val theme: Theme
) : View() {
    
    init {
        // Observe current view and swap content
        viewModel.currentView.onEach { view ->
            children.clear()
            
            val content = when (view) {
                GuiView.Start -> ConfigurationView(viewModel, theme)
                GuiView.Tree -> LdapTreeView(viewModel, theme)
                GuiView.Record -> {
                    viewModel.selectedEntry.value?.let {
                        RecordTableView(it, theme)
                    } ?: EmptyView("No record selected")
                }
                GuiView.Query -> QueryView(viewModel, theme)
            }
            
            children += content
        }.launchIn(viewModelScope)
    }
}
```

### 3.5 Event Handling and Navigation

```kotlin
class NavigationController(private val viewModel: MainViewModel) {
    
    fun handleTreeSelection(node: TreeNode) {
        viewModelScope.launch {
            val entry = viewModel.ldapClient.getEntry(node.dn)
            viewModel.selectEntry(entry)
            viewModel.navigateTo(GuiView.Record)
        }
    }
    
    fun handleQueryExecution(query: String) {
        viewModelScope.launch {
            try {
                val results = viewModel.ldapClient.search(
                    baseDN = viewModel.config.ldap.baseDN,
                    filter = query,
                    scope = SearchScope.SUB
                )
                viewModel.setQueryResults(results)
            } catch (e: Exception) {
                viewModel.showError("Query failed: ${e.message}")
            }
        }
    }
    
    fun handleConnectionSave(connection: SavedConnection) {
        viewModel.config.addSavedConnection(connection)
        viewModelScope.launch {
            viewModel.config.save()
        }
    }
}
```

### 3.6 Styling and Theming

**Doodle Theme Definition:**

```kotlin
class MoribitoTheme : Theme {
    // Color palette
    object Colors {
        val primaryBlue = Color.rgb(0x0066CC)
        val secondaryTeal = Color.rgb(0x008080)
        val backgroundLight = Color.rgb(0xF5F5F5)
        val backgroundDark = Color.rgb(0x333333)
        val textPrimary = Color.rgb(0x212121)
        val textSecondary = Color.rgb(0x757575)
        val success = Color.rgb(0x4CAF50)
        val error = Color.rgb(0xF44336)
        val warning = Color.rgb(0xFF9800)
    }
    
    // Typography
    object Fonts {
        val heading = Font(family = "Inter", size = 18, weight = FontWeight.Bold)
        val body = Font(family = "Inter", size = 14, weight = FontWeight.Normal)
        val caption = Font(family = "Inter", size = 12, weight = FontWeight.Normal)
        val code = Font(family = "JetBrains Mono", size = 13, weight = FontWeight.Normal)
    }
    
    // Component styles
    fun buttonStyle(button: PushButton, primary: Boolean = false) {
        button.apply {
            backgroundColor = if (primary) Colors.primaryBlue else Colors.backgroundLight
            foregroundColor = if (primary) Color.White else Colors.textPrimary
            font = Fonts.body
            borderRadius = 4.0
            padding = Insets(8.0, 16.0)
        }
    }
    
    fun textFieldStyle(field: TextField) {
        field.apply {
            backgroundColor = Color.White
            foregroundColor = Colors.textPrimary
            font = Fonts.body
            borderColor = Color.rgb(0xCCCCCC)
            borderWidth = 1.0
            borderRadius = 4.0
            padding = Insets(8.0)
        }
    }
    
    fun tableStyle(table: Table<*>) {
        table.apply {
            backgroundColor = Color.White
            headerBackgroundColor = Colors.backgroundLight
            headerForegroundColor = Colors.textPrimary
            headerFont = Fonts.body.copy(weight = FontWeight.Bold)
            gridColor = Color.rgb(0xE0E0E0)
        }
    }
}
```

**Gradient Utilities:**

```kotlin
object ColorUtils {
    fun blueToTealGradient(position: Double): Color {
        val blue = Color.rgb(0x0066CC)
        val teal = Color.rgb(0x008080)
        
        return Color.rgb(
            red = lerp(blue.red, teal.red, position),
            green = lerp(blue.green, teal.green, position),
            blue = lerp(blue.blue, teal.blue, position)
        )
    }
    
    private fun lerp(start: Float, end: Float, fraction: Double): Float {
        return start + (end - start) * fraction.toFloat()
    }
}
```

### 3.7 GUI Module Structure

```
gui/src/jvmMain/kotlin/com/moribito/gui/
├── MoribitoGuiApp.kt           # Main GUI application
├── viewmodel/
│   ├── MainViewModel.kt        # Main view model
│   ├── TreeViewModel.kt        # Tree-specific VM
│   ├── RecordViewModel.kt      # Record-specific VM
│   └── QueryViewModel.kt       # Query-specific VM
├── views/
│   ├── MainApplicationView.kt  # Main window
│   ├── ConfigurationView.kt    # Start/config screen
│   ├── LdapTreeView.kt         # Tree browser
│   ├── RecordTableView.kt      # Record details
│   └── QueryView.kt            # Query interface
├── components/
│   ├── TabBar.kt               # Tab navigation
│   ├── StatusBar.kt            # Status display
│   ├── ConnectionIndicator.kt  # Connection status
│   ├── LoadingSpinner.kt       # Loading indicator
│   └── SavedConnectionsList.kt # Saved connections
├── theme/
│   ├── MoribitoTheme.kt        # Theme definition
│   ├── Colors.kt               # Color palette
│   ├── Typography.kt           # Font definitions
│   └── Spacing.kt              # Layout spacing
├── navigation/
│   └── NavigationController.kt # Navigation logic
└── utils/
    ├── ColorUtils.kt           # Color utilities
    └── LayoutUtils.kt          # Layout helpers
```

---

## Implementation Timeline

### Overall Migration Phases

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| **Phase 1: Foundation** | 2 weeks | Project setup, build system, core module structure |
| **Phase 2: LDAP Core** | 2 weeks | LDAP client, data models, config management |
| **Phase 3: TUI Development** | 3 weeks | All TUI views, input handling, rendering |
| **Phase 4: GUI Development** | 4 weeks | All GUI views, Doodle integration, theming |
| **Phase 5: Testing & Polish** | 2 weeks | Integration tests, bug fixes, documentation |
| **Phase 6: Release** | 1 week | Packaging, CI/CD, release notes |

**Total Estimated Time: 14 weeks (~3.5 months)**

### Detailed Sprint Breakdown

#### Sprint 1-2: Foundation & Setup (Weeks 1-2)
- ✅ Initialize Kotlin Multiplatform project
- ✅ Configure Gradle build system
- ✅ Set up module structure (core, tui, gui, app-*)
- ✅ Configure CI/CD pipelines
- ✅ Implement version injection
- ✅ Create basic tests framework

#### Sprint 3-4: LDAP Core (Weeks 3-4)
- ✅ Implement LdapClient wrapper (UnboundID SDK)
- ✅ Migrate connection management (SSL/TLS)
- ✅ Implement retry logic with exponential backoff
- ✅ Create data models (Entry, TreeNode, SearchPage)
- ✅ Implement pagination
- ✅ Migrate configuration management
- ✅ Write comprehensive unit tests
- ✅ Set up embedded LDAP for integration tests

#### Sprint 5-7: TUI Development (Weeks 5-7)
- ✅ Set up Mordant framework
- ✅ Implement TuiController and state management
- ✅ Create StartViewRenderer (config screen)
- ✅ Create TreeViewRenderer (tree browser)
- ✅ Create RecordViewRenderer (record details)
- ✅ Create QueryViewRenderer (query interface)
- ✅ Implement input handling (keyboard, mouse)
- ✅ Implement tab navigation
- ✅ Add loading indicators
- ✅ Write TUI integration tests

#### Sprint 8-11: GUI Development (Weeks 8-11)
- ✅ Set up Doodle framework
- ✅ Create MVVM architecture
- ✅ Implement ConfigurationView
- ✅ Implement LdapTreeView with Doodle Tree
- ✅ Implement RecordTableView with Doodle Table
- ✅ Implement QueryView
- ✅ Create MoribitoTheme
- ✅ Implement navigation system
- ✅ Add connection management UI
- ✅ Create status indicators
- ✅ Write GUI tests

#### Sprint 12-13: Testing & Polish (Weeks 12-13)
- ✅ Comprehensive integration testing
- ✅ Performance testing
- ✅ Bug fixes
- ✅ Documentation updates
- ✅ User testing feedback incorporation
- ✅ Accessibility improvements

#### Sprint 14: Release (Week 14)
- ✅ Package TUI application (shadowJar)
- ✅ Package GUI application (shadowJar)
- ✅ Create distribution scripts
- ✅ Update README and documentation
- ✅ Create release notes
- ✅ Publish to GitHub Releases
- ✅ Update Homebrew formula (if applicable)

---

## Risk Assessment & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| **JVM startup latency** | High | Medium | Use GraalVM native-image for future optimization; acceptable for GUI |
| **Doodle learning curve** | Medium | Medium | Start with simple views; extensive documentation review |
| **UnboundID API differences** | Low | Medium | Comprehensive wrapper; extensive testing |
| **Mordant limitations** | Medium | Low | Fallback to custom terminal rendering if needed |
| **Cross-platform issues** | Low | High | Test on all platforms early and often |
| **Performance degradation** | Low | Medium | Profile and optimize; acceptable for typical LDAP operations |
| **Test coverage gaps** | Medium | High | Maintain >80% coverage; add tests incrementally |

---

## Success Criteria

### Functional Requirements
✅ All current Go features replicated in Kotlin
✅ TUI maintains feature parity with current implementation
✅ GUI provides equivalent (or better) functionality
✅ Configuration files remain compatible
✅ Same platform support (Linux, macOS, Windows)

### Performance Requirements
✅ LDAP operations complete within similar timeframes
✅ TUI responsiveness comparable to Go version
✅ GUI provides smooth interaction (60 FPS)
✅ Memory usage acceptable for typical workloads (<500 MB)

### Quality Requirements
✅ Test coverage >80%
✅ No critical bugs at release
✅ Documentation complete and accurate
✅ CI/CD pipeline functional

---

## Post-Migration Considerations

### Future Enhancements
1. **Native Compilation:** GraalVM native-image for TUI application
2. **Additional Platforms:** Explore Kotlin/Native for true native binaries
3. **Web Version:** Doodle's browser target for web-based LDAP explorer
4. **Enhanced GUI:** Advanced features (bookmarks, history, comparison view)
5. **Plugin System:** Extensible architecture for custom LDAP operations

### Maintenance Strategy
1. **Version Strategy:** Semantic versioning (2.0.0 for Kotlin rewrite)
2. **Support Window:** Maintain Go version for 6 months post-Kotlin release
3. **Documentation:** Maintain both versions' docs during transition
4. **Community:** Gradual migration communication plan

---

