/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This class keeps track of the CVS repository locations that are known to
 * the CVS plugin.
 */
public class KnownRepositories implements INodeChangeListener, IPreferenceChangeListener {

	private List repositoryListeners = new ArrayList();
	private Map repositories;

	private static KnownRepositories instance;
	
	public static synchronized KnownRepositories getInstance() {
		if (instance == null) {
			instance = new KnownRepositories();
		}
		return instance;
	}
	
	/*
	 * Private class used to safely notify listeners of resource sync info changes. 
	 * Subclass override the notify(IResourceStateChangeListener) method to
	 * fire specific events inside an ISafeRunnable.
	 */
	private abstract class Notification implements ISafeRunnable {
		private ICVSListener listener;
		public void handleException(Throwable exception) {
			// don't log the exception....it is already being logged in Platform#run
		}
		public void run(ICVSListener listener) {
			this.listener = listener;
			SafeRunner.run(this);
		}
		public void run() throws Exception {
			notify(listener);
		}
		/**
		 * Subclasses override this method to send an event safely to a listener
		 * @param listener
		 */
		protected abstract void notify(ICVSListener listener);
	}
	
	/**
	 * Register to receive notification of repository creation and disposal
	 */
	public void addRepositoryListener(ICVSListener listener) {
		synchronized(repositoryListeners) {
			repositoryListeners.add(listener);
		}
	}
	
	/**
	 * De-register a listener
	 */
	public void removeRepositoryListener(ICVSListener listener) {
		synchronized(repositoryListeners) {
			repositoryListeners.remove(listener);
		}
	}

	/**
	 * Add the repository to the receiver's list of known repositories. Doing this will enable
	 * password caching across platform invocations.
	 */
	public ICVSRepositoryLocation addRepository(final ICVSRepositoryLocation repository, boolean broadcast) {
		CVSRepositoryLocation existingLocation;
		synchronized (this) {
			// Check the cache for an equivalent instance and if there is one, just update the cache
			existingLocation = internalGetRepository(repository.getLocation(false));
			if (existingLocation == null) {
				// Store the location
				store((CVSRepositoryLocation)repository);
				existingLocation = (CVSRepositoryLocation)repository;
			}
		}
		// Notify no matter what since it may not have been broadcast before
		if (broadcast) {
			final CVSRepositoryLocation location = existingLocation;
			((CVSRepositoryLocation)repository).updateCache();
			fireNotification(new Notification() {
				public void notify(ICVSListener listener) {
					listener.repositoryAdded(location);
				}
			});
		}
		return existingLocation;
	}
	
	/**
	 * Dispose of the repository location
	 * 
	 * Removes any cached information about the repository such as a remembered password.
	 */
	public void disposeRepository(final ICVSRepositoryLocation repository) {
		Object removed;
		synchronized (this) {
			((CVSRepositoryLocation)repository).dispose();
			removed = getRepositoriesMap().remove(repository.getLocation(false));
		}
		if (removed != null) {
			fireNotification(new Notification() {
				public void notify(ICVSListener listener) {
					listener.repositoryRemoved(repository);
				}
			});
		}
	}

	/**
	 * Answer whether the provided repository location is known by the provider or not.
	 * The location string corresponds to the String returned by ICVSRepositoryLocation#getLocation()
	 */
	public synchronized boolean isKnownRepository(String location) {
		return internalGetRepository(location) != null;
	}

	/** 
	 * Return a list of the know repository locations
	 */
	public synchronized ICVSRepositoryLocation[] getRepositories() {
		return (ICVSRepositoryLocation[])getRepositoriesMap().values().toArray(new ICVSRepositoryLocation[getRepositoriesMap().size()]);
	}
	
