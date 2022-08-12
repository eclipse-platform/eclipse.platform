/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.IPropertyChangeListener;

/**
 * A synchronize scope defines the set of resources involved in a
 * synchronization. Instance of this interface are used to scope the resources
 * of a created {@link SubscriberParticipant}.
 *
 * @see SubscriberParticipant
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISynchronizeScope {

	/**
	 * Property used to indicate when the roots of the scope have changed.
	 */
	public static final String ROOTS = "prop_roots"; //$NON-NLS-1$

	/**
	 * Property used to indicate when the name of the scope has changed.
	 * @since 3.1
	 */
	public static final String NAME = "prop_name"; //$NON-NLS-1$

	/**
	 * Return the name of the scope
	 *
	 * @return the name of the scope
	 */
	public String getName();

	/**
	 * Return the root resources that define this scope. A return value
	 * of <code>null</code> indicates that the participant should use
	 * its default set of resources.
	 *
	 * @return the root resources or <code>null</code>
	 */
	public IResource[] getRoots();

	/**
	 * Add a property change listener that will get invoked when a
	 * property of the receiver changes.
	 *
	 * @param listener The listener to add.
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Remove a property change listener. Removing an unregistered listener
	 * has no effect.
	 *
	 * @param listener The listener to remove.
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Dispose of the scope when it is no longer needed.
	 */
	public void dispose();
}
