/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;

/**
 * This class represents provisional API. A provider is not required to
 * implement this API. Implementers, and those who reference it, do so with the
 * awareness that this class may be removed or substantially changed at future
 * times without warning.
 */
public interface ISynchronizeModelProvider {

	/**
	 * Property constant used to indicate that the viewer sorter has changed.
	 * Property change notifications for the viewer sorter change do not include
	 * the old and new viewer sorter. Instead, clients should re-obtain the sorter
	 * from the provider.
	 */
	public static final String P_VIEWER_SORTER = TeamUIPlugin.ID  + ".P_VIEWER_SORTER"; //$NON-NLS-1$

	/**
	 * Returns the sync set this model provider is showing.
	 * @return the sync set this model provider is showing.
	 */
	public abstract SyncInfoSet getSyncInfoSet();

	/**
	 * Returns the description for this model provider.
	 * @return the description for this model provider.
	 */
	public ISynchronizeModelProviderDescriptor getDescriptor();

	/**
	 * Return the <code>AbstractTreeViewer</code> associated with this content
	 * provider or <code>null</code> if the viewer is not of the proper type.
	 * @return the viewer
	 */
	public abstract StructuredViewer getViewer();

	/**
	 * Builds the viewer model based on the contents of the sync set.
	 * @param monitor the progress monitor
	 * @return the root element of the generated model.
	 */
	public abstract ISynchronizeModelElement prepareInput(IProgressMonitor monitor);

	/**
	 * Dispose of the builder
	 */
	public abstract void dispose();

	/**
	 * Returns the input created by this controller or <code>null</code> if
	 * {@link #prepareInput(IProgressMonitor)} hasn't been called on this object yet.
	 * @return the model element
	 */
	public abstract ISynchronizeModelElement getModelRoot();

	/**
	 * Returns the comparator for this model.
	 * @return the comparator for this model.
	 */
	public abstract ViewerComparator getViewerComparator();

	/**
	 * Allows the provider to save state. Is usually called before provider is disposed and it
	 * is safe to access the viewer.
	 */
	public abstract void saveState();

	/**
	 * Register a property change listener with this provider.
	 * @param listener the property change listener
	 */
	public abstract void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Remove a property change listener from this provider.
	 * @param listener the property change listener
	 */
	public abstract void removePropertyChangeListener(IPropertyChangeListener listener);
}
