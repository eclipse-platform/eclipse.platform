/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A model element that can be shown in viewers.
 *
 * @since 3.0
 */
public abstract class SynchronizeModelElement extends DiffNode implements IAdaptable, ISynchronizeModelElement {

	/*
	 * Internal flags bits for storing properties in the flags variable
	 */
	private static final int BUSY_FLAG = 0x01;
	private static final int PROPAGATED_CONFLICT_FLAG = 0x02;
	private static final int PROPAGATED_ERROR_FLAG = 0x04;
	private static final int PROPAGATED_WARNING_FLAG =0x08;

	// Instance variable containing the flags for this node
	private int flags;
	private ListenerList listeners;

	// Parent is required to ensure that busy (and other) state is cleared.
	// This is needed as DiffContainer#remove() will null the parent
	private SynchronizeModelElement parent;

	public SynchronizeModelElement(IDiffContainer parent) {
		super(parent, SyncInfo.IN_SYNC);
		internalSetParent(parent);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public synchronized void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (listeners == null) {
			listeners = new ListenerList(ListenerList.IDENTITY);
		}
		listeners.add(listener);
	}

	@Override
	public synchronized void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.isEmpty()) {
				listeners = null;
			}
		}
	}

	@Override
	public void setParent(IDiffContainer parent) {
		super.setParent(parent);
		internalSetParent(parent);
	}

	/**
	 * Return whether this node has the given property set.
	 * @param propertyName the flag to test
	 * @return <code>true</code> if the property is set
	 */
	@Override
	public boolean getProperty(String propertyName) {
		return (getFlags() & getFlag(propertyName)) > 0;
	}

	/**
	 * Add the flag to the flags for this node
	 * @param propertyName the flag to add
	 */
	@Override
	public void setProperty(String propertyName, boolean value) {
		if (value) {
			if (!getProperty(propertyName)) {
				int flag = getFlag(propertyName);
				flags |= flag;
				firePropertyChange(propertyName);
			}
		} else {
			if (getProperty(propertyName)) {
				int flag = getFlag(propertyName);
				flags ^= flag;
				firePropertyChange(propertyName);
			}
		}
	}

	@Override
	public void setPropertyToRoot(String propertyName, boolean value) {
		if (value) {
			addToRoot(propertyName);
		} else {
			removeToRoot(propertyName);
		}
	}

	public void fireChanges() {
		fireChange();
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		IResource resource = getResource();
		if(resource != null) {
			IWorkbenchAdapter adapter = ((IAdaptable) resource).getAdapter(IWorkbenchAdapter.class);
			return adapter.getImageDescriptor(resource);
		}
		return null;
	}

	@Override
	public abstract IResource getResource();

	private void addToRoot(String flag) {
		setProperty(flag, true);
		if (parent != null) {
			if (parent.getProperty(flag)) return;
			parent.addToRoot(flag);
		}
	}

	private void firePropertyChange(String propertyName) {
		Object[] allListeners;
		synchronized(this) {
			if (listeners == null) return;
			allListeners = listeners.getListeners();
		}
		boolean set = getProperty(propertyName);
		final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, Boolean.valueOf(!set), Boolean.valueOf(set));
		for (Object object : allListeners) {
			if (object instanceof IPropertyChangeListener) {
				final IPropertyChangeListener listener = (IPropertyChangeListener)object;
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void handleException(Throwable exception) {
						// Exceptions logged by the platform
					}
					@Override
					public void run() throws Exception {
						listener.propertyChange(event);
					}
				});
			}
		}
	}

	private int getFlag(String propertyName) {
		if (propertyName == null) {
			return 0;
		}

		switch (propertyName) {
		case BUSY_PROPERTY:
			return BUSY_FLAG;
		case PROPAGATED_CONFLICT_PROPERTY:
			return PROPAGATED_CONFLICT_FLAG;
		case PROPAGATED_ERROR_MARKER_PROPERTY:
			return PROPAGATED_ERROR_FLAG;
		case PROPAGATED_WARNING_MARKER_PROPERTY:
			return PROPAGATED_WARNING_FLAG;
		default:
			break;
		}
		return 0;
	}

	private int getFlags() {
		return flags;
	}

	private boolean hasChildWithFlag(String flag) {
		IDiffElement[] childen = getChildren();
		for (IDiffElement element : childen) {
			if (((SynchronizeModelElement)element).getProperty(flag)) {
				return true;
			}
		}
		return false;
	}

	private void removeToRoot(String flag) {
		boolean hasProperty = getProperty(flag);
		if(hasProperty) {
			setProperty(flag, false);
			if (parent != null) {
				// If the parent doesn't have the tag, no recalculation is required
				// Also, if the parent still has a child with the tag, no recalculation is needed
				if (parent.getProperty(flag) && !parent.hasChildWithFlag(flag)) {
					// The parent no longer has the flag so propogate the recalculation
					parent.removeToRoot(flag);
				}
			}
		}
	}

	private void internalSetParent(IDiffContainer parent) {
		if (parent != null && parent instanceof SynchronizeModelElement) {
			this.parent = (SynchronizeModelElement)parent;
		}
	}

	/**
	 * Synchronize model elements are not copied so use identity as the
	 * equality check.
	 * @param object The object to test
	 * @return true if the objects are identical
	 */
	@Override
	public boolean equals(Object object) {
		return this==object;
	}

	@Override
	public int hashCode() {
		// Use the name to get the hashCode to ensure that we can find equal elements.
		// (The inherited hashCode uses the path which can change when items are removed)
		return getName().hashCode();
	}
}
