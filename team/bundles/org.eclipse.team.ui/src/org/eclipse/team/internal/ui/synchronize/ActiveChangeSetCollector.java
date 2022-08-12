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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffChangeListener;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;
import org.eclipse.team.internal.core.subscribers.IChangeSetChangeListener;
import org.eclipse.team.internal.core.subscribers.SubscriberChangeSetManager;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;

/**
 * Group incoming changes according to the active change set that are
 * located in
 */
public class ActiveChangeSetCollector implements IDiffChangeListener {

	private final ISynchronizePageConfiguration configuration;

	/*
	 * Map active change sets to infos displayed by the participant
	 */
	private final Map<ChangeSet, SyncInfoSet> activeSets = new HashMap<>();

	/*
	 * Set which contains those changes that are not part of an active set
	 */
	private SyncInfoTree rootSet = new SyncInfoTree();

	private final ChangeSetModelProvider provider;

	/*
	 * Listener registered with active change set manager
	 */
	private IChangeSetChangeListener activeChangeSetListener = new IChangeSetChangeListener() {

		@Override
		public void setAdded(final ChangeSet set) {
			// Remove any resources that are in the new set
			provider.performUpdate(monitor -> {
				remove(set.getResources());
				createSyncInfoSet(set);
			}, true, true);
		}

		@Override
		public void defaultSetChanged(final ChangeSet previousDefault, final ChangeSet set) {
			provider.performUpdate(monitor -> {
				if (listener != null)
					listener.defaultSetChanged(previousDefault, set);
			}, true, true);
		}

		@Override
		public void setRemoved(final ChangeSet set) {
			provider.performUpdate(monitor -> {
				remove(set);
				if (!set.isEmpty()) {
					add(getSyncInfos(set).getSyncInfos());
				}
			}, true, true);
		}

		@Override
		public void nameChanged(final ChangeSet set) {
			provider.performUpdate(monitor -> {
				if (listener != null)
					listener.nameChanged(set);
			}, true, true);
		}

		@Override
		public void resourcesChanged(final ChangeSet set, final IPath[] paths) {
			// Look for any resources that were removed from the set but are still out-of sync.
			// Re-add those resources
			final List<SyncInfo> outOfSync = new ArrayList<>();
			for (IPath path : paths) {
				if (!((DiffChangeSet)set).contains(path)) {
					SyncInfo info = getSyncInfo(path);
					if (info != null && info.getKind() != SyncInfo.IN_SYNC) {
						outOfSync.add(info);
					}
				}
			}
			if (!outOfSync.isEmpty()) {
				provider.performUpdate(monitor -> add(outOfSync.toArray(new SyncInfo[outOfSync.size()])), true, true);
			}
		}
	};

	/**
	 * Listener that wants to receive change events from this collector
	 */
	private IChangeSetChangeListener listener;

	public ActiveChangeSetCollector(ISynchronizePageConfiguration configuration, ChangeSetModelProvider provider) {
		this.configuration = configuration;
		this.provider = provider;
		getActiveChangeSetManager().addListener(activeChangeSetListener);
	}

	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	public ActiveChangeSetManager getActiveChangeSetManager() {
		ISynchronizeParticipant participant = getConfiguration().getParticipant();
		if (participant instanceof IChangeSetProvider) {
			return ((IChangeSetProvider)participant).getChangeSetCapability().getActiveChangeSetManager();
		}
		return null;
	}

	/**
	 * Re-populate the change sets from the seed set.
	 * If <code>null</code> is passed, the state
	 * of the collector is cleared but the set is not
	 * re-populated.
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
	 * @param seedSet
	 */
	public void reset(SyncInfoSet seedSet) {
		// First, clean up
		rootSet.clear();
		ChangeSet[] sets = activeSets.keySet().toArray(new ChangeSet[activeSets.size()]);
		for (ChangeSet set : sets) {
			remove(set);
		}
		activeSets.clear();

		// Now re-populate
		if (seedSet != null) {
			if (getConfiguration().getComparisonType() == ISynchronizePageConfiguration.THREE_WAY) {
				// Show all active change sets even if they are empty
				sets = getActiveChangeSetManager().getSets();
				for (ChangeSet set : sets) {
					add(set);
				}
				// The above will add all sync info that are contained in sets.
				// We still need to add uncontained infos to the root set
				SyncInfo[] syncInfos = seedSet.getSyncInfos();
				for (SyncInfo info : syncInfos) {
					if (isLocalChange(info)) {
						ChangeSet[] containingSets = findChangeSets(info);
						if (containingSets.length == 0) {
							rootSet.add(info);
						}
					}
				}
			} else {
				add(seedSet.getSyncInfos());
			}
		}
	}

