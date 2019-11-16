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
package org.eclipse.team.internal.ui;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Helper class for implementing property change handling
 */
public class PropertyChangeHandler {
	private ListenerList<IPropertyChangeListener> fListeners = new ListenerList<>(ListenerList.IDENTITY);

	/**
	 * Notifies listeners of property changes, handling any exceptions
	 */
	class PropertyNotifier implements ISafeRunnable {

		private IPropertyChangeListener fListener;
		private PropertyChangeEvent fEvent;

		@Override
		public void handleException(Throwable exception) {
			TeamUIPlugin.log(IStatus.ERROR, TeamUIMessages.AbstractSynchronizeParticipant_5, exception);
		}

		@Override
		public void run() throws Exception {
			fListener.propertyChange(fEvent);
		}

		/**
		 * Notifies listeners of the property change
		 *
		 * @param event
		 *            the property change event
		 */
		public void notify(PropertyChangeEvent event) {
			if (fListeners == null) {
				return;
			}
			fEvent = event;
			Object[] copiedListeners = fListeners.getListeners();
			for (Object copiedListener : copiedListeners) {
				fListener = (IPropertyChangeListener) copiedListener;
				SafeRunner.run(this);
			}
			fListener = null;
		}
	}

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.add(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
	}

	public void firePropertyChange(Object source, String property, Object oldValue, Object newValue) {
		PropertyNotifier notifier = new PropertyNotifier();
		notifier.notify(new PropertyChangeEvent(source, property, oldValue, newValue));
	}
}
