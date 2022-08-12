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

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IDecoratorManager;

/**
 * A state change tester is used by logical models to communicate the
 * synchronization state of their logical model elements to
 * the lightweight label decorator of a team provider.
 * <p>
 * There are two different types of elements being decorated: those
 * that have a one-to-one mapping to a resource and those that do not.
 * Those that do should adapt to their corresponding resource. Doing
 * so will ensure that label updates occur when the state of that
 * resource changes (i.e. the team provider will generate label updates
 * for those resources and the model can translate them to appropriate
 * label updates of their model elements).
 * <p>
 * For those elements that do not have a one-to-one mapping to resources,
 * the model must do extra work. The purpose of this class is to allow
 * the model to decide when a label update for a logical model element is
 * required and to communicate the dirty state of their logical model
 * elements to the team decorator.
 * <p>
 * Model providers need to re-evaluate the state of a
 * model element whenever a change in the resources occurs by listening
 * to both resource deltas and change events from the team state provider
 * ({@link #getTeamStateProvider()}.
 * <p>
 * Decoration enablement changes and decoration configuration changes
 * are handled by the {@link IDecoratorManager#update(String)} API.
 * A call to this method will result in label changes to all elements.
 * The {@link #isDecorationEnabled(Object)} API on this class can
 * be used to determine if an element will receive team decorations.
 * If decoration is disabled. team state changes on the element can
 * be ignored.
 * <p>
 * Clients may subclass this class.
 *
 * @since 3.2
 * @see IWorkspace#addResourceChangeListener(IResourceChangeListener)
 * @see Subscriber#addListener(org.eclipse.team.core.subscribers.ISubscriberChangeListener)
 */
public class SynchronizationStateTester {

	/**
	 * Constant that is used as the property key on an
	 * {@link IDecorationContext}. Model based views can assign their state
	 * test to this property in the decoration context. If a context passed to a
	 * team decorator has this property, the associated state tester will be
	 * used by the decorator to determine the team state of the elements being
	 * decorated.
	 */
	public static final String PROP_TESTER = "org.eclipse.team.ui.syncStateTester"; //$NON-NLS-1$

	/**
	 * Create a synchronization state tester.
	 */
	public SynchronizationStateTester() {
		super();
	}

	/**
	 * Return whether state decoration is enabled for the context
	 * to which this tester is associated. If <code>true</code>
	 * is returned, a team decorator will use the state methods provided
	 * on this class to calculate the synchronization state of model
	 * elements for the purpose of decoration. If <code>false</code>
	 * is returned, a team decorator will not decorate the elements with any
	 * synchronization related decorations. Subclasses will want to disable
	 * state decoration if state decoration is being provided another way
	 * (e.g. by a {@link SynchronizationLabelProvider}). By default,
	 * <code>true</code>is returned. Subclasses may override.
	 * @return whether state decoration is enabled
	 */
	public boolean isStateDecorationEnabled() {
		return true;
	}

	/**
	 * Return whether decoration is enabled for the given model element in the
	 * context to which this tester is associated. By default, the value
	 * returned from {@link #isStateDecorationEnabled()} is used but subclasses
	 * may override to disable decoration of particular elements.
	 * <p>
	 * A team decorator should call this method before decorating a model
	 * element. If the method returns <code>true</code>, no team state
	 * decorations should be applied to the model element. Otherwise, the
	 * {@link #getState(Object, int, IProgressMonitor)} should be consulted in
	 * order to determine what state to decorate.
	 *
	 * @param element
	 *            the model element
	 * @return whether decoration is enabled for the given model element
	 */
	public boolean isDecorationEnabled(Object element) {
		return isStateDecorationEnabled();
	}

	/**
	 * Return the synchronization state of the given element. Only the portion
	 * of the synchronization state covered by <code>stateMask</code> is
	 * returned. By default, this method calls
	 * {@link Subscriber#getState(ResourceMapping, int, IProgressMonitor)}.
	 * <p>
	 * A team decorator will use this method to determine how to decorate the
	 * provided element. The {@link ITeamStateProvider#getDecoratedStateMask(Object)} returns the
	 * state that the corresponding team decorator is capable of decorating but
	 * the decorator may be configured to decorate only a portion of that state.
	 * When the team decorator invokes this method, it will pass the stateMask that
	 * it is currently configured to show. If a mask of zero is provided, this indicates
	 * that the team decorator is not configured to decorate the synchronization state
	 * of model elements.
	 * <p>
	 * Subclasses may want to override this method in the following cases:
	 * <ol>
	 * <li>The subclass wishes to fire appropriate label change events when the
	 * decorated state of a model element changes. In this case the subclass
	 * can override this method to record the stateMask and returned state. It can
	 * use this recorded information to determine whether local changes or subscriber changes
	 * result in a change in the decorated state of the model element.
	 * <li>The subclasses wishes to provide a more accurate change description for a model
	 * element that represents only a portion of the file. In this case, the subclass can
	 * use the remote file contents available from the provider to determine the proper
	 * state for the element.
	 * </ol>
	 *
	 * @param element the model element
	 * @param stateMask the mask that identifies which state flags are desired if
	 *            present
	 * @param monitor a progress monitor
	 * @return the synchronization state of the given element
	 * @throws CoreException if an error occurs
	 */
	public int getState(Object element, int stateMask, IProgressMonitor monitor) throws CoreException {
		ITeamStateDescription desc = getTeamStateProvider().getStateDescription(element, stateMask, new String[0], monitor);
		if (desc != null)
			return desc.getStateFlags();
		return IDiff.NO_CHANGE;
	}

	/**
	 * Return a team state provider that delegates to the appropriate team
	 * provider.
	 * @return a team state provider that delegates to the appropriate team
	 * provider
	 */
	public final ITeamStateProvider getTeamStateProvider() {
		return TeamUIPlugin.getPlugin().getDecoratedStateProvider();
	}

	/**
	 * A callback to the tester made from the team decorator to notify the
	 * tester that the given element has been decorated with the given state.
	 * The purpose of the callback is to allow the owner of the tester to
	 * cache the decorated state in order to detect whether a future state
	 * change requires a label update for the element.
	 * @param element the element that was decorated
	 * @param description a description of the decorated state of the element
	 */
	public void elementDecorated(Object element, ITeamStateDescription description) {
		// do nothing by default
	}


}
