# Eclipse Platform Terminal

The Eclipse Platform Terminal provides a comprehensive terminal emulation framework for the Eclipse IDE, enabling users to interact with command-line interfaces directly within the Eclipse workbench.

## Overview

The Terminal feature provides a flexible, extensible terminal emulation system that supports multiple connection types (local processes, SSH, Telnet) and integrates seamlessly with other Eclipse Platform components. It consists of a UI-less terminal model with an asynchronous SWT widget for high-performance terminal operations.

## Architecture

The Terminal feature is organized into several OSGi bundles:

### Core Bundles

#### org.eclipse.terminal.control
The foundation bundle providing the terminal control widget and connector framework.

**Key Features:**
- Terminal widget with ANSI escape sequence support
- Character grid model for terminal display
- Terminal connector extension point (`org.eclipse.terminal.control.connectors`)
- VT100/VT102-compatible terminal emulation (subset)
- Support for screen-oriented applications (vi, Emacs, readline-enabled apps)

**Key APIs:**
- `ITerminalConnector` - Interface for connection type implementations
- `ITerminalControl` - Control interface for terminal operations
- `ITerminalTextData` - Model interface for terminal character data
- `TerminalConnectorExtension` - Extension point for custom connectors

**Extension Point:**
```xml
<extension point="org.eclipse.terminal.control.connectors">
  <connector
      class="com.example.MyTerminalConnector"
      id="com.example.myconnector"
      name="My Custom Connector"/>
</extension>
```

#### org.eclipse.terminal.view.core
Core services and APIs for terminal view management.

**Key Features:**
- Terminal service API for programmatic terminal management
- Terminal lifecycle events and listeners
- Context properties framework
- Line separator handling

**Key APIs:**
- `ITerminalService` - Service for opening, closing, and managing terminals
- `ITerminalTabListener` - Listener interface for terminal tab events
- `ITerminalContextPropertiesProvider` - Provider for terminal context properties

**Usage Example:**
```java
// Get the terminal service
ITerminalService terminalService = // obtain via OSGi service
Map<String, Object> properties = new HashMap<>();
properties.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID, 
               "org.eclipse.terminal.connector.local.LocalConnector");
terminalService.openConsole(properties);
```

#### org.eclipse.terminal.view.ui
User interface components for terminal integration in Eclipse workbench.

**Key Features:**
- Terminal view (`TerminalsView`)
- Multiple terminal tabs support
- Terminal launcher framework
- Preferences and settings UI
- "Show In" integration for explorers
- Stream-based terminal support

**Key APIs:**
- `ILauncherDelegate` - Interface for custom terminal launchers
- Terminal launcher delegate extension point (`org.eclipse.terminal.view.ui.launcherDelegates`)

**Extension Point:**
```xml
<extension point="org.eclipse.terminal.view.ui.launcherDelegates">
  <delegate
      class="com.example.MyLauncherDelegate"
      id="com.example.mylauncher"
      label="My Custom Launcher">
  </delegate>
</extension>
```

**View Integration:**
The terminal view is registered as `org.eclipse.terminal.view.ui.TerminalsView` and appears in:
- Resource Perspective (stacked with Task List)
- Debug Perspective (stacked with Console View)
- Java Perspective (stacked with Problems View)
- PDE Perspective (stacked with Problems View)

### Connector Bundles

#### org.eclipse.terminal.connector.local
Local terminal connector for native shell access.

**Features:**
- Opens system default shell (bash, cmd.exe, etc.)
- PTY (pseudo-terminal) support via CDT utilities
- Working directory context from workspace resources
- "Show In > Terminal" integration for Project/Package Explorer

**Usage:**
- Right-click on a project/folder in Project Explorer
- Navigate > Show In > Terminal
- Opens terminal in the selected resource's directory

#### org.eclipse.terminal.connector.process
Generic process connector for custom command execution.

**Features:**
- Execute arbitrary processes with terminal UI
- Capture stdout/stderr in terminal
- Process environment configuration
- Working directory support

**Key APIs:**
- `ProcessConnector` - Connector for process-based terminals
- `IProcessSettings` - Configuration interface for process execution

#### org.eclipse.terminal.connector.ssh
SSH terminal connector using JSch library.

**Features:**
- SSH connection support
- Password and key-based authentication
- Session management
- Eclipse secure storage integration

**Configuration:**
Users can configure SSH connections via the Terminal view's connection dialog, providing:
- Host and port
- Username
- Authentication method (password or key file)

#### org.eclipse.terminal.connector.telnet
Telnet terminal connector for legacy systems.

**Features:**
- Telnet protocol support
- Connection dialog with host/port configuration
- Session management

## Integration with Other Platform Components

### Debug Framework (org.eclipse.debug.terminal)

The Terminal feature is integrated with the Eclipse Debug framework to provide terminal-based process interaction.

**Key Integration Points:**

1. **Process Factories:**
   - `PtyProcessFactory` - Creates terminal-backed process instances
   - Extension point: `org.eclipse.debug.core.processFactories`
   
2. **Exec Factories:**
   - `PtyExecFactory` - Provides process execution with PTY support
   - Extension point: `org.eclipse.debug.core.execFactories`

3. **Console Integration:**
   - `TerminalConsoleFactory` - Creates terminal consoles in Console view
   - Extension point: `org.eclipse.ui.console.consoleFactories`
   - Accessible via Console view dropdown > "Terminal"

**Benefits:**
- Native shell interaction for debug launches
- Full terminal capabilities for launched processes
- Better support for interactive applications
- Pseudo-terminal (PTY) support for Unix-like behavior

**Usage in Debug Configurations:**
Debug configurations can specify the terminal connector to use for process I/O, enabling full terminal capabilities for launched applications.

