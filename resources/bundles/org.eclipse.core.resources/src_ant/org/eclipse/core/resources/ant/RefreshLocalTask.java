/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.core.resources.ant;

import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An Ant task which refreshes the Eclipse Platform's view of the local filesystem.
 *
 * @see IResource#refreshLocal(int, IProgressMonitor)
 */
public class RefreshLocalTask extends Task {
	/**
	 * Unique identifier constant (value <code>"DEPTH_ZERO"</code>)
	 * indicating that refreshes should be performed only on the target
	 * resource itself
	 */
	public static final String DEPTH_ZERO = "zero"; //$NON-NLS-1$

	/**
	 * Unique identifier constant (value <code>"DEPTH_ONE"</code>)
	 * indicating that refreshes should be performed on the target
	 * resource and its children
	 */
	public static final String DEPTH_ONE = "one"; //$NON-NLS-1$

	/**
	 * Unique identifier constant (value <code>"DEPTH_INFINITE"</code>)
	 * indicating that refreshes should be performed on the target
	 * resource and all of its recursive children
	 */
	public static final String DEPTH_INFINITE = "infinite"; //$NON-NLS-1$

	/**
	 * The resource to refresh.
	 */
	protected IResource resource;

	/**
	 * The depth to refresh to.
	 */
	protected int depth = IResource.DEPTH_INFINITE;

	/**
	 * Constructs a new <code>RefreshLocal</code> instance.
	 */
	public RefreshLocalTask() {
		super();
	}

	/**
	 * Performs the refresh operation.
	 *
	 * @exception BuildException thrown if a problem occurs during execution.
	 */
	@Override
	public void execute() throws BuildException {
		if (resource == null)
			throw new BuildException(Policy.bind("exception.resourceNotSpecified")); //$NON-NLS-1$
		try {
			IProgressMonitor monitor = null;
			Hashtable<String, Object> references = getProject().getReferences();
			if (references != null)
				monitor = (IProgressMonitor) references.get(AntCorePlugin.ECLIPSE_PROGRESS_MONITOR);
			resource.refreshLocal(depth, monitor);
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	/**
	 * Sets the depth of this task appropriately.  The specified argument must
	 * by one of <code>RefreshLocal.DEPTH_ZERO</code>, <code>RefreshLocal.DEPTH_ONE</code>
	 * or <code>RefreshLocal.DEPTH_INFINITE</code>.
	 *
	 * @param value the depth to refresh to
	 */
	public void setDepth(String value) {
		if (DEPTH_ZERO.equalsIgnoreCase(value))
			depth = IResource.DEPTH_ZERO;
		else if (DEPTH_ONE.equalsIgnoreCase(value))
			depth = IResource.DEPTH_ONE;
		else if (DEPTH_INFINITE.equalsIgnoreCase(value))
			depth = IResource.DEPTH_INFINITE;
	}

	/**
	 * Sets the root of the workspace resource tree to refresh.
	 *
	 * @param value the root value
	 */
	public void setResource(String value) {
		IPath path = IPath.fromOSString(value);
		resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (resource == null) {
			// if it does not exist we guess it is a folder or a project
			if (path.segmentCount() > 1)
				resource = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
			else {
				resource = ResourcesPlugin.getWorkspace().getRoot().getProject(value);
				if (!resource.exists())
					log(Policy.bind("warning.projectDoesNotExist", value), Project.MSG_WARN); //$NON-NLS-1$
			}
		}
	}

}
