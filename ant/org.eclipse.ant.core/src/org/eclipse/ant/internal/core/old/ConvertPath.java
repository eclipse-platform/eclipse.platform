package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import java.io.File;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.Path;

/**
 * An Ant task which allows to switch from a file system path to a resource path, 
 * and vice versa, and store the result in a user property which name is set by the user. If the 
 * resource does not exist, the property is set to "false".
 * <p>
 * The attribute "property" must be specified, as well as only one of "fileSystemPath" or "resourcePath".
 * <p><p>
 * Example:<p>
 *	&lt;eclipse.convertPath fileSystemPath="D:\MyWork\MyProject" property="myProject.resourcePath"/&gt;
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
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
 * Constructs a new <code>ConvertPath</code> instance.
 */
public ConvertPath() {
	super();
}

/**
 * Performs the project copy operation.
 * 
 * @exception BuildException thrown if a problem occurs during execution.
 */
public void execute() throws BuildException {
	
	validateAttributes();
	
	if (fileSystemPath == null) 
		// here, resourcePath is not null
		convertResourcePathToFileSystemPath(resourcePath);
	else
		convertFileSystemPathToResourcePath(fileSystemPath);
}

protected void convertFileSystemPathToResourcePath(IPath path) {
	IResource resource = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(path);
	if (resource == null)
		throw new BuildException(Policy.bind("exception.noProjectMatchThePath", fileSystemPath.toOSString()));
	
	if (property != null)
		project.setUserProperty(property, resource.getFullPath().toString());
	if (pathID != null) {
		Path newPath = new Path(project, resource.getFullPath().toString());
		project.addReference(pathID, newPath);
	}
}

protected void convertResourcePathToFileSystemPath(IPath path) {
	IResource resource = null;
	if (path.segmentCount() == 1)
		resource = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
	else
		resource = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		
	if (resource.getLocation() == null)
		// can occur if the first segment is not a project
		throw new BuildException(Policy.bind("exception.pathNotValid", path.toString()));
	
	if (property != null)
		project.setUserProperty(property, resource.getLocation().toOSString());
	if (pathID != null) {
		Path newPath = new Path(project, resource.getLocation().toOSString());
		project.addReference(pathID, newPath);
	}
}

/**
 * Sets the file system path.
 * 
 * @param the file corresponding to the path supplied by the user
 */
public void setFileSystemPath(File value) {
	if (resourcePath != null)
		throw new BuildException(Policy.bind("exception.cantUseBoth"));
	fileSystemPath = new org.eclipse.core.runtime.Path(value.toString());
}

/**
 * Sets the resource path.
 * 
 * @param the path	
 */
public void setResourcePath(String value) {
	if (fileSystemPath != null)
		throw new BuildException(Policy.bind("exception.cantUseBoth"));
	resourcePath = new org.eclipse.core.runtime.Path(value);
}

/**
 * Sets the name of the property where the result may stored.
 * 
 * @param the name of the property		
 */
public void setProperty(String value) {
	property = value;

}

/**
 * Sets the id for the path where the result may be stored
 * 
 * @param the id of the path
 */
public void setPathId(String value) {
	pathID = value;
}

/**
 * Performs a validation of the receiver.
 * 
 * @exception BuildException thrown if a problem occurs during validation.
 */
protected void validateAttributes() throws BuildException {
	if (property == null && pathID == null) 
		throw new BuildException(Policy.bind("exception.propertyAndPathIdNotSpecified"));

	if (resourcePath != null && (!resourcePath.isValidPath(resourcePath.toString()) || resourcePath.isEmpty())) 
		throw new BuildException(Policy.bind("exception.invalidPath", resourcePath.toOSString()));
	else if (fileSystemPath != null && !fileSystemPath.isValidPath(fileSystemPath.toOSString())) 
		throw new BuildException(Policy.bind("exception.invalidPath", fileSystemPath.toOSString()));
		
	if (resourcePath == null && fileSystemPath == null)
		throw new BuildException(Policy.bind("exception.mustHaveOneAttribute"));
}

}
