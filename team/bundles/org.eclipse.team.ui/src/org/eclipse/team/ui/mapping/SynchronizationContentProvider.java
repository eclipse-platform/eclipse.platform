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
package org.eclipse.team.ui.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffChangeListener;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;

/**
 * Abstract team aware content provider that delegates to another content provider.
 *
 * @since 3.2
 */
public abstract class SynchronizationContentProvider implements ICommonContentProvider, IDiffChangeListener, IPropertyChangeListener {

	private Viewer viewer;
	private boolean empty;
	private ICommonContentExtensionSite site;

	@Override
	public Object[] getChildren(Object parent) {
		return internalGetChildren(parent, false);
	}

	@Override
	public Object[] getElements(Object parent) {
		return internalGetChildren(parent, true);
	}

	@Override
	public Object getParent(Object element) {
		element = internalGetElement(element);
		if (element instanceof ModelProvider)
			return null;
		if (element == getModelRoot())
			return null;
		Object parent = getDelegateContentProvider().getParent(element);
		if (parent == getModelRoot())
			return getModelProvider();
		return parent;
	}

	@Override
	public boolean hasChildren(Object element) {
		return internalHasChildren(element);
	}

	private Object[] internalGetChildren(Object parent, boolean isElement) {
		Object element = internalGetElement(parent);
		if (element instanceof ISynchronizationScope) {
			// If the root is a scope, we want to include all models in the scope
			ISynchronizationScope rms = (ISynchronizationScope) element;
			if (rms.getMappings(getModelProviderId()).length > 0) {
				empty = false;
				return new Object[] { getModelProvider() };
			}
			empty = true;
			return new Object[0];
		} else if (element instanceof ISynchronizationContext) {
			ISynchronizationContext context = (ISynchronizationContext)element;
			// If the root is a context, we want to filter by the context
			ISynchronizationContext sc = (ISynchronizationContext) element;
			if (sc.getScope().getMappings(getModelProviderId()).length > 0) {
				Object root = getModelRoot();
				boolean initialized = isInitialized(context);
				if (!initialized || getChildrenInContext(sc, root, getDelegateChildren(root, isElement)).length > 0) {
					if (!initialized)
						requestInitialization(context);
					empty = false;
					return new Object[] { getModelProvider() };
				}
			}
			empty = true;
			return new Object[0];
		}
		if (element == getModelProvider()) {
			ISynchronizationContext context = getContext();
			if (context != null && !isInitialized(context)) {
				return new Object[0];
			}
			element = getModelRoot();
			if (parent instanceof TreePath) {
				parent = TreePath.EMPTY.createChildPath(element);
			} else {
				parent = element;
			}
		}
		Object[] delegateChildren = getDelegateChildren(parent, isElement);
		ISynchronizationContext context = getContext();
		if (context == null) {
			ISynchronizationScope scope = getScope();
			if (scope == null) {
				return delegateChildren;
			} else {
				return getChildrenInScope(scope, parent, delegateChildren);
			}
		} else {
			return getChildrenInContext(context, parent, delegateChildren);
		}
	}

	/**
	 * Return whether the content provider has been initialized and is ready to
	 * provide content in the given context. By default, <code>true</code> is returned. Subclasses
	 * that need to perform extra processing to prepare should override this method and
	 * also override {@link #requestInitialization(ISynchronizationContext)}.
	 *
	 * @param context the context
	 * @return whether the content provider has been initialized and is ready to
	 * provide content in he given context.
	 */
	protected boolean isInitialized(ISynchronizationContext context) {
		return true;
	}

	/**
	 * Subclasses that need to perform extra processing to prepare their model
	 * to be displayed by this content provider should override this method and
	 * launch a background task to prepare what is required to display their
	 * model for the given context. An appropriate viewer refresh on the model
	 * provider should be issued when the model is prepared.
	 *
	 * @param context
	 *            the context
	 */
	protected void requestInitialization(ISynchronizationContext context) {
		// Do nothing by default
	}

	/**
	 * Return the children for the given element from the
	 * delegate content provider.
	 * @param parent the parent element
	 * @return the children for the given element from the
	 * delegate content provider
	 */
	protected Object[] getDelegateChildren(Object parent) {
		return getDelegateContentProvider().getChildren(internalGetElement(parent));
	}

	private Object[] getDelegateChildren(Object parent, boolean isElement) {
		if (isElement)
			return getDelegateContentProvider().getElements(parent);
		return getDelegateChildren(parent);
	}

