/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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

package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Adapter for the platform's retargettable "add memory rendering" action.
 * Clients implementing this adapter are expected to add the necessary memory
 * blocks and renderings when the adapter is invoked.
 * <p>
 * Typically, to add a memory rendering, client needs to do the following:
 * </p>
 * <ol>
 * <li>Create a new memory block</li>
 * <li>Add the new memory block to the Memory Block Manager.
 * (<code>IMemoryBlockManager</code>)</li>
 * <li>Create the new rendering from
 * <code>IMemoryRenderingTypeDelegate</code></li>
 * <li>Bring the required memory view to the top.
 * (<code>IMemoryRenderingSite</code>)</li>
 * <li>Find the container from the memory view to host the new memory rendering.
 * (<code>IMemoryRenderingContainer</code>)</li>
 * <li>Initialize the new rendering with the appropriate memory block and
 * container.</li>
 * <li>Add the new rendering to the container.</li>
 * </ol>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @since 3.2
 * @see AddMemoryRenderingActionDelegate
 */
public interface IAddMemoryRenderingsTarget {
	/**
	 * Returns whether a memory rendering can be added from the specified
	 * part, based on the the given selection, which is the active debug context
	 * in the current workbench window.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection the active debug context in the active workbench window
	 * @return <code>true</code> if a memory rendering can be added from the specified
	 * part with the given selection, <code>false</code> otherwise
	 */
	boolean canAddMemoryRenderings(IWorkbenchPart part, ISelection selection);

	/**
	 * Adds memory renderings. Based on the part and selection (active debug context), this
	 * adapter does the following:
	 * <ol>
	 * <li>creates and adds the required memory block to the memory block manager</li>
	 * <li>creates the specified renderings and add the them
	 *   to the appropriate memory rendering containers</li>
	 * </ol>
	 * @param part the part on which the action has been invoked
	 * @param selection the active debug context
	 * @param renderingTypes renderings to add
	 * @throws CoreException if unable to perform the action
	 *
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval
	 * @see org.eclipse.debug.ui.memory.IMemoryRenderingManager
	 * @see org.eclipse.debug.core.IMemoryBlockManager
	 * @see org.eclipse.debug.ui.memory.IMemoryRenderingSite
	 * @see org.eclipse.debug.ui.memory.IMemoryRenderingContainer
	 */
	void addMemoryRenderings(IWorkbenchPart part, ISelection selection, IMemoryRenderingType[] renderingTypes) throws CoreException;

	/**
	 * Returns a list of rendering types that can be added from the given workbench part and active
	 * debug context, possibly empty.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection the active debug context
	 * @return a list of rendering types that can be added, possibly empty
	 */
	IMemoryRenderingType[] getMemoryRenderingTypes(IWorkbenchPart part, ISelection selection);
}
