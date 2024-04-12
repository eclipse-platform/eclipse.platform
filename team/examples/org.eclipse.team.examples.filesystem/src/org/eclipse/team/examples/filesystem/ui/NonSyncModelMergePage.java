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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.internal.ui.mapping.SynchronizationResourceMappingContext;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.team.ui.synchronize.ModelOperation;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.Page;

/**
 * This class provides the page for the {@link NonSyncMergePart}.
 */
public class NonSyncModelMergePage extends Page {

	IMergeContext context;
	TreeViewer viewer;
	List mappings;

	/*
	 * Content provider that returns the list of conflicting mappings
	 */
	class PageContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IMergeContext) {
				if (mappings == null)
					// TODO: should be using a real progress monitor
					computeMappings(new NullProgressMonitor());
				return mappings.toArray();
			}
			return new Object[0];
		}
		@Override
		public Object getParent(Object element) {
			if (element instanceof ResourceMapping) {
				return context;
			}
			return null;
		}
		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof IMergeContext) {
				return true;
			}
			return false;
		}
		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}
		@Override
		public void dispose() {
			// Nothing to do
		}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Nothing to do
		}
	}

	/*
	 * Label provider that provides a label and image for conflicting resource mappings
	 */
	class PageLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof ResourceMapping) {
				ResourceMapping mapping = (ResourceMapping) element;
				ISynchronizationCompareAdapter adapter = NonSyncMergePart.getCompareAdapter(mapping);
				if (adapter != null)
					return adapter.getPathString(mapping) + "(" + mapping.getModelProvider().getDescriptor().getLabel() + ")";
			}
			if (element instanceof ICompareInput) {
				ICompareInput ci = (ICompareInput) element;
				ci.getName();
			}
			return super.getText(element);
		}
		@Override
		public Image getImage(Object element) {
			if (element instanceof ICompareInput) {
				ICompareInput ci = (ICompareInput) element;
				ci.getImage();
			}
			if (element instanceof ResourceMapping) {
				ResourceMapping mapping = (ResourceMapping) element;
				ISynchronizationCompareAdapter adapter = NonSyncMergePart.getCompareAdapter(mapping);
				ICompareInput input = adapter.asCompareInput(context, mapping.getModelObject());
				if (input != null)
					return input.getImage();
			}
			return super.getImage(element);
		}
	}

	/*
	 * Sorter that sorts mappings by model and then name
	 */
	class PageSorter extends ViewerComparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof ResourceMapping && e2 instanceof ResourceMapping) {
				ResourceMapping m1 = (ResourceMapping) e1;
				ResourceMapping m2 = (ResourceMapping) e2;
				if (m1.getModelProvider() == m2.getModelProvider()) {
					return getLabel(m1).compareTo(getLabel(m2));
				}
				return compare(m1, m2);
			}
			return super.compare(viewer, e1, e2);
		}
		private int compare(ResourceMapping m1, ResourceMapping m2) {
			ModelProvider[] sorted = ModelOperation.sortByExtension(new ModelProvider[] { m1.getModelProvider(), m2.getModelProvider() });
			return sorted[0] == m1.getModelProvider() ? -1 : 1;
		}
		private String getLabel(ResourceMapping mapping) {
			ISynchronizationCompareAdapter adapter = NonSyncMergePart.getCompareAdapter(mapping);
			if (adapter != null)
				return adapter.getPathString(mapping);
			return "";
		}
	}

	public NonSyncModelMergePage(IMergeContext context) {
		super();
		this.context = context;
	}

	/**
	 * Create the list of all mappings that overlap with the out-of-sync files.
	 */
	public void computeMappings(IProgressMonitor monitor) {
		IModelProviderDescriptor[] descriptors = ModelProvider.getModelProviderDescriptors();
		mappings = new ArrayList();
		for (IModelProviderDescriptor descriptor : descriptors) {
			// Get the subset of files that this model provider cares about
			try {
				IResource[] resources = descriptor.getMatchingResources(getOutOfSyncFiles());
				if (resources.length > 0) {
					ModelProvider provider = descriptor.getModelProvider();
					// Get the mappings for those resources
					ResourceMapping[] mappings = provider.getMappings(resources, new SynchronizationResourceMappingContext(context), monitor);
					this.mappings.addAll(Arrays.asList(mappings ));
				}
			} catch (CoreException e) {
				FileSystemPlugin.log(e);
			}
		}
	}

	private IResource[] getOutOfSyncFiles() {
		IDiff[] diffs = getContext().getDiffTree().getDiffs(ResourcesPlugin.getWorkspace().getRoot(), IResource.DEPTH_INFINITE);
		List<IResource> result = new ArrayList<>();
		for (IDiff diff : diffs) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource.getType() == IResource.FILE)
				result.add(resource);
		}
		return result.toArray(new IResource[result.size()]);
	}

	/**
	 * Return the merge context.
	 * @return the merge context
	 */
	public IMergeContext getContext() {
		return context;
	}

	@Override
	public void createControl(Composite parent) {
		viewer = new TreeViewer(parent);
		viewer.setContentProvider(new PageContentProvider());
		viewer.setLabelProvider(new PageLabelProvider());
		viewer.setComparator(new PageSorter());
		hookContextMenu(viewer);
		viewer.setInput(context);
	}

	/*
	 * Hook the context menu to display the Overwrite and Mark-as-merged actions
	 */
	private void hookContextMenu(final TreeViewer viewer) {
		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this::fillContextMenu);
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}

	/**
	 * Fill the context menu.
	 * @param manager the context menu manager
	 */
	protected void fillContextMenu(IMenuManager manager) {
		/*
		 * Add a mark as merged action. Because we are not using the
		 * Synchronization framework and, more specifically, the
		 * Common Navigator content provider for the model providers,
		 * we do not have access to the merge handlers of the model.
		 * Therefore, we are writing are action to detect whether there
		 * are files that overlap between the selected model elements and
		 * unselected model elements.
		 */
		Action markAsMerged = new Action("Mark as Merged") {
			@Override
			public void run() {
				try {
					final IStructuredSelection selection = viewer.getStructuredSelection();
					PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
						IDiff[] diffs = getSelectedDiffs(selection, monitor);
						if (!checkForModelOverlap(diffs, monitor)) {
							return;
						}
						try {
							context.markAsMerged(diffs, false, monitor);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					});
				} catch (InvocationTargetException e) {
					FileSystemPlugin.log(new Status(IStatus.ERROR, FileSystemPlugin.ID, 0, e.getTargetException().getMessage(), e.getTargetException()));
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		};
		manager.add(markAsMerged);
	}

	protected IDiff[] getSelectedDiffs(IStructuredSelection selection, IProgressMonitor monitor) {
		Object[] elements = selection.toArray();
		return getDiffs(elements, monitor);
	}

	private IDiff[] getDiffs(Object[] elements, IProgressMonitor monitor) {
		Set<IDiff> result = new HashSet<>();
		for (Object element : elements) {
			try {
				if (element instanceof ResourceMapping) {
					ResourceMapping mapping = (ResourceMapping) element;
					ResourceTraversal[] traversals = mapping.getTraversals(new SynchronizationResourceMappingContext(context), monitor);
					result.addAll(Arrays.asList(context.getDiffTree().getDiffs(traversals)));
				}
			} catch (CoreException e) {
				FileSystemPlugin.log(e);
			}
		}
		return result.toArray(new IDiff[result.size()]);
	}

	/**
	 * Check whether any of the diffs overlap with mappings that are not selected
	 */
	protected boolean checkForModelOverlap(IDiff[] diffs, IProgressMonitor monitor) {
		// TODO: This check should see if the diffs are also part of mappings
		// that are not included in the selection.
		return true;
	}

	@Override
	public Control getControl() {
		if (viewer != null)
			return viewer.getControl();
		return null;
	}

	@Override
	public void setFocus() {
		if (viewer != null)
			viewer.getControl().setFocus();
	}

	public ISelectionProvider getViewer() {
		return viewer;
	}

}
