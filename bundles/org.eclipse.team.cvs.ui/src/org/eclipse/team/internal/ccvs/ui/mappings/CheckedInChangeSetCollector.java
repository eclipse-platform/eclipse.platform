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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.diff.provider.DiffTree;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.mapping.CVSCheckedInChangeSet;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSChangeSetCollector;
import org.eclipse.team.internal.ccvs.ui.subscriber.LogEntryCacheUpdateHandler;
import org.eclipse.team.internal.ccvs.ui.subscriber.LogEntryCacheUpdateHandler.ILogsFetchedListener;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.internal.core.subscribers.*;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

public class CheckedInChangeSetCollector extends BatchingChangeSetManager implements ILogsFetchedListener {

	/*
	 * Constant used to store the log entry handler in the configuration so it can
	 * be kept around over layout changes
	 */
	private static final String LOG_ENTRY_HANDLER = CVSUIPlugin.ID + ".LogEntryHandler"; //$NON-NLS-1$
	
	/* *****************************************************************************
	 * Special sync info that has its kind already calculated.
	 */
	private class CVSUpdatableSyncInfo extends CVSSyncInfo {
		public int kind;
		public CVSUpdatableSyncInfo(int kind, IResource local, IResourceVariant base, IResourceVariant remote, Subscriber s) {
			super(local, base, remote, s);
			this.kind = kind;
		}

		@Override
		protected int calculateKind() throws TeamException {
			return kind;
		}
	}
	
