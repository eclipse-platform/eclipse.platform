/*******************************************************************************
 * Copyright (c) 2011, 2018 Wind River Systems and others.
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
 *     IBM Corporation - bug fixing
 *     Umair Sair (Mentor Graphics) - Bug 550220
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementCompareRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDeltaVisitor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStateUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

/**
 * Class containing logic to save and restore expanded state of the tree model
 * viewer.
 * <p>
 * When the input to the viewer is changes, the tree model viewer attempts to
 * save the expansion state of elements as well as currently selected element and
 * scroll position.  Each expanded element is queried for its memento and all the
 * collected mementos are saved into a delta tree then serialized by the viewer.<br>
 * When a new input is set to the viewer, the viewer compares the input's memento
 * with the stored mementos and if a match is found, it attempts to restore the
 * previous expansion state to the viewer.  As elements are revealed and realized
 * in the viewer, the element's memento is compared against the memento stored in
 * the saved state delta.  If matching elements are found in the delta, the expansion
 * and selection state is then restored to those elements.
 * </p><p>
 * Additionally to saving restoring state on input change, the viewer also
 * saves/restores elements' state when the model requests viewer to refresh model
 * structure.  Since the viewer items are matched to the model elements using items'
 * indexes, inserting or removing elements in model can cause the expansion state
 * of elements to shift after a refresh.  To compensate for this, the viewer saves
 * the elements before a refresh is performed into a delta, but without encoding
 * elements using mementos.  As the refresh of the tree progresses, the save state
 * is restored to the tree and elements are expanded or collapsed as needed to
 * compensate for changes in model structure.
 * </p>
 * @see TreeModelContentProvider
 */
class ViewerStateTracker {

	// State update type constants used in notifying listeners
	static final int STATE_SAVE_SEQUENCE_BEGINS = 4;
	static final int STATE_SAVE_SEQUENCE_COMPLETE = 5;
	static final int STATE_RESTORE_SEQUENCE_BEGINS = 6;
	static final int STATE_RESTORE_SEQUENCE_COMPLETE = 7;

	/**
	 * Collector of memento encoding requests.
	 */
	interface IElementMementoCollector {

		/**
		 * Adds the request to this manager.
		 *
		 * @param request to add
		 */
		void addRequest(ElementMementoRequest request);

		/**
		 * Notification the request is complete.
		 *
		 * @param request that was completed
		 */
		void requestComplete(ElementMementoRequest request);

		/**
		 * Process the queued requests. Accepts no more new requests.
		 */
		void processReqeusts();

		/**
		 * Cancels the requests in progress.
		 */
		void cancel();
	}

	/**
	 * LRU cache for viewer states
	 */
	class LRUMap<K, V> extends LinkedHashMap<K, V> {
		private static final long serialVersionUID = 1L;

		private int fMaxSize;

		LRUMap(int maxSize) {
			super();
			fMaxSize = maxSize;
		}

		@Override
		protected boolean removeEldestEntry(Entry<K, V> eldest) {
			return size() > fMaxSize;
		}
	}

	/**
	 * Content provider that is using this state tracker.
	 */
	private TreeModelContentProvider fContentProvider;

	ViewerStateTracker(TreeModelContentProvider contentProvider) {
		fContentProvider = contentProvider;
	}

	/**
	 * Map of viewer states keyed by viewer input mementos
	 */
	private Map<String, ModelDelta> fViewerStates = new LRUMap<>(20);

	/**
	 * Pending viewer state to be restored
	 */
	private ModelDelta fPendingState = null;

	/**
	 * Flag indicating that the content provider is performing
	 * state restore operations.
	 */
	private boolean fInStateRestore = false;

	/**
	 * State update listeners
	 */
	private ListenerList<IStateUpdateListener> fStateUpdateListeners = new ListenerList<>();

	/**
	 * Postpone restoring REVEAL element until the current updates are complete.
	 * See bug 324100
	 */
	protected PendingRevealDelta fPendingSetTopItem = null;

	/**
	 * Set of IMementoManager's that are currently saving state
	 */
	private Set<IElementMementoCollector> fPendingStateSaves = new HashSet<>();

	/**
	 * Used to queue a viewer input for state restore
	 */
	private Object fQueuedRestore = null;

	/**
	 * Object used to key compare requests in map.
	 */
	private static class CompareRequestKey {

		CompareRequestKey(TreePath path, IModelDelta delta) {
			fPath = path;
			fDelta = delta;
		}