	/**
	 * Get the repository instance which matches the given String. The format of the String is
	 * the same as that returned by ICVSRepositoryLocation#getLocation().
	 * The format is:
	 * 
	 *   :connection:user[:password]@host[#port]:root
	 * 
	 * where [] indicates optional and the identifier meanings are:
	 * 
	 * 	 connection The connection method to be used
	 *   user The username for the connection
	 *   password The password used for the connection (optional)
	 *   host The host where the repository resides
	 *   port The port to connect to (optional)
	 *   root The server directory where the repository is located
	 * 
	 * If the repository is already registered, the cached instance is returned.
	 * Otherwise, a new uncached instance is returned.
	 * 
	 * WARNING: Providing the password as part of the String will result in the password being part
	 * of the location permanently. This means that it cannot be modified by the authenticator. 
	 */
	public synchronized ICVSRepositoryLocation getRepository(String location) throws CVSException {
		ICVSRepositoryLocation repository = internalGetRepository(location);
		if (repository == null) {
			repository = CVSRepositoryLocation.fromString(location);
		}
		return repository;
	}
	
	private CVSRepositoryLocation internalGetRepository(String location) {
		return (CVSRepositoryLocation)getRepositoriesMap().get(location);
	}
	
	/*
	 * Cache the location and store it in the preferences for persistence
	 */
	private void store(CVSRepositoryLocation location) {
		// Cache the location instance for later retrieval
		getRepositoriesMap().put(location.getLocation(), location);
		location.storePreferences();
	}
	
	private Map getRepositoriesMap() {
		if (repositories == null) {
			// Load the repositories from the preferences
			repositories = new HashMap();
			IEclipsePreferences prefs = (IEclipsePreferences) CVSRepositoryLocation.getParentPreferences();
			prefs.addNodeChangeListener(this);
			try {
				String[] keys = prefs.childrenNames();
				for (String key : keys) {
					try {
						IEclipsePreferences node = (IEclipsePreferences) prefs.node(key);
						node.addPreferenceChangeListener(this);
						String location = node.get(CVSRepositoryLocation.PREF_LOCATION, null);
						if (location != null) {
							repositories.put(location, CVSRepositoryLocation.fromString(location));
						} else {
							node.removeNode();
							prefs.flush();
						}
					} catch (CVSException e) {
						// Log and continue
						CVSProviderPlugin.log(e);
					}
				}
				if (repositories.isEmpty()) {
					getRepositoriesFromProjects();
				}
			} catch (BackingStoreException e) {
				// Log and continue (although all repos will be missing)
				CVSProviderPlugin.log(IStatus.ERROR, CVSMessages.KnownRepositories_0, e); 
			} catch (CVSException e) {
				CVSProviderPlugin.log(e);
			}
		}
		return repositories;
	}
	
	private void getRepositoriesFromProjects() throws CVSException {
		// If the file did not exist, then prime the list of repositories with
		// the providers with which the projects in the workspace are shared.
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			RepositoryProvider provider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
			if (provider!=null) {
				ICVSFolder folder = (ICVSFolder) CVSWorkspaceRoot.getCVSResourceFor(project);
				FolderSyncInfo info = folder.getFolderSyncInfo();
				if (info != null) {
					addRepository(getRepository(info.getRoot()), false);
				}
			}
		}
	}
	
	private ICVSListener[] getListeners() {
		synchronized(repositoryListeners) {
			return (ICVSListener[]) repositoryListeners.toArray(new ICVSListener[repositoryListeners.size()]);
		}
	}
	
	private void fireNotification(Notification notification) {
		// Get a snapshot of the listeners so the list doesn't change while we're firing
		ICVSListener[] listeners = getListeners();
		// Notify each listener in a safe manner (i.e. so their exceptions don't kill us)
		for (ICVSListener listener : listeners) {
			notification.run(listener);
		}
	}

	public void added(NodeChangeEvent event) {
		((IEclipsePreferences)event.getChild()).addPreferenceChangeListener(this);
	}

	public void removed(NodeChangeEvent event) {
		// Cannot remove the listener once the node is removed
		//((IEclipsePreferences)event.getChild()).removePreferenceChangeListener(this);
	}

	public void preferenceChange(PreferenceChangeEvent event) {
		if (CVSRepositoryLocation.PREF_LOCATION.equals(event.getKey())) {
			String location = (String)event.getNewValue();
			if (location == null) {
				((IEclipsePreferences)event.getNode()).removePreferenceChangeListener(this);
			} else {
				try {
					addRepository(CVSRepositoryLocation.fromString(location), true);
				} catch (CVSException e) {
					CVSProviderPlugin.log(e);
				}
			}
		}
	}
}
