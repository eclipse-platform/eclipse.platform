package org.eclipse.ant.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import org.apache.tools.ant.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ant.core.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;import org.eclipse.jface.wizard.WizardDialog;


public class RunAntActionDelegate implements IWorkbenchWindowActionDelegate {

	private IFile selection;

/*
 * @see IWorkbenchWindowActionDelegate
 */
public void dispose() {
}
protected EclipseProject extractProject(IFile sourceFile) {
	// create a project and initialize it
	EclipseProject antProject = new EclipseProject();
	antProject.init();
	antProject.setProperty("ant.file",sourceFile.getLocation().toOSString());
	
	try {
		ProjectHelper.configureProject(antProject,new File(sourceFile.getLocation().toOSString()));
	} catch (Exception e) {
		// If the document is not well-formated for example
		String message = e.getMessage();
		if (message == null)
			message = Policy.bind("error.antParsingError");
		IStatus status = new Status(IStatus.ERROR,AntUIPlugin.PI_ANTUI,IStatus.ERROR,message,e);
		ErrorDialog.openError(
			AntUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow().getShell(),
			Policy.bind("error.antScriptError"),
			Policy.bind("error.antParsingError"),
			status);
			
		return null;
	}
	return antProject;
}

/**
  * Returns the active shell.
  */
protected Shell getShell() {
	return AntUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow().getShell();
}
/*
 * @see IWorkbenchWindowActionDelegate
 */
public void init(IWorkbenchWindow window) {
}
/*
 * @see IActionDelegate
 */
public void run(IAction action) {
	EclipseProject project = extractProject(selection);
	if (project == null)
		return;
		
	AntLaunchWizard wizard = new AntLaunchWizard(project,selection);
	wizard.setNeedsProgressMonitor(true);
	WizardDialog dialog = new WizardDialog(getShell(),wizard);
	dialog.create();
	dialog.open();
}
/*
 * @see IWorkbenchActionDelegate
 */
public void selectionChanged(IAction action, ISelection selection) {
	this.selection = null;
	if (selection instanceof IStructuredSelection) {
		IStructuredSelection structuredSelection = (IStructuredSelection)selection;
		if (structuredSelection.size() == 1) {
			Object selectedResource = structuredSelection.getFirstElement();
			if (selectedResource instanceof IFile)
				this.selection = (IFile)selectedResource;
		}
	}
}

}
