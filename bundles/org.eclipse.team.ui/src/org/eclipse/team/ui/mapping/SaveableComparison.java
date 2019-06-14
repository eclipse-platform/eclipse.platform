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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.Saveable;

/**
 * A saveable comparison is used to buffer changes made when comparing
 * or merging model elements. A buffer can be shared between multiple
 * typed elements within a comparison. The saveable is used by the comparison
 * container in order to determine when a save is required.
 * <p>
 * Clients may subclass this class.
 *
 * @since 3.2
 */
public abstract class SaveableComparison extends Saveable {

	/**
	 * The property id for <code>isDirty</code>.
	 */
	public static final int PROP_DIRTY = IWorkbenchPartConstants.PROP_DIRTY;

	private boolean dirty;
	private ListenerList<IPropertyListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doSave(IProgressMonitor monitor) throws CoreException {
		if (!isDirty())
			return;
		performSave(monitor);
		setDirty(false);
	}

	/**
	 * Revert any changes in the buffer back to the last saved state.
	 * @param monitor a progress monitor on <code>null</code>
	 * if progress feedback is not required
	 */
	public void doRevert(IProgressMonitor monitor) {
		if (!isDirty())
			return;
		performRevert(monitor);
		setDirty(false);
	}

	/**
	 * Add a property change listener. Adding a listener
	 * that is already registered has no effect.
	 * @param listener the listener
	 */
	public void addPropertyListener(IPropertyListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a property change listener. Removing a listener
	 * that is not registered has no effect.
	 * @param listener the listener
	 */
	public void removePropertyListener(IPropertyListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Set the dirty state of this buffer. If the state
	 * has changed, a property change event will be fired.
	 * @param dirty the dirty state
	 */
	protected void setDirty(boolean dirty) {
		if (this.dirty == dirty) {
			return;
		}
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	/**
	 * Fire a property change event for this buffer.
	 * @param property the property that changed
	 */
	protected void firePropertyChange(final int property) {
		Object[] allListeners = listeners.getListeners();
		for (Object object : allListeners) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					((IPropertyListener)object).propertyChanged(SaveableComparison.this, property);
				}
				@Override
				public void handleException(Throwable exception) {
					// handled by platform
				}
			});
		}
	}

	/**
	 * Method invoked from {@link #doSave(IProgressMonitor)} to write
	 * out the buffer. By default, this method invokes <code>doSave</code>
	 * on the buffers saveable model.
	 * @param monitor a progress monitor
	 * @throws CoreException if errors occur
	 */
	protected abstract void performSave(IProgressMonitor monitor) throws CoreException;

	/**
	 * Method invoked from {@link #doRevert(IProgressMonitor)} to discard the
	 * changes in the buffer.
	 * @param monitor a progress monitor
	 */
	protected abstract void performRevert(IProgressMonitor monitor);
}
