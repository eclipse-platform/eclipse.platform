package org.eclipse.ant.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.*;

public class AntAction extends Action {
	
	private IFile file;
	
public AntAction(IFile file) {
	super();
	this.file = file;
	if (file != null) {
		setText(file.getName());
		setToolTipText(file.getFullPath().toOSString());
	}
	else
		setText(Policy.bind("launcher.noFile"));
}

public void run() {
	if (file == null)
		return;
	
	// save the modified files if required by the user
	if (AntUIPlugin.getPlugin().getPreferenceStore().getBoolean(IAntPreferenceConstants.AUTO_SAVE)) {
		IEditorPart[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditors();
		for (int i=0; i<editors.length; i++) {
			if (editors[i].isDirty()) {
				editors[i].getSite().getPage().saveEditor(editors[i],false);
			}
		}
	}
	
	Project project = extractProject(file);
	if (project == null)
		return;
		
	AntLaunchWizard wizard = new AntLaunchWizard(project,file);
	wizard.setNeedsProgressMonitor(true);
	WizardDialog dialog = new WizardDialog(AntUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow().getShell(),wizard);
	dialog.create();
	dialog.open();
}

protected Project extractProject(IFile sourceFile) {
	// create a project and initialize it
	Project antProject = new Project();
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

}

