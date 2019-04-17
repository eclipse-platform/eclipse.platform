/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     CSC - Intial implementation
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction.IProviderAction;

/**
 * This Action gets the <code>EditorsInfo[]</code>
 * It is used by the <code>ShowEditorAction</code> 
 * and <code>ShowEditorAction</code>.
 * 
 * @author <a href="mailto:gregor.kohlwes@csc.com,kohlwes@gmx.net">Gregor
 * Kohlwes</a>
 * 
 */
public class EditorsAction implements IProviderAction, IRunnableWithProgress {
	EditorsInfo[] f_editorsInfo = new EditorsInfo[0];
	CVSTeamProvider f_provider;
	IResource[] f_resources;

	public EditorsAction() {
	}
	
	public EditorsAction(CVSTeamProvider provider, IResource[] resources) {
		f_provider = provider;
		f_resources = resources;
	}

	@Override
	public IStatus execute(CVSTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws CVSException {
		f_editorsInfo = provider.editors(resources, monitor);
		return Team.OK_STATUS;
	}
	
	public boolean promptToEdit(Shell shell) {
		if (!isEmpty()) {
			final EditorsDialog view = new EditorsDialog(shell, f_editorsInfo);
			// Open the dialog using a sync exec (there are no guarentees that we
			// were called from the UI thread
			CVSUIPlugin.openDialog(shell, new CVSUIPlugin.IOpenableInShell() {
				@Override
				public void open(Shell shell) {
					view.open();
				}
			}, CVSUIPlugin.PERFORM_SYNC_EXEC);
			return (view.getReturnCode() == Window.OK);
		}
		return true;
	}

	/**
	 * Contact the server to determine if there are any editors on the associatd files.
	 * 
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (f_provider == null || f_resources == null) {
			throw new InvocationTargetException(new RuntimeException(NLS.bind(CVSUIMessages.EditorsAction_classNotInitialized, new String[] { this.getClass().getName() }))); 
		}
		try {
			execute(f_provider,f_resources,monitor);
		} catch (CVSException e) {
			throw new InvocationTargetException(e);
		}
	}

	/**
	 * Returns the f_editorsInfo.
	 * @return EditorsInfo[]
	 */
	public EditorsInfo[] getEditorsInfo() {
		return f_editorsInfo;
	}

	/**
	 * Indicates whether there are editors of any of the associated files.
	 * The <code>run(IProgressMonitor)</code> must be invoked first to 
	 * fetch any editors from the server.
	 * @return boolean
	 */
	public boolean isEmpty() {
		return f_editorsInfo.length == 0;
	}

}
