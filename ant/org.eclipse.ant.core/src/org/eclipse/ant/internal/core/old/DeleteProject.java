package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.apache.tools.ant.*;

/**
 * An Ant task which deletes an existing Eclipse project.
 * <p>
 * The name of the project to delete, as well as the force and 
 * the deleteContent attributes, must be specified.
 * <p><p>
 * Example:<p>
 *	&lt;eclipse.deleteProject name="My Project" force="true" deleteContent="true"/&gt;
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 * @see IProject#delete
 */
public class DeleteProject extends Task {
	
	/**
	 * The name of the project.
	 */
	private String name = null;
	
	/**
	 * The force attribute for the deletion.
	 */
	private boolean force;
	// isForceSet is used to know if the user has actually set this attribute
	private boolean isForceSet = false;
	
	/**
	 * The deleteContent attribute for the deletion.
	 */
	private boolean deleteContent;
	// same as for Force
	private boolean isDeleteContentSet = false;
	

/**
 * Constructs a new <code>DeleteProject</code> instance.
 */
public DeleteProject() {
	super();
}

/**
 * Performs the project deletion operation.
 * 
 * @exception BuildException thrown if a problem occurs during execution.
 */
public void execute() throws BuildException {
	
	validateAttributes();
	
	IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	
	try {
		if (!project.exists())
			throw new BuildException(Policy.bind("exception.projectDoesntExist", name));
			
		if (!project.isOpen())
			project.open(null);
		
		project.delete(deleteContent, force, null);
			
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

/**
 * Sets the name of the project that the user wants to delete.
 * 
 * @param the name of the project		
 */
public void setName(String value) {
	name = value;
}

/**
 * Sets the force attribute.
 * 
 * @param true/false		
 */
public void setForce(boolean value) {
	force = value;
	isForceSet = true;
}

/**
 * Sets the deleteContent attribute.
 * 
 * @param true/false		
 */
public void setDeleteContent(boolean value) {
	deleteContent = value;
	isDeleteContentSet = true;
}

/**
 * Performs a validation of the receiver.
 * 
 * @exception BuildException thrown if a problem occurs during validation.
 */
protected void validateAttributes() throws BuildException {
	if (name == null) 
		throw new BuildException(Policy.bind("exception.nameNotSpecified"));
		
	if (!isForceSet)
		throw new BuildException(Policy.bind("exception.forceNotSpecified"));
		
	if (!isDeleteContentSet)
		throw new BuildException(Policy.bind("exception.deleteContentNotSpecified"));
}

}