	private boolean internalHasChildren(Object elementOrPath) {
		//TODO: What about the context and scope
		Object element = internalGetElement(elementOrPath);
		if (element instanceof ModelProvider) {
			element = getModelRoot();
		}
		if (getDelegateContentProvider().hasChildren(element)) {
			ISynchronizationContext sc = getContext();
			if (sc == null) {
				ISynchronizationScope scope = getScope();
				if (scope == null) {
					return true;
				} else {
					return hasChildrenInScope(scope, elementOrPath);
				}
			} else {
				return hasChildrenInContext(sc, elementOrPath);
			}
		} else {
			ISynchronizationContext sc = getContext();
			if (sc != null)
				return hasChildrenInContext(sc, elementOrPath);
		}
		return false;
	}

	/**
	 * Return whether the given element has children in the given scope.
	 * By default, true is returned if the given element contains any elements
	 * in the scope or if any of the elements in the scope contain the given
	 * element and the delegate provider returns children for the element.
	 * The {@link ResourceMapping#contains(ResourceMapping)} is used to test
	 * for containment.
	 * Subclasses may override to provide a more efficient implementation.
	 * @param scope the scope
	 * @param element the element
	 * @return whether the given element has children in the given scope
	 */
	protected boolean hasChildrenInScope(ISynchronizationScope scope, Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(internalGetElement(element));
		if (mapping != null) {
			ResourceMapping[] mappings = scope.getMappings(mapping.getModelProviderId());
			for (ResourceMapping sm : mappings) {
				if (mapping.contains(sm)) {
					return true;
				}
				if (sm.contains(mapping)) {
					return getDelegateChildren(element).length > 0;
				}
			}
		}
		return false;
	}

	/**
	 * Return whether the given element has children in the given
	 * context. The children may or may not exist locally.
	 * By default, this method returns true if the traversals for
	 * the element contain any diffs. This could result in false
	 * positives. Subclasses can override to provide a more
	 * efficient or precise answer.
	 * @param element a model element.
	 * @return whether the given element has children in the given context
	 */
	protected boolean hasChildrenInContext(ISynchronizationContext context, Object element) {
		ResourceTraversal[] traversals = getTraversals(context, element);
		if (traversals == null)
			return true;
		return context.getDiffTree().getDiffs(traversals).length > 0;
	}

