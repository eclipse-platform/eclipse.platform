/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.model.RemoteModule;
import org.eclipse.team.internal.ccvs.ui.tags.TagConfigurationDialog;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;
;

/**
 * DefineTagAction remembers a tag by name
 */
public class ConfigureTagsFromRepoViewOnFolder extends CVSAction {
	
	/**
	 * Returns the selected remote folders
	 */
	protected ICVSRemoteFolder[] getSelectedRemoteFolders() {
		ArrayList resources = null;
		IStructuredSelection selection = getSelection();
		if (!selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof RemoteModule) {
					next = ((RemoteModule) next).getCVSResource();
				}
				if (next instanceof ICVSRemoteFolder) {
					resources.add(next);
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			return (ICVSRemoteFolder[])resources.toArray(new ICVSRemoteFolder[resources.size()]);
		}
		return new ICVSRemoteFolder[0];
	}

	/*
	 * @see CVSAction@execute(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				final ICVSRemoteFolder[] roots = getSelectedRemoteFolders();
				final Shell shell = getShell();
				shell.getDisplay().syncExec(new Runnable() {
					public void run() {
						ICVSFolder[] cvsFolders = new ICVSFolder[roots.length];
						for (int i = 0; i < roots.length; i++) {
							cvsFolders[i] = roots[i];
						}
						TagConfigurationDialog d = new TagConfigurationDialog(shell, TagSource.create(cvsFolders));
						d.open();
					}
				});
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}

	/*
	 * @see TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		return true;
	}
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return CVSUIMessages.ConfigureTagsFromRepoViewConfigure_Tag_Error_1; 
	}

}