		TreePath fPath;
		IModelDelta fDelta;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CompareRequestKey) {
				CompareRequestKey key = (CompareRequestKey) obj;
				return key.fDelta.equals(fDelta) && key.fPath.equals(fPath);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return fDelta.hashCode() + fPath.hashCode();
		}
	}

	/**
	 * Compare requests that are currently running.
	 */
	private Map<CompareRequestKey, ElementCompareRequest> fCompareRequestsInProgress = new LinkedHashMap<>();


	/**
	 * Cancels pending updates.
	 */
	void dispose() {
		Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );
		for (IElementMementoCollector emc : fPendingStateSaves) {
			emc.cancel();
		}
		fStateUpdateListeners.clear();
		for (ElementCompareRequest ecr : fCompareRequestsInProgress.values()) {
			ecr.cancel();
		}
		fCompareRequestsInProgress.clear();

		if (fPendingSetTopItem != null) {
			fPendingSetTopItem.dispose();
		}
	}

	/**
	 * Restores viewer state for the given input
	 *
	 * @param input
	 *            viewer input
	 */
	private void startRestoreViewerState(final Object input) {
		Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

		fPendingState = null;
		final IElementMementoProvider defaultProvider = ViewerAdapterService.getMementoProvider(input);
		if (defaultProvider != null) {
			// build a model delta representing expansion and selection state
			final ModelDelta delta = new ModelDelta(input, IModelDelta.NO_CHANGE);
			final XMLMemento inputMemento = XMLMemento.createWriteRoot("VIEWER_INPUT_MEMENTO"); //$NON-NLS-1$
			final IElementMementoCollector manager = new IElementMementoCollector() {

				private IElementMementoRequest fRequest;

				@Override
				public void requestComplete(ElementMementoRequest request) {
					if (fContentProvider.isDisposed()) {
						return;
					}

					notifyStateUpdate(input, TreeModelContentProvider.UPDATE_COMPLETE, request);

					if (!request.isCanceled() && (request.getStatus() == null || request.getStatus().isOK())) {
						XMLMemento keyMemento = (XMLMemento) delta.getElement();
						StringWriter writer = new StringWriter();
						try {
							keyMemento.save(writer);
							final String keyMementoString = writer.toString();
							ModelDelta stateDelta = fViewerStates.get(keyMementoString);
							if (stateDelta != null) {
								if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext()))  {
									DebugUIPlugin.trace("STATE RESTORE INPUT COMARE ENDED : " + fRequest + " - MATCHING STATE FOUND"); //$NON-NLS-1$ //$NON-NLS-2$
								}

								// Process start of restore in an async cycle because we may still be inside inputChanged()
								// call. I.e. the "input.equals(fContentProvider.getViewer().getInput())" test may fail.
								fContentProvider.getViewer().getDisplay().asyncExec(() -> {
									if (!fContentProvider.isDisposed()
											&& input.equals(fContentProvider.getViewer().getInput())) {
										ModelDelta stateDelta2 = fViewerStates.remove(keyMementoString);
										if (stateDelta2 != null) {
											if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE
													&& DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(
															fContentProvider.getPresentationContext())) {
												DebugUIPlugin.trace("STATE RESTORE BEGINS"); //$NON-NLS-1$
												DebugUIPlugin.trace("\tRESTORE: " + stateDelta2); //$NON-NLS-1$
												notifyStateUpdate(input, STATE_RESTORE_SEQUENCE_BEGINS, null);
											}
											stateDelta2.setElement(input);
											fPendingState = stateDelta2;
											doInitialRestore(fPendingState);
										}
									} else {
										if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE
												&& DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(
														fContentProvider.getPresentationContext())) {
											DebugUIPlugin.trace("STATE RESTORE CANCELED."); //$NON-NLS-1$
										}
									}
								});
							} else {
								if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext()))  {
									DebugUIPlugin.trace("STATE RESTORE INPUT COMARE ENDED : " + fRequest + " - NO MATCHING STATE"); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
						} catch (IOException e) {
							DebugUIPlugin.log(e);
						}
					} else {
						notifyStateUpdate(input, STATE_RESTORE_SEQUENCE_BEGINS, null);
					}
				}

				@Override
				public void processReqeusts() {
					notifyStateUpdate(input, STATE_RESTORE_SEQUENCE_BEGINS, null);
					if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
						DebugUIPlugin.trace("STATE RESTORE INPUT COMARE BEGIN : " + fRequest); //$NON-NLS-1$
					}
					notifyStateUpdate(input, TreeModelContentProvider.UPDATE_BEGINS, fRequest);
					defaultProvider.encodeElements(new IElementMementoRequest[] { fRequest });
				}

				@Override
				public void addRequest(ElementMementoRequest req) {
					fRequest = req;
				}

				@Override
				public void cancel() {
					// not used
				}

			};
			manager.addRequest(
				new ElementMementoRequest(fContentProvider, fContentProvider.getViewer().getInput(), manager,
					delta.getElement(), fContentProvider.getViewerTreePath(delta), inputMemento, delta));
			manager.processReqeusts();
		} else {
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext()))  {
				DebugUIPlugin.trace("STATE RESTORE: No input memento provider"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Appends the state of the given subtree to the current pending delta.
	 * @param path Path to subtree to restore.
	 */
	void appendToPendingStateDelta(final TreePath path) {
		if (fContentProvider.getViewer() == null)
		 {
			return; // Not initialized yet.
		}

		if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext()))  {
			DebugUIPlugin.trace("STATE APPEND BEGIN: " + path.getLastSegment()); //$NON-NLS-1$
		}

		// build a model delta representing expansion and selection state
		final ModelDelta appendDeltaRoot = new ModelDelta(fContentProvider.getViewer().getInput(), IModelDelta.NO_CHANGE);
		ModelDelta delta = appendDeltaRoot;
		for (int i = 0; i < path.getSegmentCount(); i++) {
			delta = delta.addNode(path.getSegment(i), IModelDelta.NO_CHANGE);
		}

		if (!fContentProvider.getViewer().saveElementState(path, delta, IModelDelta.COLLAPSE | IModelDelta.EXPAND | IModelDelta.SELECT)) {
			// Path to save the state was not found or there was no
			// (expansion) state to save!  Abort.
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
				DebugUIPlugin.trace("STATE APPEND CANCEL: Element " + path.getLastSegment() + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return;
		}

		// Append a marker CONTENT flag to all the delta nodes that contain the
		// EXPAND node. These
		// markers are used by the restore logic to know when a delta node can
		// be removed.
		delta.accept((d, depth) -> {
			if ((d.getFlags() & IModelDelta.EXPAND) != 0) {
				((ModelDelta) d).setFlags(d.getFlags() | IModelDelta.CONTENT);
			}
			return true;
		});

		if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
			DebugUIPlugin.trace("\tAPPEND DELTA: " + appendDeltaRoot); //$NON-NLS-1$
		}

		if (fPendingState != null) {
			// If the restore for the current input was never completed,
			// preserve
			// that restore along with the restore that was completed.
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
				DebugUIPlugin.trace("\tAPPEND OUTSTANDING RESTORE: " + fPendingState); //$NON-NLS-1$
			}

			// If the append delta is generated for a sub-tree, copy the pending delta
			// attributes into the pending delta.
			if (path.getSegmentCount() > 0) {
				fPendingState.accept((pendingDeltaNode, depth) -> {
					TreePath pendingDeltaPath = fContentProvider.getViewerTreePath(pendingDeltaNode);
					if (path.startsWith(pendingDeltaPath, null)) {
						ModelDelta appendDelta = findDeltaForPath(appendDeltaRoot, pendingDeltaPath);
						appendDelta.setFlags(pendingDeltaNode.getFlags());
						appendDelta.setChildCount(pendingDeltaNode.getChildCount());
						appendDelta.setIndex(pendingDeltaNode.getIndex());
						return true;
					}
					return false;
				});
			}

			// Copy the pending state into the new appended state.
			fPendingState.accept((pendingDeltaNode, depth) -> {
				// Skip the top element
				if (pendingDeltaNode.getParentDelta() == null) {
					return true;
				}

				// Find the node in the save delta which is the parent
				// of to the current pending delta node.
				// If the parent node cannot be found, it means that
				// most likely the user collapsed the parent node before
				// the children were ever expanded.
				// If the pending state node already exists in the parent
				// node, it is already processed and we can skip it.
				// If the pending state node does not contain any flags,
				// we can also skip it.
				ModelDelta saveDeltaNode = findSubDeltaParent(appendDeltaRoot, pendingDeltaNode);
				if (saveDeltaNode != null && !isDeltaInParent(pendingDeltaNode, saveDeltaNode)
						&& pendingDeltaNode.getFlags() != IModelDelta.NO_CHANGE) {
					saveDeltaNode.setChildCount(pendingDeltaNode.getParentDelta().getChildCount());
					copyIntoDelta(pendingDeltaNode, saveDeltaNode);
				} else {
					if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE
							&& DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
						DebugUIPlugin.trace("\tSKIPPED: " + pendingDeltaNode.getElement()); //$NON-NLS-1$
					}
				}

				// If the pending delta node has a memento element, its
				// children should also be mementos therefore the copy
				// delta operation should have added all the children
				// of this pending delta node into the save delta.
				if (pendingDeltaNode.getElement() instanceof IMemento) {
					return false;
				} else {
					return pendingDeltaNode.getChildCount() > 0;
				}
			});
		}

		if (appendDeltaRoot.getChildDeltas().length > 0) {
			// Set the new delta root as the pending state delta.
			if (fPendingState == null) {
				notifyStateUpdate(appendDeltaRoot.getElement(), STATE_RESTORE_SEQUENCE_BEGINS, null);
			}
			fPendingState = appendDeltaRoot;
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
				DebugUIPlugin.trace("STATE APPEND COMPLETE " + fPendingState); //$NON-NLS-1$
			}
		} else {
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
				DebugUIPlugin.trace("STATE APPEND CANCELED: No Data"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Saves the viewer's state for the previous input. * @param oldInput
	 * @param input the {@link ModelDelta} input
	 */
	protected void saveViewerState(Object input) {
		for (Iterator<ElementCompareRequest> itr = fCompareRequestsInProgress.values().iterator(); itr.hasNext();) {
			itr.next().cancel();
			itr.remove();
		}

		IElementMementoProvider stateProvider = ViewerAdapterService.getMementoProvider(input);
		if (stateProvider != null) {
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
				DebugUIPlugin.trace("STATE SAVE BEGIN: " + input); //$NON-NLS-1$
			}

			// build a model delta representing expansion and selection state
			final ModelDelta saveDeltaRoot = new ModelDelta(input, IModelDelta.NO_CHANGE);
			buildViewerState(saveDeltaRoot);
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
				DebugUIPlugin.trace("\tSAVE DELTA FROM VIEW:\n" + saveDeltaRoot); //$NON-NLS-1$
			}

			// check if pending restore reveal
			if (fPendingSetTopItem != null) {
				// set back the pending reveal flag
				ModelDelta revealDelta = fPendingSetTopItem.getDelta();
				revealDelta.setFlags(revealDelta.getFlags() | IModelDelta.REVEAL);

				fPendingSetTopItem.dispose();

				ModelDelta saveDeltaNode = findSubDeltaParent(saveDeltaRoot, revealDelta);
				if (saveDeltaNode != null) {
					clearRevealFlag(saveDeltaRoot);
					ModelDelta child = saveDeltaNode.getChildDelta(revealDelta.getElement(), revealDelta.getIndex());
					if (child != null) {
						child.setFlags(child.getFlags() | IModelDelta.REVEAL);
					} else {
						// the node should be added if not found
						saveDeltaNode.setChildCount(revealDelta.getParentDelta().getChildCount());
						copyIntoDelta(revealDelta, saveDeltaNode);
					}
				} else {
					if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
						DebugUIPlugin.trace("\tSKIPPED: " + revealDelta.getElement()); //$NON-NLS-1$
					}
				}
			}

			if (fPendingState != null) {
				// If the restore for the current input was never completed,
				// preserve
				// that restore along with the restore that was completed.
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					DebugUIPlugin.trace("\tSAVE OUTSTANDING RESTORE: " + fPendingState); //$NON-NLS-1$
				}

				IModelDeltaVisitor pendingStateVisitor = (pendingDeltaNode, depth) -> {
					// Ignore the top element.
					if (pendingDeltaNode.getParentDelta() == null) {
						return true;
					}

					// Find the node in the save delta which is the parent
					// of to the current pending delta node.
					// If the parent node cannot be found, it means that
					// most likely the user collapsed the parent node before
					// the children were ever expanded.
					// If the pending state node already exists in the
					// parent
					// node, it is already processed and we can skip it.
					// If the pending state node does not contain any flags,
					// we can also skip it.
					ModelDelta saveDeltaNode = findSubDeltaParent(saveDeltaRoot, pendingDeltaNode);
					if (saveDeltaNode != null && !isDeltaInParent(pendingDeltaNode, saveDeltaNode)
							&& pendingDeltaNode.getFlags() != IModelDelta.NO_CHANGE) {
						// There should be only one delta element with
						// the REVEAL flag in the entire save delta. The
						// reveal flag in the pending delta trumps the one
						// in the save delta because most likely the restore
						// operation did not yet complete the reveal
						// operation.
						if ((pendingDeltaNode.getFlags() & IModelDelta.REVEAL) != 0) {
							clearRevealFlag(saveDeltaRoot);
						}
						saveDeltaNode.setChildCount(pendingDeltaNode.getParentDelta().getChildCount());
						copyIntoDelta(pendingDeltaNode, saveDeltaNode);
					} else {
						if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin
								.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
							DebugUIPlugin.trace("\tSKIPPED: " + pendingDeltaNode.getElement()); //$NON-NLS-1$
						}
					}

					// If the pending delta node has a memento element, its
					// children should also be mementos therefore the copy
					// delta operation should have added all the children
					// of this pending delta node into the save delta.
					if (pendingDeltaNode.getElement() instanceof IMemento) {
						return false;
					} else {
						return pendingDeltaNode.getChildCount() > 0;
					}
				};
				fPendingState.accept(pendingStateVisitor);
			}

			if (saveDeltaRoot.getChildDeltas().length > 0) {
				// encode delta with mementos in place of elements, in non-UI
				// thread
				encodeDelta(saveDeltaRoot, stateProvider);
			} else {
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					DebugUIPlugin.trace("STATE SAVE CANCELED, NO DATA"); //$NON-NLS-1$
				}
			}
		}
	}

	private void clearRevealFlag(ModelDelta saveRootDelta) {
		IModelDeltaVisitor clearDeltaVisitor = (delta, depth) -> {
			if ((delta.getFlags() & IModelDelta.REVEAL) != 0) {
				((ModelDelta) delta).setFlags(delta.getFlags() & ~IModelDelta.REVEAL);
			}
			return true;
		};
		saveRootDelta.accept(clearDeltaVisitor);
	}

	private ModelDelta findSubDeltaParent(ModelDelta destinationDeltaRoot, IModelDelta subDelta) {
		// Create a path of elements to the sub-delta.
		LinkedList<IModelDelta> deltaPath = new LinkedList<>();
		IModelDelta delta = subDelta;
		while (delta.getParentDelta() != null) {
			delta = delta.getParentDelta();
			deltaPath.addFirst(delta);
		}

		// For each element in the path of the sub-delta, find the corresponding
		// element in the destination delta
		Iterator<IModelDelta> itr = deltaPath.iterator();
		// Skip the root element
		itr.next();
		ModelDelta saveDelta = destinationDeltaRoot;
		while (saveDelta != null && itr.hasNext()) {
			IModelDelta itrDelta = itr.next();
			saveDelta = saveDelta.getChildDelta(itrDelta.getElement(), itrDelta.getIndex());
		}
		return saveDelta;
	}

	private ModelDelta findDeltaForPath(ModelDelta root, TreePath path) {
		ModelDelta delta = root;
		for (int i = 0; i < path.getSegmentCount(); i++) {
			delta = delta.getChildDelta(path.getSegment(i));
			if (delta == null) {
				return null;
			}
		}
		return delta;
	}

	private boolean isDeltaInParent(IModelDelta delta, ModelDelta destParent) {
		return destParent.getChildDelta(delta.getElement(), delta.getIndex()) != null;
	}

	private void copyIntoDelta(IModelDelta delta, ModelDelta destParent) {
		ModelDelta newDelta = destParent.addNode(delta.getElement(), delta.getIndex(), delta.getFlags(), delta
			.getChildCount());
		for (int i = 0; i < delta.getChildDeltas().length; i++) {
			copyIntoDelta(delta.getChildDeltas()[i], newDelta);
		}
	}

	/**
	 * Encodes delta elements into mementos using the given provider.
	 * @param rootDelta the {@link ModelDelta} to encode
	 * @param defaultProvider the default provider to use when processing the given delta
	 *
	 */
	protected void encodeDelta(final ModelDelta rootDelta, final IElementMementoProvider defaultProvider) {
		final Object input = rootDelta.getElement();
		final XMLMemento inputMemento = XMLMemento.createWriteRoot("VIEWER_INPUT_MEMENTO"); //$NON-NLS-1$
		final XMLMemento childrenMemento = XMLMemento.createWriteRoot("CHILDREN_MEMENTO"); //$NON-NLS-1$
		final IElementMementoCollector manager = new IElementMementoCollector() {

			/**
			 * list of memento fRequests
			 */
			private List<IElementMementoRequest> fRequests = new ArrayList<>();

			/**
			 * Flag indicating whether the encoding of delta has been canceled.
			 */
			private boolean fCanceled = false;

			@Override
			public void requestComplete(ElementMementoRequest request) {
				Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

				notifyStateUpdate(input, TreeModelContentProvider.UPDATE_COMPLETE, request);
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					DebugUIPlugin.trace("\tSTATE END: " + request); //$NON-NLS-1$
				}

				if (!request.isCanceled() && (request.getStatus() == null || request.getStatus().isOK())) {
					boolean requestsComplted = false;
					if (!fCanceled) {
						fRequests.remove(request);
						requestsComplted = fRequests.isEmpty();
					}
					if (requestsComplted) {
						XMLMemento keyMemento = (XMLMemento) rootDelta.getElement();
						StringWriter writer = new StringWriter();
						try {
							keyMemento.save(writer);
							fViewerStates.put(writer.toString(), rootDelta);
						} catch (IOException e) {
							DebugUIPlugin.log(e);
						}
						if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
							DebugUIPlugin.trace("STATE SAVE COMPLETED: " + rootDelta); //$NON-NLS-1$
						}
						stateSaveComplete(input, this);
					}
				} else {
					cancel();
				}
			}

			@Override
			public void cancel() {
				Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

				if (fCanceled) {
					return;
				}

				fCanceled = true;
				for (IElementMementoRequest req : fRequests) {
					req.cancel();
				}
				fRequests.clear();
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					DebugUIPlugin.trace("STATE SAVE ABORTED: " + rootDelta.getElement()); //$NON-NLS-1$
				}
				stateSaveComplete(input, this);
			}

			@Override
			public void processReqeusts() {
				Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

				Map<IElementMementoProvider, List<IElementMementoRequest>> providers = new HashMap<>();
				for (IElementMementoRequest request : fRequests) {
					notifyStateUpdate(input, TreeModelContentProvider.UPDATE_BEGINS, request);
					IElementMementoProvider provider = ViewerAdapterService.getMementoProvider(request.getElement());
					if (provider == null) {
						provider = defaultProvider;
					}
					List<IElementMementoRequest> reqs = providers.get(provider);
					if (reqs == null) {
						reqs = new ArrayList<>();
						providers.put(provider, reqs);
					}
					reqs.add(request);
				}
				for (Entry<IElementMementoProvider, List<IElementMementoRequest>> entry : providers.entrySet()) {
					IElementMementoProvider provider = entry.getKey();
					List<IElementMementoRequest> reqs = entry.getValue();
					provider.encodeElements(reqs.toArray(new IElementMementoRequest[reqs.size()]));
				}
			}

			@Override
			public void addRequest(ElementMementoRequest request) {
				Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

				fRequests.add(request);
			}

		};
		IModelDeltaVisitor visitor = (delta, depth) -> {
			// Add the CONTENT flag to all nodes with an EXPAND flag.
			// During restoring, this flag is used as a marker indicating
			// whether all the content of a given element has been
			// retrieved.
			if ((delta.getFlags() | IModelDelta.EXPAND) != 0) {
				((ModelDelta) delta).setFlags(delta.getFlags() | IModelDelta.CONTENT);
			}

			// This is the root element, save the root element memento in 'inputMemento'.
			if (delta.getParentDelta() == null) {
				manager.addRequest(new ElementMementoRequest(fContentProvider, input, manager, delta.getElement(),
						fContentProvider.getViewerTreePath(delta), inputMemento, (ModelDelta) delta));
			} else {
				// If this is another node element, save the memento to a children memento.
				if (!(delta.getElement() instanceof XMLMemento)) {
					manager.addRequest(new ElementMementoRequest(fContentProvider, input, manager, delta.getElement(),
							fContentProvider.getViewerTreePath(delta), childrenMemento.createChild("CHILD_ELEMENT"), //$NON-NLS-1$
							(ModelDelta) delta));
				}
			}
			return true;
		};
		rootDelta.accept(visitor);
		stateSaveStarted(input, manager);
		manager.processReqeusts();
	}

	/**
	 * Called when a state save is starting.
	 * @param input the {@link ModelDelta} input
	 * @param manager the manager to notify
	 */
	private void stateSaveStarted(Object input, IElementMementoCollector manager) {
		Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

		notifyStateUpdate(input, STATE_SAVE_SEQUENCE_BEGINS, null);
		fPendingStateSaves.add(manager);
	}

	/**
	 * Called when a state save is complete.
	 * @param input the {@link ModelDelta} input
	 * @param manager the manager to notify
	 */
	private void stateSaveComplete(Object input, IElementMementoCollector manager) {
		Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

		notifyStateUpdate(input, STATE_SAVE_SEQUENCE_COMPLETE, null);
		fPendingStateSaves.remove(manager);
		if (fQueuedRestore != null) {
			Object temp = fQueuedRestore;
			fQueuedRestore = null;
			restoreViewerState(temp);
		}
	}

	/**
	 * Returns whether any state saving is in progress.
	 *
	 * @return whether any state saving is in progress
	 */
	private boolean isSavingState() {
		Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

		return !fPendingStateSaves.isEmpty();
	}

	/**
	 * Restores the viewer state unless a save is taking place. If a save is
	 * taking place, the restore is queued.
	 *
	 * @param input
	 *            viewer input
	 */
	protected void restoreViewerState(final Object input) {
		Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

		fPendingState = null;
		if (isSavingState()) {
			fQueuedRestore = input;
		} else {
			startRestoreViewerState(input);
		}
	}


	public void cancelRestore(final TreePath path, final int flags) {
		if (fInStateRestore) {
			// If we are currently processing pending state already, ignore
			// cancelRestore requests.  These requests may be triggered in the viewer
			// by changes to the tree state (Bug 295585).
			return;
		}

		if ((flags & IModelDelta.REVEAL) != 0 && fPendingSetTopItem != null) {
			fPendingSetTopItem.dispose();
			return;
		}

		// Nothing else to do
		if (fPendingState == null) {
			return;
		}

		if ((flags & (IModelDelta.SELECT | IModelDelta.REVEAL)) != 0) {
			// If we're canceling reveal and this is waiting for updates to complete
			// then just cancel it and return

			// If we're canceling select or reveal, cancel it for all of pending deltas
			final int mask = flags & (IModelDelta.SELECT | IModelDelta.REVEAL);
			fPendingState.accept((delta, depth) -> {
				int deltaFlags = delta.getFlags();
				int newFlags = deltaFlags & ~mask;
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE
						&& DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					if (deltaFlags != newFlags) {
						DebugUIPlugin.trace(
								"\tCANCEL: " + delta.getElement() + "(" + Integer.toHexString(deltaFlags & mask) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
				((ModelDelta) delta).setFlags(newFlags);
				return true;
			});
		}
		if ((flags & ~(IModelDelta.SELECT | IModelDelta.REVEAL)) != 0) {
			final int mask = flags & ~(IModelDelta.SELECT | IModelDelta.REVEAL);
			// For other flags (EXPAND/COLLAPSE), cancel only from the matching path.
			fPendingState.accept((delta, depth) -> {
				if (depth < path.getSegmentCount()) {
					// Descend until we reach a matching depth.
					TreePath deltaPath1 = fContentProvider.getViewerTreePath(delta);
					if (path.startsWith(deltaPath1, null)) {
						return true;
					} else {
						return false;
					}
				} else if (depth == path.getSegmentCount()) {
					TreePath deltaPath2 = fContentProvider.getViewerTreePath(delta);
					if (deltaPath2.equals(path)) {
						int deltaFlags = delta.getFlags();
						int newFlags = deltaFlags & ~mask;
						if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin
								.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
							if (deltaFlags != newFlags) {
								DebugUIPlugin.trace("\tCANCEL: " + delta.getElement() + "(" //$NON-NLS-1$ //$NON-NLS-2$
										+ Integer.toHexString(deltaFlags & mask) + ")"); //$NON-NLS-1$
							}
						}
						((ModelDelta) delta).setFlags(newFlags);
						if ((flags & IModelDelta.EXPAND) != 0) {
							// Descend delta to clear the EXPAND flags of a canceled expand
							return true;
						}
					}
					return false;
				} else {
					// We're clearing out flags of a matching sub-tree
					// assert (flags & IModelDelta.EXPAND) != 0;

					if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE
							&& DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
						if (delta.getFlags() != IModelDelta.NO_CHANGE) {
							DebugUIPlugin.trace("\tCANCEL: " + delta.getElement() + "(" //$NON-NLS-1$ //$NON-NLS-2$
									+ Integer.toHexString(delta.getFlags()) + ")"); //$NON-NLS-1$
						}
					}
					((ModelDelta) delta).setFlags(IModelDelta.NO_CHANGE);
					return true;
				}
			});
		}
	}


	/**
	 * Perform any restoration required for the given tree path.
	 * <p>
	 * This method is called after every viewer update completion to continue
	 * restoring the expansion state that was previously saved.
	 *
	 * @param path the tree path to update
	 * @param modelIndex the index in the current model
	 * @param knowsHasChildren if the content provider knows it has children already
	 * @param knowsChildCount if the content provider knows the current child count already
	 * @param checkChildrenRealized if any realized children should be checked or not
	 */
	void restorePendingStateOnUpdate(final TreePath path, final int modelIndex, final boolean knowsHasChildren,
		final boolean knowsChildCount, final boolean checkChildrenRealized)
	{
		Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

		if (fPendingState == null) {
			return;
		}

		IModelDeltaVisitor visitor = (delta, depth) -> {

			Object element = delta.getElement();
			Object potentialMatch = depth != 0 ? path.getSegment(depth - 1) : fContentProvider.getViewer().getInput();
			// Only process if the depth in the delta matches the tree path.
			if (depth == path.getSegmentCount()) {
				if (element instanceof IMemento) {
					IElementMementoProvider provider = ViewerAdapterService.getMementoProvider(potentialMatch);
					if (provider == null) {
						provider = ViewerAdapterService.getMementoProvider(fContentProvider.getViewer().getInput());
					}
					if (provider != null) {
						CompareRequestKey key = new CompareRequestKey(path, delta);
						ElementCompareRequest existingRequest = fCompareRequestsInProgress.get(key);
						if (existingRequest != null) {
							// Check all the running compare updates for a
							// matching tree path.
							// If found, just update the flags.
							existingRequest.setKnowsHasChildren(knowsHasChildren);
							existingRequest.setKnowsChildCount(knowsChildCount);
							existingRequest.setCheckChildrenRealized(checkChildrenRealized);
						} else {
							// Start a new compare request
							ElementCompareRequest compareRequest = new ElementCompareRequest(fContentProvider,
									fContentProvider.getViewer().getInput(), potentialMatch, path, (IMemento) element,
									(ModelDelta) delta, modelIndex, knowsHasChildren, knowsChildCount,
									checkChildrenRealized);
							fCompareRequestsInProgress.put(key, compareRequest);
							if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin
									.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
								DebugUIPlugin.trace("\tSTATE BEGIN: " + compareRequest); //$NON-NLS-1$
							}
							notifyStateUpdate(element, TreeModelContentProvider.UPDATE_BEGINS, compareRequest);
							provider.compareElements(new IElementCompareRequest[] { compareRequest });
						}
					}
				} else if (element.equals(potentialMatch)) {
					// Element comparison already succeeded, and it matches
					// our element.
					// Call restore with delta to process the delta flags.
					restorePendingStateNode((ModelDelta) delta, knowsHasChildren, knowsChildCount,
							checkChildrenRealized);
				}
				return false;
			}
			// Only follow the paths that match the delta.
			return element.equals(potentialMatch);
		};

		try {
			fInStateRestore = true;
			fPendingState.accept(visitor);
		}
		finally {
			fInStateRestore = false;
		}
		checkIfRestoreComplete();
	}

	/**
	 * Checks whether restoring pending state is already complete.
	 */
	void checkIfRestoreComplete() {
		Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

		if (fPendingState == null) {
			return;
		}

		/**
		 * Used to determine when restoration delta has been processed
		 */
		class CheckState implements IModelDeltaVisitor {
			private boolean complete = true;

			@Override
			public boolean visit(IModelDelta delta, int depth) {
				// Filter out the CONTENT flags from the delta flags, the content
				// flag is only used as a marker indicating that all the sub-elements
				// of a given delta have been retrieved.
				int flags = (delta.getFlags() & ~IModelDelta.CONTENT);

				if (flags != IModelDelta.NO_CHANGE) {
					IModelDelta parentDelta = delta.getParentDelta();
					// Do not visit children if :
					// - The parent delta has no more flags on it (the content flag is removed as well),
					// which means that parent element's children have been completely exposed.
					// - There are no more pending updates for the element.
					// - If element is a memento, there are no state requests pending.
					if (parentDelta != null && parentDelta.getFlags() == IModelDelta.NO_CHANGE) {
						TreePath deltaPath = fContentProvider.getViewerTreePath(delta);
						if ( !fContentProvider.areElementUpdatesPending(deltaPath) &&
							 (!(delta.getElement() instanceof IMemento) || !areMementoUpdatesPending(delta)) )
						{
							return false;
						}
					}

					if (flags != IModelDelta.REVEAL || (delta.getElement() instanceof IMemento)) {
						complete = false;
						return false;
					}
				}
				return true;
			}

			public boolean isComplete() {
				return complete;
			}

			private boolean areMementoUpdatesPending(IModelDelta delta) {
				for (CompareRequestKey key : fCompareRequestsInProgress.keySet()) {
					if (delta.getElement().equals(key.fDelta.getElement())) {
						return true;
					}
				}
				return false;
			}
		}

		CheckState state = new CheckState();
		fPendingState.accept(state);
		if (state.isComplete()) {
			// notify restore complete if REVEAL was restored also, otherwise
			// postpone until then.
			if (fPendingSetTopItem == null) {
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					DebugUIPlugin.trace("STATE RESTORE COMPELTE: " + fPendingState); //$NON-NLS-1$
				}

				notifyStateUpdate(fPendingState.getElement(), STATE_RESTORE_SEQUENCE_COMPLETE, null);
			}

			fPendingState = null;
		}
	}

	/**
	 * Restores the pending state in the given delta node.  This method is called
	 * once the state tracker has found the element which matches the element in
	 * the given delta node.
	 * @param delta the {@link ModelDelta} to restore from
	 * @param knowsHasChildren if the content provider has computed its children
	 * @param knowsChildCount if the content provider has already computed the child count
	 * @param checkChildrenRealized if any realized children should be checked
	 */
	void restorePendingStateNode(final ModelDelta delta, boolean knowsHasChildren, boolean knowsChildCount, boolean checkChildrenRealized) {
		final TreePath treePath = fContentProvider.getViewerTreePath(delta);
		final IInternalTreeModelViewer viewer = fContentProvider.getViewer();

		// Attempt to expand the node only if the children are known.
		if (knowsHasChildren) {
			if ((delta.getFlags() & IModelDelta.EXPAND) != 0) {
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					DebugUIPlugin.trace("\tRESTORE EXPAND: " + treePath.getLastSegment()); //$NON-NLS-1$
				}
				viewer.expandToLevel(treePath, 1);
				delta.setFlags(delta.getFlags() & ~IModelDelta.EXPAND);
			}
			if ((delta.getFlags() & IModelDelta.COLLAPSE) != 0) {
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					DebugUIPlugin.trace("\tRESTORE COLLAPSE: " + treePath.getLastSegment()); //$NON-NLS-1$
				}
				// Check auto-expand before collapsing an element (bug 335734)
				int autoexpand = fContentProvider.getViewer().getAutoExpandLevel();
				if (autoexpand != ITreeModelViewer.ALL_LEVELS && autoexpand < (treePath.getSegmentCount() + 1)) {
					fContentProvider.getViewer().setExpandedState(treePath, false);
				}
				delta.setFlags(delta.getFlags() & ~IModelDelta.COLLAPSE);
			}
		}

		if ((delta.getFlags() & IModelDelta.SELECT) != 0) {
			delta.setFlags(delta.getFlags() & ~IModelDelta.SELECT);
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
				DebugUIPlugin.trace("\tRESTORE SELECT: " + treePath.getLastSegment()); //$NON-NLS-1$
			}
			ITreeSelection currentSelection = (ITreeSelection)viewer.getSelection();
			if (currentSelection == null || currentSelection.isEmpty()) {
				viewer.setSelection(new TreeSelection(treePath), false, false);
			} else {
				TreePath[] currentPaths = currentSelection.getPaths();
				boolean pathInSelection = false;
				for (TreePath currentPath : currentPaths) {
					if (currentPath.equals(treePath)) {
						pathInSelection = true;
						break;
					}
				}
				// Only set the selection if the element is not yet in
				// selection.  Otherwise the setSelection() call will
				// update selection listeners needlessly.
				if (!pathInSelection) {
					TreePath[] newPaths = new TreePath[currentPaths.length + 1];
					System.arraycopy(currentPaths, 0, newPaths, 0, currentPaths.length);
					newPaths[newPaths.length - 1] = treePath;
					viewer.setSelection(new TreeSelection(newPaths), false, false);
				}
			}
		}

		if ((delta.getFlags() & IModelDelta.REVEAL) != 0) {
			delta.setFlags(delta.getFlags() & ~IModelDelta.REVEAL);
			// Look for the reveal flag in the child deltas.  If
			// A child delta has the reveal flag, do not set the
			// top element yet.
			boolean setTopItem = true;
			for (IModelDelta childDelta : delta.getChildDeltas()) {
				int modelIndex = childDelta.getIndex();
				if (modelIndex >= 0 && (childDelta.getFlags() & IModelDelta.REVEAL) != 0) {
					setTopItem = false;
				}
			}

			if (setTopItem) {
				Assert.isTrue(fPendingSetTopItem == null);

				fPendingSetTopItem = new PendingRevealDelta(treePath, delta);
				viewer.addViewerUpdateListener(fPendingSetTopItem);
			}
		}

		// If we know the child count of the element, look for the reveal
		// flag in the child deltas.  For the children with reveal flag start
		// a new update.
		// If the child delta's index is out of range, strip the reveal flag
		// since it is no longer applicable.
		if (knowsChildCount) {
			int childCount = viewer.getChildCount(treePath);
			if (childCount >= 0) {
				for (ModelDelta childDelta : (ModelDelta[])delta.getChildDeltas()) {
					int modelIndex = childDelta.getIndex();
					if (modelIndex >= 0 && (childDelta.getFlags() & IModelDelta.REVEAL) != 0) {
						if (modelIndex < childCount) {
							fContentProvider.doUpdateElement(treePath, modelIndex);
						} else {
							childDelta.setFlags(childDelta.getFlags() & ~IModelDelta.REVEAL);
						}
					}
				}
			}
		}

		// Some children of this element were just updated.  If all its
		// children are now realized, clear out any elements that still
		// have flags, because they represent elements that were removed.
		if ((checkChildrenRealized &&
			 !fContentProvider.areChildrenUpdatesPending(treePath) &&
			 fContentProvider.getViewer().getElementChildrenRealized(treePath)) ||
			(knowsHasChildren && !viewer.getHasChildren(treePath)) )
		{
			if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
				DebugUIPlugin.trace("\tRESTORE CONTENT: " + treePath.getLastSegment()); //$NON-NLS-1$
			}
			delta.setFlags(delta.getFlags() & ~IModelDelta.CONTENT);
		}
	}

	/**
	 * Utility that reveals the saved top item in the viewer.  It listens for
	 * all content updates to complete in order to avoid having the desired top item
	 * scroll out as view content is filled in.
	 * <br>
	 * Revealing some elements can trigger expanding some of elements
	 * that have been just revealed. Therefore, we have to check one
	 * more time after the new triggered updates are completed if we
	 * have to set again the top index
	 */
	private class PendingRevealDelta implements IViewerUpdateListener {

		private final TreePath fPathToReveal;
		private final ModelDelta fRevealDelta;

		PendingRevealDelta(TreePath pathToReveal, ModelDelta revealDelta) {
			fPathToReveal = pathToReveal;
			fRevealDelta = revealDelta;
		}

		/**
		 * Counter that tracks how many time the viewer updates were completed.
		 */
		private int fCounter = 0;
		private Object fModelInput = fPendingState.getElement();

		@Override
		public void viewerUpdatesComplete() {
			Assert.isTrue( fContentProvider.getViewer().getDisplay().getThread() == Thread.currentThread() );

			IInternalTreeModelViewer viewer = fContentProvider.getViewer();
			if (viewer == null || fPendingSetTopItem != this) {
				return;
			}

			TreePath topPath = viewer.getTopElementPath();
			if (!fPathToReveal.equals(topPath)) {
				TreePath parentPath = fPathToReveal.getParentPath();
				int index = viewer.findElementIndex(parentPath, fPathToReveal.getLastSegment());
				if (index >= 0) {
					if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
						DebugUIPlugin.trace("\tRESTORE REVEAL: " + fPathToReveal.getLastSegment()); //$NON-NLS-1$
					}
					viewer.reveal(parentPath, index);

				}
			}

			fCounter++;
			// in case the pending state was already set to null, we assume that
			// all others elements are restored, so we don't expect that REVEAL will
			// trigger other updates
			if (fCounter > 1 || fPendingState == null) {
				dispose();
			}
		}

		@Override
		public void viewerUpdatesBegin() {}
		@Override
		public void updateStarted(IViewerUpdate update) {}
		@Override
		public void updateComplete(IViewerUpdate update) {}

		/**
		 * Returns delta that is used to reveal the item.
		 * @return delta to be revealed.
		 */
		public ModelDelta getDelta() {
			return fRevealDelta;
		}

		/**
		 * Resets the item
		 */
		public void dispose() {
			// top item is set
			fPendingSetTopItem = null;

			IInternalTreeModelViewer viewer = fContentProvider.getViewer();
			if (viewer == null) {
				return;
			}

			// remove myself as viewer update listener
			viewer.removeViewerUpdateListener(this);

			if (fPendingState == null) {
				if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
					DebugUIPlugin.trace("STATE RESTORE COMPELTE: " + fPendingState); //$NON-NLS-1$
				}
				notifyStateUpdate(fModelInput, STATE_RESTORE_SEQUENCE_COMPLETE, null);
			} else {
				checkIfRestoreComplete();
			}
		}

	}

	/**
	 * Restore selection/expansion based on items already in the viewer
	 * @param delta the {@link ModelDelta} to restore from
	 */
	protected void doInitialRestore(ModelDelta delta) {
		// Find the reveal delta and mark nodes on its path
		// to reveal as elements are updated.
		markRevealDelta(delta);

		// Restore visible items.
		// Note (Pawel Piech): the initial list of items is normally
		// empty, so in most cases the code below does not do anything.
		// Instead doRestore() is called when various updates complete.
		int count = fContentProvider.getViewer().getChildCount(TreePath.EMPTY);
		for (int i = 0; i < count; i++) {
			Object data = fContentProvider.getViewer().getChildElement(TreePath.EMPTY, i);
			if (data != null) {
				restorePendingStateOnUpdate(new TreePath(new Object[]{data}), i, false, false, false);
			}
		}

	}

	/**
	 * Finds the delta with the reveal flag, then it walks up this
	 * delta and marks all the parents of it with the reveal flag.
	 * These flags are then used by the restore logic to restore
	 * and reveal all the nodes leading up to the element that should
	 * be ultimately at the top.
	 * @param rootDelta Delta to search
	 * @return The node just under the rootDelta which contains
	 * the reveal flag.  <code>null</code> if no reveal flag was found.
	 */
	private ModelDelta markRevealDelta(ModelDelta rootDelta) {
		final ModelDelta[] revealDelta = new ModelDelta[1];
		IModelDeltaVisitor visitor = (delta, depth) -> {
			if ((delta.getFlags() & IModelDelta.REVEAL) != 0) {
				revealDelta[0] = (ModelDelta) delta;
			}
			// Keep recursing only if we haven't found our delta yet.
			return revealDelta[0] == null;
		};

		rootDelta.accept(visitor);
		if (revealDelta[0] != null) {
			ModelDelta parentDelta = (ModelDelta)revealDelta[0].getParentDelta();
			while(parentDelta.getParentDelta() != null) {
				revealDelta[0] = parentDelta;
				revealDelta[0].setFlags(revealDelta[0].getFlags() | IModelDelta.REVEAL);
				parentDelta = (ModelDelta)parentDelta.getParentDelta();
			}
		}
		return revealDelta[0];
	}

	/**
	 * Builds a delta with the given root delta for expansion/selection state.
	 *
	 * @param delta
	 *            root delta
	 */
	private void buildViewerState(ModelDelta delta) {
		IInternalTreeModelViewer viewer = fContentProvider.getViewer();
		viewer.saveElementState(TreeModelContentProvider.EMPTY_TREE_PATH, delta, IModelDelta.SELECT | IModelDelta.EXPAND);

		// Add memento for top item if it is mapped to an element.  The reveal memento
		// is in its own path to avoid requesting unnecessary data when restoring it.
		TreePath topElementPath = viewer.getTopElementPath();
		if (topElementPath != null) {
			ModelDelta parentDelta = delta;
			TreePath parentPath = TreeModelContentProvider.EMPTY_TREE_PATH;
			for (int i = 0; i < topElementPath.getSegmentCount(); i++) {
				Object element = topElementPath.getSegment(i);
				int index = viewer.findElementIndex(parentPath, element);
				ModelDelta childDelta = parentDelta.getChildDelta(element);
				if (childDelta == null) {
					parentDelta = parentDelta.addNode(element, index, IModelDelta.NO_CHANGE);
				} else {
					parentDelta = childDelta;
				}
				parentPath = parentPath.createChildPath(element);
			}
			parentDelta.setFlags(parentDelta.getFlags() | IModelDelta.REVEAL);
		}
	}

	/**
	 * Cancels any outstanding compare requests for given element and its children.
	 * @param path Path of element to cancel updates for.
	 */
	void cancelStateSubtreeUpdates(TreePath path) {
		for (Iterator<CompareRequestKey> itr = fCompareRequestsInProgress.keySet().iterator(); itr.hasNext();) {
			CompareRequestKey key = itr.next();
			if (key.fPath.startsWith(path, null)) {
				ElementCompareRequest compareRequest = fCompareRequestsInProgress.get(key);
				compareRequest.cancel();
				itr.remove();
			}
		}
	}

	void compareFinished(ElementCompareRequest request, ModelDelta delta) {
		notifyStateUpdate(request.getViewerInput(), TreeModelContentProvider.UPDATE_COMPLETE, request);
		if (DebugUIPlugin.DEBUG_STATE_SAVE_RESTORE && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(fContentProvider.getPresentationContext())) {
			DebugUIPlugin.trace("\tSTATE END: " + request + " = " + false); //$NON-NLS-1$ //$NON-NLS-2$
		}

		fCompareRequestsInProgress.remove(new CompareRequestKey(request.getElementPath(), delta));
		if (!request.isCanceled()) {
			if (request.isEqual()) {
				delta.setElement(request.getElement());
				restorePendingStateNode(delta, request.knowsHasChildren(), request.knowChildCount(), request.checkChildrenRealized());
			} else if (request.getModelIndex() != -1) {
				// Comparison failed.
				// Check if the delta has a reveal flag, and if its index
				// matches the index of the element that it was compared
				// against. If this is the case, strip the reveal flag from
				// the delta as it is most likely not applicable anymore.
				if ((delta.getFlags() & IModelDelta.REVEAL) != 0 && delta.getIndex() == request.getModelIndex()) {
					delta.setFlags(delta.getFlags() & ~IModelDelta.REVEAL);
				}
			}
		}
		checkIfRestoreComplete();
	}


	void addStateUpdateListener(IStateUpdateListener listener) {
		fStateUpdateListeners.add(listener);
	}

	void removeStateUpdateListener(IStateUpdateListener listener) {
		fStateUpdateListeners.remove(listener);
	}

	void notifyStateUpdate(final Object input, final int type, final IViewerUpdate update) {
		if (!fStateUpdateListeners.isEmpty()) {
			for (IStateUpdateListener iStateUpdateListener : fStateUpdateListeners) {
				final IStateUpdateListener listener = iStateUpdateListener;
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						switch (type) {
							case STATE_SAVE_SEQUENCE_BEGINS:
								listener.stateSaveUpdatesBegin(input);
								break;
							case STATE_SAVE_SEQUENCE_COMPLETE:
								listener.stateSaveUpdatesComplete(input);
								break;
							case STATE_RESTORE_SEQUENCE_BEGINS:
								listener.stateRestoreUpdatesBegin(input);
								break;
							case STATE_RESTORE_SEQUENCE_COMPLETE:
								listener.stateRestoreUpdatesComplete(input);
								break;
							case TreeModelContentProvider.UPDATE_BEGINS:
								listener.stateUpdateStarted(input, update);
								break;
							case TreeModelContentProvider.UPDATE_COMPLETE:
								listener.stateUpdateComplete(input, update);
								break;
							default:
								break;
						}
					}

					@Override
					public void handleException(Throwable exception) {
						DebugUIPlugin.log(exception);
					}
				});
			}
		}
	}
}
