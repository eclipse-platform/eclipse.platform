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
	
public class HackUIBuildListener {
	private AntConsole[] consoles;
	private int logLength = 0;

public HackUIBuildListener(AntConsole[] consoles) {
	this.consoles = consoles;
	if (consoles != null)
    	for (int i=0; i < consoles.length; i++) {
			consoles[i].initializeOutputStructure();
			consoles[i].initializeTreeInput();
    	}
}
public void logMessage(String message) {
	for (int i=0; i < consoles.length; i++)
		consoles[i].append(message, 0);
	logLength += message.length();
}
/*
 * Used to create output structure elements for targets.
 * 
 * Note: we need to have two different #createNewOutputStructureElement methods because
 * when we create a target, we need to take the two-last line index as the start index, not
 * the current index (this is because the Ant output is not well structured)
 */
/*
 * Used to create output structure elements for projects and tasks
 */
}
