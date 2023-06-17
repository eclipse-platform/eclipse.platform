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
 *******************************************************************************/
package org.eclipse.ant.internal.launching.launchConfigurations;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.internal.launching.AntLaunching;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.AbstractRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A classpath entry that contains a set of archives for a particular ANT_HOME.
 *
 * @since 3.0
 */
@SuppressWarnings("restriction")
public class AntHomeClasspathEntry extends AbstractRuntimeClasspathEntry {

	public static final String TYPE_ID = "org.eclipse.ant.ui.classpathentry.antHome"; //$NON-NLS-1$

	/**
	 * Local path on disk where Ant Home is located or <code>null</code> to indicate the use of the default Ant Home.
	 */
	private String antHomeLocation = null;

	/**
	 * Creates an AntHome entry for the default AntHome installation.
	 */
	public AntHomeClasspathEntry() {
		antHomeLocation = null;
	}

	/**
	 * Constructs an AntHome entry for the Ant installed at the specified root directory.
	 *
	 * @param antHome
	 *            path in the local file system to an Ant installation
	 */
	public AntHomeClasspathEntry(String antHome) {
		antHomeLocation = antHome;
	}

	@Override
	protected void buildMemento(Document document, Element memento) throws CoreException {
		if (antHomeLocation == null) {
			memento.setAttribute("default", "true"); //$NON-NLS-1$//$NON-NLS-2$
		} else {
			memento.setAttribute("antHome", IPath.fromOSString(antHomeLocation).toString()); //$NON-NLS-1$
		}
	}

	@Override
	public void initializeFrom(Element memento) throws CoreException {
		String antHome = memento.getAttribute("antHome"); //$NON-NLS-1$
		if (antHome != null && (antHome.length() > 0)) {
			IPath path = IPath.fromOSString(antHome);
			antHomeLocation = path.toOSString();
		} else {
			antHomeLocation = null;
		}
	}

	@Override
	public String getTypeId() {
		return TYPE_ID;
	}

	@Override
	public IRuntimeClasspathEntry[] getRuntimeClasspathEntries(ILaunchConfiguration configuration) throws CoreException {
		ArrayList<IRuntimeClasspathEntry> libs = new ArrayList<>(40);
		AntCorePreferences preferences = AntCorePlugin.getPlugin().getPreferences();
		if (antHomeLocation == null) {
			for (IAntClasspathEntry entry : preferences.getAntHomeClasspathEntries()) {
				libs.add(JavaRuntime.newStringVariableClasspathEntry(entry.getLabel()));
			}
		} else {
			IPath libDir = IPath.fromOSString(antHomeLocation).append("lib"); //$NON-NLS-1$
			for (String name : resolveAntHome().list()) {
				IPath path = IPath.fromOSString(name);
				String fileExtension = path.getFileExtension();
				if ("jar".equalsIgnoreCase(fileExtension)) { //$NON-NLS-1$
					libs.add(JavaRuntime.newArchiveRuntimeClasspathEntry(libDir.append(path)));
				}
			}
		}
		return libs.toArray(new IRuntimeClasspathEntry[libs.size()]);
	}

	public File resolveAntHome() throws CoreException {
		if (antHomeLocation == null) { // using the default ant home
			return null;
		}
		IPath libDir = IPath.fromOSString(antHomeLocation).append("lib"); //$NON-NLS-1$
		File lib = libDir.toFile();
		File parentDir = lib.getParentFile();
		if (parentDir == null || !parentDir.exists()) {
			abort(MessageFormat.format(AntLaunchConfigurationMessages.AntHomeClasspathEntry_10, new Object[] { antHomeLocation }), null);
		}
		if (!lib.exists() || !lib.isDirectory()) {
			abort(MessageFormat.format(AntLaunchConfigurationMessages.AntHomeClasspathEntry_11, new Object[] { antHomeLocation }), null);
		}
		return lib;
	}

	@Override
	protected void abort(String message, Throwable exception) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, AntLaunching.getUniqueIdentifier(), IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR, message, exception);
		throw new CoreException(status);
	}

	@Override
	public String getName() {
		if (antHomeLocation == null) {
			return AntLaunchConfigurationMessages.AntHomeClasspathEntry_8;
		}
		return MessageFormat.format(AntLaunchConfigurationMessages.AntHomeClasspathEntry_9, new Object[] { antHomeLocation });
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
		return obj instanceof AntHomeClasspathEntry && equalsOrNull(antHomeLocation, ((AntHomeClasspathEntry) obj).antHomeLocation);
	}

	/**
	 * Return whether s1 is equivalent to s2.
	 *
	 * @param s1
	 * @param s2
	 * @return whether s1 is equivalent to s2
	 */
	private boolean equalsOrNull(String s1, String s2) {
		if (s1 == null || s2 == null) {
			return s1 == s2;
		}
		return s1.equalsIgnoreCase(s2);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * Sets the ant home to use.
	 *
	 * @param path
	 *            path to toor of an ant home installation
	 */
	public void setAntHome(String path) {
		antHomeLocation = path;
	}

	/**
	 * Returns the ant home location
	 *
	 * @return path to root ant installation directory
	 */
	public String getAntHome() {
		if (antHomeLocation == null) {
			return AntCorePlugin.getPlugin().getPreferences().getAntHome();
		}
		return antHomeLocation;
	}
}