	@Override
	public void dispose() {
		ICommonContentExtensionSite extensionSite = getExtensionSite();
		if (extensionSite != null) {
			extensionSite.getExtensionStateModel().removePropertyChangeListener(this);
		}
		ISynchronizationContext context = getContext();
		if (context != null)
			context.getDiffTree().removeDiffChangeListener(this);
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (configuration != null)
			configuration.removePropertyChangeListener(this);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		getDelegateContentProvider().inputChanged(viewer, oldInput, newInput);
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		// Set the site
		this.site = site;
		// Configure the content provider based on the site and state model
		site.getExtensionStateModel().addPropertyChangeListener(this);
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (configuration != null)
			configuration.addPropertyChangeListener(this);
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).init(site);
		}
		ISynchronizationContext context = getContext();
		if (context != null)
			context.getDiffTree().addDiffChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		// TODO: this could happen at the root as well
		if (event.getProperty().equals(ISynchronizePageConfiguration.P_MODE)) {
			refresh();
		}
	}

	/**
	 * Return whether elements with the given direction should be included in
	 * the contents. The direction is one of {@link IThreeWayDiff#INCOMING},
	 * {@link IThreeWayDiff#OUTGOING} or {@link IThreeWayDiff#CONFLICTING}.
	 * This method is invoked by the
	 * {@link #getChildrenInContext(ISynchronizationContext, Object, Object[])}
	 * method to filter the list of children returned when
	 * {@link #getChildren(Object) } is called. It accessing the
	 * <code>ISynchronizePageConfiguration.P_MODE</code> property on the state
	 * model provided by the view to determine what kinds should be included.
	 *
	 * @param direction
	 *            the synchronization direction
	 * @return whether elements with the given synchronization kind should be
	 *         included in the contents
	 */
	protected boolean includeDirection(int direction) {
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (configuration != null)
			return ((SynchronizePageConfiguration)configuration).includeDirection(direction);
		return true;
	}

	/**
	 * Return the synchronization context associated with the view to which
	 * this content provider applies. A <code>null</code> is returned if
	 * no context is available.
	 * @return the synchronization context or <code>null</code>
	 */
	protected ISynchronizationContext getContext() {
		ICommonContentExtensionSite extensionSite = getExtensionSite();
		if (extensionSite != null)
			return (ISynchronizationContext) extensionSite
					.getExtensionStateModel()
						.getProperty(
							ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
		return null;
	}

	/**
	 * Return the resource mapping scope associated with the view to which
	 * this content provider applies. A <code>null</code> is returned if
	 * no scope is available.
	 * @return the resource mapping scope or <code>null</code>
	 */
	protected ISynchronizationScope getScope() {
		ICommonContentExtensionSite extensionSite = getExtensionSite();
		if (extensionSite != null)
			return (ISynchronizationScope) extensionSite
					.getExtensionStateModel()
						.getProperty(
							ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE);
		return null;
	}

	/**
	 * Return the synchronization page configuration associated with the view to which
	 * this content provider applies. A <code>null</code> is returned if
	 * no configuration is available.
	 * @return the synchronization page configuration or <code>null</code>
	 */
	protected ISynchronizePageConfiguration getConfiguration() {
		ICommonContentExtensionSite extensionSite = getExtensionSite();
		if (extensionSite != null)
			return (ISynchronizePageConfiguration) extensionSite
					.getExtensionStateModel()
						.getProperty(
							ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION);
		return null;
	}

	@Override
	public void restoreState(IMemento aMemento) {
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).restoreState(aMemento);
		}
	}

	@Override
	public void saveState(IMemento aMemento) {
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).saveState(aMemento);
		}
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		refresh();
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Property changes only affect labels
	}

	/**
	 * Refresh the subtree associated with this model.
	 */
	protected void refresh() {
		Utils.syncExec((Runnable) () -> {
			TreeViewer treeViewer = ((TreeViewer)getViewer());
			// TODO: Need to know if the model root is present in order to refresh properly
			if (empty)
				treeViewer.refresh();
			else
				treeViewer.refresh(getModelProvider());
		}, getViewer().getControl());
	}

	/**
	 * Return the model content provider that the team aware content
	 * provider delegates to.
	 * @return the model content provider
	 */
	protected abstract ITreeContentProvider getDelegateContentProvider();

	/**
	 * Return the model provider for this content provider.
	 * @return the model provider for this content provider
	 */
	protected final ModelProvider getModelProvider() {
		try {
			return ModelProvider.getModelProviderDescriptor(getModelProviderId()).getModelProvider();
		} catch (CoreException e) {
			// TODO: this is a bit harsh. can we do something less destructive
			throw new IllegalStateException();
		}
	}

	/**
	 * Return the id of model provider for this content provider.
	 * @return the model provider for this content provider
	 */
	protected abstract String getModelProviderId();

	/**
	 * Return the object that acts as the model root. It is used when getting the children
	 * for a model provider.
	 * @return the object that acts as the model root
	 */
	protected abstract Object getModelRoot();

	/**
	 * Return the viewer to which the content provider is associated.
	 * @return the viewer to which the content provider is associated
	 */
	protected final Viewer getViewer() {
		return viewer;
	}

	/**
	 * Return the subset of the given children that are in the
	 * given scope or are parents of elements that are in scope.
	 * @param scope the scope
	 * @param parent the parent of the given children
	 * @param children all the children of the parent that are in scope.
	 * @return the subset of the given children that are in the
	 * scope of the content provider
	 */
	protected Object[] getChildrenInScope(ISynchronizationScope scope, Object parent, Object[] children) {
		List<Object> result = new ArrayList<>();
		for (Object object : children) {
			if (object != null && isInScope(scope, parent, object)) {
				result.add(object);
			}
		}
		return result.toArray(new Object[result.size()]);
	}

	/**
	 * Return the subset of children that are of interest from the given context.
	 * By default, this method returns those children whose traversals contain
	 * a diff in the context. However, it does not include those model elements
	 * that do not exist locally but are within the context (e.g. locally deleted
	 * elements and remotely added elements). Subclasses must override to include
	 * these.
	 * @param context the context
	 * @param parent the parent of the children
	 * @param children the children
	 * @return the subset of children that are of interest from the given context
	 */
	protected Object[] getChildrenInContext(ISynchronizationContext context, Object parent, Object[] children) {
		if (children.length != 0)
			children = getChildrenInScope(context.getScope(), parent, children);
		if (parent instanceof IResource) {
			IResource resource = (IResource) parent;
			children = getChildrenWithPhantoms(context, resource, children);
		}
		if (children.length == 0)
			return children;
		return internalGetChildren(context, parent, children);
	}

	private Object[] getChildrenWithPhantoms(ISynchronizationContext context, IResource resource, Object[] children) {
		IResource[] setChildren = context.getDiffTree().members(resource);
		if (setChildren.length == 0)
			return children;
		if (children.length == 0)
			return setChildren;
		Set<Object> result = new HashSet<>(children.length);
		Collections.addAll(result, children);
		Collections.addAll(result, setChildren);
		return result.toArray();
	}

	private Object[] internalGetChildren(ISynchronizationContext context, Object parent, Object[] children) {
		List<Object> result = new ArrayList<>(children.length);
		for (Object object : children) {
			// If the parent is a TreePath then the subclass is
			// TreePath aware and we can send a TrePath to the
			// isVisible method
			if (parent instanceof TreePath) {
				TreePath tp = (TreePath) parent;
				object = tp.createChildPath(object);
			}
			if (isVisible(context, object))
				result.add(internalGetElement(object));
		}
		return result.toArray(new Object[result.size()]);
	}

	/**
	 * Return whether the given object is visible in the synchronization page
	 * showing this content based on the diffs in the given context. Visibility
	 * is determined by obtaining the diffs for the object from the context by
	 * calling {@link #getTraversals(ISynchronizationContext, Object)} to get
	 * the traversals, then obtaining the diffs from the context's diff tree and
	 * then calling {@link #isVisible(IDiff)} for each diff.
	 *
	 * @param context
	 *            the synchronization context
	 * @param object
	 *            the object
	 * @return whether the given object is visible in the synchronization page
	 *         showing this content
	 */
	protected boolean isVisible(ISynchronizationContext context, Object object) {
		ResourceTraversal[] traversals = getTraversals(context, object);
		IDiff[] deltas = context.getDiffTree().getDiffs(traversals);
		boolean visible = false;
		if (isVisible(deltas)) {
			visible = true;
		}
		return visible;
	}

	private boolean isVisible(IDiff[] diffs) {
		if (diffs.length > 0) {
			for (IDiff diff : diffs) {
				if (isVisible(diff)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return whether the given diff should be visible based on the
	 * configuration of the synchronization page showing this content. An
	 * {@link IThreeWayDiff} is visible if the direction of the change matches
	 * the mode of the synchronization page. An {@link ITwoWayDiff} is visible
	 * if it has a kind that represents a change.
	 *
	 * @param diff
	 *            the diff
	 * @return whether the diff should be visible
	 */
	protected boolean isVisible(IDiff diff) {
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			return includeDirection(twd.getDirection());
		}
		return diff.getKind() != IDiff.NO_CHANGE;
	}

	/**
	 * Return the traversals for the given object in the given context. This
	 * method must not be long running. If a long running calculation is required
	 * to calculate the traversals, an empty traversal should be returned and the
	 * content provider should initiate a background task to calculate the
	 * required traversals and update the view according when the task completes.
	 * @param context the synchronization context
	 * @param object the object
	 * @return the traversals for the given object in the given context
	 */
	protected abstract ResourceTraversal[] getTraversals(ISynchronizationContext context, Object object);

	/**
	 * Handle the given exception that occurred while calculating the
	 * children for an element.
	 * @param e the exception
	 */
	protected void handleException(CoreException e) {
		TeamPlugin.log(e);
	}

	/**
	 * Return whether the given object is within the scope of this
	 * content provider. The object is in scope if it is part of
	 * a resource mapping in the scope or is the parent of resources
	 * covered by one or more resource mappings in the scope.
	 * By default, this compares the mapping of the given element
	 * with those in the scope using the {@link ResourceMapping#contains(ResourceMapping)}
	 * method to determine if the element is in the scope. Subclasses may
	 * override to provide a more efficient means of doing the check.
	 * @param scope the scope
	 * @param parent the parent of the object
	 * @param element the object
	 * @return whether the given object is within the scope of this
	 * content provider
	 */
	protected boolean isInScope(ISynchronizationScope scope, Object parent, Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(internalGetElement(element));
		if (mapping != null) {
			ResourceMapping[] mappings = scope.getMappings(mapping.getModelProviderId());
			for (ResourceMapping sm : mappings) {
				if (mapping.contains(sm)) {
					return true;
				}
				if (sm.contains(mapping)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return the Common Navigator extension site for this
	 * content provider.
	 * @return the Common Navigator extension site for this
	 * content provider
	 */
	public ICommonContentExtensionSite getExtensionSite() {
		return site;
	}

	private Object internalGetElement(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			return tp.getLastSegment();
		}
		return elementOrPath;
	}

	/**
	 * Return whether the page has been set to use a flat layout.
	 * @return whether the page has been set to use a flat layout
	 * @since 3.3
	 */
	protected final boolean isFlatLayout() {
		ISynchronizePageConfiguration c = getConfiguration();
		if (c != null) {
			String p = (String)c.getProperty(ITeamContentProviderManager.PROP_PAGE_LAYOUT);
			return p != null && p.equals(ITeamContentProviderManager.FLAT_LAYOUT);
		}
		return false;
	}
}
