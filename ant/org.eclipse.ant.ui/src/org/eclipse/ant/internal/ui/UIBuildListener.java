package org.eclipse.ant.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.*;
import org.eclipse.ant.core.AntRunner;import org.eclipse.ant.core.AntRunnerListener;import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

// TBD
// * Marker mechanism doesn't work for Locations other than
//   the original build file. This could pose problems for
//   ant tasks.
// * incremental task shows minimal feedback

public class UIBuildListener implements AntRunnerListener {
	
	private AntRunner runner;
	private IProgressMonitor fMonitor;
	private Target fTarget;
	private Task fTask;
	private IFile fBuildFile;
	private int msgOutputLevel = Project.MSG_INFO;
	private AntConsole console;
	
public UIBuildListener(AntRunner runner, IProgressMonitor monitor, IFile file, AntConsole console) {
	super();
	this.console = console;
	this.runner = runner;
	fMonitor = monitor;
	fBuildFile = file;
}
/**
 * @deprecated
 */
public UIBuildListener(AntRunner runner, IProgressMonitor monitor, IFile file) {
	super();
	this.runner = runner;
	fMonitor = monitor;
	fBuildFile = file;
}
public void buildFinished(BuildEvent be){
	fMonitor.done();
	if (be.getException() != null) {
		handleBuildException(be.getException());
	}
}
public void buildStarted(BuildEvent be) {
	fMonitor.subTask(Policy.bind("monitor.buildStarted"));
	msgOutputLevel = runner.getOutputMessageLevel();
	removeMarkers();
}
private void checkCanceled() {
	if (fMonitor.isCanceled())
		throw new BuildCanceledException();
}
private void createMarker(IFile file, BuildException be) {
	try {
		int lineNumber= getLineFromLocation(be.getLocation());
		IMarker marker= file.createMarker(IMarker.PROBLEM);
		Map map= new HashMap();
		map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
		map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
		map.put(IMarker.MESSAGE, be.getMessage());
		map.put(IMarker.LOCATION, Integer.toString(lineNumber));
		marker.setAttributes(map);
	} catch (CoreException e) {
		e.printStackTrace();
	}
}
private int getLineFromLocation(Location l) {
	String locstr= l.toString();
	int end= locstr.lastIndexOf(':');
	int start= locstr.lastIndexOf(':', end-1);
	String lstr= locstr.substring(start+1, end);
	try {
		return Integer.parseInt(lstr);
	} catch (NumberFormatException e) {
		return -1;
	}
}
private void handleBuildException(Throwable t) {
	if (console != null)
		console.append(Policy.bind("exception.buildException", t.toString()) + "\n", Project.MSG_ERR);
		
	if (t instanceof BuildException) {
		BuildException bex= (BuildException)t;
		// the build exception has a location that
		// refers to a build file
		if (bex.getLocation() != Location.UNKNOWN_LOCATION) {
			createMarker(fBuildFile, bex);					
			System.out.println(bex.getLocation());
		}
	}
}
public void messageLogged(BuildEvent event) {
	checkCanceled();
    if (console != null && event.getPriority() <= msgOutputLevel)
		console.append(event.getMessage() + "\n", event.getPriority());
}

public void messageLogged(String message,int priority) {
	checkCanceled();
    if ((console != null) && priority <= msgOutputLevel)
		console.append(message + "\n", priority);
}
private void removeMarkers() {
	try {
		fBuildFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
	} catch (CoreException e) {
		e.printStackTrace();
	}
}
public void targetFinished(BuildEvent be) {
	checkCanceled();
	if (be.getException() != null)
		handleBuildException(be.getException());
	 
	//	one task is done: say it to the monitor
	fMonitor.worked(1);
}
public void targetStarted(BuildEvent be) {
	checkCanceled();
	fTarget= be.getTarget();
	fMonitor.subTask(Policy.bind("monitor.targetColumn")+"\""+fTarget.getName()+"\" "+Policy.bind("monitor.started"));
}
public void taskFinished(BuildEvent be) {
	checkCanceled();
}
public void taskStarted(BuildEvent be) {
	checkCanceled();
	fTask= be.getTask();
	fMonitor.subTask(Policy.bind("monitor.targetColumn")+"\""+fTarget.getName()+"\" - "+fTask.getTaskName());
	if (be.getException() != null)
		handleBuildException(be.getException());
}
	
}
