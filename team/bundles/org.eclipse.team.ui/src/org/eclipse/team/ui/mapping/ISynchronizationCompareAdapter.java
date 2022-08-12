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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.ui.IMemento;

/**
 * The compare adapter provides compare support for the model objects
 * associated with a model provider.
 * <p>
 * Clients should not implement this interface but should subclass {@link SynchronizationCompareAdapter}
 * instead.
 *
 * @since 3.2
 */
public interface ISynchronizationCompareAdapter {

	/**
	 * Return whether their is a compare input associated with the given object.
	 * In other words, return <code>true</code> if {@link #asCompareInput(ISynchronizationContext, Object) }
	 * would return a value and <code>false</code> if it would return <code>null</code>.
	 * @param context the synchronization context
	 * @param object the object.
	 * @return whether their is a compare input associated with the given object
	 */
	boolean hasCompareInput(ISynchronizationContext context, Object object);

	/**
	 * Return a compare input for the given model object. Creation of the input
	 * should be fast. Synchronization information calculations that are longer
	 * running should be performed in the
	 * {@link ISynchronizationCompareInput#prepareInput(org.eclipse.compare.CompareConfiguration, IProgressMonitor)}
	 * method. Clients should call this method once per context before obtaining
	 * any compare inputs from the adapter. A <code>null</code> should be
	 * returned if the model object is in-sync or otherwise cannot be compared.
	 * <p>
	 * Model providers can choose to return an instance of {@link ISynchronizationCompareInput}
	 * if they wish to tailor the compare editor used to show the compare input
	 * of provide an {@link SaveableComparison} in order to have more control
	 * over the save lifecycle during a merge.
	 *
	 * @param context the synchronization context
	 * @param o the model object
	 * @return a compare input or <code>null</code> if the model object is
	 *         in-sync or otherwise cannot be compared.
	 */
	ICompareInput asCompareInput(ISynchronizationContext context, Object o);

	/**
	 * Get the name associated with the model object of the given mapping.
	 * This name should be suitable for display to the user.
	 * @param mapping the mapping
	 * @return the name of the mapping's model object
	 */
	public String getName(ResourceMapping mapping);

	/**
	 * Get the path associated with the model object
	 * of the given mapping.
	 * This path should be suitable for display to the user.
	 * @param mapping the mapping
	 * @return the path of the model object of the mapping
	 */
	public String getPathString(ResourceMapping mapping);

	/**
	 * Return an image descriptor that can be displayed as an icon for the
	 * model object of the given resource mapping.
	 * @param mapping the mapping
	 * @return an image descriptor that can be displayed as an icon for the
	 * model object of the given resource mapping
	 * @since 3.3
	 */
	public ImageDescriptor getImageDescriptor(ResourceMapping mapping);

	/**
	 * Save the given resource mappings from this adapters
	 * model provider into the given memento in a form
	 * that can be restored at a future time.
	 * @param mappings the resource mappings to save
	 * @param memento the memento where the mappings should be saved
	 */
	public void save(ResourceMapping[] mappings, IMemento memento);

	/**
	 * Restore the previously saved resource mappings.
	 * @param memento a memento
	 * @return the mappings restored from the given memento
	 */
	public ResourceMapping[] restore(IMemento memento);

	/**
	 * Return the synchronization state of the resource mapping with respect to
	 * the given team state provider. This method is invoked from instances of
	 * {@link ITeamStateProvider} when the synchronization state description for
	 * an element is requested. This method can return -1 to indicate that the state
	 * calculation should be performed using the resources that the element maps to.
	 *
	 * @param provider
	 *            the team state provider
	 * @param mapping
	 *            the element
	 * @param stateMask
	 *            the state mask that indicates which state flags are desired
	 * @param monitor
	 *            a progress monitor
	 * @return the synchronization state of the element or -1 if the calculation
	 *         of the state should be done using the resources of the mapping.
	 * @throws CoreException on failures; depends on concrete implementation
	 *
	 * @since 3.3
	 */
	public int getSynchronizationState(ITeamStateProvider provider, ResourceMapping mapping, int stateMask, IProgressMonitor monitor) throws CoreException;

}
