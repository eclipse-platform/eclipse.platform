/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This interface defines extensions to the {@link ICompareInput}
 * interface that are used for compare inputs that are returned from the
 * {@link ISynchronizationCompareAdapter#asCompareInput(org.eclipse.team.core.mapping.ISynchronizationContext, Object)}
 * method.
 * <p>
 * Clients may implement this interface.
 *
 * @see ISynchronizationCompareAdapter#asCompareInput(org.eclipse.team.core.mapping.ISynchronizationContext, Object)
 *
 * @since 3.2
 */
public interface ISynchronizationCompareInput extends ICompareInput {

	/**
	 * Return the saveable for this compare input or
	 * <code>null</code> if the input is saved directly to disk when the
	 * compare editor input is changed or the compare editor is closed.
	 * @return the saveable for this compare input
	 */
	SaveableComparison getSaveable();

	/**
	 * Prepare the compare input associated with a model element for display using
	 * the compare configuration.
	 * @param configuration the compare configuration for the editor that will display the input
	 * @param monitor a progress monitor
	 * @throws CoreException if an error occurs
	 */
	void prepareInput(CompareConfiguration configuration, IProgressMonitor monitor) throws CoreException;

	/**
	 * Return a human readable path for the compare input that can be
	 * used in a tooltip or other displays that can show more detailed information.
	 * @return a human readable path for the compare input
	 */
	String getFullPath();

	/**
	 * Return whether this compare input matches the given object for the
	 * purpose of change navigation.
	 * @param object the object
	 * @return whether
	 */
	boolean isCompareInputFor(Object object);

}
