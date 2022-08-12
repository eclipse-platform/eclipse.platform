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
package org.eclipse.team.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Dialog area which displays the resources for a resource mapping
 */
public class ResourceMappingResourceDisplayArea extends DialogArea {
	private ResourceMapping mapping;
	private ResourceMappingContext context = ResourceMappingContext.LOCAL_CONTEXT;
	private TreeViewer viewer;
	private Label label;
	private IResourceMappingResourceFilter filter;
	private Map<ResourceMapping, Map<IResource, List<IResource>>> cachedFiltering = new HashMap<>();
	private String message;

	private static IWorkbenchAdapter getWorkbenchAdapter(IAdaptable o) {
		return o.getAdapter(IWorkbenchAdapter.class);
	}

	/**
	 * Return the label that should be used for the given mapping
	 * as determined using the IWorkbnchAdaptable for the mapping
	 * or it's model object.
	 * @param mapping the mappings
	 * @return it's label
	 */
	public static String getLabel(ResourceMapping mapping) {
		Object o = mapping;
		IWorkbenchAdapter workbenchAdapter = getWorkbenchAdapter((IAdaptable)o);
		if (workbenchAdapter == null) {
			Object modelObject = mapping.getModelObject();
			if (modelObject instanceof IAdaptable) {
				workbenchAdapter = getWorkbenchAdapter((IAdaptable)modelObject);
				o = modelObject;
			}
		}
		if (workbenchAdapter == null) {
			return mapping.toString();
		}
		return workbenchAdapter.getLabel(o);
	}

	public class ResourceMappingElement implements IWorkbenchAdapter, IAdaptable {
		private ResourceMapping mapping;
		private ResourceMappingContext context;

		public ResourceMappingElement(ResourceMapping mapping, ResourceMappingContext context) {
			this.mapping = mapping;
			this.context = context;
		}

		@Override
		public Object[] getChildren(Object o) {
			ResourceTraversal[] traversals = getTraversals();
			List<ResourceTraversalElement> result = new ArrayList<>();
			for (ResourceTraversal traversal : traversals) {
				IResource[] resources = traversal.getResources();
				for (IResource resource : resources) {
					if (isIncludedInFilter(resource, traversal))
						result.add(new ResourceTraversalElement(this, traversal, resource, context));
				}
			}
			return result.toArray(new Object[result.size()]);
		}

		private ResourceTraversal[] getTraversals() {
			return ResourceMappingResourceDisplayArea.getTraversals(mapping, context);
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object o) {
			o = mapping;
			IWorkbenchAdapter workbenchAdapter = getWorkbenchAdapter((IAdaptable)o);
			if (workbenchAdapter == null) {
				Object modelObject = mapping.getModelObject();
				if (modelObject instanceof IAdaptable) {
					workbenchAdapter = getWorkbenchAdapter((IAdaptable)modelObject);
					o = modelObject;
				}
			}
			if (workbenchAdapter == null) {
				return null;
			}
			return workbenchAdapter.getImageDescriptor(o);
		}

		@Override
		public String getLabel(Object o) {
			return ResourceMappingResourceDisplayArea.getLabel(mapping);
		}

		@Override
		public Object getParent(Object o) {
			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return (T) this;
			return null;
		}
	}

	/**
	 * The model element for resources that are obtained from a traversal.
	 */
	public class ResourceTraversalElement implements IWorkbenchAdapter, IAdaptable {
		private ResourceTraversal traversal;
		private ResourceMappingContext context;
		private IResource resource;
		private Object parent;

		public ResourceTraversalElement(Object parent, ResourceTraversal traversal, IResource resource, ResourceMappingContext context) {
			this.parent = parent;
			this.traversal = traversal;
			this.resource = resource;
			this.context = context;
		}

		@Override
		public Object[] getChildren(Object o) {
			if (traversal.getDepth() == IResource.DEPTH_INFINITE) {
				return getChildren(true);
			} else if (traversal.getDepth() == IResource.DEPTH_ONE && isTraversalRoot(resource)) {
				return getChildren(false);
			}
			return new Object[0];
		}

		private Object[] getChildren(boolean includeFolders) {
			try {
				if (resource.getType() != IResource.FILE) {
					IResource[] members = members(((IContainer)resource));
					List<ResourceTraversalElement> result = new ArrayList<>();
					for (IResource child : members) {
						if ((includeFolders || child.getType() == IResource.FILE)
								&& isIncludedInFilter(child, traversal))
							result.add(new ResourceTraversalElement(this, traversal, child, context));
					}
					return result.toArray(new Object[result.size()]);
				}
			} catch (CoreException e) {
				TeamUIPlugin.log(IStatus.ERROR, "An error occurred fetching the members of " + resource.getFullPath(), e); //$NON-NLS-1$
			}
			return new Object[0];
		}

		private IResource[] members(IContainer container) throws CoreException {
			if (context instanceof RemoteResourceMappingContext) {
				RemoteResourceMappingContext remoteContext = (RemoteResourceMappingContext) context;
				return ResourceMappingResourceDisplayArea.members(container, remoteContext);
			}
			return container.members();
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			IWorkbenchAdapter workbenchAdapter = getWorkbenchAdapter(resource);
			if (workbenchAdapter == null)
				return null;
			return workbenchAdapter.getImageDescriptor(resource);
		}

