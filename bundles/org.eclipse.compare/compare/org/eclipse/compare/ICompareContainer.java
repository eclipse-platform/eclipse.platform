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
package org.eclipse.compare;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.*;
import org.eclipse.ui.services.IServiceLocator;

/**
 * A compare container is used to represent any UI that can contain compare viewers.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.3
 */
public interface ICompareContainer extends IRunnableContext{
	
	/**
	 * Register for change events for the given compare input. Although clients can register
	 * with the compare input directly, registering through the container allows for 
	 * deterministic and optimized behavior in some cases. Registering multiple times for the 
	 * same compare input has no effect.
	 * @param input the compare input
	 * @param listener the compare input change listener
	 */
	public void addCompareInputChangeListener(ICompareInput input, ICompareInputChangeListener listener);
	
	/**
	 * Remove the change listener from the given compare input. Removing a listener that is not
	 * registered has no effect.
	 * @param input the compare input
	 * @param listener the compare input change listener
	 */
	public void removeCompareInputChangeListener(ICompareInput input, ICompareInputChangeListener listener);

	/**
	 * Register the content menu with the container to give the container a chance to
	 * add additional items to the context menu such as popup menu object contributions.
	 * The provided menu should have a {@link IWorkbenchActionConstants#MB_ADDITIONS}
	 * separator as this is where the container will add actions.
	 * @param menu the menu being registered
	 * @param selectionProvider the selection provider
	 */
	public void registerContextMenu(MenuManager menu, ISelectionProvider selectionProvider);
	
	/**
	 * Set the status message displayed by the container to the given message
	 * @param message the status message
	 */
	public void setStatusMessage(String message);
	
	/**
	 * Return the action bars for the container or <code>null</code> if the container
	 * does not have an action bars.
	 * @return the action bars for the container or <code>null</code>
	 */
	public IActionBars getActionBars();
	
	/**
	 * Return the service locator for the container or <code>null</code> if the container
	 * does not have one.
	 * @return the service locator for the container or <code>null</code>
	 */
	public IServiceLocator getServiceLocator();

	/**
	 * Return the {@link ICompareNavigator} associated with this container or <code>null</code>
	 * if the container does not have a global navigator.
	 * @return the {@link ICompareNavigator} associated with this container or <code>null</code>
	 */
	public ICompareNavigator getNavigator();
	
	/**
	 * Queue the given task to be run asynchronously. If the given runnable was
	 * previously queued to run asynchronously and it has not yet run, the task
	 * position will be moved to the end of the queue. If the task that is being
	 * queued is currently running, the running task will be canceled and added
	 * to the end of the queue.
	 * <p>
	 * This method should be treated as a request to run the given task asynchronously.
	 * However, clients should not assume that the code will be run asynchronously. 
	 * Depending on the container implementation, a call to this method may or may
	 * not block the caller until the task is completed. Also, the task may be executed
	 * in a modal or non-modal fashion.
	 * 
	 * @param runnable the task to be performed
	 */
	public void runAsynchronously(IRunnableWithProgress runnable);

	/**
	 * Return the workbench part associated with this container or
	 * <code>null</code> if there is no part or it is not available.
	 * @return the workbench part associated with this container or
	 * <code>null</code>
	 */
	public IWorkbenchPart getWorkbenchPart();
	
}
