package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.apache.tools.ant.*;

/**
 * An Ant task which copies an existing Eclipse project.
 * <p>
 * The name of the project to copy (source), its new name (destination) and the "force" attribute 
 * must be specified.<br>
 * The comment, the nature(s) and the referenced projects are optional.<br>
 * Concerning the location attribute (the directory where the project will be copied on the file system),
 * there are several cases:<br>
 * <ul>
 * 	<li>If "location" is specified, the project will be copied to the given directory. 
 * 		To specify the default workspace directory, location should be set to "null".
 * 		However, it is not possible to copy the project in the same directory.</li>
 * 	<li>If "location" is not specified, the copy of the project will be located in the
 * 		default worksapce directory.</li>
 * </ul>
 * <br>
 * It is also possible to define commands with nested elements.
 * <p><p>
 * Example:<p>
 *	&lt;eclipse.copyProject source="Foo" destination="Bar" location="D:\MyWork\My Project"/&gt;
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 * @see IProject#copy
 * @see CommandDataType
 */
public class CopyProject extends Task {
	
	/**
	 * The name of the source project.
	 */
	private String source = null;
	
	/**
	 * The name of the project after it has been copied.
	 */
	private String destination = null;

	/**
	 * The force attribute.
	 */
	private boolean force;
	// variable used to know if the user has actually set the "force" attribute
	private boolean isForceSet = false;
	
	/**
	 * The folder where the user wants to move the project.
	 */
	private IPath location = null;
	// variable used to know if the user has specified "null" or not
	private boolean isLocationSet = false;
	
	/**
	 * The natures, if the user wants to modify them.
	 */
	private String[] natures = null;
	
	/**
	 * The comment, if the user wants to modify it
	 */
	private String comment = null;
	
	/**
	 * The projects that the project references, if the user wants to modify them
	 */
	private IProject[] referencedProjects = null;

	/**
	 * The builders and their arguments (commands), if the user wants to modify them
	 */
	private Vector commands;
	

/**
 * Constructs a new <code>CopyProject</code> instance.
 */
public CopyProject() {
	super();
	commands = new Vector();
}

/**
 * Performs the project copy operation.
 * 
 * @exception BuildException thrown if a problem occurs during execution.
 */
public void execute() throws BuildException {
	
	validateAttributes();
	
	IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(source);
	
	try {
		if (!newProject.exists()) 
			throw new BuildException(Policy.bind("exception.projectDoesntExist", source));
			
		IProject temp = ResourcesPlugin.getWorkspace().getRoot().getProject(destination);
		if (temp.exists())
			throw new BuildException(Policy.bind("exception.nameAlreadyUsed", destination));
			
		if (!newProject.isOpen())
			newProject.open(null);
		
		IProjectDescription desc = newProject.getDescription();
		desc.setName(destination);
		setLocationToDescription(newProject, desc);
		
		if (natures != null)
			desc.setNatureIds(natures);
		if (comment != null)
			desc.setComment(comment);
		if (referencedProjects != null)
			desc.setReferencedProjects(referencedProjects);
		if (!commands.isEmpty())
			desc.setBuildSpec(createCommands(desc));
			
		newProject.copy(desc, force, null);
		
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

protected void setLocationToDescription(IProject newProject, IProjectDescription desc) {
	if (isLocationSet) {
		// the user wants to change the location.
		// location can be null if the user set the attribute to "null" in order to copy the folder in the default workspace directory
		if (location!= null && !ResourcesPlugin.getWorkspace().validateProjectLocation(newProject, location).isOK())
			throw new BuildException(Policy.bind("exception.folderAlreadyUsedForAnotherProject"));
	}
	desc.setLocation(location);
}

protected ICommand[] createCommands(IProjectDescription description) {
	ICommand[] commandArray = new ICommand[commands.size()];
	int index = 0;
	for (Iterator i = commands.iterator(); i.hasNext();) {
		CommandDataType b = (CommandDataType) i.next();
		commandArray[index] = description.newCommand();
		// needs the project of this target to be able to get the name and the arguments
		commandArray[index].setBuilderName(b.getName(project));
		commandArray[index].setArguments(b.getArguments(project));
		index++;
	}
	return commandArray;
}

/**
 * Sets the name of the source project that the user wants to copy.
 * 
 * @param the name of the project		
 */
public void setSource(String value) {
	source = value;
}

/**
 * Sets the new name for the project.
 * 
 * @param the new name	
 */
public void setDestination(String value) {
	destination = value;
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
 * Sets the directory where the project will be copied. The parameter is a String
 * and not a File because the user can specify "null" if he wants to refer to the
 * default workspace directory.
 * 
 * @param the name of the folder or "null" for the default workspace directory
 */
public void setLocation(String value) {
	isLocationSet = true;
	if (value.equals("null"))
		// we leave the location as it currently is (i.e. null)
		return;
	location = new Path(value);
	if (!location.isAbsolute()) {
		// the location can be relative if the user used a period in the path
		org.eclipse.core.runtime.Path defaultPath = new org.eclipse.core.runtime.Path(project.getBaseDir().toString());
		location = defaultPath.append(location);
	}
}

/**
 * Sets the natures for the project. They are given as a list of nature identifers
 * separated by a coma.
 * 
 * @param the natures specified by the user
 */
public void setNatures(String value) {
	StringTokenizer tokenizer = new StringTokenizer(value, ",");
	Vector vect = new Vector(1);
	while (tokenizer.hasMoreTokens())
		vect.add(((String)tokenizer.nextToken()).trim());
	natures = new String[vect.size()];
	vect.toArray(natures);
}

/**
 * Sets the comment for the project.
 * 
 * @param the comment
 */
public void setComment(String value) {
	comment = value;
}

/**
 * Sets the projects that the project references. They are given as a list of project names
 * separated by a coma.
 * 
 * @param the natures specified by the user
 */
public void setReferencedProjects(String value) {
	StringTokenizer tokenizer = new StringTokenizer(value, ",");
	Vector vect = new Vector(1);
	while (tokenizer.hasMoreTokens()) {
		IProject currentProject = ResourcesPlugin.getWorkspace().getRoot().getProject(((String) tokenizer.nextToken()).trim());
		if (currentProject.exists())
			vect.add(currentProject);
		else
			throw new BuildException(Policy.bind("exception.unknownProject", currentProject.getName()));
	}
		
	referencedProjects = new IProject[vect.size()];
	vect.toArray(referencedProjects);
}

/**
 * Adds a CommandDataType object to the set of commands specified for this project.
 * 
 * @param the command
 */
public void addCommand(CommandDataType command) {
	commands.add(command);
}

/**
 * Performs a validation of the receiver.
 * 
 * @exception BuildException thrown if a problem occurs during validation.
 */
protected void validateAttributes() throws BuildException {
	if (source == null) 
		throw new BuildException(Policy.bind("exception.sourceNotSpecified"));

	if (destination == null) 
		throw new BuildException(Policy.bind("exception.destinationNotSpecified"));

	if (location!= null && !location.isValidPath(location.toOSString()))
		throw new BuildException(Policy.bind("exception.invalidPath", location.toOSString()));
	
	if (!isForceSet)
		throw new BuildException(Policy.bind("exception.forceNotSpecified"));
}

}
