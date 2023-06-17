/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Ericsson AB, Hamdan Msheik - Bug 389564
 *     Ericsson AB, Julian Enoch - Bug 470390
 *******************************************************************************/
package org.eclipse.ant.internal.launching.launchConfigurations;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.launching.AntLaunching;
import org.eclipse.ant.internal.launching.AntLaunchingUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.AbstractRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A classpath entry that contains a contributed classpath entries via the <code>extraClasspathEntries</code> extension point.
 *
 * @since 3.0
 */
@SuppressWarnings("restriction")
public class ContributedClasspathEntriesEntry extends AbstractRuntimeClasspathEntry {

	public static final String TYPE_ID = "org.eclipse.ant.ui.classpathentry.extraClasspathEntries"; //$NON-NLS-1$

	public static List<IRuntimeClasspathEntry> fgSWTEntries = null;

	/**
	 * Default contructor required to instantiate persisted extensions.
	 */
	public ContributedClasspathEntriesEntry() {
	}

	@Override
	protected void buildMemento(Document document, Element memento) throws CoreException {
		// do nothing
	}

	@Override
	public void initializeFrom(Element memento) throws CoreException {
		// do nothing
	}

	@Override
	public String getTypeId() {
		return TYPE_ID;
	}

	@Override
	public IRuntimeClasspathEntry[] getRuntimeClasspathEntries(ILaunchConfiguration configuration) throws CoreException {
		boolean separateVM = AntLaunchingUtil.isSeparateJREAntBuild(configuration);
		boolean setInputHandler = configuration.getAttribute(AntLaunching.SET_INPUTHANDLER, true);
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		IAntClasspathEntry[] antClasspathEntries = prefs.getContributedClasspathEntries();
		IAntClasspathEntry[] userEntries = prefs.getAdditionalClasspathEntries();
		List<IRuntimeClasspathEntry> rtes = new ArrayList<>(antClasspathEntries.length + userEntries.length);
		IAntClasspathEntry entry;
		for (IAntClasspathEntry antClasspathEntry : antClasspathEntries) {
			entry = antClasspathEntry;
			if (!separateVM || (separateVM && !entry.isEclipseRuntimeRequired())) {
				rtes.add(JavaRuntime.newStringVariableClasspathEntry(entry.getLabel()));
			}
		}
		boolean haveToolsEntry = false;
		String path;
		for (IAntClasspathEntry userEntry : userEntries) {
			entry = userEntry;
			path = entry.getLabel();
			IPath toolsPath = IPath.fromOSString(path);
			if (toolsPath.lastSegment().equals("tools.jar")) { //$NON-NLS-1$
				haveToolsEntry = true;
				// replace with dynamically resolved tools.jar based on
				// the JRE being used
				addToolsJar(configuration, rtes, path);
			} else {
				rtes.add(JavaRuntime.newStringVariableClasspathEntry(path));
			}
		}
		if (!haveToolsEntry) {
			addToolsJar(configuration, rtes, null);
		}

		if (setInputHandler && separateVM) {
			addSWTJars(rtes);
		}

		return rtes.toArray(new IRuntimeClasspathEntry[rtes.size()]);
	}

	private void addToolsJar(ILaunchConfiguration configuration, List<IRuntimeClasspathEntry> rtes, String path) {
		IRuntimeClasspathEntry tools = getToolsJar(configuration);
		if (tools == null) {
			if (path != null) {
				// use the global entry
				rtes.add(JavaRuntime.newArchiveRuntimeClasspathEntry(IPath.fromOSString(path)));
			} else {
				// use the default vm install to try to find a tools.jar
				IVMInstall install = JavaRuntime.getDefaultVMInstall();
				if (install != null) {
					IAntClasspathEntry entry = AntCorePlugin.getPlugin().getPreferences().getToolsJarEntry(IPath.fromOSString(install.getInstallLocation().getAbsolutePath()));
					if (entry != null) {
						try {
							URL entryURL = entry.getEntryURL();
							String pathString = resolveFileFromUrl(entryURL.getFile());
							if (!pathString.isEmpty()) {
								rtes.add(JavaRuntime.newArchiveRuntimeClasspathEntry(IPath.fromOSString(pathString)));
							}
						}
						catch (MalformedURLException e) {
							AntLaunching.log(e);
						}
					}
				}
			}
		} else {
			rtes.add(tools);
		}
	}