	/**
	 * Handle a sync info set change event from the provider's
	 * seed set.
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
	 * Remove the given resources from all sets of this collector.
	 * @param resources the resources to be removed
	 */
	protected void remove(IResource[] resources) {
		for (Object element : activeSets.values()) {
			SyncInfoSet set = (SyncInfoSet) element;
			set.removeAll(resources);
		}
		rootSet.removeAll(resources);
	}

	protected void add(SyncInfo[] infos) {
		rootSet.beginInput();
		for (SyncInfo info : infos) {
			if (isLocalChange(info) && select(info)) {
				ChangeSet[] sets = findChangeSets(info);
				if (sets.length == 0) {
					rootSet.add(info);
				} else {
					for (ChangeSet set : sets) {
						SyncInfoSet targetSet = getSyncInfoSet(set);
						if (targetSet == null) {
							// This will add all the appropriate sync info to the set
							createSyncInfoSet(set);
						} else {
							targetSet.add(info);
						}
					}
				}
			}
		}
		rootSet.endInput(null);
	}

	private ChangeSet[] findChangeSets(SyncInfo info) {
		ActiveChangeSetManager manager = getActiveChangeSetManager();
		ChangeSet[] sets = manager.getSets();
		List<ChangeSet> result = new ArrayList<>();
		for (ChangeSet set : sets) {
			if (set.contains(info.getLocal())) {
				result.add(set);
			}
		}
		return result.toArray(new ChangeSet[result.size()]);
	}

	/*
	 * Return if this sync info is an outgoing change.
	 */
	private boolean isLocalChange(SyncInfo info) {
		if (!info.getComparator().isThreeWay()) {
			try {
				// Obtain the sync info from the subscriber and use it to see if the change is local
				info = ((SubscriberChangeSetManager)getActiveChangeSetManager()).getSubscriber().getSyncInfo(info.getLocal());
			} catch (TeamException e) {
				TeamUIPlugin.log(e);
			}
		}
		return (info.getComparator().isThreeWay()
				&& ((info.getKind() & SyncInfo.DIRECTION_MASK) == SyncInfo.OUTGOING ||
						(info.getKind() & SyncInfo.DIRECTION_MASK) == SyncInfo.CONFLICTING));
	}

	public SyncInfoTree getRootSet() {
		return rootSet;
	}

	/*
	 * Add the set from the collector.
	 */
	public void add(ChangeSet set) {
		SyncInfoSet targetSet = getSyncInfoSet(set);
		if (targetSet == null) {
			createSyncInfoSet(set);
		}
		if (listener != null) {
			listener.setAdded(set);
		}
	}

	private SyncInfoTree createSyncInfoSet(ChangeSet set) {
		SyncInfoTree sis = getSyncInfoSet(set);
		// Register the listener last since the add will
		// look for new elements
		boolean added = false;
		// Use a variable to ensure that both begin and end are invoked
		try {
			if (sis == null) {
				sis = new SyncInfoTree();
				activeSets.put(set, sis);
				added = true;
			}
			sis.beginInput();
			if (!sis.isEmpty())
				sis.removeAll(sis.getResources());
			sis.addAll(getSyncInfos(set));
		} finally {
			if (sis != null)
				sis.endInput(null);
		}
		if (added) {
			((DiffChangeSet)set).getDiffTree().addDiffChangeListener(this);
			if (listener != null)
				listener.setAdded(set);
		}
		return sis;
	}

