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
package org.eclipse.team.internal.core.subscribers;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.internal.core.Policy;

/**
 * A change set manager that batches change event notification.
 */
public class BatchingChangeSetManager extends ChangeSetManager {

	private ILock lock = Job.getJobManager().newLock();

	public static class CollectorChangeEvent {

		Set added = new HashSet();
		Set removed = new HashSet();
		Map changed = new HashMap();
		private final BatchingChangeSetManager collector;

		public CollectorChangeEvent(BatchingChangeSetManager collector) {
			this.collector = collector;
		}

		private void setAdded(ChangeSet set) {
			added.add(set);
			removed.remove(set);
		}

		private void setRemoved(ChangeSet set) {
			added.remove(set);
			removed.add(set);
			// Do not clear the changes list since clients may want to know what resources
			// were changed before the set was removed
		}

		private void changed(ChangeSet changeSet, IPath[] allAffectedResources) {
			if (added.contains(changeSet))
				return;
			IPath[] paths = (IPath[])changed.get(changeSet);
			if (paths == null) {
				changed.put(changeSet, allAffectedResources);
			} else {
				Set allPaths = new HashSet();
				for (int i = 0; i < paths.length; i++) {
					IPath path = paths[i];
					allPaths.add(path);
				}
				for (int i = 0; i < allAffectedResources.length; i++) {
					IPath path = allAffectedResources[i];
					allPaths.add(path);
				}
				changed.put(changeSet, (IPath[]) allPaths.toArray(new IPath[allPaths.size()]));
			}
		}

		public boolean isEmpty() {
			return changed.isEmpty() && added.isEmpty() && removed.isEmpty();
		}

		public ChangeSet[] getAddedSets() {
			return (ChangeSet[]) added.toArray(new ChangeSet[added.size()]);
		}

		public ChangeSet[] getRemovedSets() {
			return (ChangeSet[]) removed.toArray(new ChangeSet[removed.size()]);
		}

		public ChangeSet[] getChangedSets() {
			return (ChangeSet[]) changed.keySet().toArray(new ChangeSet[changed.size()]);
		}

		public IPath[] getChangesFor(ChangeSet set) {
			return (IPath[])changed.get(set);
		}

		public BatchingChangeSetManager getSource() {
			return collector;
		}
	}

	public static interface IChangeSetCollectorChangeListener {
		public void changeSetChanges(CollectorChangeEvent event, IProgressMonitor monitor);
	}

	private CollectorChangeEvent changes = new CollectorChangeEvent(this);

	public void beginInput() {
		lock.acquire();
	}

	public void endInput(IProgressMonitor monitor) {
		try {
			if (lock.getDepth() == 1) {
				// Remain locked while firing the events so the handlers
				// can expect the set to remain constant while they process the events
				fireChanges(Policy.monitorFor(monitor));
			}
		} finally {
			lock.release();
		}
	}

    private void fireChanges(final IProgressMonitor monitor) {
    	if (changes.isEmpty()) {
    		return;
    	}
    	final CollectorChangeEvent event = changes;
    	changes = new CollectorChangeEvent(this);
        Object[] listeners = getListeners();
        for (int i = 0; i < listeners.length; i++) {
            final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
            if (listener instanceof IChangeSetCollectorChangeListener) {
				final IChangeSetCollectorChangeListener csccl = (IChangeSetCollectorChangeListener) listener;
				SafeRunner.run(new ISafeRunnable() {
					public void handleException(Throwable exception) {
						// Exceptions are logged by the platform
					}
					public void run() throws Exception {
						csccl.changeSetChanges(event, monitor);
					}
				});
			}
        }
	}

    public void add(ChangeSet set) {
    	try {
    		beginInput();
    		super.add(set);
    		changes.setAdded(set);
    	} finally {
    		endInput(null);
    	}
    }

    public void remove(ChangeSet set) {
    	try {
    		beginInput();
    		super.remove(set);
    		changes.setRemoved(set);
    	} finally {
    		endInput(null);
    	}
    }

    protected void fireResourcesChangedEvent(ChangeSet changeSet, IPath[] allAffectedResources) {
    	super.fireResourcesChangedEvent(changeSet, allAffectedResources);
    	try {
    		beginInput();
    		changes.changed(changeSet, allAffectedResources);
    	} finally {
    		endInput(null);
    	}
    }

    protected void initializeSets() {
    	// Nothing to do
    }
}
