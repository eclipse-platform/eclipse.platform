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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.ChangeSetManager;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * This abstract class provides API for accumulating the <code>SyncInfo</code>
 * from a seed <code>SyncInfoSet</code> into a set of <code>ChangeSet</code>
 * instances. It is used to provide the input to a synchronize page when
 * change sets are enabled.
 * <p>
 * This class does not register as a change listener with the seed set. It
 * is up to clients to invoke either the <code>reset</code> or <code>handleChange</code>
 * methods in response to seed set changes.
 * @since 3.1
 */
public abstract class SyncInfoSetChangeSetCollector extends ChangeSetManager {

	private final ISynchronizePageConfiguration configuration;
	private ChangeSetModelProvider provider;

	/*
	 * Listener that will remove sets when they become empty.
	 * The sets in this collector are only modified from either the
	 * UI thread or the provider's event handler thread so updates
	 * done by this listener will update the view properly.
	 */
	ISyncInfoSetChangeListener changeSetListener = new ISyncInfoSetChangeListener() {

		@Override
		public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
			handleChangeEvent(set);
		}

		@Override
		public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
			handleChangeEvent(event.getSet());
		}

		@Override
		public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
			// TODO Auto-generated method stub
		}

		/*
		 * The collector removes change sets once they are empty
		 */
		private void handleChangeEvent(SyncInfoSet set) {
			if (set.isEmpty()) {
				ChangeSet changeSet = getChangeSet(set);
				if (changeSet != null) {
					remove(changeSet);
				}
			}
		}
	};

	/**
	 * Create a collector that contains the sync info from the given seed set
	 * @param configuration the set used to determine which sync info
	 * should be included in the change sets.
	 */
	public SyncInfoSetChangeSetCollector(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Add the given resource sync info nodes to the appropriate
	 * change sets, adding them if necessary.
	 * This method is invoked by the <code>handleChanges</code>
	 * and <code>reset</code> methods
	 * when the model provider changes state. Updates done to the collector
	 * from within this thread will be thread-safe and update the view
	 * properly. Updates done from other threads should perform adds
	 * within a runnable passed to the
	 * <code>performUpdate</code> method to ensure the view is
	 * updated properly.
	 * <p>
	 * Subclasses must override this method.
	 * @param infos the sync infos to add
	 */
	protected abstract void add(SyncInfo[] infos);

	/**
	 * Remove the given resources from all sets of this collector.
	 * This method is invoked by the <code>handleChanges</code> method
	 * when the model provider changes state. It should not
	 * be invoked by other clients. The model provider
	 * will invoke this method from a particular thread (which may
	 * or may not be the UI thread).
	 * Updates done from other threads should perform removes
	 * within a runnable passed to the
	 * <code>performUpdate</code> method to ensure the view is
	 * updated properly.
	 * <p>
	 * Subclasses may override this method.
	 * @param resources the resources to be removed
	 */
	protected void remove(IResource[] resources) {
		ChangeSet[] sets = getSets();
		for (ChangeSet set : sets) {
			set.remove(resources);
		}
	}

	protected ISyncInfoSetChangeListener getChangeSetChangeListener() {
		return changeSetListener;
	}

	/**
	 * Re-populate the change sets from the seed set.
	 * If <code>null</code> is passed, clear any state
	 * but do not re-populate.
	 * <p>
	 * This method is invoked by the model provider when the
	 * model provider changes state. It should not
	 * be invoked by other clients. The model provider
	 * will invoke this method from a particular thread (which may
	 * or may not be the UI thread). Updates done to the collector
	 * from within this thread will be thread-safe and update the view
	 * properly. Updates done from other threads should use the
	 * <code>performUpdate</code> method to ensure the view is
	 * updated properly.
	 * <p>
	 * Subclasses may override this method.
	 * @param seedSet
	 */
	public void reset(SyncInfoSet seedSet) {
		// First, remove all the sets
		ChangeSet[] sets = getSets();
		for (ChangeSet set2 : sets) {
			remove(set2);
		}
		if (seedSet != null) {
			add(seedSet.getSyncInfos());
		}
	}

	/**
	 * This method is invoked by the model provider when the
	 * seed <code>SyncInfoSet</code> changes. It should not
	 * be invoked by other clients. The model provider
	 * will invoke this method from a particular thread (which may
	 * or may not be the UI thread). Updates done to the collector
	 * from within this thread will be thread-safe and update the view
	 * properly. Updates done from other threads should use the
	 * <code>performUpdate</code> method to ensure the view is
	 * updated properly.
	 * <p>
	 * Subclasses may override this method.
	 * @param event the set change event.
	 */
	public void handleChange(ISyncInfoSetChangeEvent event) {
		List<IResource> removals = new ArrayList<>();
		List<SyncInfo> additions = new ArrayList<>();
		removals.addAll(Arrays.asList(event.getRemovedResources()));
		additions.addAll(Arrays.asList(event.getAddedResources()));
		SyncInfo[] changed = event.getChangedResources();
		for (SyncInfo info : changed) {
			additions.add(info);
			removals.add(info.getLocal());
		}
		if (!removals.isEmpty()) {
			remove(removals.toArray(new IResource[removals.size()]));
		}
		if (!additions.isEmpty()) {
			add(additions.toArray(new SyncInfo[additions.size()]));
		}
	}

	/**
	 * Return the configuration for the page that is displaying the model created
	 * using this collector.
	 * @return the configuration for the page that is displaying the model created
	 * using this collector
	 */
	public final ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Execute the given runnable which updates the sync sets contained
	 * in this collector. This method should be used by subclasses when they
	 * are populating or modifying sets from another thread. In other words,
	 * if the sets of this collector are updated directly in the <code>add</code>
	 * method then this method is not required. However, if sets are created
	 * or modified by another thread, that thread must use this method to ensure
	 * the updates occur in the proper thread in order to ensure thread safety.
	 * <p>
	 * The update may be run in a different thread then the caller.
	 * However, regardless of which thread the update is run in, the view
	 * will be updated once the update is completed.
	 * @param runnable the workspace runnable that updates the sync sets.
	 * @param preserveExpansion whether the expansed items in the view should
	 * remain expanded after the update is performed.
	 * @param monitor a progress monitor
	 */
	protected final void performUpdate(IWorkspaceRunnable runnable, boolean preserveExpansion, IProgressMonitor monitor) {
		provider.performUpdate(runnable, preserveExpansion, false /* run in the handler thread and refresh at the end */);
	}

	/*
	 * Sets the provider for this collector. This method is for internal use only.
	 */
	public final void setProvider(ChangeSetModelProvider provider) {
		this.provider = provider;
	}

	/**
	 * This method should wait until any background processing is
	 * completed. It is for testing purposes. By default, it does not wait at all.
	 * Subclasses that perform work in the background should override.
	 * @param monitor a progress monitor
	 */
	public void waitUntilDone(IProgressMonitor monitor) {
		// Do nothing, by default
	}

	@Override
	protected void handleSetAdded(ChangeSet set) {
		((CheckedInChangeSet)set).getSyncInfoSet().addSyncSetChangedListener(getChangeSetChangeListener());
		super.handleSetAdded(set);
	}

	@Override
	protected void handleSetRemoved(ChangeSet set) {
		((CheckedInChangeSet)set).getSyncInfoSet().removeSyncSetChangedListener(getChangeSetChangeListener());
		super.handleSetRemoved(set);
	}

	/**
	 * Return the Change Set whose sync info set is the
	 * one given.
	 * @param set a sync info set
	 * @return the change set for the given sync info set
	 */
	protected ChangeSet getChangeSet(SyncInfoSet set) {
		ChangeSet[] sets = getSets();
		for (ChangeSet changeSet : sets) {
			if (((CheckedInChangeSet)changeSet).getSyncInfoSet() == set) {
				return changeSet;
			}
		}
		return null;
	}

	public SyncInfoTree getSyncInfoSet(ChangeSet set) {
		return ((CheckedInChangeSet)set).getSyncInfoSet();
	}
}
