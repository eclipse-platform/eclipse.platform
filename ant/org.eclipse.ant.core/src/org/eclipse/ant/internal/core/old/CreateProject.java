package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import java.io.File;
import java.util.*;
import org.apache.tools.ant.*;

/**
 * An Ant task which creates an Eclipse project.
 * <p>
 * The name of the project to create must be specified.<br>
 * The directory where the project is located on the file system, the comment, the nature(s)
 * and the referenced projects are optional. If the directory is not specified, the project will be
 * located in the default workspace folder.<br>
 * It is also possible to define commands with nested elements.
 * <p><p>
 * Example:<p>
 *	&lt;eclipse.createProject name="My Project" location="D:\MyWork\FirstProject" 
 *  natures="org.eclipse.jdt.core.javanature"/&gt;
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 * @see IProject#create
 * @see CommandDataType
 */
public class CreateProject extends Task {
	
	/**
	 * The name of the project.
	 */
	private String name = null;
	
	/**
	 * The location of the project.
	 */
	private IPath location = null;
	
	/**
	 * The nature of the project.
	 */
	private String[] natures = null;
	
	/**
	 * The comment for this project
	 */
	private String comment = null;
	
	/**
	 * The projects that the project references
	 */
	private IProject[] referencedProjects = null;

	/**
	 * The builders and their arguments (commands)
	 */
	private Vector commands;


/**
 * Constructs a new <code>CreateProject</code> instance.
 */
public CreateProject() {
	super();
	commands = new Vector();
}

/**
 * Performs the project creation operation.
 * 
 * @exception BuildException thrown if a problem occurs during execution.
 */
public void execute() throws BuildException {
	
	validateAttributes();
	
	IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	
	try {
		if (newProject.exists())
			throw new BuildException(Policy.bind("exception.nameAlreadyUsed", name));
			
		IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
		description.setName(name);
		description.setLocation(location);
		if (comment != null)
			description.setComment(comment);
		if (referencedProjects != null)
			description.setReferencedProjects(referencedProjects);
		if (!commands.isEmpty())
			description.setBuildSpec(createCommands(description));
		
		newProject.create(description, null);
		newProject.open(null);
		
		if (natures!=null) {
			// should be able to do that while creating the project, but it doesn't seem to work until the project is opened.
			// see Project#checkAccessible while JavaNature tries to configure the project.
			IProjectDescription desc = newProject.getDescription();
			description.setNatureIds(natures);
			newProject.setDescription(desc, null);
		}
		
	} catch (CoreException e) {
		if (newProject.exists())
			try {
				newProject.delete(true, true, null);
			} catch (CoreException ce) {
			}
		throw new BuildException(e);
	}
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
 * Sets the name of the project that the user wants to create.
 * 
 * @param the name of the project		
 */
public void setName(String value) {
	name = value;
}

/**
 * Sets the folder where the project should be created.
 * 
 * @param the file corresponding to the folder specified by the user
 */
public void setLocation(File value) {
	 location = new Path(value.toString());
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
	IWorkspaceRoot root =  ResourcesPlugin.getWorkspace().getRoot();
	while (tokenizer.hasMoreTokens()) {
		IProject currentProject = root.getProject(((String) tokenizer.nextToken()).trim());
		vect.add(currentProject);
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
	if (name == null) 
		throw new BuildException(Policy.bind("exception.nameNotSpecified"));
	
	if (location!=null && !location.isValidPath(location.toOSString()))
		throw new BuildException(Policy.bind("exception.invalidPath", location.toOSString()));
}

}