### CDT Integration

The Terminal feature uses CDT (C/C++ Development Tools) utilities for platform-specific PTY and process spawning:
- `org.eclipse.cdt.utils.pty` - PTY (pseudo-terminal) support
- `org.eclipse.cdt.utils.spawner` - Native process spawning

Platform-specific fragments are required:
- `org.eclipse.cdt.core.linux.x86_64`
- `org.eclipse.cdt.core.macosx`
- `org.eclipse.cdt.core.win32.x86_64`
- And others for different architectures

## Features and Capabilities

### Terminal Emulation
- **ANSI Control Characters:** NUL, backspace, carriage return, linefeed
- **ANSI Escape Sequences:** Cursor movement, text formatting, colors
- **Screen Support:** Compatible with vi, Emacs, nano, less, top, htop
- **Readline Support:** Full support for GNU readline (used in bash, bc, Python REPL, etc.)
- **Color Support:** 16-color ANSI palette
- **Text Styling:** Bold, underline, reverse video

### User Interface
- **Multiple Tabs:** Support for multiple terminal sessions in one view
- **Tab Naming:** Automatic and custom tab naming
- **Context Menus:** Copy, paste, clear, terminate, close actions
- **Scrollback Buffer:** Configurable history size
- **Text Selection:** Mouse-based text selection and copying
- **Font Configuration:** Customizable terminal font and size
- **Encoding Support:** Configurable character encoding

### Programmatic Access
- **Terminal Service:** Open/close terminals programmatically
- **Connector API:** Create custom connection types
- **Launcher API:** Integrate custom launchers
- **Event Listeners:** Monitor terminal lifecycle events
- **Stream API:** Redirect streams to/from terminals

## Extending the Terminal Feature

### Creating a Custom Terminal Connector

1. **Implement the Connector:**
```java
public class MyConnector extends AbstractTerminalConnector {
    @Override
    public void connect(ITerminalControl control) {
        // Establish connection
        // Get streams and connect to terminal
        OutputStream terminalToRemote = getTerminalToRemoteStream();
        InputStream remoteToTerminal = // your input source
        
        // Start pumping data
        // ...
    }
    
    @Override
    public void disconnect() {
        // Clean up connection
    }
    
    @Override
    public OutputStream getTerminalToRemoteStream() {
        return outputStream;
    }
}
```

2. **Register via Extension Point:**
```xml
<extension point="org.eclipse.terminal.control.connectors">
  <connector
      class="com.example.MyConnector"
      id="com.example.myconnector"
      name="My Connector"/>
</extension>
```

### Creating a Custom Launcher Delegate

1. **Implement the Delegate:**
```java
public class MyLauncherDelegate implements ILauncherDelegate {
    @Override
    public boolean needsUserConfiguration() {
        return true; // Show configuration dialog
    }
    
    @Override
    public void execute(Map<String, Object> properties, 
                       ILauncherDoneCallback callback) {
        // Configure and launch terminal
        properties.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID,
                      "com.example.myconnector");
        // ...
        callback.done(Status.OK_STATUS);
    }
}
```

2. **Register via Extension Point:**
```xml
<extension point="org.eclipse.terminal.view.ui.launcherDelegates">
  <delegate
      class="com.example.MyLauncherDelegate"
      id="com.example.mylauncher"
      label="My Launcher"/>
</extension>
```

## Configuration and Preferences

Terminal preferences are available at: **Window > Preferences > Terminal**

Key settings:
- **Font:** Terminal display font and size
- **Buffer Size:** Scrollback buffer line limit
- **Encoding:** Character encoding for terminal I/O
- **Invert Colors:** Black-on-white or white-on-black display
- **Local Echo:** Echo characters locally (for certain connector types)

## Building and Testing

### Build Individual Bundles
```bash
cd terminal/bundles/<bundle-name>
mvn clean verify -Pbuild-individual-bundles
```

### Build All Terminal Bundles
```bash
cd terminal
mvn clean verify
```

### Run Tests
```bash
cd terminal/tests/org.eclipse.terminal.test
mvn clean verify -Pbuild-individual-bundles
```

**Note:** Some tests require a graphical display. Use `Xvfb` or `Xvnc` in headless environments.

## Dependencies

### Required Bundles
- Eclipse Platform UI (`org.eclipse.ui`)
- Eclipse Core Runtime (`org.eclipse.core.runtime`)
- Eclipse SWT (`org.eclipse.swt`)
- CDT Core Native utilities (platform-specific)

### Optional Bundles
- `org.eclipse.core.resources` - For workspace integration
- `org.eclipse.debug.ui` - For debug integration
- `org.eclipse.jsch.core` - For SSH support

## Known Limitations

- **VT100/VT102 Compatibility:** Not fully compliant; supports a commonly-used subset
- **Unicode Support:** Limited support for wide characters and combining characters
- **Terminal Size:** May not update properly for some edge cases
- **Platform Dependencies:** Requires platform-specific CDT fragments for PTY support

## Contributing

Contributions to the Terminal feature are welcome! Please follow the [Eclipse Platform contribution guidelines](../../CONTRIBUTING.md).

Key areas for contribution:
- Enhanced VT100/VT102 compatibility
- Additional connector implementations
- Performance improvements
- Unicode and internationalization support
- Additional platform support

## Resources

- **Terminal Control Bundle README:** `bundles/org.eclipse.terminal.control/README.txt`
- **Extension Point Schemas:** `bundles/*/schema/*.exsd`
- **Eclipse Terminal Wiki:** https://wiki.eclipse.org/Terminal
- **Bug Reports:** https://github.com/eclipse-platform/eclipse.platform/issues

## License

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/

SPDX-License-Identifier: EPL-2.0
