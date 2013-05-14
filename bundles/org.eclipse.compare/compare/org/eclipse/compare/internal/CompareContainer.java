/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.ICompareContainer;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.*;
import org.eclipse.ui.services.IServiceLocator;

public class CompareContainer implements ICompareContainer {
	
	private WorkerJob worker;

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#setStatusMessage(java.lang.String)
	 */
	public void setStatusMessage(String message) {
		// Do nothing, by default
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#addCompareInputChangeListener(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener)
	 */
	public void addCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		input.addCompareInputChangeListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#removeCompareInputChangeListener(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener)
	 */
	public void removeCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		input.removeCompareInputChangeListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#registerContextMenu(org.eclipse.jface.action.MenuManager, org.eclipse.jface.viewers.ISelectionProvider)
	 */
	public void registerContextMenu(MenuManager menu,
			ISelectionProvider selectionProvider) {
		// Nothing to do
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#getServiceLocator()
	 */
	public IServiceLocator getServiceLocator() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#getActionBars()
	 */
	public IActionBars getActionBars() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableContext#run(boolean, boolean, org.eclipse.jface.operation.IRunnableWithProgress)
	 */
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable)
			throws InvocationTargetException, InterruptedException {
		PlatformUI.getWorkbench().getProgressService().run(fork, cancelable, runnable);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#getNavigator()
	 */
	public ICompareNavigator getNavigator() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#runAsynchronously(org.eclipse.jface.operation.IRunnableWithProgress)
	 */
	public synchronized void runAsynchronously(IRunnableWithProgress runnable) {
		if (worker == null)
			worker = createWorkerJob();
		worker.add(runnable);
	}

	protected WorkerJob createWorkerJob() {
		WorkerJob workerJob = new WorkerJob(getWorkerJobName());
		return workerJob;
	}

	protected String getWorkerJobName() {
		return CompareMessages.CompareContainer_0;
	}

	public IWorkbenchPart getWorkbenchPart() {
		return null;
	}

}
