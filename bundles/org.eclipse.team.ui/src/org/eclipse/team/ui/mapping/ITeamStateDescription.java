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

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.ui.synchronize.TeamStateDescription;
import org.eclipse.team.ui.synchronize.TeamStateProvider;

/**
 * A description of the the state of a logical model element with respect to a
 * team repository.
 * <p>
 * This interface is not intended to be implemented by clients. Clients that wish
 * to create a description should use {@link TeamStateDescription}.
 *
 * @see TeamStateProvider
 * @since 3.2
 */
public interface ITeamStateDescription {

	/**
	 * Return the synchronization state flags for the element for which this
	 * state description was generated. Only the portion of the synchronization
	 * state covered by <code>stateMask</code> used when obtaining this
	 * description is returned.
	 *
	 * @return the synchronization state of the given element
	 * @see IDiff
	 * @see IThreeWayDiff
	 * @see IResourceDiff
	 */
	int getStateFlags();

	/**
	 * Return the portion of the state flags that represent the kind associated
	 * with the element for which this description was generated. See
	 * {@link IDiff#getKind()} for a description of what this value represents.
	 *
	 * @return the kind associated with the element for which this description
	 *         was generated
	 */
	int getKind();

	/**
	 * Return the portion of the state flags that represent the direction
	 * associated with the element for which this description was generated. See
	 * {@link IThreeWayDiff#getDirection()} for a description of what this value
	 * represents.
	 *
	 * @return the direction associated with the element for which this
	 *         description was generated
	 */
	int getDirection();

	/**
	 * Return the properties names for all decorated properties associated with
	 * the element for which this description was generated.
	 *
	 * @return the properties names for all decorated properties
	 */
	public abstract String[] getPropertyNames();

	/**
	 * Return the value associated with the given property. A <code>null</code>
	 * is returned if the property has no value.
	 * @param property the property
	 * @return the value associated with the given property or <code>null</code>
	 */
	public abstract Object getProperty(String property);

	/**
	 * Return whether this state description is equal the to given object.
	 * Clients should use this method to test whether two state
	 * descriptions are equal.
	 * @param object the object
	 * @return whether this state description is equal the to given object
	 */
	@Override
	public boolean equals(Object object);

}
