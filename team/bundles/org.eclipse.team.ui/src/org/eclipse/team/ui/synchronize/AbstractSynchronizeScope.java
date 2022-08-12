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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IMemento;

/**
 * Abstract superclass of resource scopes for <code>SubscriberParticipant</code>
 * instances.
 *
 * @see SubscriberParticipant
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class AbstractSynchronizeScope implements ISynchronizeScope {

	/*
	 * Key for scope in memento
	 */
	private static final String CTX_SUBSCRIBER_SCOPE_TYPE = TeamUIPlugin.ID + ".SCOPE_TYPE"; //$NON-NLS-1$

	/*
	 * Scope change listeners
	 */
	private ListenerList<IPropertyChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

	/**
	 * Save the scope to the given memento
	 *
	 * @param scope a scope
	 * @param settings a memento
	 */
	protected static void saveScope(ISynchronizeScope scope, IMemento settings) {
		settings.putString(CTX_SUBSCRIBER_SCOPE_TYPE, getType(scope));
		((AbstractSynchronizeScope)scope).saveState(settings);
	}

	/**
	 * Restore a scope from the given memento
	 *
	 * @param settings a memento
	 * @return the scope restored from the given memento
	 */
	protected static ISynchronizeScope createScope(IMemento settings) {
		String type = settings.getString(CTX_SUBSCRIBER_SCOPE_TYPE);
		if (type == null) {
			return new WorkspaceScope();
		}
		if (type.equals("ResourceScope")) { //$NON-NLS-1$
			return new ResourceScope(settings);
		}
		if (type.equals("WorkingSetScope")) { //$NON-NLS-1$
			return new WorkingSetScope(settings);
		}
		return new WorkspaceScope();
	}

	private static String getType(ISynchronizeScope scope) {
		String name = scope.getClass().getName();
		int lastDot = name.lastIndexOf("."); //$NON-NLS-1$
		if (lastDot == -1) {
			return name;
		}
		return name.substring(lastDot + 1);
	}

	/**
	 * Constructor a scope from scratch
	 */
	protected AbstractSynchronizeScope() {
	}

	/**
	 * Constructor a scope from a previously saved state
	 */
	protected AbstractSynchronizeScope(IMemento memento) {
		init(memento);
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		synchronized(listeners) {
			listeners.remove(listeners);
		}
	}

	@Override
	public void dispose() {
		// Do nothing by default
	}

	/**
	 * Fires the given property change event to all registered listeners.
	 *
	 * @param event the property change event to be fired
	 */
	protected void firePropertyChangedEvent(final PropertyChangeEvent event) {
		Object[] allListeners;
		synchronized(listeners) {
			allListeners = listeners.getListeners();
		}
		for (Object l : allListeners) {
			final IPropertyChangeListener listener = (IPropertyChangeListener) l;
			SafeRunner.run(new SafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.propertyChange(event);
				}
			});
		}
	}
	/**
	 * Fires a change event for property <code>ISynchronizeScope.ROOTS</code>
	 * containing the new roots. The old roots are not provided in the event.
	 */
	protected void fireRootsChanges() {
		firePropertyChangedEvent(new PropertyChangeEvent(this, ROOTS, new IResource[0], getRoots()));
	}

	/**
	 * Persist the state of this scope. Clients must persist enough additional
	 * state to know what type (i.e. subclass) of scope to be recreated.
	 *
	 * @param memento the memento into which the scope is to be saved
	 */
	public void saveState(IMemento memento) {
		// Do nothing by default
	}

	/**
	 * Method invoked from the constructor which populates the fields of this scope
	 *
	 * @param memento the memento into which the scope was previously saved
	 */
	protected void init(IMemento memento) {
		// Do nothing by default
	}

	/**
	 * Return whether the given resource is within this scope.
	 * By default, a resource is considered in the scope if
	 * it is a root or a descendant of a root.
	 * @param resource the resource
	 * @return whether the given resource is within this scope
	 * @since 3.2
	 */
	public boolean contains(IResource resource) {
		IResource[] roots = getRoots();
		IPath resourcePath = resource.getFullPath();
		for (IResource root : roots) {
			if (root.getFullPath().isPrefixOf(resourcePath)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the resource mappings that define this scope.
	 * By default, the mappings are just be deep traversals
	 * of the roots of the scope but subclasses may override.
	 * @return the resource mappings that define this scope
	 * @since 3.2
	 */
	public ResourceMapping[] getMappings() {
		List<ResourceMapping> result = new ArrayList<>();
		IResource[] roots = getRoots();
		for (IResource resource : roots) {
			result.add(resource.getAdapter(ResourceMapping.class));
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}
}
