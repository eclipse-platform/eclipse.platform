/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

public abstract class CVSModelElement implements IWorkbenchAdapter, IAdaptable {
	
	private IRunnableContext runnableContext;
	private IWorkingSet workingSet;
		
	public IWorkingSet getWorkingSet() {
		return workingSet;
	}

	public void setWorkingSet(IWorkingSet workingSet) {
		this.workingSet = workingSet;
	}

	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class)
			return adapter.cast(this);
		if ((adapter == IDeferredWorkbenchAdapter.class) && this instanceof IDeferredWorkbenchAdapter)
			return adapter.cast(this);
		return null;
	}

	/**
	 * Returns the runnableContext.
	 * @return ITeamRunnableContext
	 */
	public IRunnableContext getRunnableContext() {
		if (runnableContext == null) {
			return PlatformUI.getWorkbench().getProgressService();
		}
		return runnableContext;
	}

	/**
	 * Sets the runnableContext.
	 * @param runnableContext The runnableContext to set
	 */
	public void setRunnableContext(IRunnableContext runnableContext) {
		this.runnableContext = runnableContext;
	}

	public Object[] getChildren(Object o) {
		try {
			return fetchChildren(o, null);
		} catch (TeamException e) {
			handle(e);
		}
		
		return new Object[0];
	}
	
	abstract protected Object[] fetchChildren(Object o, IProgressMonitor monitor) throws TeamException;
		
	/**
	 * Handle an exception that occurred in CVS model elements by displaying an error dialog.
	 * @param title the title of the error dialog
	 * @param description the description to be displayed
	 * @param e the exception that occurred
	 */
	protected void handle(final String title, final String description, final Throwable e) {
		CVSUIPlugin.openError(null, title, description, e, CVSUIPlugin.LOG_NONTEAM_EXCEPTIONS | CVSUIPlugin.PERFORM_SYNC_EXEC);
	}
	
	/**
	 * Helper method error handler that displays a generic dialog title and message when displaying an error to the user.
	 * @param t the exception that occurred.
	 */
	protected void handle(Throwable t) {
		handle(CVSUIMessages.CVSModelElement_0, CVSUIMessages.CVSModelElement_1, t); // 
	}
	
	/**
	 * Handle an exception that occurred while fetching the children for a deferred workbench adapter.
	 * @param collector the collector for the adapter
	 * @param e the exception that occurred
	 */
	protected void handle(IElementCollector collector, Throwable t) {
		// TODO: For now, just display a dialog (see bug 65008 and 65741)
		handle(t);
	}

}
