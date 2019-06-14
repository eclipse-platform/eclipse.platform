/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener;
import org.eclipse.team.internal.ccvs.core.Policy;

/**
 * Class that manages the listeners of CVS sync change notification
 */
public class ResourceStateChangeListeners {

	private static List listeners = new ArrayList();
	
	private static ResourceStateChangeListeners instance;
	
	public static synchronized ResourceStateChangeListeners getListener() {
		if (instance == null) {
			instance = new ResourceStateChangeListeners();
		}
		return instance;
	}
	
	/*
	 * Private class used to safely notify listeners of resouce sync info changes. 
	 * Subclass override the notify(IResourceStateChangeListener) method to
	 * fire specific events inside an ISafeRunnable.
	 */
	private abstract class Notification implements ISafeRunnable {
		private IResourceStateChangeListener listener;
		public void handleException(Throwable exception) {
			// don't log the exception....it is already being logged in Platform#run
		}
		public void run(IResourceStateChangeListener listener) {
			this.listener = listener;
			Platform.run(this);
		}
		public void run() throws Exception {
			notify(listener);
		}
		/**
		 * Subsclasses overide this method to send an event safely to a lsistener
		 * @param listener
		 */
		protected abstract void notify(IResourceStateChangeListener listener);
	}
	
	private IResourceStateChangeListener[] getListeners() {
		synchronized(listeners) {
			return (IResourceStateChangeListener[]) listeners.toArray(new IResourceStateChangeListener[listeners.size()]);
		}
	}
	
	private void fireNotification(Notification notification) {
		// Get a snapshot of the listeners so the list doesn't change while we're firing
		IResourceStateChangeListener[] listeners = getListeners();
		// Notify each listener in a safe manner (i.e. so their exceptions don't kill us)
		for (IResourceStateChangeListener listener : listeners) {
			notification.run(listener);
		}
	}
	
	public void addResourceStateChangeListener(IResourceStateChangeListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}

	public void removeResourceStateChangeListener(IResourceStateChangeListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	
	public void resourceSyncInfoChanged(final IResource[] resources) {
		if (Policy.DEBUG_SYNC_CHANGE_EVENTS) {
			printDebugInfo("Sync info change event ", resources); //$NON-NLS-1$
		}
		fireNotification(new Notification() {
			public void notify(IResourceStateChangeListener listener) {
				listener.resourceSyncInfoChanged(resources);
			}
		});
	}
	
	public void externalSyncInfoChange(final IResource[] resources) {
		if (Policy.DEBUG_SYNC_CHANGE_EVENTS) {
			printDebugInfo("External sync info change event ", resources); //$NON-NLS-1$
		}
		fireNotification(new Notification() {
			public void notify(IResourceStateChangeListener listener) {
				listener.externalSyncInfoChange(resources);
			}
		});
	}
	
	public void resourceModified(final IResource[] resources) {
		if (Policy.DEBUG_SYNC_CHANGE_EVENTS) {
			printDebugInfo("Resource modified change event ", resources); //$NON-NLS-1$
		}
		fireNotification(new Notification() {
			public void notify(IResourceStateChangeListener listener) {
				listener.resourceModified(resources);
			}
		});
	}
	public void projectConfigured(final IProject project) {
		if (Policy.DEBUG_SYNC_CHANGE_EVENTS) {
			printDebugInfo("Project configured change event ", new IResource[] { project }); //$NON-NLS-1$
		}
		fireNotification(new Notification() {
			public void notify(IResourceStateChangeListener listener) {
				listener.projectConfigured(project);
			}
		});
	}
	public void projectDeconfigured(final IProject project) {
		if (Policy.DEBUG_SYNC_CHANGE_EVENTS) {
			printDebugInfo("Project deconfigured change event ", new IResource[] { project }); //$NON-NLS-1$
		}
		fireNotification(new Notification() {
			public void notify(IResourceStateChangeListener listener) {
				listener.projectDeconfigured(project);
			}
		});
	}
	
	private void printDebugInfo(String prefix, IResource[] resources) {
		System.out.print(prefix);
		System.out.print(" from thread  " + Thread.currentThread().getName()); //$NON-NLS-1$
		System.out.print(" for the following " + resources.length + " resources"); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(":"); //$NON-NLS-1$
		for (IResource resource : resources) {
			System.out.println(resource.getFullPath().toString());
		}
	}
	
}
