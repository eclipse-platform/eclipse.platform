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
import org.apache.tools.ant.Task;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Ant task which runs the platform's incremental build facilities.
 *
 * @see IProject#build(int, IProgressMonitor)
 * @see IWorkspace#build(int, IProgressMonitor)
 */
public class IncrementalBuild extends Task {
	private String builder;
	private String project;
	private int kind = IncrementalProjectBuilder.INCREMENTAL_BUILD;

	/**
	 * Unique identifier constant (value <code>"incremental"</code>)
	 * indicating that an incremental build should be performed.
	 */
	public final static String KIND_INCREMENTAL = "incremental"; //$NON-NLS-1$

	/**
	 * Unique identifier constant (value <code>"full"</code>)
	 * indicating that a full build should be performed.
	 */
	public final static String KIND_FULL = "full"; //$NON-NLS-1$

	/**
	 * Unique identifier constant (value <code>"auto"</code>)
	 * indicating that an auto build should be performed.
	 */
	public final static String KIND_AUTO = "auto"; //$NON-NLS-1$

	/**
	 * Unique identifier constant (value <code>"clean"</code>)
	 * indicating that a CLEAN build should be performed.
	 */
	public final static String KIND_CLEAN = "clean"; //$NON-NLS-1$

	/**
	 * Constructs an <code>IncrementalBuild</code> instance.
	 */
	public IncrementalBuild() {
		super();
	}

	/**
	 * Executes this task.
	 *
	 * @exception BuildException thrown if a problem occurs during execution
	 */
	@Override
	public void execute() throws BuildException {
		try {
			IProgressMonitor monitor = null;
			Hashtable<String, Object> references = getProject().getReferences();
			if (references != null)
				monitor = (IProgressMonitor) references.get(AntCorePlugin.ECLIPSE_PROGRESS_MONITOR);
			if (project == null) {
				ResourcesPlugin.getWorkspace().build(kind, monitor);
			} else {
				IProject targetProject = ResourcesPlugin.getWorkspace().getRoot().getProject(project);
				if (builder == null)
					targetProject.build(kind, monitor);
				else
					targetProject.build(kind, builder, null, monitor);
			}
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	/**
	 * Sets the name of the receiver's builder.
	 *
	 * @param value the name of the receiver's builder
	 */
	public void setBuilder(String value) {
		builder = value;
	}

	/**
	 * Sets the receiver's kind attribute. This value must be one of:
	 * <code>IncrementalBuild.KIND_FULL</code>,
	 * <code>IncrementalBuild.KIND_AUTO</code>,
	 * <code>IncrementalBuild.KIND_INCREMENTAL</code>,
	 * <code>IncrementalBuild.KIND_CLEAN</code>.
	 *
	 * @param value the receiver's kind attribute
	 */
	public void setKind(String value) {
		if (IncrementalBuild.KIND_FULL.equalsIgnoreCase(value))
			kind = IncrementalProjectBuilder.FULL_BUILD;
		else if (IncrementalBuild.KIND_AUTO.equalsIgnoreCase(value))
			kind = IncrementalProjectBuilder.AUTO_BUILD;
		else if (IncrementalBuild.KIND_CLEAN.equalsIgnoreCase(value))
			kind = IncrementalProjectBuilder.CLEAN_BUILD;
		else if (IncrementalBuild.KIND_INCREMENTAL.equalsIgnoreCase(value))
			kind = IncrementalProjectBuilder.INCREMENTAL_BUILD;
	}

	/**
	 * Sets the receiver's target project.
	 *
	 * @param value the receiver's target project
	 */
	public void setProject(String value) {
		project = value;
	}
}