		@Override
		public String getLabel(Object o) {
			if (resource.getType() != IResource.PROJECT && isTraversalRoot(resource))
				return resource.getFullPath().toString();
			IWorkbenchAdapter workbenchAdapter = getWorkbenchAdapter(resource);
			if (workbenchAdapter == null)
				return resource.getName();
			return workbenchAdapter.getLabel(resource);
		}

		private boolean isTraversalRoot(IResource resource) {
			return ResourceMappingResourceDisplayArea.isTraversalRoot(traversal, resource);
		}

		@Override
		public Object getParent(Object o) {
			return parent;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return (T) this;
			return null;
		}

		public IResource getResource() {
			return resource;
		}
	}

	/**
	 * Create a dialog area that will display the resources contained in the
	 * given mapping.
	 *
	 * @param mapping the mapping
	 * @param filter the filter
	 * @param string the message to display
	 */
	public ResourceMappingResourceDisplayArea(ResourceMapping mapping, String string, IResourceMappingResourceFilter filter) {
		this.mapping = mapping;
		this.filter = filter;
		this.message = string;
	}

	@Override
	public void createArea(Composite parent) {
		Composite composite = createComposite(parent, 1, true);

		label = createWrappingLabel(composite, message, 1);
		viewer = new TreeViewer(composite);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 100;
		viewer.getControl().setLayoutData(gridData);
		viewer.setContentProvider(new WorkbenchContentProvider());
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.setComparator(new ResourceComparator(ResourceComparator.NAME) {
			@Override
			public int compare(Viewer viewer, Object o1, Object o2) {
				if (o1 instanceof ResourceTraversalElement && o2 instanceof ResourceTraversalElement) {
					ResourceTraversalElement e1 = (ResourceTraversalElement) o1;
					ResourceTraversalElement e2 = (ResourceTraversalElement) o2;
					return super.compare(viewer, e1.getResource(), e2.getResource());
				}
				return super.compare(viewer, o1, o2);
			}
		});
		setInput(message);
		Dialog.applyDialogFont(parent);
	}

	private void setInput(String labelText) {
		if (viewer != null) {
			Object o = null;
			if (mapping != null)
				o = new ResourceMappingElement(mapping, context);
			viewer.setInput(o);
		}
		if (label != null) {
			this.message = labelText;
			label.setText(labelText);
		}
	}

	public void setMapping(ResourceMapping mapping, String labelText) {
		this.mapping = mapping;
		setInput(labelText);
	}

	private boolean isIncludedInFilter(IResource resource, ResourceTraversal traversal) {
		if (filter == null)
			return true;
		Map<IResource, List<IResource>> mappingResources = cachedFiltering.get(mapping);
		if (mappingResources == null) {
			mappingResources = buildFilteredResourceMap(mapping, context);
			cachedFiltering.put(mapping, mappingResources);
		}
		return mappingResources.containsKey(resource);
	}

	private Map<IResource, List<IResource>> buildFilteredResourceMap(final ResourceMapping mapping, final ResourceMappingContext context) {
		final Map<IResource, List<IResource>> result = new HashMap<>();
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask(null, IProgressMonitor.UNKNOWN);
						ResourceTraversal[] traversals = mapping.getTraversals(context, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
						for (ResourceTraversal traversal : traversals) {
							buildFilteredResourceMap(mapping, traversal, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN), result);
						}
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}

				private void buildFilteredResourceMap(final ResourceMapping mapping,
						final ResourceTraversal traversal, IProgressMonitor monitor,
						final Map<IResource, List<IResource>> result) throws CoreException {
					traversal.accept(resource -> {
						if (filter.select(resource, mapping, traversal)) {
							// Add the resource to the result
							result.put(resource, new ArrayList<>());
							// Make sure that there are parent folders for the resource up to the traversal root
							IResource child = resource;
							while (!isTraversalRoot(traversal, child)) {
								IContainer parent = child.getParent();
								List<IResource> children = result.get(parent);
								if (children == null) {
									children = new ArrayList<>();
									result.put(parent, children);
								}
								children.add(child);
								child = parent;
							}
						}
						return true;
					});

				}
			});
		} catch (InvocationTargetException e) {
			TeamUIPlugin.log(IStatus.ERROR, "An error occurred while filtering " + getLabel(mapping), e); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// Ignore
		}
		return result;
	}

	/* private */ static ResourceTraversal[] getTraversals(final ResourceMapping mapping, final ResourceMappingContext context) {
		final List<ResourceTraversal[]> traversals = new ArrayList<>();
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				try {
					traversals.add(mapping.getTraversals(context, monitor));
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
			return traversals.get(0);
		} catch (InvocationTargetException e) {
			TeamUIPlugin.log(IStatus.ERROR, "An error occurred while traversing " + getLabel(mapping), e); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// Ignore
		}
		return new ResourceTraversal[0];
	}

	/* private */ static IResource[] members(final IContainer container, final RemoteResourceMappingContext context) {
		final List<IResource[]> members = new ArrayList<>();
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				try {
					members.add(context.fetchMembers(container, monitor));
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
			return members.get(0);
		} catch (InvocationTargetException e) {
			TeamUIPlugin.log(IStatus.ERROR, "An error occurred while fetching the members of" + container.getFullPath(), e); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// Ignore
		}
		return new IResource[0];
	}

	/* private */ static boolean isTraversalRoot(ResourceTraversal traversal, IResource resource) {
		IResource[] resources = traversal.getResources();
		for (IResource root : resources) {
			if (root.equals(resource)) {
				return true;
			}
		}
		return false;
	}
}
