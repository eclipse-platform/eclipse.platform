/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.ICompareContainer;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.services.IServiceLocator;

public class CompareContainer implements ICompareContainer {

	private WorkerJob worker;

	@Override
	public void setStatusMessage(String message) {
		// Do nothing, by default
	}

	@Override
	public void addCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		input.addCompareInputChangeListener(listener);
	}

	@Override
	public void removeCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		input.removeCompareInputChangeListener(listener);
	}

	@Override
	public void registerContextMenu(MenuManager menu,
			ISelectionProvider selectionProvider) {
		// Nothing to do
	}

	@Override
	public IServiceLocator getServiceLocator() {
		return null;
	}

	@Override
	public IActionBars getActionBars() {
		return null;
	}

	@Override
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable)
			throws InvocationTargetException, InterruptedException {
		Utilities.executeRunnable(runnable, fork, cancelable);
	}

	@Override
	public ICompareNavigator getNavigator() {
		return null;
	}

	@Override
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

	@Override
	public IWorkbenchPart getWorkbenchPart() {
		return null;
	}

}
