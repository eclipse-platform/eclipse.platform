package org.eclipse.ant.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.*;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.internal.core.old.EclipseProject;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

// TBD
// * Marker mechanism doesn't work for Locations other than
//   the original build file. This could pose problems for
//   ant tasks.
// * incremental task shows minimal feedback

// public class UIBuildListener implements IAntRunnerListener { // FIXME
	
public class UIBuildListener implements BuildListener {
	private Target fTarget;
	private Task fTask;
	private int msgOutputLevel = Project.MSG_INFO;
	private AntConsole[] consoles;
	private int logLength = 0;
	// index of the last target end
	private int lastTargetEndIndex = 0;
	private boolean isTargetWithDependencies = false;
public UIBuildListener(AntRunner runner, IProgressMonitor monitor, IFile file, AntConsole[] consoles) {
	super();
	this.consoles = consoles;
	if (consoles != null)
    	for (int i=0; i < consoles.length; i++) {
			consoles[i].initializeOutputStructure();
			consoles[i].initializeTreeInput();
    	}
}
public void buildFinished(BuildEvent be){
	
	// We must give the name of the project here because when the build starts, the name has not been parsed yet.
	setProjectNameForOutputStructures(be.getProject().getName());
	
	// and we finish the curent element
	finishCurrentOutputStructureElement();
	
	// And finaly tell the consoles to update
	refreshConsoleTrees();
}
private void setProjectNameForOutputStructures(String name) {
	if (consoles != null)
		for (int i=0; i < consoles.length; i++)
			consoles[i].currentElement.setName(name);
}
protected void refreshConsoleTrees() {
    if (consoles != null)
    	// create a new thread for synchronizing all the refresh operations
    	// we get the display from the console #0 (that exists for sure because consoles!=null)
    	consoles[0].getSite().getShell().getDisplay().syncExec(new Runnable() {
    		public void run() {
   				for (int i=0; i < consoles.length; i++)
					consoles[i].refreshTree();
    		}
    	});
}
public void buildStarted(BuildEvent be) {
	
	// the current (first) output element is the one for the script, so we have to set the end index for it
	finishCurrentOutputStructureElement();
	
	// we create the second element which represents the project.
	// Unfortunately, the name has not been parsed yet, so we'll have to catch it at the very end.
	// We give a default name ("Project") till we can actually set the real name.
	createNewOutputStructureElement(Policy.bind("console.project"));
}
public void messageLogged(BuildEvent event) {
}
private void logMessage(String message, int priority) {
    if (consoles != null && priority <= msgOutputLevel) {
		for (int i=0; i < consoles.length; i++)
			consoles[i].append(message, priority);
		logLength += message.length();
    }
}
public void targetStarted(BuildEvent be) {
	fTarget= be.getTarget();
	int startIndex = logLength;
	// the targets that need to look for the last target end index are targets that have no dependency and that
	// are in an EclipseProject (if they are in an standard Project, this means that they were executed with the 'ant'
	// task, and therefore have no ouput to look for)
	if (!isTargetWithDependencies && (be.getProject() instanceof EclipseProject))
		startIndex = lastTargetEndIndex;
	createNewOutputStructureElement(fTarget.getName(), startIndex);	
}
public void targetFinished(BuildEvent be) {
	
	finishCurrentOutputStructureElement();
	
	// store the end index of this target's log (so that we can use it later)
	lastTargetEndIndex = logLength;
	
	refreshConsoleTrees();
}
public void taskStarted(BuildEvent be) {
	fTask= be.getTask();
	createNewOutputStructureElement(fTask.getTaskName());
}
public void taskFinished(BuildEvent be) {
	
	finishCurrentOutputStructureElement();
	
	refreshConsoleTrees();
}
/*
 * Used to create output structure elements for targets.
 * 
 * Note: we need to have two different #createNewOutputStructureElement methods because
 * when we create a target, we need to take the two-last line index as the start index, not
 * the current index (this is because the Ant output is not well structured)
 */
protected void createNewOutputStructureElement(String name, int index) {
	if (consoles != null)
    	for (int i=0; i < consoles.length; i++) {
		    // creates a new OutputStructureElement with the current element as a parameter for the parent of this object
			OutputStructureElement newElement = new OutputStructureElement(name, consoles[i].currentElement, index);
			// and sets the current element to the one that has just been created
			consoles[i].currentElement = newElement;
    	}
}
/*
 * Used to create output structure elements for projects and tasks
 */
protected void createNewOutputStructureElement(String name) {
	createNewOutputStructureElement(name, logLength);
}
protected void finishCurrentOutputStructureElement() {
	if (consoles != null)
    	for (int i=0; i < consoles.length; i++) {
		    // sets the index that indicates the end of the log part linked to this element
			consoles[i].currentElement.setEndIndex(logLength);
			// and sets the current element to the parent of the element
			consoles[i].currentElement = consoles[i].currentElement.getParent();
    	}
}
}
