package org.eclipse.ant.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.viewers.*;

/**
 * Content provider for the Ant dialog: it provides the ListViewer with the targets defined in the xml file.
 */

public class TargetsListContentProvider implements IStructuredContentProvider {
	
	private static TargetsListContentProvider instance;
	
	static {
		instance = new TargetsListContentProvider();
	}
	
// private to ensure that it remains a singleton
private TargetsListContentProvider() {
	super();
}

public static TargetsListContentProvider getInstance() {
	return instance;
}

/**
 * Returns the targets found in the xml file after parsing.
 * 
 * @param groupName the name of the group
 * @return the array of the targets found
 */
public Object[] getElements(Object inputElement) {
	Project project = (Project)inputElement;
	return project.getTargets().values().toArray();
}

public void dispose() {
}

public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
}

}
