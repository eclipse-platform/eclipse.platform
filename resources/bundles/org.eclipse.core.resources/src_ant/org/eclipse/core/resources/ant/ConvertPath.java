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

import java.io.File;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.Path;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

/**
 * An Ant task which allows to switch from a file system path to a resource path,
 * and vice versa, and store the result in a user property whose name is set by the user. If the
 * resource does not exist, the property is set to <code>false</code>.
 * <p>
 * The attribute "property" must be specified, as well as only one of "fileSystemPath" or "resourcePath".
 * <p>
 * Example:<p>
 *	&lt;eclipse.convertPath fileSystemPath="D:\MyWork\MyProject" property="myProject.resourcePath"/&gt;
 */
public class ConvertPath extends Task {

	/**
	 * The file system path.
	 */
	private IPath fileSystemPath = null;

	/**
	 * The resource path.
	 */
	private IPath resourcePath = null;

	/**
	 * The name of the property where the result may be stored.
	 */
	private String property = null;

	/**
	 * The id of the new Path object that may be created.
	 */
	private String pathID = null;

	/**
	 * If false, note errors to the output, but keep going.
	 */
	private boolean failOnError = true;

	/**
	 * Constructs a new <code>ConvertPath</code> instance.
	 */
	public ConvertPath() {
		super();
	}

	/**
	 * Performs the path conversion operation.
	 *
	 * @exception BuildException thrown if a problem occurs during execution.
	 */
	@Override
	public void execute() throws BuildException {
		validateAttributes();
		if (fileSystemPath == null)
			// here, resourcePath is not null
			convertResourcePathToFileSystemPath(resourcePath);
		else
			convertFileSystemPathToResourcePath(fileSystemPath);
	}

	protected void convertFileSystemPathToResourcePath(IPath path) {
		IResource resource;
		if (Platform.getLocation().equals(path)) {
			resource = ResourcesPlugin.getWorkspace().getRoot();
		} else {
			resource = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(path);
			if (resource == null) {
				String message = Policy.bind("exception.noProjectMatchThePath", fileSystemPath.toOSString()); //$NON-NLS-1$
				if (failOnError)
					throw new BuildException(message);
				log(message, Project.MSG_ERR);
				return;
			}
		}
		if (property != null)
			getProject().setUserProperty(property, resource.getFullPath().toString());
		if (pathID != null) {
			Path newPath = new Path(getProject(), resource.getFullPath().toString());
			getProject().addReference(pathID, newPath);
		}
	}

	protected void convertResourcePathToFileSystemPath(IPath path) {
		IResource resource = null;
		switch (path.segmentCount()) {
			case 0 :
				resource = ResourcesPlugin.getWorkspace().getRoot();
				break;
			case 1 :
				resource = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
				break;
			default :
				resource = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		}

		if (resource.getLocation() == null) {
			// can occur if the first segment is not a project
			String message = Policy.bind("exception.pathNotValid", path.toString()); //$NON-NLS-1$
			if (failOnError)
				throw new BuildException(message);
			log(message, Project.MSG_ERR);
			return;
		}
		if (property != null)
			getProject().setUserProperty(property, resource.getLocation().toOSString());
		if (pathID != null) {
			Path newPath = new Path(getProject(), resource.getLocation().toOSString());
			getProject().addReference(pathID, newPath);
		}
	}

	/**
	 * Sets the file system path.
	 *
	 * @param value the file corresponding to the path supplied by the user
	 */
	public void setFileSystemPath(File value) {
		if (resourcePath != null)
			throw new BuildException(Policy.bind("exception.cantUseBoth")); //$NON-NLS-1$
		fileSystemPath = new org.eclipse.core.runtime.Path(value.toString());
	}

	/**
	 * Sets the resource path.
	 *
	 * @param value the path
	 */
	public void setResourcePath(String value) {
		if (fileSystemPath != null)
			throw new BuildException(Policy.bind("exception.cantUseBoth")); //$NON-NLS-1$
		resourcePath = new org.eclipse.core.runtime.Path(value);
	}

	/**
	 * Sets the name of the property where the result may stored.
	 *
	 * @param value the name of the property
	 */
	public void setProperty(String value) {
		property = value;

	}

	/**
	 * Sets the id for the path where the result may be stored
	 *
	 * @param value the id of the path
	 */
	public void setPathId(String value) {
		pathID = value;
	}

	/**
	 * Sets whether the build should fail for unknown workspace pathes.
	 * If false, note errors to the output, but keep going.
	 * <p>
	 * This affects how non-existing workspace resources are handled. If true, a non-existing path
	 * will cause the build to fail. If false, the result property and/or path will remain unset
	 * and the error will be logged to the output.
	 * <p>
	 * Defaults to true
	 *
	 * @param value the failure policy
	 */
	public void setFailOnError(boolean value) {
		failOnError = value;
	}

	/**
	 * Performs a validation of the receiver.
	 *
	 * @exception BuildException thrown if a problem occurs during validation.
	 */
	protected void validateAttributes() throws BuildException {
		if (property == null && pathID == null)
			throw new BuildException(Policy.bind("exception.propertyAndPathIdNotSpecified")); //$NON-NLS-1$

		if (resourcePath != null && (!resourcePath.isValidPath(resourcePath.toString()) || resourcePath.isEmpty()))
			throw new BuildException(Policy.bind("exception.invalidPath", resourcePath.toOSString())); //$NON-NLS-1$
		else if (fileSystemPath != null && !fileSystemPath.isValidPath(fileSystemPath.toOSString()))
			throw new BuildException(Policy.bind("exception.invalidPath", fileSystemPath.toOSString())); //$NON-NLS-1$

		if (resourcePath == null && fileSystemPath == null)
			throw new BuildException(Policy.bind("exception.mustHaveOneAttribute")); //$NON-NLS-1$
	}
}