	IDiffChangeListener diffTreeListener = new IDiffChangeListener() {
		@Override
		public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
			// Ignore
		}
		@Override
		public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
			if (event.getTree().isEmpty()) {
				ChangeSet changeSet = getChangeSet(event.getTree());
				if (changeSet != null) {
					remove(changeSet);
				}
			} else {
				ChangeSet changeSet = getChangeSet(event.getTree());
				if (changeSet != null) {
					fireResourcesChangedEvent(changeSet, getAffectedPaths(event));
				}
			}
		}
		private IPath[] getAffectedPaths(IDiffChangeEvent event) {
			Set<IPath> result = new HashSet<>();
			IPath[] removed = event.getRemovals();
			Collections.addAll(result, removed);
			IDiff[] diffs = event.getAdditions();
			for (IDiff diff : diffs) {
				result.add(diff.getPath());
			}
			diffs = event.getChanges();
			for (IDiff diff : diffs) {
				result.add(diff.getPath());
			}
			return result.toArray(new IPath[result.size()]);
		}
	};

	private final ISynchronizePageConfiguration configuration;
	private boolean disposed;
	private LogEntryCache logEntryCache;
	private final Subscriber subscriber;

	private HashSet<ChangeSet> updatedSets;
	
	public CheckedInChangeSetCollector(ISynchronizePageConfiguration configuration, Subscriber subscriber) {
		this.configuration = configuration;
		this.subscriber = subscriber;
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
	
	@Override
	protected void handleSetAdded(ChangeSet set) {
		((DiffChangeSet)set).getDiffTree().addDiffChangeListener(diffTreeListener);
		super.handleSetAdded(set);
		if (updatedSets != null) {
			updatedSets.add(set);
			((DiffTree)((DiffChangeSet)set).getDiffTree()).beginInput();
		}
	}
	
	@Override
	protected void handleSetRemoved(ChangeSet set) {
		((DiffChangeSet)set).getDiffTree().removeDiffChangeListener(diffTreeListener);
		super.handleSetRemoved(set);
	}
	
	protected ChangeSet getChangeSet(IDiffTree tree) {
		ChangeSet[] sets = getSets();
		for (ChangeSet changeSet : sets) {
			if (((DiffChangeSet)changeSet).getDiffTree() == tree) {
				return changeSet;
			}
		}
		return null;
	}
	
	public void handleChange(IDiffChangeEvent event) {
		List<IPath> removals = new ArrayList<>();
		List<IDiff> additions = new ArrayList<>();
		removals.addAll(Arrays.asList(event.getRemovals()));
		additions.addAll(Arrays.asList(event.getAdditions()));
		IDiff[] changed = event.getChanges();
		for (IDiff diff : changed) {
			additions.add(diff);
			removals.add(diff.getPath());
		}
		if (!removals.isEmpty()) {
			remove(removals.toArray(new IPath[removals.size()]));
		}
		if (!additions.isEmpty()) {
			add(additions.toArray(new IDiff[additions.size()]));
		}
	}
	
	protected void remove(IPath[] paths) {
		ChangeSet[] sets = getSets();
		for (ChangeSet s : sets) {
			DiffChangeSet set = (DiffChangeSet) s;
			set.remove(paths);
		}
	}
	
	public synchronized LogEntryCacheUpdateHandler getLogEntryHandler() {
		LogEntryCacheUpdateHandler handler = (LogEntryCacheUpdateHandler)getConfiguration().getProperty(LOG_ENTRY_HANDLER);
		if (handler == null) {
			handler = initializeLogEntryHandler(getConfiguration());
		}
		handler.setListener(this);
		return handler;
	}
	
	/*
	 * Initialize the log entry handler and place it in the configuration
	 */
	private LogEntryCacheUpdateHandler initializeLogEntryHandler(final ISynchronizePageConfiguration configuration) {
		final LogEntryCacheUpdateHandler logEntryHandler = new LogEntryCacheUpdateHandler(configuration);
		configuration.setProperty(LOG_ENTRY_HANDLER, logEntryHandler);
		// Use an action group to get notified when the configuration is disposed
		configuration.addActionContribution(new SynchronizePageActionGroup() {
			@Override
			public void dispose() {
				super.dispose();
				LogEntryCacheUpdateHandler handler = (LogEntryCacheUpdateHandler)configuration.getProperty(LOG_ENTRY_HANDLER);
				if (handler != null) {
					handler.shutdown();
					configuration.setProperty(LOG_ENTRY_HANDLER, null);
				}
			}
		});
		return logEntryHandler;
	}

	protected void add(IDiff[] diffs) {
		LogEntryCacheUpdateHandler handler = getLogEntryHandler();
		if (handler != null)
			try {
				handler.fetch(getSyncInfos(diffs));
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
	}
	
	private SyncInfo[] getSyncInfos(IDiff[] diffs) {
		SyncInfoSet set = new SyncInfoSet();
		for (IDiff diff : diffs) {
			set.add(getConverter().asSyncInfo(diff, getSubscriber().getResourceComparator()));
		}
		return set.getSyncInfos();
	}

	public Subscriber getSubscriber() {
		return subscriber;
	}

	@Override
	public void dispose() {
		// No longer listen for log entry changes
		// (The handler is disposed with the page)
		disposed = true;
		LogEntryCacheUpdateHandler handler = getLogEntryHandler();
		if (handler != null) handler.setListener(null);
		getConfiguration().setProperty(CVSChangeSetCollector.CVS_CHECKED_IN_COLLECTOR, null);
		logEntryCache = null;
		super.dispose();
	}
	
	/**
	 * Fetch the log histories for the remote changes and use this information
	 * to add each resource to an appropriate commit set.
	 */
	private void handleRemoteChanges(final SyncInfo[] infos, final LogEntryCache logEntries, final IProgressMonitor monitor) {
		try {
			beginSetUpdate();
			addLogEntries(infos, logEntries, monitor);
		} finally {
			endSetUpdate(monitor);
		}
	}
	
	private void beginSetUpdate() {
		updatedSets = new HashSet<>();
	}

	private void endSetUpdate(IProgressMonitor monitor) {
		for (Iterator iter = updatedSets.iterator(); iter.hasNext();) {
			DiffChangeSet set = (DiffChangeSet) iter.next();
			try {
				((DiffTree)set.getDiffTree()).endInput(monitor);
			} catch (RuntimeException e) {
				CVSUIPlugin.log(IStatus.ERROR, "Internal error", e); //$NON-NLS-1$
			}
		}
		updatedSets = null;
	}

	/*
	 * Add the following sync info elements to the viewer. It is assumed that these elements have associated
	 * log entries cached in the log operation.
	 */
	private void addLogEntries(SyncInfo[] commentInfos, LogEntryCache logs, IProgressMonitor monitor) {
		try {
			monitor.beginTask(null, commentInfos.length * 10);
			if (logs != null) {
				for (SyncInfo commentInfo : commentInfos) {
					addSyncInfoToCommentNode(commentInfo, logs);
					monitor.worked(10);
				}
			}
		} finally {
			monitor.done();
		}
	}
	
	/*
	 * Create a node for the given sync info object. The logs should contain the log for this info.
	 * 
	 * @param info the info for which to create a node in the model
	 * @param log the cvs log for this node
	 */
	private void addSyncInfoToCommentNode(SyncInfo info, LogEntryCache logs) {
		LogEntryCacheUpdateHandler handler = getLogEntryHandler();
		if (handler != null) {
			ICVSRemoteResource remoteResource = handler.getRemoteResource(info);
			if(handler.getSubscriber() instanceof CVSCompareSubscriber && remoteResource != null) {
				addMultipleRevisions(info, logs, remoteResource);
			} else {
				addSingleRevision(info, logs, remoteResource);
			}
		}
	}
	
	/*
	 * Add a single log entry to the model.
	 * 
	 * @param info
	 * @param logs
	 * @param remoteResource
	 */
	private void addSingleRevision(SyncInfo info, LogEntryCache logs, ICVSRemoteResource remoteResource) {
		ILogEntry logEntry = logs.getLogEntry(remoteResource);
		if (remoteResource != null && !remoteResource.isFolder()) {
			// For incoming deletions grab the comment for the latest on the same branch
			// which is now in the attic.
			try {
				String remoteRevision = ((ICVSRemoteFile) remoteResource).getRevision();
				if (isDeletedRemotely(info)) {
					ILogEntry[] logEntries = logs.getLogEntries(remoteResource);
					for (ILogEntry entry : logEntries) {
						String revision = entry.getRevision();
						if (entry.isDeletion() && ResourceSyncInfo.isLaterRevision(revision, remoteRevision)) {
							logEntry = entry;
						}
					}
				}
			} catch (TeamException e) {
				// continue and skip deletion checks
			}
		}
		addRemoteChange(info, remoteResource, logEntry);
	}
	
	/*
	 * Add multiple log entries to the model.
	 * 
	 * @param info
	 * @param logs
	 * @param remoteResource
	 */
	private void addMultipleRevisions(SyncInfo info, LogEntryCache logs, ICVSRemoteResource remoteResource) {
		ILogEntry[] logEntries = logs.getLogEntries(remoteResource);
		if(logEntries == null || logEntries.length == 0) {
			// If for some reason we don't have a log entry, try the latest
			// remote.
			addRemoteChange(info, null, null);
		} else {
			for (ILogEntry entry : logEntries) {
				addRemoteChange(info, remoteResource, entry);
			}
		}
	}
	
	private boolean isDeletedRemotely(SyncInfo info) {
		int kind = info.getKind();
		if(kind == (SyncInfo.INCOMING | SyncInfo.DELETION)) return true;
		if(SyncInfo.getDirection(kind) == SyncInfo.CONFLICTING && info.getRemote() == null) return true;
		return false;
	}
	
	/*
	 * Add the remote change to an incoming commit set
	 */
	private void addRemoteChange(SyncInfo info, ICVSRemoteResource remoteResource, ILogEntry logEntry) {
		if (disposed) return;
		LogEntryCacheUpdateHandler handler = getLogEntryHandler();
		if(handler != null && remoteResource != null && logEntry != null && handler.isRemoteChange(info)) {
			if(requiresCustomSyncInfo(info, remoteResource, logEntry)) {
				info = new CVSUpdatableSyncInfo(info.getKind(), info.getLocal(), info.getBase(), (RemoteResource)logEntry.getRemoteFile(), getSubscriber());
				try {
					info.init();
				} catch (TeamException e) {
					// this shouldn't happen, we've provided our own calculate kind
				}
			}
			IDiff diff = getConverter().getDeltaFor(info);
			// Only add the info if the base and remote differ
			IResourceVariant base = info.getBase();
			IResourceVariant remote = info.getRemote();
			if ((base == null && remote != null) || (remote == null && base != null) || (remote != null && base != null && !base.equals(remote))) {
				synchronized(this) {
					CVSCheckedInChangeSet set = getChangeSetFor(logEntry);
					if (set == null) {
						set = createChangeSetFor(logEntry);
						add(set);
					}
					set.add(diff);
				}
			}
		} else {
			// The info was not retrieved for the remote change for some reason.
			// Add the node to the root
			//addToDefaultSet(DEFAULT_INCOMING_SET_NAME, info);
		}
	}

	private SyncInfoToDiffConverter getConverter() {
		SyncInfoToDiffConverter converter = Adapters.adapt(subscriber, SyncInfoToDiffConverter.class);
		if (converter == null)
			converter = SyncInfoToDiffConverter.getDefault();
		return converter;
	}

	private CVSCheckedInChangeSet createChangeSetFor(ILogEntry logEntry) {
		return new CVSCheckedInChangeSet(logEntry);
	}

	private CVSCheckedInChangeSet getChangeSetFor(ILogEntry logEntry) {
		ChangeSet[] sets = getSets();
		for (ChangeSet set : sets) {
			if (set instanceof CVSCheckedInChangeSet &&
					set.getComment().equals(logEntry.getComment()) &&
					((CVSCheckedInChangeSet)set).getAuthor().equals(logEntry.getAuthor())) {
				return (CVSCheckedInChangeSet)set;
			}
		}
		return null;
	}
	
	private boolean requiresCustomSyncInfo(SyncInfo info, ICVSRemoteResource remoteResource, ILogEntry logEntry) {
		// Only interested in non-deletions
		if (logEntry.isDeletion()) return false;
		// Only require a custom sync info if the remote of the sync info
		// differs from the remote in the log entry
		IResourceVariant remote = info.getRemote();
		if (remote == null) return true;
		return !remote.equals(remoteResource);
	}
	
	/*
	 * @see
	 * org.eclipse.team.ui.synchronize.SyncInfoSetChangeSetCollector#waitUntilDone(
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void waitUntilDone(IProgressMonitor monitor) {
		monitor.worked(1);
		// wait for the event handler to process changes.
		LogEntryCacheUpdateHandler handler = getLogEntryHandler();
		if (handler != null) {
			while(handler.getEventHandlerJob().getState() != Job.NONE) {
				monitor.worked(1);
				try {
					Thread.sleep(10);		
				} catch (InterruptedException e) {
				}
				Policy.checkCanceled(monitor);
			}
		}
		monitor.worked(1);
	}

	@Override
	public void logEntriesFetched(SyncInfoSet set, LogEntryCache logEntryCache, IProgressMonitor monitor) {
		if (disposed) return;
		// Hold on to the cache so we can use it while commit sets are visible
		this.logEntryCache = logEntryCache;
		try {
			beginInput();
			handleRemoteChanges(set.getSyncInfos(), logEntryCache, monitor);
		} finally {
			endInput(monitor);
		}
	}

	public ICVSRemoteFile getImmediatePredecessor(ICVSRemoteFile file) throws TeamException {
		if (logEntryCache != null)
			return logEntryCache.getImmediatePredecessor(file);
		return null;
	}
}
