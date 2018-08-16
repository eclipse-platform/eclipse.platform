/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelCompareParticipant;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;
import org.eclipse.team.internal.core.BackgroundEventHandler;
import org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector;
import org.eclipse.team.ui.synchronize.*;

/**
 * This class wraps a LogEntryCache in order to clear entries once they are no longer
 * in the subscriber.
 */
public class LogEntryCacheUpdateHandler extends BackgroundEventHandler {
   
    private static final int REMOVAL = 1;
    private static final int CHANGE = 2;
    private static final int FETCH_REQUEST = 3;
    private static final int PAUSE = 4;
    
    /*
     * Lock used to ensure that fetches are queued when the job is
     * a non-system job.
     */
    private final Object queueLock = new Object();
    
    /*
     * Exception used to stop processing so the job can be restarted as a non-system job
     */
    private static final OperationCanceledException PAUSE_EXCEPTION = new OperationCanceledException();
    
    /*
     * Contants for configuring how long to wait for the job to be paused
     * when a fetch is required and the job needs to be converted to a non-system
     * job. If the wait time is elapsed, an exception is thrown.
     */
    private static final int WAIT_INCREMENT = 10;
    private static final int MAX_WAIT = 1000;
    
    /*
     * Set that keeps track of all resource for which we haved fetched log entries
     */
    private final SyncInfoTree collectedInfos = new SyncInfoTree();
    
    /*
     * The cache that hold the log entries while the job is running
     */
    //private LogEntryCache logEntriesCache;
    
    /*
     * SoftReference used to hold on to the log entry cache while
     * the job is not running so the cache can be cleared if memory is low.
     */
    private SoftReference cacheReference;
    
    /*
     * Collector that forewards subscriber changes so that
     * stale cache entries can be cleared.
     */
    private final LogEntryResourceCollector collector;

    /*
     * The subscriber generating the SyncInfo and log entries
     */
    private final Subscriber subscriber;
    
    /*
     * The accumulated list of updates that need to be dispatched
     * (i.e. the cache should be purged of out-of-date resources).
     * This list is only modified and accessed from the event processing
     * thread.
     */
    private final List updates = new ArrayList();
    
    /*
     * The accumulated list of fetches that have been requested
     */
    private final List fetches = new ArrayList();
    private final ISynchronizePageConfiguration configuration;
    
    
    /*
     * Interface for notifying a single client that the infos have been fetched
     */
    public interface ILogsFetchedListener {

        void logEntriesFetched(SyncInfoSet set, LogEntryCache logEntryCache, IProgressMonitor monitor);
        
    }
    
    /*
     * The listener or null if noone is listening
     */
    private ILogsFetchedListener listener;
    
    /*
     * Subscriber resource collector that forwards subscriber changes
     * through the handler so that stale cache entries can be cleared
     */
    private class LogEntryResourceCollector extends SubscriberResourceCollector {

