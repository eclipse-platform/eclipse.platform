/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.team.core.RepositoryProvider;

public class RepositoryProviderManager implements IRepositoryProviderListener {

	private static RepositoryProviderManager instance;
	private ListenerList listeners = new ListenerList();

	public static synchronized RepositoryProviderManager getInstance() {
		if (instance == null) {
			instance = new RepositoryProviderManager();
		}
		return instance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.IRepositoryProviderListener#providerMapped(org.eclipse.team.core.RepositoryProvider)
	 */
	public void providerMapped(RepositoryProvider provider) {
		Object[] allListeners = listeners.getListeners();
		for (int i = 0; i < allListeners.length; i++) {
			IRepositoryProviderListener listener = (IRepositoryProviderListener)allListeners[i];
			listener.providerMapped(provider);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.IRepositoryProviderListener#providerUnmapped(org.eclipse.core.resources.IProject)
	 */
	public void providerUnmapped(IProject project) {
		Object[] allListeners = listeners.getListeners();
		for (int i = 0; i < allListeners.length; i++) {
			IRepositoryProviderListener listener = (IRepositoryProviderListener)allListeners[i];
			listener.providerUnmapped(project);
		}
	}

	public void addListener(IRepositoryProviderListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IRepositoryProviderListener listener) {
		listeners.remove(listener);
	}


}
