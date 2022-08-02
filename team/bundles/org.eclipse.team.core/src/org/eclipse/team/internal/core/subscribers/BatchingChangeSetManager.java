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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.internal.core.Policy;

/**
 * A change set manager that batches change event notification.
 */
public class BatchingChangeSetManager extends ChangeSetManager {

	private ILock lock = Job.getJobManager().newLock();

	public static class CollectorChangeEvent {

		Set<ChangeSet> added = new HashSet<>();
		Set<ChangeSet> removed = new HashSet<>();
		Map<ChangeSet, IPath[]> changed = new HashMap<>();
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
			IPath[] paths = changed.get(changeSet);
			if (paths == null) {
				changed.put(changeSet, allAffectedResources);
			} else {
				Set<IPath> allPaths = new HashSet<>();
				Collections.addAll(allPaths, paths);
				Collections.addAll(allPaths, allAffectedResources);
				changed.put(changeSet, allPaths.toArray(new IPath[allPaths.size()]));
			}
		}

		public boolean isEmpty() {
			return changed.isEmpty() && added.isEmpty() && removed.isEmpty();
		}

		public ChangeSet[] getAddedSets() {
			return added.toArray(new ChangeSet[added.size()]);
		}

		public ChangeSet[] getRemovedSets() {
			return removed.toArray(new ChangeSet[removed.size()]);
		}

		public ChangeSet[] getChangedSets() {
			return changed.keySet().toArray(new ChangeSet[changed.size()]);
		}

		public IPath[] getChangesFor(ChangeSet set) {
			return changed.get(set);
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
		for (Object l : listeners) {
			final IChangeSetChangeListener listener = (IChangeSetChangeListener) l;
			if (listener instanceof IChangeSetCollectorChangeListener) {
				final IChangeSetCollectorChangeListener csccl = (IChangeSetCollectorChangeListener) listener;
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void handleException(Throwable exception) {
						// Exceptions are logged by the platform
					}
					@Override
					public void run() throws Exception {
						csccl.changeSetChanges(event, monitor);
					}
				});
			}
		}
	}

	@Override
	public void add(ChangeSet set) {
		try {
			beginInput();
			super.add(set);
			changes.setAdded(set);
		} finally {
			endInput(null);
		}
	}

	@Override
	public void remove(ChangeSet set) {
		try {
			beginInput();
			super.remove(set);
			changes.setRemoved(set);
		} finally {
			endInput(null);
		}
	}

	@Override
	protected void fireResourcesChangedEvent(ChangeSet changeSet, IPath[] allAffectedResources) {
		super.fireResourcesChangedEvent(changeSet, allAffectedResources);
		try {
			beginInput();
			changes.changed(changeSet, allAffectedResources);
		} finally {
			endInput(null);
		}
	}

	@Override
	protected void initializeSets() {
		// Nothing to do
	}
}