        public LogEntryResourceCollector(Subscriber subscriber) {
            super(subscriber);
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector#remove(org.eclipse.core.resources.IResource)
         */
        protected void remove(IResource resource) {
            queueEvent(new ResourceEvent(resource, REMOVAL, IResource.DEPTH_INFINITE), false /* do not put in on the front of the queue*/);  
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector#change(org.eclipse.core.resources.IResource, int)
         */
        protected void change(IResource resource, int depth) {
            queueEvent(new ResourceEvent(resource, CHANGE, depth), false /* do not put in on the front of the queue*/); 
        }

        protected boolean hasMembers(IResource resource) {
            return collectedInfos.hasMembers(resource);
        }
    }
    
    /*
     * Custom event for queue a log entry fetch request
     */
    private class FetchRequest extends Event {
        private final SyncInfo[] infos;
        public FetchRequest(SyncInfo[] infos) {
            super(FETCH_REQUEST);
            this.infos = infos;
        }
        public SyncInfo[] getInfos() {
            return infos;
        }
    }
    
    public LogEntryCacheUpdateHandler(ISynchronizePageConfiguration configuration) {
        super(CVSUIMessages.LogEntryCacheUpdateHandler_1, CVSUIMessages.LogEntryCacheUpdateHandler_0); // 
        this.configuration = configuration;
        this.subscriber = getSubscriber(configuration);
        cacheReference = new SoftReference(new LogEntryCache());
        collector = new LogEntryResourceCollector(subscriber);
    }

    private Subscriber getSubscriber(ISynchronizePageConfiguration configuration) {
        ISynchronizeParticipant participant = configuration.getParticipant();
        if (participant instanceof SubscriberParticipant) {
			SubscriberParticipant sp = (SubscriberParticipant) participant;
			return sp.getSubscriber();
		}
        if (participant instanceof ModelCompareParticipant) {
			ModelCompareParticipant mcp = (ModelCompareParticipant) participant;
			return mcp.getSubscriber();
		}
        return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
    }

    public ISynchronizePageConfiguration getConfiguration() {
        return configuration;
    }
    
    public Subscriber getSubscriber() {
        return subscriber;
    }
    
    /**
     * Set the listener that should receive notification when log entries
     * have been fetched and are avalable.
     * @param listener the listener or <code>null</code>
     */
    public void setListener(ILogsFetchedListener listener) {
        this.listener = listener;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#getJobFamiliy()
     */
    protected Object getJobFamiliy() {
        return ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#createEventHandlingJob()
     */
    protected void createEventHandlingJob() {
        super.createEventHandlingJob();
        Job job = getEventHandlerJob();
        job.setSystem(false);
        job.setUser(false);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#processEvent(org.eclipse.team.internal.core.BackgroundEventHandler.Event, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void processEvent(Event event, IProgressMonitor monitor) throws CoreException {
        Policy.checkCanceled(monitor);
        switch (event.getType()) {
        	case REMOVAL:
        	case CHANGE:
        		updates.add(event);
        		break;
        	case FETCH_REQUEST:
        	    fetches.add(event);
        	    break;
        	case PAUSE:
        	    throw PAUSE_EXCEPTION;
        }
        
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#doDispatchEvents(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected boolean doDispatchEvents(IProgressMonitor monitor) throws TeamException {
        Policy.checkCanceled(monitor);
        boolean dispatched = false;
        monitor.beginTask(null, 50);
        dispatched |= updateCache(Policy.subMonitorFor(monitor, 20));
        dispatched |= processQueuedFetches(Policy.subMonitorFor(monitor, 80));
        monitor.done();
        return dispatched;
    }
	
    /*
     * Remove any stale or unneeded log entries from the cache.
     * Return whether there were any entries to purge.
     */
    private boolean updateCache(IProgressMonitor monitor) {
        if (updates.isEmpty()) return false;
        try {
            collectedInfos.beginInput();
            // Cycle through the update events
            for (Iterator iter = updates.iterator(); iter.hasNext();) {
                Event event = (Event) iter.next();
                Policy.checkCanceled(monitor);
                if (event.getType() == REMOVAL) {
                    remove(event.getResource(), ((ResourceEvent)event).getDepth());
                } else if (event.getType() == CHANGE) {
                    change(event.getResource(), ((ResourceEvent)event).getDepth());
                }
                // Use the iterator to remove so that updates will not be lost
                // if the job is cancelled and then restarted.
                iter.remove();
            }
        } finally {
            collectedInfos.endInput(monitor);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#shutdown()
     */
    public void shutdown() {
        super.shutdown();
        collector.dispose();
        // Probably not necessary as GC would take care of it but we'll do it anyway
        if (cacheReference != null) {
	        LogEntryCache cache = (LogEntryCache)cacheReference.get();
	        if (cache != null) {
	            cache.clearEntries();
	        }
        }
        collectedInfos.clear();
        
    }
    
    private void remove(IResource resource, int depth) {
        collectedInfos.remove(resource, depth);
    }

    private void remove(SyncInfo info) {
        if (info != null) {
            collectedInfos.remove(info.getLocal());
            LogEntryCache cache = (LogEntryCache)cacheReference.get();
            if (cache != null) {
				ICVSRemoteResource remoteResource = getRemoteResource(info);
				if (remoteResource != null)
					cache.clearEntries(remoteResource);
			}
        }
    }

    public ICVSRemoteResource getRemoteResource(SyncInfo info) {
		try {
			ICVSRemoteResource remote = (ICVSRemoteResource) info.getRemote();
			ICVSRemoteResource local = CVSWorkspaceRoot.getRemoteResourceFor(info.getLocal());
			if(local == null) {
				local = (ICVSRemoteResource)info.getBase();
			}
			
			boolean useRemote = true;
			if (local != null && remote != null) {
				String remoteRevision = getRevisionString(remote);
				String localRevision = getRevisionString(local);
				useRemote = useRemote(localRevision, remoteRevision);
			} else if (remote == null) {
				useRemote = false;
			}
			if (useRemote) {
				return remote;
			} else if (local != null) {
				return local;
			}
			return null;
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			return null;
		}
	}

    private boolean useRemote(String localRevision, String remoteRevision) {
        boolean useRemote;
        if (remoteRevision == null && localRevision == null) {
            useRemote = true;
        } else if (localRevision == null) {
            useRemote = true;
        } else if (remoteRevision == null) {
            useRemote = false;
        } else {
            useRemote = ResourceSyncInfo.isLaterRevision(remoteRevision, localRevision);
        }
        return useRemote;
    }

    private String getRevisionString(ICVSRemoteResource remoteFile) {
		if(remoteFile instanceof RemoteFile) {
			return ((RemoteFile)remoteFile).getRevision();
		}
		return null;
	}
    
    private void change(IResource resource, int depth) {
        // We only need to remove collected log entries that don't apply
        // any longer. They will be refetched when they are required.
        SyncInfo[] collected = collectedInfos.getSyncInfos(resource, depth);
        change(collected);
    }
    
    private void change(SyncInfo[] collected) {
        Subscriber subscriber = getSubscriber();
        for (int i = 0; i < collected.length; i++) {
            try {
                SyncInfo info = collected[i];
                SyncInfo newInfo = subscriber.getSyncInfo(info.getLocal());
                if (newInfo == null || !newInfo.equals(info)) {
                    // The cached log entry no longer applies to the new sync info.
                    // It will be refetched when required.
                    remove(info);
                }
            } catch (TeamException e) {
                // Log and continue
                CVSUIPlugin.log(e);
            }
        }
    }

    /**
     * Queue a request to fetch log entries for the given SyncInfo nodes.
     * The event handler must be a non-system job when revision histories 
     * are fetched.
     * @param infos the nodes whose log entries are to be fetched
     */
    public void fetch(SyncInfo[] infos) throws CVSException {
        synchronized(queueLock) {
	        Job job = getEventHandlerJob();
	        if (job.isSystem() && job.getState() != Job.NONE) {
	            // queue an event to pause the processor
	            super.queueEvent(new Event(PAUSE), true /* put on the front of the queue */);
	            int count = 0;
	            while (job.getState() != Job.NONE && count < MAX_WAIT) {
	                count += WAIT_INCREMENT;
	                try {
                        Thread.sleep(WAIT_INCREMENT); // Wait a little while
                    } catch (InterruptedException e) {
                        // Ignore
                    }
	            }
	            if (job.getState() != Job.NONE) {
	                // The job never completed in the time aloted so throw an exception
	                throw new CVSException(CVSUIMessages.LogEntryCacheUpdateHandler_2); 
	            }
	        }
	        // Queue the event even if the job didn't stop in the time aloted
	        queueEvent(new FetchRequest(infos), false /* don't place at the end */);
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#queueEvent(org.eclipse.team.internal.core.BackgroundEventHandler.Event, boolean)
     */
    protected void queueEvent(Event event, boolean front) {
        // Override to snure that queues by this handler are serialized
        synchronized(queueLock) {
            Job job = getEventHandlerJob();
            if (job.getState() == Job.NONE) {
                job.setSystem(event.getType() != FETCH_REQUEST); 
            }
            super.queueEvent(event, front);
        }
    }
    
    /*
     * Method invoked during event dispatch to fetch log entries
     */
    private boolean processQueuedFetches(IProgressMonitor monitor) {
        if (fetches.isEmpty()) return false;
        try {
            // Now perform the fetching
            Map projectMapping = getFetchesByProject();
            if (projectMapping.isEmpty()) return true;
            LogEntryCache logEntriesCache = (LogEntryCache)cacheReference.get();
            if (logEntriesCache == null) {
                logEntriesCache = new LogEntryCache();
                cacheReference = new SoftReference(logEntriesCache);
            }
            monitor.beginTask(CVSUIMessages.CVSChangeSetCollector_4, 100 * projectMapping.size()); 
            monitor.setTaskName(CVSUIMessages.CVSChangeSetCollector_4); 
            for (Iterator iter = projectMapping.values().iterator(); iter.hasNext();) {
                SyncInfoSet set = (SyncInfoSet) iter.next();
                Policy.checkCanceled(monitor);
                fetchLogEntries(logEntriesCache, set, Policy.subMonitorFor(monitor, 90));
                fireFetchedNotification(logEntriesCache, set, Policy.subMonitorFor(monitor, 10));
            }
        } finally {
            // Clear the fetches even if we were cancelled.
            // Restarting will need to re-request all infos
            fetches.clear();
            monitor.done();
        }
        return true;
    }

    private void fireFetchedNotification(LogEntryCache logEntriesCache, SyncInfoSet set, IProgressMonitor monitor) {
        if (listener != null) {
            listener.logEntriesFetched(set, logEntriesCache, monitor);
        }
    }

    /*
     * Return a map of IProject to SyncInfoSet as that is how entries are fetched.
     * The set for each project includes all infos from the original set.
     * This is one so that the completion notification contains all infos
     * including those were a fetch was not required either because the
     * entry was already cached or the resource has no history.
     */
    private Map getFetchesByProject() {
        Map result = new HashMap();
        for (Iterator iter = fetches.iterator(); iter.hasNext();) {
            FetchRequest request = (FetchRequest) iter.next();
            SyncInfo[] infos = request.getInfos();
            for (int i = 0; i < infos.length; i++) {
                SyncInfo info = infos[i];
                IProject project = info.getLocal().getProject();
                SyncInfoSet infoSet = (SyncInfoSet)result.get(project);
                if (infoSet == null) {
                    infoSet = new SyncInfoSet();
                    result.put(project, infoSet);
                }
                infoSet.add(info);
            }
        }
        return result;
    }

    private boolean isFetchRequired(SyncInfo info) {
        // We only need to fetch if we don't have the log entry already
        // and the change is a remote change
        return info.getLocal().getType() == IResource.FILE && !isLogEntryCached(info) && isRemoteChange(info);
        
    }
    
	/*
     * Return whether the given SyncInfo is cached. If there is
     * an info for the resource that does not match the given info,
     * it is removed and false is returned.
     */
    private boolean isLogEntryCached(SyncInfo info) {
        SyncInfo collectedInfo = collectedInfos.getSyncInfo(info.getLocal());
        if (collectedInfo != null && !collectedInfo.equals(info)) {
            remove(collectedInfo);
            collectedInfo = null;
        }
        return collectedInfo != null;
    }

    /*
	 * Return if this sync info should be considered as part of a remote change
	 * meaning that it can be placed inside an incoming commit set (i.e. the
	 * set is determined using the comments from the log entry of the file). 
	 */
	public boolean isRemoteChange(SyncInfo info) {
		int kind = info.getKind();
		if(info.getLocal().getType() != IResource.FILE) return false;
		if(info.getComparator().isThreeWay()) {
			return (kind & SyncInfo.DIRECTION_MASK) != SyncInfo.OUTGOING;
		}
		// For two-way, the change is only remote if it has a remote or has a base locally
		if (info.getRemote() != null) return true;
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor((IFile)info.getLocal());
		try {
            return file.getSyncBytes() != null;
        } catch (CVSException e) {
            // Log the error and exclude the file from consideration
            CVSUIPlugin.log(e);
            return false;
        }
	}
	
    /*
     * Fetch the log entries for the info in the given set
     */
    private void fetchLogEntries(LogEntryCache logEntriesCache, SyncInfoSet set, IProgressMonitor monitor) {
	    try {
            if (subscriber instanceof CVSCompareSubscriber) {
                CVSCompareSubscriber compareSubscriber = (CVSCompareSubscriber)subscriber;
                fetchLogEntries(logEntriesCache, compareSubscriber, set, monitor);
            } else {
                // Run the log command once with no tags
            	fetchLogs(logEntriesCache, set, null, null, monitor);
            }
        } catch (CVSException e) {
            handleException(e);
        } catch (InterruptedException e) {
            throw new OperationCanceledException();
        }
        
    }
    
    private void fetchLogEntries(LogEntryCache logEntriesCache, CVSCompareSubscriber compareSubscriber, SyncInfoSet set, IProgressMonitor monitor) throws CVSException, InterruptedException {
        Map localTagMap = getLocalTagMap(set);
        monitor.beginTask(null, 100 * localTagMap.size());
        for (Iterator iter = localTagMap.keySet().iterator(); iter.hasNext();) {
            CVSTag localTag = (CVSTag) iter.next();        
	        fetchLogEntries(logEntriesCache, compareSubscriber, set, localTag, Policy.subMonitorFor(monitor, 100));
        }
        Policy.checkCanceled(monitor);
        monitor.done();
    }

    /*
     * Return the resources grouped by the tag found in the
     * workspace. The map is CVSTag->SyncInfoSet
     */
    private Map getLocalTagMap(SyncInfoSet set) {
        Map result = new HashMap();
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            SyncInfo info = (SyncInfo) iter.next();
            CVSTag tag = getLocalTag(info);
            SyncInfoSet tagSet = (SyncInfoSet)result.get(tag);
            if (tagSet == null) {
                tagSet = new SyncInfoSet();
                result.put(tag, tagSet);
            }
            tagSet.add(info);
        }
        return result;
    }

    private CVSTag getLocalTag(SyncInfo syncInfo) {
		try {
            IResource local = syncInfo.getLocal();
            ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(local);
            CVSTag tag = null;
            if(cvsResource.isFolder()) {
            	FolderSyncInfo info = ((ICVSFolder)cvsResource).getFolderSyncInfo();
            	if(info != null) {
            		tag = info.getTag();									
            	}
            	if (tag != null && tag.getType() == CVSTag.BRANCH) {
            		tag = Util.getAccurateFolderTag(local, tag);
            	}
            } else {
            	tag = CVSAction.getAccurateFileTag(cvsResource);
            }
            if(tag == null) {
            	tag = new CVSTag();
            }
            return tag;
        } catch (CVSException e) {
            CVSUIPlugin.log(e);
            return new CVSTag();
        }
    }

    private void fetchLogEntries(LogEntryCache logEntriesCache, CVSCompareSubscriber compareSubscriber, SyncInfoSet set, CVSTag localTag, IProgressMonitor monitor) throws CVSException, InterruptedException {
        if (compareSubscriber.isMultipleTagComparison()) {
            Map rootToInfoMap = getRootToInfoMap(compareSubscriber, set);
            monitor.beginTask(null, 100 * rootToInfoMap.size());
            for (Iterator iterator = rootToInfoMap.keySet().iterator(); iterator.hasNext();) {
                IResource root = (IResource) iterator.next();
                Policy.checkCanceled(monitor);
                fetchLogs(logEntriesCache, set, localTag, compareSubscriber.getTag(root), Policy.subMonitorFor(monitor, 100));
            }
            monitor.done();
        } else {
            Policy.checkCanceled(monitor);
            fetchLogs(logEntriesCache, set, localTag, compareSubscriber.getTag(), monitor);
        }
    }

    private Map getRootToInfoMap(CVSCompareSubscriber compareSubscriber, SyncInfoSet set) {
        Map rootToInfosMap = new HashMap();
        IResource[] roots = compareSubscriber.roots();
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            SyncInfo info = (SyncInfo) iter.next();
            IPath localPath = info.getLocal().getFullPath();
            for (int j = 0; j < roots.length; j++) {
                IResource resource = roots[j];
                if (resource.getFullPath().isPrefixOf(localPath)) {
                    SyncInfoSet infoList = (SyncInfoSet)rootToInfosMap.get(resource);
                    if (infoList == null) {
                        infoList = new SyncInfoSet();
                        rootToInfosMap.put(resource, infoList);
                    }
                    infoList.add(info);
                    break; // out of inner loop
                }
            }
            
        }
        return rootToInfosMap;
    }

    private void fetchLogs(LogEntryCache logEntriesCache, SyncInfoSet set, CVSTag localTag, CVSTag remoteTag, IProgressMonitor monitor) throws CVSException, InterruptedException {
	    ICVSRemoteResource[] remoteResources = getRemotesToFetch(set.getSyncInfos());
	    if (remoteResources.length > 0) {
			RemoteLogOperation logOperation = new RemoteLogOperation(getConfiguration().getSite().getPart(), remoteResources, localTag, remoteTag, logEntriesCache);
			logOperation.execute(monitor);
	    }
	    collectedInfos.addAll(set);
	}
	
	private ICVSRemoteResource[] getRemotesToFetch(SyncInfo[] infos) {
		List remotes = new ArrayList();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (isFetchRequired(info)) {
				ICVSRemoteResource remote = getRemoteResource(info);
				if(remote != null) {
					remotes.add(remote);
				}
			}
		}
		return (ICVSRemoteResource[]) remotes.toArray(new ICVSRemoteResource[remotes.size()]);
	}

    /**
     * Stop any current fetch in process.
     */
    public void stopFetching() {
        try {
            getEventHandlerJob().cancel();
            getEventHandlerJob().join();
        } catch (InterruptedException e) {
        }
    }
}
