/*******************************************************************************
 * Copyright (c) 2009, 2018 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Dorin Ciuca - Top index fix (Bug 324100)
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.viewers.model.ElementCompareRequest;
import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.ILabelUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStateUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerFilter;
import org.junit.Assert;

public class TestModelUpdatesListener implements IViewerUpdateListener, ILabelUpdateListener, IModelChangedListener, ITestModelUpdatesListenerConstants, IStateUpdateListener, IJobChangeListener {
	public static final ViewerFilter[] EMPTY_FILTER_ARRAY = new ViewerFilter[0];

	private final ITreeModelViewer fViewer;

	private IStatus fJobError;

	private boolean fFailOnRedundantUpdates;
	private boolean fFailOnRedundantLabelUpdates;
	private Set<IViewerUpdate> fRedundantUpdates = new HashSet<>();
	private Set<ILabelUpdate> fRedundantLabelUpdates = new HashSet<>();
	private Set<TreePath> fRedundantHasChildrenUpdateExceptions = new HashSet<>();
	private Set<TreePath> fRedundantChildCountUpdateExceptions = new HashSet<>();
	private Set<TreePath> fRedundantChildrenUpdateExceptions = new HashSet<>();
	private Set<TreePath> fRedundantLabelUpdateExceptions = new HashSet<>();

	private boolean fFailOnMultipleModelUpdateSequences;
	private boolean fFailOnMultipleLabelUpdateSequences;

	private Set<TreePath> fHasChildrenUpdatesScheduled = new HashSet<>();
	private Set<IViewerUpdate> fHasChildrenUpdatesRunning = new HashSet<>();
	private Set<IViewerUpdate> fHasChildrenUpdatesCompleted = new HashSet<>();
	private Map<TreePath, Set<Integer>> fChildrenUpdatesScheduled = new HashMap<>();
	private Set<IViewerUpdate> fChildrenUpdatesRunning = new HashSet<>();
	private Set<IViewerUpdate> fChildrenUpdatesCompleted = new HashSet<>();
	private Set<TreePath> fChildCountUpdatesScheduled = new HashSet<>();
	private Set<IViewerUpdate> fChildCountUpdatesRunning = new HashSet<>();
	private Set<IViewerUpdate> fChildCountUpdatesCompleted = new HashSet<>();
	private Set<TreePath> fLabelUpdates = new HashSet<>();
	private Set<ILabelUpdate> fLabelUpdatesRunning = new HashSet<>();
	private Set<ILabelUpdate> fLabelUpdatesCompleted = new HashSet<>();
	private Set<TestModel> fProxyModels = new HashSet<>();
	private Set<TreePath> fStateUpdates = new HashSet<>();
	private int fViewerUpdatesStarted = 0;
	private int fViewerUpdatesComplete = 0;
	private int fViewerUpdatesStartedAtReset;
	private int fViewerUpdatesCompleteAtReset;
	private int fLabelUpdatesStarted = 0;
	private int fLabelUpdatesComplete = 0;
	private int fLabelUpdatesStartedAtReset;
	private int fLabelUpdatesCompleteAtReset;
	private boolean fModelChangedComplete;
	private boolean fStateSaveStarted;
	private boolean fStateSaveComplete;
	private boolean fStateRestoreStarted;
	private boolean fStateRestoreComplete;
	private int fViewerUpdatesCounter;
	private int fLabelUpdatesCounter;
	private int fTimeoutInterval = 60000;
	private long fTimeoutTime;

	private boolean fExpectRestoreAfterSaveComplete;

	private RuntimeException fFailExpectation;

	public TestModelUpdatesListener(ITreeModelViewer viewer, boolean failOnRedundantUpdates, boolean failOnMultipleModelUpdateSequences) {
		this(viewer);
		setFailOnRedundantUpdates(failOnRedundantUpdates);
		setFailOnMultipleModelUpdateSequences(failOnMultipleModelUpdateSequences);
	}

	public TestModelUpdatesListener(ITreeModelViewer viewer) {
		fViewer = viewer;
		Job.getJobManager().addJobChangeListener(this);
		fViewer.addLabelUpdateListener(this);
		fViewer.addModelChangedListener(this);
		fViewer.addStateUpdateListener(this);
		fViewer.addViewerUpdateListener(this);
	}

	public void dispose() {
		Job.getJobManager().removeJobChangeListener(this);
		fViewer.removeLabelUpdateListener(this);
		fViewer.removeModelChangedListener(this);
		fViewer.removeStateUpdateListener(this);
		fViewer.removeViewerUpdateListener(this);
	}

	@Override
	public void aboutToRun(IJobChangeEvent event) {
	}

	@Override
	public void awake(IJobChangeEvent event) {
	}

	@Override
	public void running(IJobChangeEvent event) {
	}

	@Override
	public void scheduled(IJobChangeEvent event) {
	}

	@Override
	public void sleeping(IJobChangeEvent event) {
	}

	@Override
	public void done(IJobChangeEvent event) {
		IStatus result = event.getJob().getResult();
		if (result != null && result.getSeverity() == IStatus.ERROR) {
			fJobError = result;
		}
	}

	public void setFailOnRedundantUpdates(boolean failOnRedundantUpdates) {
		fFailOnRedundantUpdates = failOnRedundantUpdates;
	}

	public void setFailOnRedundantLabelUpdates(boolean failOnRedundantLabelUpdates) {
		fFailOnRedundantLabelUpdates = failOnRedundantLabelUpdates;
	}

	public void setFailOnMultipleModelUpdateSequences(boolean failOnMultipleLabelUpdateSequences) {
		fFailOnMultipleModelUpdateSequences = failOnMultipleLabelUpdateSequences;
	}

	public void setFailOnMultipleLabelUpdateSequences(boolean failOnMultipleLabelUpdateSequences) {
		fFailOnMultipleLabelUpdateSequences = failOnMultipleLabelUpdateSequences;
	}

	public void expectRestoreAfterSaveComplete() {
		fExpectRestoreAfterSaveComplete = true;
	}

	/**
	 * Sets the the maximum amount of time (in milliseconds) that the update
	 * listener is going to wait. If set to -1, the listener will wait
	 * indefinitely.
	 */
	public void setTimeoutInterval(int milis) {
		fTimeoutInterval = milis;
	}

	public void reset(TreePath path, TestElement element, int levels, boolean failOnRedundantUpdates, boolean failOnMultipleUpdateSequences) {
		reset(path, element, EMPTY_FILTER_ARRAY, levels, failOnRedundantUpdates, failOnMultipleUpdateSequences);
	}

	public void reset(TreePath path, TestElement element, ViewerFilter[] filters, int levels, boolean failOnRedundantUpdates, boolean failOnMultipleUpdateSequences) {
		reset();
		addUpdates(path, element, filters, levels);
		addProxies(element);
		setFailOnRedundantUpdates(failOnRedundantUpdates);
		setFailOnMultipleModelUpdateSequences(failOnMultipleUpdateSequences);
		setFailOnMultipleLabelUpdateSequences(false);
	}

	public void reset(boolean failOnRedundantUpdates, boolean failOnMultipleUpdateSequences) {
		reset();
		setFailOnRedundantUpdates(failOnRedundantUpdates);
		setFailOnMultipleModelUpdateSequences(failOnMultipleUpdateSequences);
		setFailOnMultipleLabelUpdateSequences(false);
	}

	public void reset() {
		fJobError = null;
		fFailExpectation = null;
		fRedundantUpdates.clear();
		fRedundantLabelUpdates.clear();
		fRedundantHasChildrenUpdateExceptions.clear();
		fRedundantChildCountUpdateExceptions.clear();
		fRedundantChildrenUpdateExceptions.clear();
		fRedundantLabelUpdateExceptions.clear();
		fHasChildrenUpdatesScheduled.clear();
		fHasChildrenUpdatesRunning.clear();
		fHasChildrenUpdatesCompleted.clear();
		fChildrenUpdatesScheduled.clear();
		fChildrenUpdatesRunning.clear();
		fChildrenUpdatesCompleted.clear();
		fChildCountUpdatesScheduled.clear();
		fChildCountUpdatesRunning.clear();
		fChildCountUpdatesCompleted.clear();
		fLabelUpdates.clear();
		fLabelUpdatesRunning.clear();
		fLabelUpdatesCompleted.clear();
		fProxyModels.clear();
		fViewerUpdatesStartedAtReset = fViewerUpdatesStarted;
		fViewerUpdatesCompleteAtReset = fViewerUpdatesComplete;
		fLabelUpdatesStartedAtReset = fLabelUpdatesStarted;
		fLabelUpdatesCompleteAtReset = fLabelUpdatesComplete;
		fStateUpdates.clear();
		fStateSaveStarted = false;
		fStateSaveComplete = false;
		fStateRestoreStarted = false;
		fStateRestoreComplete = false;
		fExpectRestoreAfterSaveComplete = false;
		fTimeoutTime = System.currentTimeMillis() + fTimeoutInterval;
		ILog.of(TestModelUpdatesListener.class).log(Status.info("fTimeOut Reset: " + fTimeoutTime)); //$NON-NLS-1$
		resetModelChanged();
	}

	public void resetModelChanged() {
		fModelChangedComplete = false;
	}

	public void addHasChildrenUpdate(TreePath path) {
		fHasChildrenUpdatesScheduled.add(path);
	}

	public void removeHasChildrenUpdate(TreePath path) {
		fHasChildrenUpdatesScheduled.remove(path);
	}

	public void addChildreCountUpdate(TreePath path) {
		fChildCountUpdatesScheduled.add(path);
	}

	public void removeChildreCountUpdate(TreePath path) {
		fChildCountUpdatesScheduled.remove(path);
	}

	public void addChildreUpdate(TreePath path, int index) {
		Set<Integer> childrenIndexes = fChildrenUpdatesScheduled.get(path);
		if (childrenIndexes == null) {
			childrenIndexes = new TreeSet<>();
			fChildrenUpdatesScheduled.put(path, childrenIndexes);
		}
		childrenIndexes.add(Integer.valueOf(index));
	}

	public void removeChildrenUpdate(TreePath path, int index) {
		Set<?> childrenIndexes = fChildrenUpdatesScheduled.get(path);
		if (childrenIndexes != null) {
			childrenIndexes.remove(Integer.valueOf(index));
			if (childrenIndexes.isEmpty()) {
				fChildrenUpdatesScheduled.remove(path);
			}
		}
	}

	public void addLabelUpdate(TreePath path) {
		fLabelUpdates.add(path);
	}

	public void removeLabelUpdate(TreePath path) {
		fLabelUpdates.remove(path);
	}

	public void addUpdates(TreePath path, TestElement element, int levels) {
		addUpdates(null, path, element, EMPTY_FILTER_ARRAY, levels, ALL_UPDATES_COMPLETE);
	}

	public void addUpdates(TreePath path, TestElement element, ViewerFilter[] filters, int levels) {
		addUpdates(null, path, element, filters, levels, ALL_UPDATES_COMPLETE);
	}

	public void addStateUpdates(IInternalTreeModelViewer viewer, TreePath path, TestElement element) {
		addUpdates(viewer, path, element, -1, STATE_UPDATES);
	}

	public void addStateUpdates(IInternalTreeModelViewer viewer, IModelDelta pendingDelta, int deltaFlags) {
		TreePath treePath = getViewerTreePath(pendingDelta);
		if (!TreePath.EMPTY.equals(treePath) && (pendingDelta.getFlags() & deltaFlags) != 0) {
			addUpdates(viewer, treePath, (TestElement) treePath.getLastSegment(), 0, STATE_UPDATES);
		}
		IModelDelta[] childDeltas = pendingDelta.getChildDeltas();
		for (IModelDelta childDelta : childDeltas) {
			addStateUpdates(viewer, childDelta, deltaFlags);
		}
	}

	public void addRedundantExceptionHasChildren(TreePath path) {
		fRedundantHasChildrenUpdateExceptions.add(path);
	}

	public void addRedundantExceptionChildCount(TreePath path) {
		fRedundantChildCountUpdateExceptions.add(path);
	}

	public void addRedundantExceptionChildren(TreePath path) {
		fRedundantChildrenUpdateExceptions.add(path);
	}

	public void addRedundantExceptionLabel(TreePath path) {
		fRedundantLabelUpdateExceptions.add(path);
	}

	public boolean checkCoalesced(TreePath path, int offset, int length) {
		for (Iterator<IViewerUpdate> itr = fChildrenUpdatesCompleted.iterator(); itr.hasNext();) {
			IChildrenUpdate update = (IChildrenUpdate) itr.next();
			if (path.equals(update.getElementPath()) && offset == update.getOffset() && length == update.getLength()) {
				return true;
			}
		}
		return false;
	}

	public Set<IViewerUpdate> getHasChildrenUpdatesCompleted() {
		return fHasChildrenUpdatesCompleted;
	}

	public Set<IViewerUpdate> getChildCountUpdatesCompleted() {
		return fChildCountUpdatesCompleted;
	}

	public Set<IViewerUpdate> getChildrenUpdatesCompleted() {
		return fChildrenUpdatesCompleted;
	}

	/**
	 * Returns a tree path for the node, *not* including the root element.
	 *
	 * @param node model delta
	 * @return corresponding tree path
	 */
	private TreePath getViewerTreePath(IModelDelta node) {
		ArrayList<Object> list = new ArrayList<>();
		IModelDelta parentDelta = node.getParentDelta();
		while (parentDelta != null) {
			list.add(0, node.getElement());
			node = parentDelta;
			parentDelta = node.getParentDelta();
		}
		return new TreePath(list.toArray());
	}

	public void addUpdates(TreePath path, TestElement element, int levels, int flags) {
		addUpdates(null, path, element, levels, flags);
	}

	public void addUpdates(IInternalTreeModelViewer viewer, TreePath path, TestElement element, int levels, int flags) {
		addUpdates(viewer, path, element, EMPTY_FILTER_ARRAY, levels, flags);
	}

	public static boolean isFiltered(Object element, ViewerFilter[] filters) {
		for (ViewerFilter filter : filters) {
			if (!filter.select(null, null, element)) {
				return true;
			}
		}
		return false;
	}

	public void addUpdates(IInternalTreeModelViewer viewer, TreePath path, TestElement element, ViewerFilter[] filters, int levels, int flags) {
		if (isFiltered(path.getLastSegment(), filters)) {
			return;
		}

		if (!path.equals(TreePath.EMPTY)) {
			if ((flags & LABEL_UPDATES) != 0) {
				fLabelUpdates.add(path);
			}
			if ((flags & HAS_CHILDREN_UPDATES) != 0) {
				fHasChildrenUpdatesScheduled.add(path);
			}

			if ((flags & STATE_UPDATES) != 0) {
				fStateUpdates.add(path);
			}
		}

		if (levels-- != 0) {
			TestElement[] children = element.getChildren();
			if (children.length > 0 && (viewer == null || path.getSegmentCount() == 0 || viewer.getExpandedState(path))) {
				if ((flags & CHILD_COUNT_UPDATES) != 0) {
					fChildCountUpdatesScheduled.add(path);
				}
				if ((flags & CHILDREN_UPDATES) != 0) {
					Set<Integer> childrenIndexes = new HashSet<>();
					for (int i = 0; i < children.length; i++) {
						if (!isFiltered(children[i], filters)) {
							childrenIndexes.add(Integer.valueOf(i));
						}
					}
					fChildrenUpdatesScheduled.put(path, childrenIndexes);
				}

				for (TestElement child : children) {
					addUpdates(viewer, path.createChildPath(child), child, filters, levels, flags);
				}
			}

		}
	}

	private void addProxies(TestElement element) {
		TestModel model = element.getModel();
		if (model.getModelProxy() == null) {
			fProxyModels.add(element.getModel());
		}
		TestElement[] children = element.getChildren();
		for (TestElement child : children) {
			addProxies(child);
		}
	}

	public boolean isFinished() {
		return isFinished(ALL_UPDATES_COMPLETE);
	}

	public boolean isTimedOut() {
		return fTimeoutInterval > 0 && fTimeoutTime < System.currentTimeMillis();
	}

	public boolean isFinished(int flags) {
		if (isTimedOut()) {
			throw new RuntimeException("Timed Out: " + toString(flags)); //$NON-NLS-1$
		}

		if (fFailExpectation != null) {
			throw fFailExpectation;
		}

		if (fJobError != null) {
			throw new RuntimeException("Job Error: " + fJobError); //$NON-NLS-1$
		}

		if (fFailOnRedundantUpdates && !fRedundantUpdates.isEmpty()) {
			Assert.fail("Redundant Updates: " + fRedundantUpdates); //$NON-NLS-1$
		}
		if (fFailOnRedundantLabelUpdates && !fRedundantLabelUpdates.isEmpty()) {
			Assert.fail("Redundant Label Updates: " + fRedundantLabelUpdates); //$NON-NLS-1$
		}
		if (fFailOnMultipleLabelUpdateSequences && fLabelUpdatesComplete > (fLabelUpdatesCompleteAtReset + 1)) {
			Assert.fail("Multiple label update sequences detected"); //$NON-NLS-1$
		}
		if (fFailOnMultipleModelUpdateSequences && fViewerUpdatesComplete > (fViewerUpdatesCompleteAtReset + 1)) {
			Assert.fail("Multiple viewer update sequences detected"); //$NON-NLS-1$
		}

		if ((flags & LABEL_SEQUENCE_COMPLETE) != 0) {
			if (fLabelUpdatesComplete == fLabelUpdatesCompleteAtReset || fLabelUpdatesComplete != fLabelUpdatesStarted) {
				return false;
			}
		}
		if ((flags & LABEL_SEQUENCE_STARTED) != 0) {
			if (fLabelUpdatesStarted == fLabelUpdatesStartedAtReset) {
				return false;
			}
		}
		if ((flags & LABEL_UPDATES) != 0) {
			if (!fLabelUpdates.isEmpty()) {
				return false;
			}
		}
		if ((flags & CONTENT_SEQUENCE_STARTED) != 0) {
			if (fViewerUpdatesStarted == fViewerUpdatesStartedAtReset) {
				return false;
			}
		}
		if ((flags & CONTENT_SEQUENCE_COMPLETE) != 0) {
			if (fViewerUpdatesComplete == fViewerUpdatesCompleteAtReset || fViewerUpdatesStarted != fViewerUpdatesComplete) {
				return false;
			}
		}
		if ((flags & HAS_CHILDREN_UPDATES_STARTED) != 0) {
			if (fHasChildrenUpdatesRunning.isEmpty() && fHasChildrenUpdatesCompleted.isEmpty()) {
				return false;
			}
		}
		if ((flags & HAS_CHILDREN_UPDATES) != 0) {
			if (!fHasChildrenUpdatesScheduled.isEmpty()) {
				return false;
			}
		}
		if ((flags & CHILD_COUNT_UPDATES_STARTED) != 0) {
			if (fChildCountUpdatesRunning.isEmpty() && fChildCountUpdatesCompleted.isEmpty()) {
				return false;
			}
		}
		if ((flags & CHILD_COUNT_UPDATES) != 0) {
			if (!fChildCountUpdatesScheduled.isEmpty()) {
				return false;
			}
		}
		if ((flags & CHILDREN_UPDATES_STARTED) != 0) {
			// Some children updates have already been started or completed.
			if (fChildrenUpdatesRunning.isEmpty() && fChildrenUpdatesCompleted.isEmpty()) {
				return false;
			}
		}
		if ((flags & CHILDREN_UPDATES_RUNNING) != 0) {
			if (!isFinishedChildrenRunning()) {
				return false;
			}
		}
		if ((flags & CHILDREN_UPDATES) != 0) {
			if (!fChildrenUpdatesScheduled.isEmpty()) {
				return false;
			}
		}
		if ((flags & MODEL_CHANGED_COMPLETE) != 0) {
			if (!fModelChangedComplete) {
				return false;
			}
		}
		if ((flags & STATE_SAVE_COMPLETE) != 0) {
			if (!fStateSaveComplete) {
				return false;
			}
		}
		if ((flags & STATE_SAVE_STARTED) != 0) {
			if (!fStateSaveStarted) {
				return false;
			}
		}
		if ((flags & STATE_RESTORE_COMPLETE) != 0) {
			if (!fStateRestoreComplete) {
				return false;
			}
		}
		if ((flags & STATE_RESTORE_STARTED) != 0) {
			if (!fStateRestoreStarted) {
				return false;
			}
		}
		if ((flags & STATE_UPDATES) != 0) {
			if (!fStateUpdates.isEmpty()) {
				return false;
			}
		}
		if ((flags & MODEL_PROXIES_INSTALLED) != 0) {
			if (!fProxyModels.isEmpty()) {
				return false;
			}
		}
		if ((flags & VIEWER_UPDATES_RUNNING) != 0) {
			if (fViewerUpdatesCounter != 0) {
				return false;
			}
		}
		if ((flags & LABEL_UPDATES_RUNNING) != 0) {
			if (fLabelUpdatesCounter != 0) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns true if all children updates that were scheduled are either
	 * currently running or have already completed.
	 *
	 * @see CHILDREN_UPDATES_RUNNING
	 * @return
	 */
	private boolean isFinishedChildrenRunning() {
		// All children updates that have been scheduled are either running or
		// completed.
		int scheduledCount = 0;
		for (Iterator<Set<Integer>> itr = fChildrenUpdatesScheduled.values().iterator(); itr.hasNext();) {
			scheduledCount += ((Set<?>) itr.next()).size();
		}

		int runningCount = 0;
		for (Iterator<IViewerUpdate> itr = fChildrenUpdatesRunning.iterator(); itr.hasNext();) {
			IChildrenUpdate update = ((IChildrenUpdate) itr.next());
			Set<?> set = fChildrenUpdatesScheduled.get(update.getElementPath());
			for (int i = update.getOffset(); set != null && i < update.getOffset() + update.getLength(); i++) {
				if (set.contains(Integer.valueOf(i))) {
					runningCount++;
				}
			}
		}
		for (Iterator<IViewerUpdate> itr = fChildrenUpdatesCompleted.iterator(); itr.hasNext();) {
			IChildrenUpdate update = ((IChildrenUpdate) itr.next());
			Set<?> set = fChildrenUpdatesScheduled.get(update.getElementPath());
			for (int i = update.getOffset(); set != null && i < update.getOffset() + update.getLength(); i++) {
				if (set.contains(Integer.valueOf(i))) {
					runningCount++;
				}
			}
		}
		return scheduledCount == runningCount;
	}

	@Override
	public void updateStarted(IViewerUpdate update) {
		synchronized (this) {
			fViewerUpdatesCounter++;
			if (update instanceof IHasChildrenUpdate) {
				fHasChildrenUpdatesRunning.add(update);
			}
			if (update instanceof IChildrenCountUpdate) {
				fChildCountUpdatesRunning.add(update);
			} else if (update instanceof IChildrenUpdate) {
				fChildrenUpdatesRunning.add(update);
			}
		}
	}

	@Override
	public void updateComplete(IViewerUpdate update) {
		synchronized (this) {
			fViewerUpdatesCounter--;
		}

		if (!update.isCanceled()) {
			TreePath updatePath = update.getElementPath();
			if (update instanceof IHasChildrenUpdate) {
				fHasChildrenUpdatesRunning.remove(update);
				fHasChildrenUpdatesCompleted.add(update);
				if (!fHasChildrenUpdatesScheduled.remove(updatePath) && fFailOnRedundantUpdates && fRedundantHasChildrenUpdateExceptions.contains(updatePath)) {
					fRedundantUpdates.add(update);
				}
			}
			if (update instanceof IChildrenCountUpdate) {
				fChildCountUpdatesRunning.remove(update);
				fChildCountUpdatesCompleted.add(update);
				if (!fChildCountUpdatesScheduled.remove(updatePath) && fFailOnRedundantUpdates && !fRedundantChildCountUpdateExceptions.contains(updatePath)) {
					fRedundantUpdates.add(update);
				}
			} else if (update instanceof IChildrenUpdate) {
				fChildrenUpdatesRunning.remove(update);
				fChildrenUpdatesCompleted.add(update);

				int start = ((IChildrenUpdate) update).getOffset();
				int end = start + ((IChildrenUpdate) update).getLength();

				Set<?> childrenIndexes = fChildrenUpdatesScheduled.get(updatePath);
				if (childrenIndexes != null) {
					for (int i = start; i < end; i++) {
						childrenIndexes.remove(Integer.valueOf(i));
					}
					if (childrenIndexes.isEmpty()) {
						fChildrenUpdatesScheduled.remove(updatePath);
					}
				} else if (fFailOnRedundantUpdates && fRedundantChildrenUpdateExceptions.contains(updatePath)) {
					fRedundantUpdates.add(update);
				}
			}
		}
	}

	@Override
	public void viewerUpdatesBegin() {
		if (fViewerUpdatesStarted > fViewerUpdatesComplete) {
			fFailExpectation = new RuntimeException("Unmatched updatesStarted/updateCompleted notifications observed."); //$NON-NLS-1$
		}
		fViewerUpdatesStarted++;
	}

	@Override
	public void viewerUpdatesComplete() {
		if (fViewerUpdatesStarted <= fViewerUpdatesComplete) {
			fFailExpectation = new RuntimeException("Unmatched updatesStarted/updateCompleted notifications observed."); //$NON-NLS-1$
		}
		fViewerUpdatesComplete++;
	}

	@Override
	public void labelUpdateComplete(ILabelUpdate update) {
		fLabelUpdatesRunning.remove(update);
		fLabelUpdatesCompleted.add(update);
		fLabelUpdatesCounter--;
		if (!fLabelUpdates.remove(update.getElementPath()) && fFailOnRedundantLabelUpdates && !fRedundantLabelUpdateExceptions.contains(update.getElementPath())) {
			fRedundantLabelUpdates.add(update);
			Assert.fail("Redundant update: " + update); //$NON-NLS-1$
		}
	}

	@Override
	public void labelUpdateStarted(ILabelUpdate update) {
		fLabelUpdatesRunning.add(update);
		fLabelUpdatesCounter++;
	}

	@Override
	public void labelUpdatesBegin() {
		if (fLabelUpdatesStarted > fLabelUpdatesComplete) {
			fFailExpectation = new RuntimeException("Unmatched labelUpdatesStarted/labelUpdateCompleted notifications observed."); //$NON-NLS-1$
		}
		fLabelUpdatesStarted++;
	}

	@Override
	public void labelUpdatesComplete() {
		if (fLabelUpdatesStarted <= fLabelUpdatesComplete) {
			fFailExpectation = new RuntimeException("Unmatched labelUpdatesStarted/labelUpdateCompleted notifications observed."); //$NON-NLS-1$
		}
		fLabelUpdatesComplete++;
	}

	@Override
	public void modelChanged(IModelDelta delta, IModelProxy proxy) {
		fModelChangedComplete = true;

		for (Iterator<TestModel> itr = fProxyModels.iterator(); itr.hasNext();) {
			TestModel model = itr.next();
			if (model.getModelProxy() == proxy) {
				itr.remove();
				break;
			}
		}
	}

	@Override
	public void stateRestoreUpdatesBegin(Object input) {
		if (fExpectRestoreAfterSaveComplete && !fStateSaveComplete) {
			fFailExpectation = new RuntimeException("RESTORE should begin after SAVE completed!"); //$NON-NLS-1$
		}
		fStateRestoreStarted = true;
	}

	@Override
	public void stateRestoreUpdatesComplete(Object input) {
		Assert.assertFalse("RESTORE STATE already complete!", fStateRestoreComplete); //$NON-NLS-1$
		fStateRestoreComplete = true;
	}

	@Override
	public void stateSaveUpdatesBegin(Object input) {
		fStateSaveStarted = true;
	}

	@Override
	public void stateSaveUpdatesComplete(Object input) {
		fStateSaveComplete = true;
	}

	@Override
	public void stateUpdateComplete(Object input, IViewerUpdate update) {
		if (!(update instanceof ElementCompareRequest) || ((ElementCompareRequest) update).isEqual()) {
			fStateUpdates.remove(update.getElementPath());
		}
	}

	@Override
	public void stateUpdateStarted(Object input, IViewerUpdate update) {
	}

	private String toString(int flags) {
		StringBuilder buf = new StringBuilder("Viewer Update Listener"); //$NON-NLS-1$

		if (fJobError != null) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fJobError = " + fJobError); //$NON-NLS-1$
			if (fJobError.getException() != null) {
				StackTraceElement[] trace = fJobError.getException().getStackTrace();
				for (StackTraceElement t : trace) {
					buf.append("\n\t\t"); //$NON-NLS-1$
					buf.append(t);
				}
			}
		}

		if (fFailOnRedundantUpdates) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fRedundantUpdates = " + fRedundantUpdates); //$NON-NLS-1$
		}
		if ((flags & LABEL_SEQUENCE_COMPLETE) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fLabelUpdatesComplete = " + fLabelUpdatesComplete); //$NON-NLS-1$
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fLabelUpdatesCompleteAtReset = "); //$NON-NLS-1$
			buf.append(fLabelUpdatesCompleteAtReset);
		}
		if ((flags & LABEL_UPDATES_RUNNING) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fLabelUpdatesRunning = " + fLabelUpdatesCounter); //$NON-NLS-1$
		}
		if ((flags & LABEL_SEQUENCE_STARTED) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fLabelUpdatesStarted = "); //$NON-NLS-1$
			buf.append(fLabelUpdatesStarted);
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fLabelUpdatesCompleted = "); //$NON-NLS-1$
			buf.append(fLabelUpdatesCompleted);
		}
		if ((flags & LABEL_UPDATES) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fLabelUpdates = "); //$NON-NLS-1$
			buf.append(toString(fLabelUpdates));
		}
		if ((flags & VIEWER_UPDATES_RUNNING) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fViewerUpdatesStarted = " + fViewerUpdatesStarted); //$NON-NLS-1$
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fViewerUpdatesRunning = " + fViewerUpdatesCounter); //$NON-NLS-1$
		}
		if ((flags & CONTENT_SEQUENCE_COMPLETE) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fViewerUpdatesComplete = " + fViewerUpdatesComplete); //$NON-NLS-1$
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fViewerUpdatesCompleteAtReset = " + fViewerUpdatesCompleteAtReset); //$NON-NLS-1$
		}
		if ((flags & HAS_CHILDREN_UPDATES_STARTED) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fHasChildrenUpdatesRunning = "); //$NON-NLS-1$
			buf.append(fHasChildrenUpdatesRunning);
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fHasChildrenUpdatesCompleted = "); //$NON-NLS-1$
			buf.append(fHasChildrenUpdatesCompleted);
		}
		if ((flags & HAS_CHILDREN_UPDATES) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fHasChildrenUpdates = "); //$NON-NLS-1$
			buf.append(toString(fHasChildrenUpdatesScheduled));
		}
		if ((flags & CHILD_COUNT_UPDATES_STARTED) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fChildCountUpdatesRunning = "); //$NON-NLS-1$
			buf.append(fChildCountUpdatesRunning);
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fChildCountUpdatesCompleted = "); //$NON-NLS-1$
			buf.append(fChildCountUpdatesCompleted);
		}
		if ((flags & CHILD_COUNT_UPDATES) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fChildCountUpdates = "); //$NON-NLS-1$
			buf.append(toString(fChildCountUpdatesScheduled));
		}
		if ((flags & CHILDREN_UPDATES_STARTED) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fChildrenUpdatesRunning = "); //$NON-NLS-1$
			buf.append(fChildrenUpdatesRunning);
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fChildrenUpdatesCompleted = "); //$NON-NLS-1$
			buf.append(fChildrenUpdatesCompleted);
		}
		if ((flags & CHILDREN_UPDATES) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fChildrenUpdates = "); //$NON-NLS-1$
			buf.append(toString(fChildrenUpdatesScheduled));
		}
		if ((flags & MODEL_CHANGED_COMPLETE) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fModelChangedComplete = " + fModelChangedComplete); //$NON-NLS-1$
		}
		if ((flags & STATE_SAVE_COMPLETE) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fStateSaveComplete = " + fStateSaveComplete); //$NON-NLS-1$
		}
		if ((flags & STATE_RESTORE_COMPLETE) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fStateRestoreComplete = " + fStateRestoreComplete); //$NON-NLS-1$
		}
		if ((flags & MODEL_PROXIES_INSTALLED) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fProxyModels = " + fProxyModels); //$NON-NLS-1$
		}
		if ((flags & STATE_UPDATES) != 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fStateUpdates = " + toString(fStateUpdates)); //$NON-NLS-1$
		}
		if (fTimeoutInterval > 0) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fTimeoutInterval = " + fTimeoutInterval); //$NON-NLS-1$
		}
		if (fTimeoutTime < System.currentTimeMillis()) {
			buf.append("\n\t"); //$NON-NLS-1$
			buf.append("fTimeoutTime = " + fTimeoutTime); //$NON-NLS-1$
			buf.append("Current Time = " + System.currentTimeMillis()); //$NON-NLS-1$
		}
		return buf.toString();
	}

	private String toString(Set<TreePath> set) {
		if (set.isEmpty()) {
			return "(EMPTY)"; //$NON-NLS-1$
		}
		StringBuilder buf = new StringBuilder();
		for (Iterator<TreePath> itr = set.iterator(); itr.hasNext();) {
			buf.append("\n\t\t"); //$NON-NLS-1$
			buf.append(toString(itr.next()));
		}
		return buf.toString();
	}

	private String toString(Map<TreePath, Set<Integer>> map) {
		if (map.isEmpty()) {
			return "(EMPTY)"; //$NON-NLS-1$
		}
		StringBuilder buf = new StringBuilder();
		for (Iterator<TreePath> itr = map.keySet().iterator(); itr.hasNext();) {
			buf.append("\n\t\t"); //$NON-NLS-1$
			TreePath path = itr.next();
			buf.append(toString(path));
			Set<?> updates = map.get(path);
			buf.append(" = "); //$NON-NLS-1$
			buf.append(updates.toString());
		}
		return buf.toString();
	}

	private String toString(TreePath path) {
		if (path.getSegmentCount() == 0) {
			return "/"; //$NON-NLS-1$
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < path.getSegmentCount(); i++) {
			buf.append("/"); //$NON-NLS-1$
			buf.append(path.getSegment(i));
		}
		return buf.toString();
	}

	@Override
	public String toString() {
		return toString(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE | STATE_SAVE_COMPLETE | STATE_RESTORE_COMPLETE | ALL_VIEWER_UPDATES_STARTED | LABEL_SEQUENCE_STARTED | STATE_UPDATES);
	}

}
