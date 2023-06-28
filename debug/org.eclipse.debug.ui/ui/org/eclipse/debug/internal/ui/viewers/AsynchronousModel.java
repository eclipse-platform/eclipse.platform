/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.elements.adapters.AsynchronousDebugLabelAdapter;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStatusMonitor;
import org.eclipse.debug.internal.ui.viewers.provisional.AsynchronousContentAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousContentAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousLabelAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IChildrenRequestMonitor;
import org.eclipse.debug.internal.ui.viewers.provisional.ILabelRequestMonitor;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Model for an asynchronous viewer
 *
 * @since 3.2
 */
public abstract class AsynchronousModel {

	private ModelNode fRoot; // root node
	private Map<Object, ModelNode[]> fElementToNodes = new HashMap<>(); // map
																							// of
																							// element
																							// to
																							// corresponding
																							// tree
																							// nodes
																							// (list)
	private Map<Object, IModelProxy> fModelProxies = new HashMap<>(); // map
																							// of
																							// installed
																							// model
																							// proxies,
																							// by
																							// element
	private AsynchronousViewer fViewer; // viewer this model works for
	private boolean fDisposed = false; // whether disposed

	static class EmptyContentAdapter extends AsynchronousContentAdapter {

		@Override
		protected Object[] getChildren(Object parent, IPresentationContext context) throws CoreException {
			return EMPTY;
		}

		@Override
		protected boolean hasChildren(Object element, IPresentationContext context) throws CoreException {
			return false;
		}

		@Override
		protected boolean supportsPartId(String id) {
			return true;
		}
	}

	protected IAsynchronousContentAdapter fEmptyContentAdapter = new EmptyContentAdapter();

	/**
	 * List of requests currently being performed.
	 */
	private List<IStatusMonitor> fPendingUpdates = new ArrayList<>();

	/**
	 * List of pending viewer updates
	 */
	private List<IStatusMonitor> fViewerUpdates = new ArrayList<>();

