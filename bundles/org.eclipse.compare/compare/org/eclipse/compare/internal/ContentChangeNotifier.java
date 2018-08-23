/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.swt.widgets.Display;

/**
 * A helper class for managing content change notification.
 */
public class ContentChangeNotifier implements IContentChangeNotifier {

	private ListenerList<IContentChangeListener> fListenerList;
	private final IContentChangeNotifier element;

	public ContentChangeNotifier(IContentChangeNotifier element) {
		this.element = element;
	}

	@Override
	public void addContentChangeListener(IContentChangeListener listener) {
		if (fListenerList == null)
			fListenerList= new ListenerList<>();
		fListenerList.add(listener);
	}

	@Override
	public void removeContentChangeListener(IContentChangeListener listener) {
		if (fListenerList != null) {
			fListenerList.remove(listener);
			if (fListenerList.isEmpty())
				fListenerList= null;
		}
	}

	/**
	 * Notifies all registered <code>IContentChangeListener</code>s of a content change.
	 */
	public void fireContentChanged() {
		if (isEmpty()) {
			return;
		}
		// Legacy listeners may expect to be notified in the UI thread.
		Runnable runnable = () -> {
			for (final IContentChangeListener contentChangeListener : fListenerList) {
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						contentChangeListener.contentChanged(element);
					}
					@Override
					public void handleException(Throwable exception) {
						// Logged by safe runner
					}
				});
			}
		};
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(runnable);
		} else {
			runnable.run();
		}
	}

	/**
	 * Return whether this notifier is empty (i.e. has no listeners).
	 * @return whether this notifier is empty
	 */
	public boolean isEmpty() {
		return fListenerList == null || fListenerList.isEmpty();
	}

}