	private void addSWTJars(List<IRuntimeClasspathEntry> rtes) {
		if (fgSWTEntries == null) {
			fgSWTEntries = new ArrayList<>();
			Bundle bundle = Platform.getBundle("org.eclipse.swt"); //$NON-NLS-1$
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			List<BundleWire> fragmentWires = wiring == null ? Collections.<BundleWire> emptyList()
					: wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE);
			for (BundleWire fragmentWire : fragmentWires) {
				Bundle fragmentBundle = fragmentWire.getRequirer().getBundle();
				URL bundleURL;
				try {
					bundleURL = FileLocator.resolve(fragmentBundle.getEntry("/")); //$NON-NLS-1$
				}
				catch (IOException e) {
					AntLaunching.log(e);
					continue;
				}
				String urlFileName = bundleURL.getFile();
				try {
					urlFileName = resolveFileFromUrl(urlFileName);
				}
				catch (MalformedURLException e) {
					AntLaunching.log(e);
					continue;
				}
				IPath fragmentPath = IPath.fromOSString(urlFileName);
				if (fragmentPath.getFileExtension() != null) { // JAR file
					fgSWTEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(fragmentPath));
				} else { // folder
					File bundleFolder = fragmentPath.toFile();
					if (!bundleFolder.isDirectory()) {
						continue;
					}
					String[] names = bundleFolder.list((dir, name) -> name.endsWith(".jar")); //$NON-NLS-1$
					for (String jarName : names) {
						fgSWTEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(fragmentPath.append(jarName)));
					}
				}
			}
		}
		rtes.addAll(fgSWTEntries);
	}

	private String resolveFileFromUrl(String urlFileName) throws MalformedURLException {
		if (!urlFileName.startsWith(IAntCoreConstants.FILE_PROTOCOL)) {
			return urlFileName;
		}
		try {
			URI uri = URIUtil.toURI(new URL(urlFileName));
			// fix bug 470390 using toFile() instead of toURL()
			urlFileName = URIUtil.toFile(uri).getAbsolutePath();
		}
		catch (URISyntaxException e) {
			AntLaunching.log(e);
		}
		if (urlFileName.endsWith("!/") || urlFileName.endsWith("!\\")) { //$NON-NLS-1$ //$NON-NLS-2$
			urlFileName = urlFileName.substring(0, urlFileName.length() - 2);
		}
		if (urlFileName.endsWith("!")) { //$NON-NLS-1$
			urlFileName = urlFileName.substring(0, urlFileName.length() - 1);
		}
		return urlFileName;
	}

	/**
	 * Returns the tools.jar to use for this launch configuration, or <code>null</code> if none.
	 *
	 * @param configuration
	 *            configuration to resolve a tools.jar for
	 * @return associated tools.jar archive, or <code>null</code>
	 */
	private IRuntimeClasspathEntry getToolsJar(ILaunchConfiguration configuration) {
		try {
			IVMInstall install = JavaRuntime.computeVMInstall(configuration);
			if (install != null) {
				IAntClasspathEntry entry = AntCorePlugin.getPlugin().getPreferences().getToolsJarEntry(IPath.fromOSString(install.getInstallLocation().getAbsolutePath()));
				if (entry != null) {
					return JavaRuntime.newArchiveRuntimeClasspathEntry(IPath.fromOSString(entry.getEntryURL().getPath()));
				}
			}
		}
		catch (CoreException ce) {
			// likely dealing with a non-Java project
		}

		return null;
	}

	@Override
	public String getName() {
		return AntLaunchConfigurationMessages.ContributedClasspathEntriesEntry_1;
	}

	@Override
	public int getType() {
		return IRuntimeClasspathEntry.OTHER;
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ContributedClasspathEntriesEntry;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
