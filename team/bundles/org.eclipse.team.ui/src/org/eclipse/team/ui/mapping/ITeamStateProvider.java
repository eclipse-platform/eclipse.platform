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

import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.ui.synchronize.SubscriberTeamStateProvider;
import org.eclipse.team.ui.synchronize.TeamStateProvider;

/**
 * A team state provider is used by the {@link SynchronizationStateTester}
 * to obtain the team state for model elements. A team
 * state provider is associated with a {@link RepositoryProviderType} using the
 * adaptable mechanism. A default decoration provider that uses the subscriber
 * of the type is provided.
 * <p>
 * This interface is not intended to be implemented by clients. Clients should
 * instead subclass {@link TeamStateProvider} or
 * {@link SubscriberTeamStateProvider}.
 *
 * @see IAdapterManager
 * @see RepositoryProviderType
 * @see RepositoryProviderType#getSubscriber()
 * @see TeamStateProvider
 * @see SubscriberTeamStateProvider
 * @see SynchronizationStateTester
 *
 * @since 3.2
 */
public interface ITeamStateProvider {

	/**
	 * A state mask that can be passed to the {@link #getStateDescription(Object, int, String[], IProgressMonitor)}
	 * method to indicate that only the decorated state flags are desired. It is equivalent to
	 * passing he mask returned from {@link #getDecoratedStateMask(Object)};
	 */
	public static final int USE_DECORATED_STATE_MASK = -1;

	/**
	 * Return whether decoration is enabled for the given model element. If
	 * decoration is not enabled, the model does not need to fire label change
	 * events when the team state of the element changes.
	 *
	 * @param element
	 *            the model element
	 * @return whether decoration is enabled for the given model element
	 */
	public boolean isDecorationEnabled(Object element);

	/**
	 * Return whether the given element has any decorated state.
	 *
	 * @param element
	 *            the element being decorated
	 * @return whether the given element has any decorated state
	 * @throws CoreException if an error occurs
	 */
	public boolean hasDecoratedState(Object element) throws CoreException;

	/**
	 * Return the mask that indicates what state the appropriate team decorator
	 * is capable of decorating. Clients can used this to obtain the current
	 * decorated state from
	 * {@link #getStateDescription(Object, int, String[], IProgressMonitor)} in
	 * order to determine if the decorated state has changed.
	 *
	 * <p>
	 * The state mask can consist of the following standard flags:
	 * <ul>
	 * <li>The diff kinds of {@link IDiff#ADD}, {@link IDiff#REMOVE} and
	 * {@link IDiff#CHANGE}.
	 * <li>The directions {@link IThreeWayDiff#INCOMING} and
	 * {@link IThreeWayDiff#OUTGOING}.
	 * </ul>
	 * For convenience sake, if there are no kind flags but there is at least
	 * one direction flag then all kinds are assumed.
	 * <p>
	 * The mask can also consist of flag bits that are unique to the repository
	 * provider associated with the resources that the element maps to.
	 *
	 * @param element
	 *            the model element to be decorated
	 * @return the mask that indicates what state the appropriate team decorator
	 *         will decorate
	 * @see IDiff
	 * @see IThreeWayDiff
	 */
	public int getDecoratedStateMask(Object element);

	/**
	 * Return the set of property identifiers that represent the set of
	 * properties that the team decorator would decorate for the given model
	 * element.
	 *
	 * @param element
	 *            the model element to be decorated
	 * @return the set of decorated properties
	 */
	public String[] getDecoratedProperties(Object element);

	/**
	 * Return the state description for the given element. A <code>null</code>
	 * is return if the element is not decorated or if decoration is disabled.
	 * Only the portion of the synchronization state covered by
	 * <code>stateMask</code> is returned. The <code>stateMask</code> should
	 * be {@link #USE_DECORATED_STATE_MASK} or the mask returned from
	 * {@link #getDecoratedStateMask(Object)} and the requested properties
	 * should be <code>null</code> or the value returned from
	 * {@link #getDecoratedProperties(Object)} if the client wishes to obtain
	 * the current decorated state.
	 *
	 * @param element
	 *            the model element
	 * @param stateMask
	 *            the mask that identifies which synchronization state flags are
	 *            desired if present
	 * @param properties
	 *            the set of properties that should be included in the result or
	 *            <code>null</code> if the decorated properties are desired
	 * @param monitor
	 *            a progress monitor
	 * @return the state for the given element or <code>null</code>
	 * @throws CoreException if an error occurs
	 */
	public ITeamStateDescription getStateDescription(Object element,
			int stateMask, String[] properties, IProgressMonitor monitor)
			throws CoreException;

	/**
	 * Return a resource mapping context that gives access to the remote state
	 * of the resources associated with the provider. If a
	 * {@link RemoteResourceMappingContext} is returned, then the client may
	 * access the remote state.
	 *
	 * @param element
	 *            the element for which remote contents are desired
	 *
	 * @return a resource mapping context that gives access to the remote state
	 *         of the resources associated with the provider
	 */
	public ResourceMappingContext getResourceMappingContext(Object element);

	/**
	 * Add a decorated state change listener to the provider.
	 * Adding the same listener more than once has no effect.
	 *
	 * @param listener
	 *            the listener
	 */
	public void addDecoratedStateChangeListener(
			ITeamStateChangeListener listener);

	/**
	 * Remove the decorated state change listener to the provider.
	 * Removing a listener that is not registered has no effect.
	 *
	 * @param listener
	 *            the listener
	 */
	public void removeDecoratedStateChangeListener(
			ITeamStateChangeListener listener);

}