	private SyncInfoSet getSyncInfos(ChangeSet set) {
		IDiff[] diffs = ((ResourceDiffTree)((DiffChangeSet)set).getDiffTree()).getDiffs();
		return asSyncInfoSet(diffs);
	}

	private SyncInfoSet asSyncInfoSet(IDiff[] diffs) {
		SyncInfoSet result = new SyncInfoSet();
		for (IDiff diff : diffs) {
			if (select(diff)) {
				SyncInfo info = asSyncInfo(diff);
				if (info != null)
					result.add(info);
			}
		}
		return result;
	}

	private SyncInfo asSyncInfo(IDiff diff) {
		try {
			return ((SubscriberParticipant)getConfiguration().getParticipant()).getSubscriber().getSyncInfo(ResourceDiffTree.getResourceFor(diff));
		} catch (TeamException e) {
			TeamUIPlugin.log(e);
		}
		return null;
	}

	private boolean select(IDiff diff) {
		return getSeedSet().getSyncInfo(ResourceDiffTree.getResourceFor(diff)) != null;
	}

	/* private */ SyncInfo getSyncInfo(IPath path) {
		return getSyncInfo(getSeedSet(), path);
	}

	/* private */ IResource[] getResources(SyncInfoSet set, IPath[] paths) {
		List<IResource> result = new ArrayList<>();
		for (IPath path : paths) {
			SyncInfo info = getSyncInfo(set, path);
			if (info != null) {
				result.add(info.getLocal());
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	private SyncInfo getSyncInfo(SyncInfoSet set, IPath path) {
		SyncInfo[] infos = set.getSyncInfos();
		for (SyncInfo info : infos) {
			if (info.getLocal().getFullPath().equals(path))
				return info;
		}
		return null;
	}

	/*
	 * Remove the set from the collector.
	 */
	public void remove(ChangeSet set) {
		((DiffChangeSet)set).getDiffTree().removeDiffChangeListener(this);
		activeSets.remove(set);
		if (listener != null) {
			listener.setRemoved(set);
		}
	}

	/*
	 * Return the sync info set for the given active change set
	 * or null if there isn't one.
	 */
	public SyncInfoTree getSyncInfoSet(ChangeSet set) {
		return (SyncInfoTree)activeSets.get(set);
	}

	private ChangeSet getChangeSet(IDiffTree tree) {
		for (Object element : activeSets.keySet()) {
			ChangeSet changeSet = (ChangeSet) element;
			if (((DiffChangeSet)changeSet).getDiffTree() == tree) {
				return changeSet;
			}
		}
		return null;
	}

	private boolean select(SyncInfo info) {
		return getSeedSet().getSyncInfo(info.getLocal()) != null;
	}

	private SyncInfoSet getSeedSet() {
		return provider.getSyncInfoSet();
	}

	public void dispose() {
		getActiveChangeSetManager().removeListener(activeChangeSetListener);
	}

	/**
	 * Set the change set listener for this collector. There is
	 * only one for this type of collector.
	 * @param listener change set change listener
	 */
	public void setChangeSetChangeListener(IChangeSetChangeListener listener) {
		this.listener = listener;
		if (listener == null) {
			getActiveChangeSetManager().removeListener(activeChangeSetListener);
		} else {
			getActiveChangeSetManager().addListener(activeChangeSetListener);
		}
	}

	@Override
	public void diffsChanged(final IDiffChangeEvent event, IProgressMonitor monitor) {
		provider.performUpdate(monitor1 -> {
			ChangeSet changeSet = getChangeSet(event.getTree());
			if (changeSet != null) {
				SyncInfoSet targetSet = getSyncInfoSet(changeSet);
				if (targetSet != null) {
					targetSet.removeAll(getResources(targetSet, event.getRemovals()));
					targetSet.addAll(asSyncInfoSet(event.getAdditions()));
					targetSet.addAll(asSyncInfoSet(event.getChanges()));
					rootSet.removeAll(((IResourceDiffTree)event.getTree()).getAffectedResources());
				}
			}
		}, true /* preserver expansion */, true /* run in UI thread */);
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Nothing to do
	}
}
