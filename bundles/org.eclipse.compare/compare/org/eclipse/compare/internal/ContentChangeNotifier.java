/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	private ListenerList fListenerList;
	private final IContentChangeNotifier element;
	
	public ContentChangeNotifier(IContentChangeNotifier element) {
		this.element = element;
	}

	/* (non-Javadoc)
	 * see IContentChangeNotifier.addChangeListener
	 */
	public void addContentChangeListener(IContentChangeListener listener) {
		if (fListenerList == null)
			fListenerList= new ListenerList();
		fListenerList.add(listener);
	}
	
	/* (non-Javadoc)
	 * see IContentChangeNotifier.removeChangeListener
	 */
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
		Runnable runnable = new Runnable() {
			public void run() {
				Object[] listeners= fListenerList.getListeners();
				for (int i= 0; i < listeners.length; i++) {
					final IContentChangeListener contentChangeListener = (IContentChangeListener)listeners[i];
					SafeRunner.run(new ISafeRunnable() {
						public void run() throws Exception {
							contentChangeListener.contentChanged(element);
						}
						public void handleException(Throwable exception) {
							// Logged by safe runner
						}
					});
				}
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