	/**
	 * Constructs a new empty tree model
	 *
	 * @param viewer associated viewer
	 */
	public AsynchronousModel(AsynchronousViewer viewer) {
		fViewer = viewer;
		if (DebugUIPlugin.DEBUG_MODEL) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("MODEL CREATED for: "); //$NON-NLS-1$
			buffer.append(fViewer);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(this);
			buffer.append(")"); //$NON-NLS-1$
			DebugUIPlugin.trace(buffer.toString());
		}
	}

	/**
	 * Initializes this model. Called once after creation.
	 *
	 * @param root root element or <code>null</code>
	 */
	public void init(Object root) {
		if (root != null) {
			fRoot = new ModelNode(null, root);
			mapElement(root, fRoot);
		}
	}

	protected AsynchronousViewer getViewer() {
		return fViewer;
	}

	/**
	 * Disposes this model
	 */
	public synchronized void dispose() {
		if (DebugUIPlugin.DEBUG_MODEL) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("MODEL DISPOSED for: "); //$NON-NLS-1$
			buffer.append(fViewer);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(this);
			buffer.append(")"); //$NON-NLS-1$
			DebugUIPlugin.trace(buffer.toString());
		}
		fDisposed = true;
		cancelPendingUpdates();
		disposeAllModelProxies();
		ModelNode rootNode = getRootNode();
		if (rootNode != null) {
			rootNode.dispose();
		}
		fElementToNodes.clear();
	}

	/**
	 * Returns whether this model has been disposed
	 * @return <code>true</code> if the model is disposed <code>false</code> otherwise
	 */
	public synchronized boolean isDisposed() {
		return fDisposed;
	}

	/**
	 * Cancels all pending update requests.
	 */
	protected synchronized void cancelPendingUpdates() {
		Iterator<IStatusMonitor> updates = fPendingUpdates.iterator();
		while (updates.hasNext()) {
			IStatusMonitor update = updates.next();
			updates.remove();
			update.setCanceled(true);
		}
		fPendingUpdates.clear();
	}

	/**
	 * Installs the model proxy for the given element into this viewer
	 * if not already installed.
	 *
	 * @param element element to install an update policy for
	 */
	public synchronized void installModelProxy(Object element) {
		if (!fModelProxies.containsKey(element)) {
			IModelProxyFactory modelProxyFactory = getModelProxyFactoryAdapter(element);
			if (modelProxyFactory != null) {
				final IModelProxy proxy = modelProxyFactory.createModelProxy(element, getPresentationContext());
				if (proxy != null) {
					fModelProxies.put(element, proxy);
					Job job = new Job("Model Proxy installed notification job") {//$NON-NLS-1$
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							if (!monitor.isCanceled()) {
								proxy.init(getPresentationContext());
								getViewer().modelProxyAdded(proxy);
								proxy.installed(getViewer());
							}
							return Status.OK_STATUS;
						}
					};
					job.setSystem(true);
					job.schedule();
				}
			}
		}
	}

	/**
	 * Uninstalls the model proxy installed for the given element, if any.
	 *
	 * @param element the element context
	 */
	protected synchronized void disposeModelProxy(Object element) {
		IModelProxy proxy = fModelProxies.remove(element);
		if (proxy != null) {
			getViewer().modelProxyRemoved(proxy);
			proxy.dispose();
		}
	}

	/**
	 * Unintalls all model proxies installed for this model
	 */
	private void disposeAllModelProxies() {
		synchronized(fModelProxies) {
			for (IModelProxy proxy : fModelProxies.values()) {
				getViewer().modelProxyRemoved(proxy);
				proxy.dispose();
			}
			fModelProxies.clear();
		}
	}

	/**
	 * Returns the presentation this model is installed in
	 *
	 * @return the presentation context from the backing viewer
	 */
	protected IPresentationContext getPresentationContext() {
		return fViewer.getPresentationContext();
	}

	/**
	 * Returns the model proxy factory for the given element of <code>null</code> if none.
	 *
	 * @param element element to retrieve adapters for
	 * @return model proxy factory adapter or <code>null</code>
	 */
	protected IModelProxyFactory getModelProxyFactoryAdapter(Object element) {
		IModelProxyFactory adapter = null;
		if (element instanceof IModelProxyFactory) {
			adapter = (IModelProxyFactory) element;
		} else if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) element;
			adapter = adaptable.getAdapter(IModelProxyFactory.class);
		}
		return adapter;
	}

	/**
	 * Maps the given element to the given node.
	 *
	 * @param element the element context
	 * @param node the model node
	 */
	protected synchronized void mapElement(Object element, ModelNode node) {
		ModelNode[] nodes = getNodes(element);
		node.remap(element);
		if (nodes == null) {
			fElementToNodes.put(element, new ModelNode[] { node});
		} else {
			for (ModelNode mnode : nodes) {
				if (mnode == node) {
					return;
				}
			}
			ModelNode[] old = nodes;
			ModelNode[] newNodes = new ModelNode[old.length + 1];
			System.arraycopy(old, 0, newNodes, 0, old.length);
			newNodes[old.length] = node;
			fElementToNodes.put(element, newNodes);
		}
		installModelProxy(element);
	}

	/**
	 * Unmaps the given node from its element and widget.
	 *
	 * @param node the model node
	 */
	protected synchronized void unmapNode(ModelNode node) {
		Object element = node.getElement();
		ModelNode[] nodes = fElementToNodes.get(element);
		if (nodes == null) {
			return;
		}
		if (nodes.length == 1) {
			fElementToNodes.remove(element);
			disposeModelProxy(element);
		} else {
			for (int i = 0; i < nodes.length; i++) {
				ModelNode node2 = nodes[i];
				if (node2 == node) {
					ModelNode[] newNodes= new ModelNode[nodes.length - 1];
					System.arraycopy(nodes, 0, newNodes, 0, i);
					if (i < newNodes.length) {
						System.arraycopy(nodes, i + 1, newNodes, i, newNodes.length - i);
					}
					fElementToNodes.put(element, newNodes);
				}
			}
		}
	}

	/**
	 * Returns the nodes in this model for the given element or
	 * <code>null</code> if none.
	 *
	 * @param element model element
	 * @return associated nodes or <code>null</code>
	 */
	public synchronized ModelNode[] getNodes(Object element) {
		return fElementToNodes.get(element);
	}

	/**
	 * Returns the root node or <code>null</code>
	 *
	 * @return the root node or <code>null</code>
	 */
	public ModelNode getRootNode() {
		return fRoot;
	}

	/**
	 * Cancels any conflicting updates for children of the given item, and
	 * schedules the new update.
	 *
	 * @param update the update to schedule
	 */
	protected void requestScheduled(IStatusMonitor update) {
		AsynchronousRequestMonitor absUpdate = (AsynchronousRequestMonitor) update;
		synchronized (fPendingUpdates) {
			ListIterator<IStatusMonitor> updates = fPendingUpdates.listIterator();
			while (updates.hasNext()) {
				AsynchronousRequestMonitor pendingUpdate = (AsynchronousRequestMonitor) updates.next();
				if (absUpdate.contains(pendingUpdate)) {
					updates.remove();
					pendingUpdate.setCanceled(true);
				}
			}
			fPendingUpdates.add(update);
		}
	}

	/**
	 * Removes the update from the pending updates list.
	 *
	 * @param update the update to remove
	 */
	protected void requestComplete(IStatusMonitor update) {
		synchronized (fPendingUpdates) {
			fPendingUpdates.remove(update);
		}
	}

	/**
	 * An viewer update has been scheduled due to the following update request.
	 *
	 * @param update the update to add
	 */
	protected void viewerUpdateScheduled(IStatusMonitor update) {
		// synch viewer updates and pending updates on same lock - fPendingUpdates
		synchronized (fPendingUpdates) {
			fViewerUpdates.add(update);
		}
	}

	/**
	 * Returns the result of running the given elements through the
	 * viewers filters.
	 *
	 * @param parent parent element
	 * @param elements the elements to filter
	 * @return only the elements which all filters accept
	 */
	protected Object[] filter(Object parent, Object[] elements) {
		ViewerFilter[] filters = getViewer().getFilters();
		if (filters != null) {
			ArrayList<Object> filtered = new ArrayList<>(elements.length);
			for (Object element : elements) {
				boolean add = true;
				for (ViewerFilter filter : filters) {
					add = filter.select(getViewer(), parent, element);
					if (!add) {
						break;
					}
				}
				if (add) {
					filtered.add(element);
				}
			}
			return filtered.toArray();
		}
		return elements;
	}

	/**
	 * Refreshes the given node.
	 *
	 * @param node the model node to update
	 */
	protected void updateLabel(ModelNode node) {
		Object element = node.getElement();
		IAsynchronousLabelAdapter adapter = getLabelAdapter(element);
		if (adapter != null) {
			ILabelRequestMonitor labelUpdate = new LabelRequestMonitor(node, this);
			requestScheduled(labelUpdate);
			adapter.retrieveLabel(element, getPresentationContext(), labelUpdate);
		}
	}

	/**
	 * Returns the label adapter for the given element or <code>null</code> if none.
	 *
	 * @param element element to retrieve adapter for
	 * @return presentation adapter or <code>null</code>
	 */
	protected IAsynchronousLabelAdapter getLabelAdapter(Object element) {
		IAsynchronousLabelAdapter adapter = null;
		if (element instanceof IAsynchronousLabelAdapter) {
			adapter = (IAsynchronousLabelAdapter) element;
		} else if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) element;
			adapter = adaptable.getAdapter(IAsynchronousLabelAdapter.class);
		}
		// if no adapter, use default (i.e. model presentation)
		if (adapter == null) {
			return new AsynchronousDebugLabelAdapter();
		}
		return adapter;
	}

	/**
	 * Returns the tree element adapter for the given element or
	 * <code>null</code> if none.
	 *
	 * @param element
	 *            element to retrieve adapter for
	 * @return presentation adapter or <code>null</code>
	 */
	protected IAsynchronousContentAdapter getContentAdapter(Object element) {
		IAsynchronousContentAdapter adapter = null;
		if (element instanceof IAsynchronousContentAdapter) {
			adapter = (IAsynchronousContentAdapter) element;
		} else if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) element;
			adapter = adaptable.getAdapter(IAsynchronousContentAdapter.class);
		}
		return adapter;
	}

	/**
	 * Updates the children of the given node.
	 *
	 * @param parent
	 *            node of which to update children
	 */
	public void updateChildren(ModelNode parent) {
		Object element = parent.getElement();
		IAsynchronousContentAdapter adapter = getContentAdapter(element);
		if (adapter == null) {
			adapter = fEmptyContentAdapter;
		}
		if (adapter != null) {
			IChildrenRequestMonitor update = new ChildrenRequestMonitor(parent, this);
			requestScheduled(update);
			adapter.retrieveChildren(element, getPresentationContext(), update);
		}
	}

	/**
	 * Update this model's viewer preserving its selection.
	 *
	 * @param update the update code to run in the backing viewer's preserving selection method
	 */
	protected void preservingSelection(Runnable update) {
		getViewer().preservingSelection(update);
	}

	/**
	 * The viewer updated associated with a request is complete.
	 *
	 * @param monitor the status to remove and complete
	 */
	protected void viewerUpdateComplete(IStatusMonitor monitor) {
		// synch viewer updates and pending updates on same lock - fPendingUpdates
		synchronized (fPendingUpdates) {
			fViewerUpdates.remove(monitor);
		}
		getViewer().updateComplete(monitor);
	}

	/**
	 * An update request was cancelled
	 *
	 * @param monitor the monitor to remove
	 */
	protected void requestCanceled(AsynchronousRequestMonitor monitor) {
		synchronized (fPendingUpdates) {
			fPendingUpdates.remove(monitor);
		}
	}

	/**
	 * Whether any updates are still in progress in the model or against the viewer.
	 *
	 * @return <code>true</code> if there are pending changes <code>false</code> otherwise
	 */
	protected boolean hasPendingUpdates() {
		synchronized (fViewerUpdates) {
			return !fPendingUpdates.isEmpty() || !fViewerUpdates.isEmpty();
		}
	}

	/**
	 * Asynchronous update for add/set children request.
	 *
	 * @param parent the parent model node
	 * @param element the element context
	 */
	protected abstract void add(ModelNode parent, Object element);

	/**
	 * Notification from children request monitor
	 *
	 * @param parentNode parent node
	 * @param kids list of model elements
	 */
	protected void setChildren(final ModelNode parentNode, List<Object> kids) {
		final Object[] children = filter(parentNode.getElement(), kids.toArray());
		final AsynchronousViewer viewer = getViewer();
		ViewerComparator comparator = viewer.getComparator();
		if (comparator != null) {
			comparator.sort(viewer, children);
		}

		ModelNode[] prevKids = null;
		ModelNode[] newChildren = null;
		ModelNode[] unmap = null;

		synchronized (this) {
			if (isDisposed()) {
				return;
			}
			prevKids = parentNode.getChildrenNodes();
			if (prevKids == null) {
				newChildren = new ModelNode[children.length];
				for (int i = 0; i < children.length; i++) {
					ModelNode node = new ModelNode(parentNode, children[i]);
					mapElement(children[i], node);
					newChildren[i] = node;
				}
				parentNode.setChildren(newChildren);
			} else {
				newChildren = new ModelNode[children.length];
				unmap = new ModelNode[prevKids.length];
				for (int i = 0; i < prevKids.length; i++) {
					unmap[i] = prevKids[i];
				}
				for (int i = 0; i < children.length; i++) {
					Object child = children[i];
					boolean found = false;
					for (int j = 0; j < prevKids.length; j++) {
						ModelNode prevKid = prevKids[j];
						if (prevKid != null && child.equals(prevKid.getElement())) {
							newChildren[i] = prevKid;
							prevKids[j] = null;
							found = true;
							break;
						}
					}
					if (!found) {
						newChildren[i] = new ModelNode(parentNode, child);
						mapElement(child, newChildren[i]);
					}
				}
				for (ModelNode kid : prevKids) {
					if (kid != null) {
						kid.dispose();
						unmapNode(kid);
					}
				}
				parentNode.setChildren(newChildren);
			}
			if (DebugUIPlugin.DEBUG_MODEL) {
				DebugUIPlugin.trace("CHILDREN CHANGED: " + parentNode); //$NON-NLS-1$
				DebugUIPlugin.trace(toString());
			}
		}

		//update viewer outside the lock
		final ModelNode[] finalUnmap = unmap;
		preservingSelection(() -> {
			if (finalUnmap != null) {
				for (ModelNode element : finalUnmap) {
					viewer.unmapNode(element);
				}
			}
			viewer.nodeChildrenChanged(parentNode);
		});

	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		if (fRoot != null) {
			buf.append("ROOT: "); //$NON-NLS-1$
			append(buf, fRoot, 0);
		} else {
			buf.append("ROOT: null"); //$NON-NLS-1$
		}
		return buf.toString();
	}

	private void append(StringBuilder buf, ModelNode node, int level) {
		for (int i = 0; i < level; i++) {
			buf.append('\t');
		}
		buf.append(node);
		buf.append('\n');
		ModelNode[] childrenNodes = node.getChildrenNodes();
		if (childrenNodes != null) {
			for (ModelNode childNode : childrenNodes) {
				append(buf, childNode, level + 1);
			}
		}
	}
}
