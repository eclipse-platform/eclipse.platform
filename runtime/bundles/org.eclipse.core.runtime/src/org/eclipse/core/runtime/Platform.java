/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gunnar Wagenknecht <gunnar@wagenknecht.org> - Fix for bug 265445
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - Fix for bug 265532
 *     Lars Vogel<Lars.Vogel@vogella.com> - Bug 478768
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.eclipse.core.internal.runtime.AuthorizationHandler;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.internal.runtime.Messages;
import org.eclipse.core.internal.runtime.MetaDataKeeper;
import org.eclipse.core.internal.runtime.PlatformActivator;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The central class of the Eclipse Platform Runtime. This class cannot
 * be instantiated or subclassed by clients; all functionality is provided
 * by static methods.  Features include:
 * <ul>
 * <li>the platform registry of installed plug-ins</li>
 * <li>the platform adapter manager</li>
 * <li>the platform log</li>
 * <li>the authorization info management</li>
 * </ul>
 * <p>
 * Most users don't have to worry about Platform's lifecycle. However, if your
 * code can call methods of this class when Platform is not running, it becomes
 * necessary to check {@link #isRunning()} before making the call. A runtime
 * exception might be thrown or incorrect result might be returned if a method
 * from this class is called while Platform is not running.
 * </p>
 */
public final class Platform {

	/**
	 * Convenience class to query for the current OS.
	 *
	 * @since 3.30
	 */
	public static final class OS {
		private OS() {
			// avoid instantiation
		}

		/**
		 * @param osString the identifier for the OS (use one of the constants that
		 *                 start with <code>OS_</code> in the <code>Platform</code>
		 *                 class).
		 *
		 * @return <code>true</code> if the current platform is the one specified as a
		 *         parameter, <code>false</code> in all other cases.
		 * @see Platform#getOS()
		 * @since 3.30
		 */
		public static boolean is(String osString) {
			return Platform.getOS().equals(osString);
		}

		/**
		 * @return <code>true</code> if the current OS is Windows
		 */
		public static boolean isWindows() {
			return is(OS_WIN32);
		}

		/**
		 * @return <code>true</code> if the current OS is Linux
		 */
		public static boolean isLinux() {
			return is(OS_LINUX);
		}

		/**
		 * @return <code>true</code> if the current OS is FreeBSD
		 */
		public static boolean isFreeBSD() {
			return is(OS_FREEBSD);
		}

		/**
		 * @return <code>true</code> if the current OS is MacOSX
		 */
		public static boolean isMac() {
			return is(OS_MACOSX);
		}

	}
	/**
	 * The unique identifier constant (value "<code>org.eclipse.core.runtime</code>")
	 * of the Core Runtime (pseudo-) plug-in.
	 */
	public static final String PI_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$

	/**
	 * The simple identifier constant (value "<code>applications</code>") of
	 * the extension point of the Core Runtime plug-in where plug-ins declare
	 * the existence of runnable applications. A plug-in may define any
	 * number of applications; however, the platform is only capable
	 * of running one application at a time.
	 */
	public static final String PT_APPLICATIONS = "applications"; //$NON-NLS-1$

	/**
	 * The simple identifier constant (value "<code>adapters</code>") of
	 * the extension point of the Core Runtime plug-in where plug-ins declare
	 * the existence of adapter factories. A plug-in may define any
	 * number of adapters.
	 *
	 * @see IAdapterManager#hasAdapter(Object, String)
	 * @since 3.0
	 */
	public static final String PT_ADAPTERS = "adapters"; //$NON-NLS-1$

	/**
	 * The simple identifier constant (value "<code>preferences</code>") of
	 * the extension point of the Core Runtime plug-in where plug-ins declare
	 * extensions to the preference facility. A plug-in may define any number
	 * of preference extensions.
	 *
	 * @see #getPreferencesService()
	 * @since 3.0
	 */
	public static final String PT_PREFERENCES =  "preferences"; //$NON-NLS-1$

	/**
	 * The simple identifier constant (value "<code>products</code>") of
	 * the extension point of the Core Runtime plug-in where plug-ins declare
	 * the existence of a product. A plug-in may define any
	 * number of products; however, the platform is only capable
	 * of running one product at a time.
	 *
	 * @see #getProduct()
	 * @since 3.0
	 */
	public static final String PT_PRODUCT = "products"; //$NON-NLS-1$

	/**
	 * Debug option value denoting the time at which the platform runtime
	 * was started.  This constant can be used in conjunction with
	 * <code>getDebugOption</code> to find the string value of
	 * <code>System.currentTimeMillis()</code> when the platform was started.
	 */
	public static final String OPTION_STARTTIME = PI_RUNTIME + "/starttime"; //$NON-NLS-1$

	/**
	 * Name of a preference for configuring the performance level for this system.
	 *
	 * <p>
	 * This value can be used by all components to customize features to suit the
	 * speed of the user's machine.  The platform job manager uses this value to make
	 * scheduling decisions about background jobs.
	 * </p>
	 * <p>
	 * The preference value must be an integer between the constant values
	 * MIN_PERFORMANCE and MAX_PERFORMANCE
	 * </p>
	 * @see #MIN_PERFORMANCE
	 * @see #MAX_PERFORMANCE
	 * @since 3.0
	 */
	public static final String PREF_PLATFORM_PERFORMANCE = "runtime.performance"; //$NON-NLS-1$

	/**
	 * Constant (value "line.separator") name of the preference used for storing
	 * the line separator.
	 *
	 * @see #knownPlatformLineSeparators
	 * @since 3.1
	 */
	public static final String PREF_LINE_SEPARATOR = "line.separator"; //$NON-NLS-1$

	/**
	 * Constant (value 1) indicating the minimum allowed value for the
	 * <code>PREF_PLATFORM_PERFORMANCE</code> preference setting.
	 * @since 3.0
	 */
	public static final int MIN_PERFORMANCE = 1;

	/**
	 * Constant (value 5) indicating the maximum allowed value for the
	 * <code>PREF_PLATFORM_PERFORMANCE</code> preference setting.
	 * @since 3.0
	 */
	public static final int MAX_PERFORMANCE = 5;

	/**
	 * Status code constant (value 1) indicating a problem in a plug-in
	 * manifest (<code>plugin.xml</code>) file.
	 */
	public static final int PARSE_PROBLEM = 1;

	/**
	 * Status code constant (value 2) indicating an error occurred while running a plug-in.
	 */
	public static final int PLUGIN_ERROR = 2;

	/**
	 * Status code constant (value 3) indicating an error internal to the
	 * platform has occurred.
	 */
	public static final int INTERNAL_ERROR = 3;

	/**
	 * Status code constant (value 4) indicating the platform could not read
	 * some of its metadata.
	 */
	public static final int FAILED_READ_METADATA = 4;

	/**
	 * Status code constant (value 5) indicating the platform could not write
	 * some of its metadata.
	 */
	public static final int FAILED_WRITE_METADATA = 5;

	/**
	 * Status code constant (value 6) indicating the platform could not delete
	 * some of its metadata.
	 */
	public static final int FAILED_DELETE_METADATA = 6;

	/**
	 * Constant string (value {@code win32}) indicating the platform is running on a
	 * Window 32-bit operating system (e.g., Windows 98, NT, 2000).
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 */
	public static final String OS_WIN32 = "win32";//$NON-NLS-1$

	/**
	 * Constant string (value {@code linux} indicating the platform is running on a
	 * Linux-based operating system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 */
	public static final String OS_LINUX = "linux";//$NON-NLS-1$

	/**
	 * Constant string (value {@code freebsd}) indicating the platform is running on a
	 * FreeBSD-based operating system.
	 *
	 * @since 4.32
	 */
	public static final String OS_FREEBSD = "freebsd";//$NON-NLS-1$

	/**
	 * Constant string (value {@code aix} indicating the platform is running on an
	 * AIX-based operating system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String OS_AIX = "aix";//$NON-NLS-1$

	/**
	 * Constant string (value {@code solaris}) indicating the platform is running on
	 * a Solaris-based operating system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String OS_SOLARIS = "solaris";//$NON-NLS-1$

	/**
	 * Constant string (value {@code hpux}) indicating the platform is running on an
	 * HP/UX-based operating system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String OS_HPUX = "hpux";//$NON-NLS-1$

	/**
	 * Constant string (value {@code qnx}) indicating the platform is running on a
	 * QNX-based operating system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String OS_QNX = "qnx";//$NON-NLS-1$

	/**
	 * Constant string (value {@code macosx}) indicating the platform is running on
	 * a Mac OS X operating system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 */
	public static final String OS_MACOSX = "macosx";//$NON-NLS-1$

	/**
	 * Constant string (value {@code unknown}) indicating the platform is running on
	 * a machine running an unknown operating system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 */
	public static final String OS_UNKNOWN = "unknown";//$NON-NLS-1$

	/**
	 * Constant string (value {@code x86}) indicating the platform is running on an
	 * x86-based architecture.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String ARCH_X86 = "x86";//$NON-NLS-1$

	/**
	 * Constant string (value {@code PA_RISC}) indicating the platform is running on
	 * an PA_RISC-based architecture.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String ARCH_PA_RISC = "PA_RISC";//$NON-NLS-1$

	/**
	 * Constant string (value {@code ppc}) indicating the platform is running on an
	 * PowerPC-based architecture.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String ARCH_PPC = "ppc";//$NON-NLS-1$

	/**
	 * Constant string (value {@code sparc}) indicating the platform is running on
	 * an Sparc-based architecture.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String ARCH_SPARC = "sparc";//$NON-NLS-1$

	/**
	 * Constant string (value {@code x86_64}) indicating the platform is running on
	 * an x86 64bit-based architecture.
	 *
	 * @since 3.1
	 */
	public static final String ARCH_X86_64 = "x86_64";//$NON-NLS-1$

	/**
	 * Constant string (value {@code aarch64}) indicating the platform is running on
	 * an AARCH64bit-based architecture.
	 *
	 * @since 3.22
	 */
	public static final String ARCH_AARCH64 = "aarch64";//$NON-NLS-1$

	/**
	 * Constant string (value {@code ppc64le}) indicating the platform is running on
	 * an little-endian PowerPC 64bit based architecture.
	 *
	 * @since 3.32
	 */
	public static final String ARCH_PPC64LE = "ppc64le"; //$NON-NLS-1$

	/**
	 * Constant string (value {@code riscv64} indicating the platform is running on
	 * an RISC-V 64bit-based architecture.
	 *
	 * @since 3.32
	 *
	 */
	public static final String ARCH_RISCV64 = "riscv64";//$NON-NLS-1$

	/**
	 * Constant string (value {@code amd64}) indicating the platform is running on
	 * an AMD64-based architecture.
	 *
	 * @since 3.0
	 * @deprecated use <code>ARCH_X86_64</code> instead. Note the values has been
	 *             changed to be the value of the <code>ARCH_X86_64</code> constant.
	 */
	@Deprecated
	public static final String ARCH_AMD64 = ARCH_X86_64;

	/**
	 * Constant string (value {@code ia64}) indicating the platform is running on an
	 * IA64-based architecture.
	 *
	 * @since 3.0
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String ARCH_IA64 = "ia64"; //$NON-NLS-1$

	/**
	 * Constant string (value {@code ia64_32}) indicating the platform is running on
	 * an IA64 32bit-based architecture.
	 *
	 * @since 3.1
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated
	public static final String ARCH_IA64_32 = "ia64_32";//$NON-NLS-1$

	/**
	 * Constant string (value {@code win32}) indicating the platform is running on a
	 * machine using the Windows windowing system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 */
	public static final String WS_WIN32 = "win32";//$NON-NLS-1$

	/**
	 * Constant string (value {@code motif}) indicating the platform is running on a
	 * machine using the Motif windowing system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 *
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String WS_MOTIF = "motif";//$NON-NLS-1$

	/**
	 * Constant string (value {@code gtk}) indicating the platform is running on a
	 * machine using the GTK windowing system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 */
	public static final String WS_GTK = "gtk";//$NON-NLS-1$

	/**
	 * Constant string (value {@code photon}) indicating the platform is running on
	 * a machine using the Photon windowing system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String WS_PHOTON = "photon";//$NON-NLS-1$

	/**
	 * Constant string (value {@code carbon}) indicating the platform is running on
	 * a machine using the Carbon windowing system (Mac OS X).
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @deprecated not supported anymore
	 *
	 * @since 3.0
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String WS_CARBON = "carbon";//$NON-NLS-1$

	/**
	 * Constant string (value {@code cocoa}) indicating the platform is running on a
	 * machine using the Cocoa windowing system (Mac OS X).
	 *
	 * @since 3.5
	 */
	public static final String WS_COCOA = "cocoa";//$NON-NLS-1$

	/**
	 * Constant string (value {@code wpf}) indicating the platform is running on a
	 * machine using the WPF windowing system.
	 *
	 * @since 3.3
	 * @deprecated not supported anymore
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static final String WS_WPF = "wpf";//$NON-NLS-1$

	/**
	 * Constant string (value {@code unknown}) indicating the platform is running on
	 * a machine running an unknown windowing system.
	 * <p>
	 * Note this constant has been moved from the deprecated
	 * org.eclipse.core.boot.BootLoader class and its value has not changed.
	 * </p>
	 *
	 * @since 3.0
	 */
	public static final String WS_UNKNOWN = "unknown";//$NON-NLS-1$

	// private constants for platform line separators and their associated platform names
	private static final String LINE_SEPARATOR_KEY_UNIX = Messages.line_separator_platform_unix;
	private static final String LINE_SEPARATOR_KEY_WINDOWS = Messages.line_separator_platform_windows;

	private static final String LINE_SEPARATOR_VALUE_LF = "\n"; //$NON-NLS-1$
	private static final String LINE_SEPARATOR_VALUE_CRLF = "\r\n"; //$NON-NLS-1$
	private static final Charset SYSTEM_CHARSET;

	static {
		Charset result = null;
		try {
			// JEP 400: Java 17+ populates this system property.
			String encoding = System.getProperty("native.encoding"); //$NON-NLS-1$
			if (encoding != null && !encoding.isBlank()) {
				result = Charset.forName(encoding);
			} else {
				// JVM internal property, works on older JVM's too
				encoding = System.getProperty("sun.jnu.encoding"); //$NON-NLS-1$
				if (encoding != null && !encoding.isBlank()) {
					result = Charset.forName(encoding);
				}
			}
		} catch (Exception e) {
			// We have no log at this moment, so just print to std error
			e.printStackTrace();
		}
		if (result == null) {
			// This is always UTF-8 on Java >= 18.
			result = Charset.defaultCharset();
		}
		SYSTEM_CHARSET = result;
	}

	/**
	 * Private constructor to block instance creation.
	 */
	private Platform() {
		super();
	}

	/**
	 * Adds the given log listener to the notification list of the platform.
	 * <p>
	 * Once registered, a listener starts receiving notification as entries
	 * are added to plug-in logs via <code>ILog.log()</code>. The listener continues to
	 * receive notifications until it is replaced or removed.
	 * </p>
	 *
	 * @param listener the listener to register
	 * @see ILog#addLogListener(ILogListener)
	 * @see #removeLogListener(ILogListener)
	 */
	public static void addLogListener(ILogListener listener) {
		InternalPlatform.getDefault().addLogListener(listener);
	}

	/**
	 * Adds the specified resource to the protection space specified by the
	 * given realm. All targets at or deeper than the depth of the last
	 * symbolic element in the path of the given resource URL are assumed to
	 * be in the same protection space.
	 *
	 * @param resourceUrl the URL identifying the resources to be added to
	 *		the specified protection space. For example,
	 *		"http://www.example.com/folder/".
	 * @param realm the name of the protection space. For example,
	 *		"realm1@example.com"
	 * @exception CoreException if there are problems setting the
	 *		authorization information. Reasons include:
	 * <ul>
	 * <li>The key ring could not be saved.</li>
	 * </ul>
	 * @deprecated Authorization database is superseded by the Equinox secure storage.
	 * Use <code>org.eclipse.equinox.security.storage.SecurePreferencesFactory</code>
	 * to obtain secure preferences and <code>org.eclipse.equinox.security.storage.ISecurePreferences</code>
	 * for data access and modifications.
	 * This API will be deleted in a future release. See bug 370248 for details.
	 */
	@Deprecated(forRemoval = true, since = "2024-03")
	public static void addProtectionSpace(URL resourceUrl, String realm) throws CoreException {
		AuthorizationHandler.addProtectionSpace(resourceUrl, realm);
	}

	/**
	 * Returns a URL that is the local equivalent of the
	 * supplied URL. This method is expected to be used with the
	 * plug-in-relative URLs returned by IPluginDescriptor, Bundle.getEntry()
	 * and Platform.find().
	 * If the specified URL is not a plug-in-relative URL, it
	 * is returned as is. If the specified URL is a plug-in-relative
	 * URL of a file (including .jar archive), it is returned as
	 * a locally accessible URL using "file:" protocol
	 * (extracting/caching the file locally, if required). If the specified URL
	 * is a plug-in-relative URL of a directory, the directory and any files and directories
	 * under it are made locally accessible likewise.
	 *
	 * @param url original plug-in-relative URL.
	 * @return the resolved URL
	 * @exception IOException if unable to resolve URL
	 * @see #resolve(URL)
	 * @see #find(Bundle, IPath)
	 * @see Bundle#getEntry(String)
	 * @deprecated use {@link FileLocator#toFileURL(URL)} instead
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static URL asLocalURL(URL url) throws IOException {
		return FileLocator.toFileURL(url);
	}

	/**
	 * Takes down the splash screen if one was put up.
	 * @deprecated use {@link IApplicationContext#applicationRunning()} instead
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static void endSplash() {
		InternalPlatform.getDefault().endSplash();
	}


	/**
	 * Returns the adapter manager used for extending
	 * <code>IAdaptable</code> objects.
	 *
	 * @return the adapter manager for this platform
	 * @see IAdapterManager
	 */
	public static IAdapterManager getAdapterManager() {
		return InternalPlatform.getDefault().getAdapterManager();
	}


	/**
	 * Returns the command line args provided to the Eclipse runtime layer when it was first run.
	 * The returned value does not include arguments consumed by the lower levels of Eclipse
	 * (e.g., OSGi or the launcher).
	 * Note that individual platform runnables may be provided with different arguments
	 * if they are being run individually rather than with <code>Platform.run()</code>.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and query it for
	 * the command-line arguments.
	 * </p>
	 * @return the command line used to start the platform
	 */
	public static String[] getCommandLineArgs() {
		return InternalPlatform.getDefault().getCommandLineArgs();
	}

	/**
	 * Returns the content type manager.
	 * <p>
	 * Clients are also able to acquire the {@link IContentTypeManager} service.
	 * </p>
	 * @return the content type manager
	 * @since 3.0
	 */
	public static IContentTypeManager getContentTypeManager() {
		return InternalPlatform.getDefault().getContentTypeManager();
	}

	/**
	 * Returns the identified option.  <code>null</code>
	 * is returned if no such option is found.   Options are specified
	 * in the general form <i>&lt;plug-in id&gt;/&lt;option-path&gt;</i>.
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 * <p>
	 * Clients are also able to acquire the {@link DebugOptions} service
	 * and query it for debug options.
	 * </p>
	 * @param option the name of the option to lookup
	 * @return the value of the requested debug option or <code>null</code>
	 */
	public static String getDebugOption(String option) {
		return InternalPlatform.getDefault().getOption(option);
	}

	/**
	 * Returns whether the identified option is <code>true</code>.
	 *
	 * <code>false</code> is returned if no such option is found, or if the string
	 * is not "true" (ignoring case).
	 *
	 * Options are specified in the general form <i>&lt;plug-in
	 * id&gt;/&lt;option-path&gt;</i>.
	 *
	 * For example, <code>org.eclipse.core.runtime/debug=true</code>
	 *
	 * Clients are also able to acquire the {@link DebugOptions} service and query
	 * it for debug options.
	 *
	 * See @link {@link Platform#getDebugOption(String)}
	 *
	 * @param option the name of the option to lookup
	 * @return <code>true</code> if the value is equal to the string "true" or
	 *         <code>false</code> otherwise
	 * @since 3.19
	 */
	public static boolean getDebugBoolean(String option) {
		return "true".equalsIgnoreCase(InternalPlatform.getDefault().getOption(option)); //$NON-NLS-1$
	}

	/**
	 * Returns the location of the platform working directory.
	 * <p>
	 * Callers of this method should consider using <code>getInstanceLocation</code>
	 * instead.  In various, typically non IDE-related configurations of Eclipse, the platform
	 * working directory may not be on the local file system.  As such, the more general
	 * form of this location is as a URL.
	 * </p><p>
	 * Alternatively, instead of calling <code>getInstanceLocation</code> clients are
	 * able to acquire the {@link Location} service (with the type {@link Location#INSTANCE_FILTER})
	 * and then change the resulting URL to a path. See the javadoc for <code>getInstanceLocation</code>
	 * for more details.
	 * </p>
	 * @return the location of the platform
	 * @see #getInstanceLocation()
	 */
	public static IPath getLocation() throws IllegalStateException {
		return InternalPlatform.getDefault().getLocation();
	}

	/**
	 * Returns the location of the platform log file.  This file may contain information
	 * about errors that have previously occurred during this invocation of the Platform.
	 * <p>
	 * It is recommended not to keep this value, as the log location may vary when an instance
	 * location is being set.</p>
	 * <p>
	 * Note: it is very important that users of this method do not leave the log
	 * file open for extended periods of time.  Doing so may prevent others
	 * from writing to the log file, which could result in important error messages
	 * being lost.  It is strongly recommended that clients wanting to read the
	 * log file for extended periods should copy the log file contents elsewhere,
	 * and immediately close the original file.</p>
	 * @return the path of the log file on disk.
	 */
	public static IPath getLogFileLocation() {
		return MetaDataKeeper.getMetaArea().getLogLocation();
	}

	/**
	 * As the org.eclipse.core.runtime.compatibility plug-in has been removed in
	 * Eclipse 4.6 this method is not supported anymore.
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static Plugin getPlugin(String id) {
		return null;
	}


	/**
	 * Removes the indicated (identical) log listener from the notification list
	 * of the platform.  If no such listener exists, no action is taken.
	 *
	 * @param listener the listener to de-register
	 * @see ILog#removeLogListener(ILogListener)
	 * @see #addLogListener(ILogListener)
	 */
	public static void removeLogListener(ILogListener listener) {
		InternalPlatform.getDefault().removeLogListener(listener);
	}

	/**
	 * Returns a URL which is the resolved equivalent of the
	 * supplied URL. This method is expected to be used with the
	 * plug-in-relative URLs returned by IPluginDescriptor, Bundle.getEntry()
	 * and Platform.find().
	 * <p>
	 * If the specified URL is not a plug-in-relative URL, it is returned
	 * as is. If the specified URL is a plug-in-relative URL, this method
	 * attempts to reduce the given URL to one which is native to the Java
	 * class library (eg. file, http, etc).
	 * </p><p>
	 * Note however that users of this API should not assume too much about the
	 * results of this method.  While it may consistently return a file: URL in certain
	 * installation configurations, others may result in jar: or http: URLs.
	 * </p>
	 * @param url original plug-in-relative URL.
	 * @return the resolved URL
	 * @exception IOException if unable to resolve URL
	 * @see #asLocalURL(URL)
	 * @see #find(Bundle, IPath)
	 * @see Bundle#getEntry(String)
	 * @deprecated use {@link FileLocator#resolve(URL)} instead
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static URL resolve(URL url) throws IOException {
		return FileLocator.resolve(url);
	}

	/**
	 * Runs the given runnable in a protected mode.   Exceptions
	 * thrown in the runnable are logged and passed to the runnable's
	 * exception handler.  Such exceptions are not rethrown by this method.
	 *
	 * @param runnable the runnable to run
	 * @deprecated clients should use <code>SafeRunner#run</code> instead
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static void run(ISafeRunnable runnable) {
		SafeRunner.run(runnable);
	}

	/**
	 * Returns the extension registry for this platform.
	 * <p>
	 * Note this method is purely a convenience and {@link RegistryFactory#getRegistry()}
	 * should generally be used instead.
	 * </p>
	 * @return the extension registry
	 * @see IExtensionRegistry
	 * @since 3.0
	 */
	public static IExtensionRegistry getExtensionRegistry() {
		return RegistryFactory.getRegistry();
	}

	/**
	 * Returns a URL for the given path in the given bundle. Returns
	 * <code>null</code> if the URL could not be computed or created.
	 *
	 * @param bundle the bundle in which to search
	 * @param path   path relative to plug-in installation location
	 * @return a URL for the given path or <code>null</code>. The actual form of the
	 *         returned URL is not specified.
	 * @see #find(Bundle, IPath, Map)
	 * @see #resolve(URL)
	 * @see #asLocalURL(URL)
	 * @since 3.0
	 * @deprecated use {@link FileLocator#find(Bundle, IPath)}
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static URL find(Bundle bundle, IPath path) {
		return FileLocator.find(bundle, path);
	}

	/**
	 * Returns a URL for the given path in the given bundle. Returns
	 * <code>null</code> if the URL could not be computed or created.
	 * <p>
	 * find looks for this path in given bundle and any attached fragments.
	 * <code>null</code> is returned if no such entry is found. Note that there is
	 * no specific order to the fragments.
	 * </p>
	 * <p>
	 * The following arguments may also be used:
	 * </p>
	 *
	 * <pre>
	 *     $nl$ - for language specific information
	 *     $os$ - for operating system specific information
	 *     $ws$ - for windowing system specific information
	 * </pre>
	 * <p>
	 * A path of $nl$/about.properties in an environment with a default locale of
	 * en_CA will return a URL corresponding to the first place about.properties is
	 * found according to the following order:
	 * </p>
	 *
	 * <pre>
	 *     plugin root/nl/en/CA/about.properties
	 *     fragment1 root/nl/en/CA/about.properties
	 *     fragment2 root/nl/en/CA/about.properties
	 *     ...
	 *     plugin root/nl/en/about.properties
	 *     fragment1 root/nl/en/about.properties
	 *     fragment2 root/nl/en/about.properties
	 *     ...
	 *     plugin root/about.properties
	 *     fragment1 root/about.properties
	 *     fragment2 root/about.properties
	 *     ...
	 * </pre>
	 * <p>
	 * The current environment variable values can be overridden using the override
	 * map argument.
	 * </p>
	 *
	 * @param bundle   the bundle in which to search
	 * @param path     file path relative to plug-in installation location
	 * @param override map of override substitution arguments to be used for any
	 *                 $arg$ path elements. The map keys correspond to the
	 *                 substitution arguments (eg. "$nl$" or "$os$"). The resulting
	 *                 values must be of type java.lang.String. If the map is
	 *                 <code>null</code>, or does not contain the required
	 *                 substitution argument, the default is used.
	 * @return a URL for the given path or <code>null</code>. The actual form of the
	 *         returned URL is not specified.
	 * @see #resolve(URL)
	 * @see #asLocalURL(URL)
	 * @since 3.0
	 * @deprecated use {@link FileLocator#find(Bundle, IPath, Map)} instead
	 */
	@Deprecated(forRemoval = true, since = "2025-03")
	public static URL find(Bundle bundle, IPath path, Map<String,String> override) {
		return FileLocator.find(bundle, path, override);
	}

	/**
	 * Returns the location in the local file system of the
	 * plug-in state area for the given bundle.
	 * If the plug-in state area did not exist prior to this call,
	 * it is created.
	 * <p>
	 * The plug-in state area is a file directory within the
	 * platform's metadata area where a plug-in is free to create files.
	 * The content and structure of this area is defined by the plug-in,
	 * and the particular plug-in is solely responsible for any files
	 * it puts there. It is recommended for plug-in preference settings and
	 * other configuration parameters.
	 * </p>
	 *
	 * @param bundle the bundle whose state location if returned
	 * @return a local file system path
	 * @since 3.0
	 */
	public static IPath getStateLocation(Bundle bundle) {
		return InternalPlatform.getDefault().getStateLocation(bundle);
	}

	/**
	 * Returns a number that changes whenever the set of installed plug-ins
	 * changes. This can be used for invalidating caches that are based on
	 * the set of currently installed plug-ins. (e.g. extensions)
	 * <p>
	 * Clients are also able to acquire the {@link PlatformAdmin} service
	 * and get the timestamp from its state object.
	 * </p>
	 * @return a number related to the set of installed plug-ins
	 * @since 3.1
	 */
	public static long getStateStamp() {
		return InternalPlatform.getDefault().getStateTimeStamp();
	}

	/**
	 * Returns the log for the given bundle.  If no such log exists, one is created.
	 *
	 * @param bundle the bundle whose log is returned
	 * @return the log for the given bundle
	 * @since 3.0
	 */
	public static ILog getLog(Bundle bundle) {
		return ILog.of(bundle);
	}

	/**
	 * Returns the log for the bundle of the given class. If no such log exists, one
	 * is created.
	 *
	 * @param clazz the class in a bundle whose log is returned
	 * @return the log for the bundle to which the bundle belongs
	 *
	 * @since 3.16
	 */
	public static ILog getLog(Class<?> clazz) {
		return ILog.of(clazz);
	}

	/**
	 * Returns the given bundle's resource bundle for the current locale.
	 * <p>
	 * This resource bundle is typically stored as the plugin.properties file in the
	 * plug-in itself, and contains any translatable strings used in the plug-in
	 * manifest file (plugin.xml).
	 * </p>
	 * <p>
	 * This mechanism is intended only for externalizing strings found in the
	 * plug-in manifest file. Using this method for externalizing strings in your
	 * code may result in degraded memory performance.
	 * </p>
	 *
	 * @param bundle the bundle whose resource bundle is being queried
	 * @return the resource bundle
	 * @exception MissingResourceException if the resource bundle was not found
	 * @since 3.0
	 */
	public static ResourceBundle getResourceBundle(Bundle bundle) throws MissingResourceException {
		return InternalPlatform.getDefault().getResourceBundle(bundle);
	}

	/**
	 * Returns a resource string corresponding to the given argument value.
	 * If the argument value specifies a resource key, the string
	 * is looked up in the default resource bundle for the given runtime bundle. If the argument does not
	 * specify a valid key, the argument itself is returned as the
	 * resource string. The key lookup is performed in the
	 * file referenced in the Bundle-Localization header of the bundle manifest. If a resource string
	 * corresponding to the key is not found in the resource bundle
	 * the key value, or any default text following the key in the
	 * argument value is returned as the resource string.
	 * A key is identified as a string beginning with the "%" character.
	 * Note, that the "%" character is stripped off prior to lookup
	 * in the resource bundle.
	 * <p>
	 * Equivalent to <code>getResourceString(bundle, value, getResourceBundle())</code>
	 * </p>
	 *
	 * @param bundle the bundle whose resource bundle is being queried
	 * @param value the value to look for
	 * @return the resource string
	 * @see #getResourceBundle(Bundle)
	 * @since 3.0
	 */
	public static String getResourceString(Bundle bundle, String value) {
		return InternalPlatform.getDefault().getResourceString(bundle, value);
	}

	/**
	 * Returns a resource string corresponding to the given argument value and
	 * resource bundle in the given runtime bundle. If the argument value specifies
	 * a resource key, the string is looked up in the given resource bundle. If the
	 * argument does not specify a valid key, the argument itself is returned as the
	 * resource string. The key lookup is performed against the specified resource
	 * bundle. If a resource string corresponding to the key is not found in the
	 * resource bundle the key value, or any default text following the key in the
	 * argument value is returned as the resource string. A key is identified as a
	 * string beginning with the "%" character. Note that the "%" character is
	 * stripped off prior to lookup in the resource bundle.
	 * <p>
	 * For example, assume resource bundle plugin.properties contains name = Project
	 * Name
	 * </p>
	 *
	 * <pre>
	 *     getResourceString("Hello World") returns "Hello World"
	 *     getResourceString("%name") returns "Project Name"
	 *     getResourceString("%name Hello World") returns "Project Name"
	 *     getResourceString("%abcd Hello World") returns "Hello World"
	 *     getResourceString("%abcd") returns "%abcd"
	 *     getResourceString("%%name") returns "%name"
	 * </pre>
	 *
	 * @param bundle         the bundle whose resource bundle is being queried
	 * @param value          the value
	 * @param resourceBundle the resource bundle to query
	 * @return the resource string
	 * @see #getResourceBundle(Bundle)
	 * @since 3.0
	 */
	public static String getResourceString(Bundle bundle, String value, ResourceBundle resourceBundle) {
		return InternalPlatform.getDefault().getResourceString(bundle, value, resourceBundle);
	}

	/**
	 * Returns the string name of the current system architecture.
	 * The value is a user-defined string if the architecture is
	 * specified on the command line, otherwise it is the value
	 * returned by <code>java.lang.System.getProperty("os.arch")</code>.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and query it for
	 * the operating-system architecture.
	 * </p>
	 * @return the string name of the current system architecture
	 * @since 3.0
	 */
	public static String getOSArch() {
		return InternalPlatform.getDefault().getOSArch();
	}

	/**
	 * Returns the string name of the current locale for use in finding files
	 * whose path starts with <code>$nl$</code>.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and query it for
	 * the NL.
	 * </p>
	 * @return the string name of the current locale
	 * @since 3.0
	 */
	public static String getNL() {
		return InternalPlatform.getDefault().getNL();
	}

	/**
	 * Returns Unicode locale extensions for the Unicode locale identifier, if they are
	 * defined. An empty string is returned if Unicode locale extensions are not defined.
	 * <p>
	 * For more information on Unicode locale extensions, see
	 * <a href="http://unicode.org/reports/tr35/">Unicode Technical Standard #35</a>.
	 *
	 * @return The defined Unicode locale extensions, or an empty string.
	 * @since 3.5
	 */
	public static String getNLExtensions() {
		return InternalPlatform.getDefault().getNLExtensions();
	}

	/**
	 * Returns the string name of the current operating system for use in finding
	 * files whose path starts with <code>$os$</code>.  <code>OS_UNKNOWN</code> is
	 * returned if the operating system cannot be determined.
	 * The value may indicate one of the operating systems known to the platform
	 * (as specified in <code>knownOSValues</code>) or a user-defined string if
	 * the operating system name is specified on the command line.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and query it for
	 * the operating-system.
	 * </p>
	 * @return the string name of the current operating system
	 * @since 3.0
	 */
	public static String getOS() {
		return InternalPlatform.getDefault().getOS();
	}

	/**
	 * Returns the string name of the current window system for use in finding files
	 * whose path starts with <code>$ws$</code>.  <code>null</code> is returned
	 * if the window system cannot be determined.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and query it for
	 * the windowing system.
	 * </p>
	 * @return the string name of the current window system or <code>null</code>
	 * @since 3.0
	 */
	public static String getWS() {
		return InternalPlatform.getDefault().getWS();
	}

	/**
	 * Returns the arguments not consumed by the framework implementation itself.  Which
	 * arguments are consumed is implementation specific. These arguments are available
	 * for use by the application.
	 *
	 * @return the array of command line arguments not consumed by the framework.
	 * @since 3.0
	 */
	public static String[] getApplicationArgs() {
		return InternalPlatform.getDefault().getApplicationArgs();
	}

	/**
	 * Returns the platform administrator for this running Eclipse.
	 * <p>
	 * Note: This is an internal method and <em>must not</em> be used by clients
	 * which are not part of the Eclipse Platform. This method allows access to
	 * classes which are not Eclipse Platform API but are part of the OSGi runtime
	 * that the Eclipse Platform is built on. Even as the Eclipse Platform evolves
	 * in compatible ways from release to release, the details of the OSGi
	 * implementation might not.
	 * </p>
	 * <p>
	 * Clients can also acquire the {@link PlatformAdmin} service to retrieve this
	 * object.
	 * </p>
	 *
	 * @return the platform admin for this instance of Eclipse
	 * @deprecated only consumer is PDE and this should never be used by other
	 *             clients, see javadoc for details.
	 * @since 3.0
	 */
	@Deprecated(forRemoval = true, since = "2024-03") // 2024-03 was added later for information purposes
	public static PlatformAdmin getPlatformAdmin() {
		return InternalPlatform.getDefault().getPlatformAdmin();
	}

	/**
	 * Returns the location of the platform's working directory (also known as the instance data area).
	 * <code>null</code> is returned if the platform is running without an instance location.
	 * <p>
	 * This method is equivalent to acquiring the <code>org.eclipse.osgi.service.datalocation.Location</code>
	 * service with the property "type" equal to {@link Location#INSTANCE_FILTER}.
	 *</p>
	 * @return the location of the platform's instance data area or <code>null</code> if none
	 * @since 3.0
	 * @see Location#INSTANCE_FILTER
	 */
	public static Location getInstanceLocation() {
		return InternalPlatform.getDefault().getInstanceLocation();
	}

	/**
	 * Returns the currently registered bundle group providers.
	 * <p>
	 * Clients are also able to acquire the {@link IBundleGroupProvider} service and query it for
	 * the registered bundle group providers.
	 * </p>
	 * @return the currently registered bundle group providers
	 * @since 3.0
	 */
	public static IBundleGroupProvider[] getBundleGroupProviders() {
		return InternalPlatform.getDefault().getBundleGroupProviders();
	}

	/**
	 * Return the interface into the preference mechanism. The returned
	 * object can be used for such operations as searching for preference
	 * values across multiple scopes and preference import/export.
	 * <p>
	 * Clients are also able to acquire the {@link IPreferencesService} service via
	 * OSGi mechanisms and use it for preference functions.
	 * </p>
	 * @return an object to interface into the preference mechanism
	 * @since 3.0
	 */
	public static IPreferencesService getPreferencesService() {
		return InternalPlatform.getDefault().getPreferencesService();
	}

	/**
	 * Returns the product which was selected when running this Eclipse instance
	 * or <code>null</code> if none
	 * @return the current product or <code>null</code> if none
	 * @since 3.0
	 */
	public static IProduct getProduct() {
		return InternalPlatform.getDefault().getProduct();
	}

	/**
	 * Registers the given bundle group provider with the platform.
	 * <p>
	 * Clients are also able to use the {@link IBundleGroupProvider} service to
	 * register themselves as a bundle group provider.
	 * </p>
	 * @param provider a provider to register
	 * @since 3.0
	 */
	public static void registerBundleGroupProvider(IBundleGroupProvider provider) {
		InternalPlatform.getDefault().registerBundleGroupProvider(provider);
	}

	/**
	 * De-registers the given bundle group provider with the platform.
	 * <p>
	 * Clients are also able to use the {@link IBundleGroupProvider} service mechanism
	 * for unregistering themselves.
	 * </p>
	 * @param provider a provider to de-register
	 * @since 3.0
	 * @see #registerBundleGroupProvider(IBundleGroupProvider)
	 */
	public static void unregisterBundleGroupProvider(IBundleGroupProvider provider) {
		InternalPlatform.getDefault().unregisterBundleGroupProvider(provider);
	}

	/**
	 * Returns the location of the configuration information
	 * used to run this instance of Eclipse.  The configuration area typically
	 * contains the list of plug-ins available for use, various settings
	 * (those shared across different instances of the same configuration)
	 * and any other such data needed by plug-ins.
	 * <code>null</code> is returned if the platform is running without a configuration location.
	 * <p>
	 * This method is equivalent to acquiring the <code>org.eclipse.osgi.service.datalocation.Location</code>
	 * service with the property "type" equal to {@link Location#CONFIGURATION_FILTER}.
	 *</p>
	 * @return the location of the platform's configuration data area or <code>null</code> if none
	 * @since 3.0
	 * @see Location#CONFIGURATION_FILTER
	 */
	public static Location getConfigurationLocation() {
		return InternalPlatform.getDefault().getConfigurationLocation();
	}

	/**
	 * Returns the location of the platform's user data area.  The user data area is a location on the system
	 * which is specific to the system's current user.  By default it is located relative to the
	 * location given by the System property "user.home".
	 * <code>null</code> is returned if the platform is running without an user location.
	 * <p>
	 * This method is equivalent to acquiring the <code>org.eclipse.osgi.service.datalocation.Location</code>
	 * service with the property "type" equal to {@link Location#USER_FILTER}.
	 *</p>
	 * @return the location of the platform's user data area or <code>null</code> if none
	 * @since 3.0
	 * @see Location#USER_FILTER
	 */
	public static Location getUserLocation() {
		return InternalPlatform.getDefault().getUserLocation();
	}

	/**
	 * Returns the location of the base installation for the running platform
	 * <code>null</code> is returned if the platform is running without a configuration location.
	 * <p>
	 * This method is equivalent to acquiring the <code>org.eclipse.osgi.service.datalocation.Location</code>
	 * service with the property "type" equal to {@link Location#INSTALL_FILTER}.
	 *</p>
	 * @return the location of the platform's installation area or <code>null</code> if none
	 * @since 3.0
	 * @see Location#INSTALL_FILTER
	 */
	public static Location getInstallLocation() {
		return InternalPlatform.getDefault().getInstallLocation();
	}

	/**
	 * Checks if the specified bundle is a fragment bundle.
	 * <p>
	 * Clients are also able to acquire the
	 * {@link org.osgi.service.packageadmin.PackageAdmin} service to query if the
	 * given bundle is a fragment by asking for the bundle type and checking against
	 * constants on the service interface.
	 * </p>
	 *
	 * @param bundle the bundle to query
	 * @return true if the specified bundle is a fragment bundle; otherwise false is
	 *         returned.
	 * @since 3.0
	 */
	public static boolean isFragment(Bundle bundle) {
		return InternalPlatform.getDefault().isFragment(bundle);
	}

	/**
	 * Returns an array of attached fragment bundles for the specified bundle. If
	 * the specified bundle is a fragment then <code>null</code> is returned. If no
	 * fragments are attached to the specified bundle then <code>null</code> is
	 * returned.
	 * <p>
	 * Clients are also able to acquire the
	 * {@link org.osgi.service.packageadmin.PackageAdmin} service and query it for
	 * the fragments of the given bundle.
	 * </p>
	 *
	 * @param bundle the bundle to get the attached fragment bundles for.
	 * @return an array of fragment bundles or <code>null</code> if the bundle does
	 *         not have any attached fragment bundles.
	 * @since 3.0
	 */
	public static Bundle[] getFragments(Bundle bundle) {
		return InternalPlatform.getDefault().getFragments(bundle);
	}

	/**
	 * Returns the resolved bundle with the specified symbolic name that has the
	 * highest version. If no resolved bundles are installed that have the specified
	 * symbolic name then null is returned.
	 * <p>
	 * Clients are also able to acquire the
	 * {@link org.osgi.service.packageadmin.PackageAdmin} service and query it for
	 * the bundle with the specified symbolic name. Clients can ask the service for
	 * all bundles with that particular name and then determine the one with the
	 * highest version. Note that clients may want to filter the results based on
	 * the state of the bundles.
	 * </p>
	 * <p>
	 * Note that looking up a Bundle by its symbolic name is less efficient than
	 * looking up a Bundle by a class loaded by that bundle. Callers should consider
	 * whether or not it is more appropriate to use
	 * {@link FrameworkUtil#getBundle(Class)} instead.
	 * </p>
	 * <p>
	 * Note also that if the purpose of looking up the Bundle in order to log a
	 * message, then it would be more appropriate to use the direct
	 * {@link #getLog(Class)} instead
	 * </p>
	 *
	 * @param symbolicName the symbolic name of the bundle to be returned.
	 * @return the bundle that has the specified symbolic name with the highest
	 *         version, or <code>null</code> if no bundle is found.
	 * @since 3.0
	 */
	public static Bundle getBundle(String symbolicName) {
		return InternalPlatform.getDefault().getBundle(symbolicName);
	}

	/**
	 * Returns all bundles with the specified symbolic name. If no resolved bundles
	 * with the specified symbolic name can be found, <code>null</code> is returned.
	 * If the version argument is not null then only the Bundles that have the
	 * specified symbolic name and a version greater than or equal to the specified
	 * version are returned. The returned bundles are ordered in descending bundle
	 * version order.
	 * <p>
	 * Clients are also able to acquire the
	 * {@link org.osgi.service.packageadmin.PackageAdmin} service and query it for
	 * all bundle versions with the given symbolic name, after turning the specific
	 * version into a version range. Note that clients may want to filter the
	 * results based on the state of the bundles.
	 * </p>
	 *
	 * @param symbolicName the symbolic name of the bundles that are to be returned.
	 * @param version      the version that the return bundle versions must match,
	 *                     or <code>null</code> if no version matching is to be
	 *                     done.
	 * @return the array of Bundles with the specified name that match the specified
	 *         version and match rule, or <code>null</code> if no bundles are found.
	 */
	public static Bundle[] getBundles(String symbolicName, String version) {
		return InternalPlatform.getDefault().getBundles(symbolicName, version);
	}

	/**
	 * Returns an array of host bundles that the specified fragment bundle is
	 * attached to or <code>null</code> if the specified bundle is not attached to a
	 * host. If the bundle is not a fragment bundle then <code>null</code> is
	 * returned.
	 * <p>
	 * Clients are also able to acquire the
	 * {@link org.osgi.service.packageadmin.PackageAdmin} service and query it for
	 * the hosts for the given bundle.
	 * </p>
	 *
	 * @param bundle the bundle to get the host bundles for.
	 * @return an array of host bundles or null if the bundle does not have any host
	 *         bundles.
	 * @since 3.0
	 */
	public static Bundle[] getHosts(Bundle bundle) {
		return InternalPlatform.getDefault().getHosts(bundle);
	}

	/**
	 * Returns whether the platform is running.
	 *
	 * @return <code>true</code> if the platform is running,
	 *		and <code>false</code> otherwise
	 *@since 3.0
	 */
	public static boolean isRunning() {
		return InternalPlatform.getDefault().isRunning();
	}

	/**
	 * Returns a list of known system architectures.
	 * <p>
	 * Note that this list is not authoritative; there may be legal values
	 * not included in this list. Indeed, the value returned by
	 * <code>getOSArch</code> may not be in this list. Also, this list may
	 * change over time as Eclipse comes to run on more operating environments.
	 * </p>
	 *
	 * @return the list of system architectures known to the system
	 * @see #getOSArch()
	 * @since 3.0
	 */
	public static String[] knownOSArchValues() {
		return InternalPlatform.getDefault().knownOSArchValues();
	}

	/**
	 * Returns a list of known operating system names.
	 * <p>
	 * Note that this list is not authoritative; there may be legal values
	 * not included in this list. Indeed, the value returned by
	 * <code>getOS</code> may not be in this list. Also, this list may
	 * change over time as Eclipse comes to run on more operating environments.
	 * </p>
	 *
	 * @return the list of operating systems known to the system
	 * @see #getOS()
	 * @since 3.0
	 */
	public static String[] knownOSValues() {
		return InternalPlatform.getDefault().knownOSValues();
	}

	/**
	 * Returns a map of known platform line separators. The keys are
	 * translated names of platforms and the values are their associated
	 * line separator strings.
	 *
	 * @return a map of platform to their line separator string
	 * @since 3.1
	 */
	public static Map<String,String> knownPlatformLineSeparators() {
		Map<String,String> result = new LinkedHashMap<>(5);
		result.put(LINE_SEPARATOR_KEY_WINDOWS, LINE_SEPARATOR_VALUE_CRLF);
		result.put(LINE_SEPARATOR_KEY_UNIX, LINE_SEPARATOR_VALUE_LF);
		return result;
	}

	/**
	 * Returns a list of known windowing system names.
	 * <p>
	 * Note that this list is not authoritative; there may be legal values
	 * not included in this list. Indeed, the value returned by
	 * <code>getWS</code> may not be in this list. Also, this list may
	 * change over time as Eclipse comes to run on more operating environments.
	 * </p>
	 *
	 * @return the list of window systems known to the system
	 * @see #getWS()
	 * @since 3.0
	 */
	public static String[] knownWSValues() {
		return InternalPlatform.getDefault().knownWSValues();
	}

	/**
	 * Returns <code>true</code> if the platform is currently running in
	 * debug mode.  The platform is typically put in debug mode using the
	 * "-debug" command line argument.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and query it
	 * to see if they are in debug mode.
	 * </p>
	 * @return whether or not the platform is running in debug mode
	 * @since 3.0
	 */
	public static boolean inDebugMode() {
		return PlatformActivator.getContext().getProperty("osgi.debug") != null; //$NON-NLS-1$
	}

	/**
	 * Returns <code>true</code> if the platform is currently running in
	 * development mode.  That is, if special procedures are to be
	 * taken when defining plug-in class paths.  The platform is typically put in
	 * development mode using the "-dev" command line argument.
	 * <p>
	 * Clients are also able to acquire the {@link EnvironmentInfo} service and query it
	 * to see if they are in development mode.
	 * </p>
	 * @return whether or not the platform is running in development mode
	 * @since 3.0
	 */
	public static boolean inDevelopmentMode() {
		return PlatformActivator.getContext().getProperty("osgi.dev") != null; //$NON-NLS-1$
	}

	/**
	 * Retrieves the system encoding ({@link Charset}) based on the locale set in
	 * the current user environment.
	 * <p>
	 * Note: the return value is <b>not</b> influenced by the
	 * {@code -Dfile.encoding} system property and is <b>not</b> meant to be used
	 * for file encoding in general (there is a workspace specific
	 * <code>IContainer.getDefaultCharset()</code> API for that).
	 * <p>
	 * This method should be used if the <b>original</b> system encoding is required
	 * (which is not necessarily the encoding used by JVM to save files). It can for
	 * example be used to properly encode <a href=
	 * "https://docs.oracle.com/en/java/javase/18/docs/specs/man/java.html#java-command-line-argument-files">Java
	 * Command-Line Argument Files</a> or to encode other platform specific data.
	 *
	 * @return system encoding, never null. In case the detection fails, returns
	 *         {@link Charset#defaultCharset()}, which is always {@code UTF-8} on
	 *         Java 18 and later.
	 * @see <a href="https://openjdk.java.net/jeps/400">JEP 400</a>
	 * @since 3.26
	 */
	public static Charset getSystemCharset() {
		return SYSTEM_CHARSET;
	}
}
