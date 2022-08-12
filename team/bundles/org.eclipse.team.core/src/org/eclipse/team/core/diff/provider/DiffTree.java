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
package org.eclipse.team.core.diff.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeListener;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.mapping.DiffChangeEvent;
import org.eclipse.team.internal.core.mapping.PathTree;
import org.eclipse.team.internal.core.subscribers.DiffTreeStatistics;

/**
 * Implementation of {@link IDiffTree}.
 *
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients. Clients can
 *           instead use {@link DiffTree}.
 */
public class DiffTree implements IDiffTree {

	/**
	 * Constant that indicates the start of the property value
	 * range that clients can use when storing properties in this tree.
	 */
	public static final int START_CLIENT_PROPERTY_RANGE = 1024;

	private ListenerList<IDiffChangeListener> listeners = new ListenerList<>();

	private PathTree pathTree = new PathTree();

	private ILock lock = Job.getJobManager().newLock();

	private DiffTreeStatistics statistics = new DiffTreeStatistics();

	private DiffChangeEvent changes;

	private  boolean lockedForModification;

	private Map<Integer, Set<IPath>> propertyChanges = new HashMap<>();

	/**
	 * Create an empty diff tree.
	 */
	public DiffTree() {
		resetChanges();
	}

	@Override
	public void addDiffChangeListener(IDiffChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeDiffChangeListener(IDiffChangeListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void accept(IPath path, IDiffVisitor visitor, int depth) {
		IDiff delta = getDiff(path);
		if (delta == null || visitor.visit(delta)) {
			if (depth == IResource.DEPTH_ZERO)
				return;
			IPath[] children = getChildren(path);
			for (IPath child : children) {
				accept(child, visitor, depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE);
			}
		}
	}

	@Override
	public IDiff getDiff(IPath path) {
		return (IDiff)pathTree.get(path);
	}

	@Override
	public IPath[] getChildren(IPath path) {
		return pathTree.getChildren(path);
	}

	@Override
	public boolean isEmpty() {
		return pathTree.isEmpty();
	}

	/**
	 * Add the given {@link IDiff} to the tree. A change event will
	 * be generated unless the call to this method is nested in between calls
	 * to <code>beginInput()</code> and <code>endInput(IProgressMonitor)</code>
	 * in which case the event for this addition and any other sync set
	 * change will be fired in a batched event when <code>endInput</code>
	 * is invoked.
	 * <p>
	 * Invoking this method outside of the above mentioned block will result
	 * in the <code>endInput(IProgressMonitor)</code> being invoked with a null
	 * progress monitor. If responsiveness is required, the client should always
	 * nest sync set modifications within <code>beginInput/endInput</code>.
	 * </p>
	 * @param delta the delta to be added to this set.
	 */
	public void add(IDiff delta) {
		try {
			beginInput();
			IDiff oldDiff = getDiff(delta.getPath());
			internalAdd(delta);
			if (oldDiff != null) {
				internalChanged(delta);
			} else {
				internalAdded(delta);
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * Remove the given local resource from the set. A change event will
	 * be generated unless the call to this method is nested in between calls
	 * to <code>beginInput()</code> and <code>endInput(IProgressMonitor)</code>
	 * in which case the event for this removal and any other sync set
	 * change will be fired in a batched event when <code>endInput</code>
	 * is invoked.
	 * <p>
	 * Invoking this method outside of the above mentioned block will result
	 * in the <code>endInput(IProgressMonitor)</code> being invoked with a null
	 * progress monitor. If responsiveness is required, the client should always
	 * nest sync set modifications within <code>beginInput/endInput</code>.
	 * </p>
	 *
	 * @param path the path to remove
	 */
	public void remove(IPath path) {
		try {
			beginInput();
			IDiff delta = getDiff(path);
			if (delta != null) {
				internalRemove(delta);
				internalRemoved(path, delta);
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * Clear the contents of the set
	 */
	public void clear() {
		try {
			beginInput();
			pathTree.clear();
			statistics.clear();
			internalReset();
		} finally {
			endInput(null);
		}
	}

	/**
	 * This method is used to obtain a lock on the set which ensures thread safety
	 * and batches change notification. If the set is locked by another thread, the
	 * calling thread will block until the lock becomes available. This method uses
	 * an <code>org.eclipse.core.runtime.jobs.ILock</code>.
	 * <p>
	 * It is important that the lock is released after it is obtained. Calls to
	 * <code>endInput</code> should be done in a finally block as illustrated in the
	 * following code snippet.
	 * </p>
	 *
	 * <pre>
	 * try {
	 * 	set.beginInput();
	 * 	// do stuff
	 * } finally {
	 * 	set.endInput(progress);
	 * }
	 * </pre>
	 * <p>
	 * Calls to <code>beginInput</code> and <code>endInput</code> can be nested and
	 * must be matched.
	 * </p>
	 */
	public void beginInput() {
		lock.acquire();
	}

	/**
	 * This method is used to release the lock on this set. The progress monitor is needed to allow
	 * listeners to perform long-running operations is response to the set change. The lock is held
	 * while the listeners are notified so listeners must be cautious in order to avoid deadlock.
	 * @param monitor a progress monitor
	 * @see #beginInput()
	 */
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

		final DiffChangeEvent event = getChangeEvent();
		resetChanges();
		final Map<Integer, Set<IPath>> propertyChanges = this.propertyChanges;
		this.propertyChanges = new HashMap<>();

		if(event.isEmpty() && ! event.isReset() && propertyChanges.isEmpty()) return;
		Object[] listeners = this.listeners.getListeners();
		for (Object l : listeners) {
			final IDiffChangeListener listener = (IDiffChangeListener) l;
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// don't log the exception....it is already being logged in Platform#run
				}
				@Override
				public void run() throws Exception {
					try {
						lockedForModification = true;
						if (!event.isEmpty() || event.isReset())
							listener.diffsChanged(event, Policy.subMonitorFor(monitor, 100));
						for (Integer key : propertyChanges.keySet()) {
							Set<IPath> paths = propertyChanges.get(key);
							listener.propertyChanged(DiffTree.this, key.intValue(), paths.toArray(new IPath[paths
									.size()]));
						}

					} finally {
						lockedForModification = false;
					}
				}
			});
		}
		monitor.done();
	}

	private DiffChangeEvent getChangeEvent() {
		return changes;
	}

	private void resetChanges() {
		changes = createEmptyChangeEvent();
	}

	private DiffChangeEvent createEmptyChangeEvent() {
		return new DiffChangeEvent(this);
	}

	private void internalAdd(IDiff delta) {
		Assert.isTrue(!lockedForModification);
		IDiff oldDiff = (IDiff)pathTree.get(delta.getPath());
		pathTree.put(delta.getPath(), delta);
		if(oldDiff == null) {
			statistics.add(delta);
		} else {
			statistics.remove(oldDiff);
			statistics.add(delta);
		}
		boolean isConflict = false;
		if (delta instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) delta;
			isConflict = twd.getDirection() == IThreeWayDiff.CONFLICTING;
		}
		setPropertyToRoot(delta, P_HAS_DESCENDANT_CONFLICTS, isConflict);
	}

	private void internalRemove(IDiff delta) {
		Assert.isTrue(!lockedForModification);
		statistics.remove(delta);
		setPropertyToRoot(delta, P_HAS_DESCENDANT_CONFLICTS, false);
		setPropertyToRoot(delta, P_BUSY_HINT, false);
		pathTree.remove(delta.getPath());
	}

	private void internalAdded(IDiff delta) {
		changes.added(delta);
	}

	private void internalChanged(IDiff delta) {
		changes.changed(delta);
	}
	private void internalRemoved(IPath path, IDiff delta) {
		changes.removed(path, delta);
	}

	private void internalReset() {
		changes.reset();
	}

	/**
	 * Return the paths in this tree that contain diffs.
	 * @return the paths in this tree that contain diffs.
	 */
	public IPath[] getPaths() {
		return pathTree.getPaths();
	}

	/**
	 * Return all the diffs contained in this diff tree.
	 * @return all the diffs contained in this diff tree
	 */
	public IDiff[] getDiffs() {
		return pathTree.values().toArray(new IDiff[pathTree.size()]);
	}

	@Override
	public long countFor(int state, int mask) {
		if (state == 0)
			return size();
		return statistics.countFor(state, mask);
	}

	@Override
	public int size() {
		return pathTree.size();
	}

	public void setPropertyToRoot(IDiff node, int property, boolean value) {
		try {
			beginInput();
			IPath[] paths = pathTree.setPropogatedProperty(node.getPath(), property, value);
			accumulatePropertyChanges(property, paths);
		} finally {
			endInput(null);
		}
	}

	private void accumulatePropertyChanges(int property, IPath[] paths) {
		Integer key = Integer.valueOf(property);
		Set<IPath> changes = propertyChanges.get(key);
		if (changes == null) {
			changes = new HashSet<>();
			propertyChanges.put(key, changes);
		}
		Collections.addAll(changes, paths);
	}

	@Override
	public boolean getProperty(IPath path, int property) {
		return pathTree.getProperty(path, property);
	}

	@Override
	public void setBusy(IDiff[] diffs, IProgressMonitor monitor) {
		try {
			beginInput();
			for (IDiff node : diffs) {
				setPropertyToRoot(node, P_BUSY_HINT, true);
			}
		} finally {
			endInput(monitor);
		}
	}

	@Override
	public void clearBusy(IProgressMonitor monitor) {
		try {
			beginInput();
			IPath[] paths = pathTree.getPaths();
			for (IPath path : paths) {
				IPath[] changed = pathTree.setPropogatedProperty(path, P_BUSY_HINT, false);
				accumulatePropertyChanges(P_BUSY_HINT, changed);
			}
		} finally {
			endInput(monitor);
		}
	}

	@Override
	public boolean hasMatchingDiffs(IPath path, final FastDiffFilter filter) {
		final RuntimeException found = new RuntimeException();
		try {
			accept(path, delta -> {
				if (filter.select(delta)) {
					throw found;
				}
				return false;
			}, IResource.DEPTH_INFINITE);
		} catch (RuntimeException e) {
			if (e == found)
				return true;
			throw e;
		}
		return false;
	}

	/**
	 * Report to any listeners that an error has occurred while populating the
	 * set. Listeners will be notified that an error occurred and can react
	 * accordingly.
	 *
	 * @param status
	 *            the status that describes the error that occurred.
	 */
	public void reportError(IStatus status) {
		try {
			beginInput();
			getChangeEvent().errorOccurred(status);
		} finally {
			endInput(null);
		}
	}
}
