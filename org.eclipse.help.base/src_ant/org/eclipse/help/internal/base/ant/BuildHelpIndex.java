/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.base.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.help.search.HelpIndexBuilder;

/**
 * A custom Ant task to pre-build search help index for a plug-in from within an
 * Ant script.
 *
 * @since 3.1
 */

public class BuildHelpIndex extends Task {
	private String manifest;

	private String destination;

	private HelpIndexBuilder builder;

	/**
	 * The default constructor.
	 */
	public BuildHelpIndex() {
	}

	@Override
	public void execute() throws BuildException {
		File file = getFile(manifest);
		if (file == null)
			throw new BuildException("Manifest not set."); //$NON-NLS-1$
		File target = getFile(destination);
		if (target == null)
			throw new BuildException("Target directory not set."); //$NON-NLS-1$
		builder = new HelpIndexBuilder();
		builder.setManifest(file);
		builder.setDestination(target);
		IProgressMonitor monitor = (IProgressMonitor) getProject()
				.getReferences().get(AntCorePlugin.ECLIPSE_PROGRESS_MONITOR);
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			builder.execute(monitor);
		} catch (CoreException e) {
			printStatus(e);
			if (e.getStatus().getSeverity()==IStatus.ERROR)
				throw new BuildException(e.getMessage(), e.getCause());
		}
	}

	private void printStatus(CoreException e) {
		IStatus status = e.getStatus();
		System.err.println(e.getMessage());
		if (status.isMultiStatus()) {
			IStatus [] children = status.getChildren();
			for (IStatus child : children) {
				System.err.println("    " + child.getMessage()); //$NON-NLS-1$
			}
		}
	}

	private File getFile(String fileName) {
		if (fileName == null)
			return null;
		IPath path = IPath.fromOSString(fileName);
		if (path.isAbsolute())
			return new File(fileName);
		File root = getProject().getBaseDir();
		if (fileName.equals(".") || fileName.equals("./")) //$NON-NLS-1$ //$NON-NLS-2$
			return root;
		if (fileName.equals("..") || fileName.equals("../")) //$NON-NLS-1$ //$NON-NLS-2$
			return root.getParentFile();
		return new File(root, fileName);
	}

	/**
	 * The location (relative or absolute) of the manifest file (plugin.xml)
	 * that contains <code>org.eclipse.help.toc</code> extensions. If help
	 * docs that need to be indexed are in the fragment, manifest must point at
	 * the referenced fragment plug-in.
	 *
	 * @param manifest
	 *            the plug-in manifest file
	 */

	public void setManifest(String manifest) {
		this.manifest = manifest;
	}

	/**
	 * The destination directory where the index will be placed. The final index
	 * directory will be created by appending locale subdirectories and the
	 * index directory name to the destination. For example, for index directory
	 * name defined as 'index', and for ja_Jp locale, the index data will be
	 * created in 'destination/nl/ja/Jp/index'.
	 *
	 * @param destination
	 *            the base directory of the search index destination (typically
	 *            plug-in or fragment root directory)
	 */

	public void setDestination(String destination) {
		this.destination = destination;
	}
}