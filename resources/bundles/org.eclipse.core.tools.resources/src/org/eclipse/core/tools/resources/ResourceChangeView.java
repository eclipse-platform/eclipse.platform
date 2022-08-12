/**********************************************************************
 * Copyright (c) 2002, 2018 Geoff Longman and others.
 *
 *   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Geoff Longman - Initial API and implementation
 * IBM - Tightening integration with existing Platform
 **********************************************************************/
package org.eclipse.core.tools.resources;

import org.eclipse.jface.action.Action;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tools.SpyView;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;

/**
 * This spy view displays a tree-based representation of resource deltas from
 * resource change events.
 */
public class ResourceChangeView extends SpyView implements IResourceChangeListener {
	static class DeltaNode implements IAdaptable {
		private ArrayList<DeltaNode> children;
		private int deltaFlags = -1;
		private int deltaKind = -1;
		private DeltaNode parent = null;
		private IPath path;
		private IResource resource;

		public DeltaNode() {
			children = new ArrayList<>();
		}

		public DeltaNode(IResourceDelta delta) {
			this();
			populateFromDelta(delta);
		}

		public void addChild(DeltaNode child) {
			children.add(child);
			child.setParent(this);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getAdapter(Class<T> key) {
			if (key != IResource.class)
				return null;
			return (T) resource;
		}

		public DeltaNode[] getChildren() {
			return children.toArray(new DeltaNode[children.size()]);
		}

		public int getDeltaFlags() {
			return deltaFlags;
		}

		public int getDeltaKind() {
			return deltaKind;
		}

		public DeltaNode getParent() {
			return parent;
		}

		public IPath getPath() {
			return path;
		}

		public IResource getResource() {
			return resource;
		}

		public boolean hasChildren() {
			return children.size() > 0;
		}

		public void populateFromDelta(IResourceDelta delta) {
			deltaFlags = delta.getFlags();
			deltaKind = delta.getKind();
			path = delta.getFullPath();
			resource = delta.getResource();
			IResourceDelta[] deltaChildren = delta.getAffectedChildren();
			for (int i = 0; i < deltaChildren.length; i++)
				addChild(new DeltaNode(deltaChildren[i]));
		}

		public void removeChild(DeltaNode child) {
			children.remove(child);
			child.setParent(null);
		}

		public void setParent(DeltaNode parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return resource.getName();
		}
	}

	class FilterAction extends Action {
		private int type;

		public FilterAction(String text, int eventType) {
			super(text);
			type = eventType;
		}

		/**
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		@Override
		public void run() {
			typeFilter = type;
			for (Action action : eventActions) {
				action.setChecked(action.equals(this));
			}
		}
	}

	class FilterPhantoms extends Action {
		public FilterPhantoms() {
			super("Show Phantoms"); //$NON-NLS-1$
		}

		@Override
		public void run() {
			showPhantoms = !showPhantoms;
			setChecked(showPhantoms);
		}
	}

	static class ResourceEventNode extends DeltaNode {
		private int eventType;

		public ResourceEventNode(IResourceChangeEvent event) {
			super(event.getDelta());
			eventType = event.getType();
		}

		public int getEventType() {
			return eventType;
		}
	}

	class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
		private DeltaNode invisibleRoot;

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public Object[] getChildren(Object parent) {
			return ((DeltaNode) parent).getChildren();
		}

		@Override
		public Object[] getElements(Object parent) {

			if (parent.equals(rootObject)) {
				if (invisibleRoot == null) {
					if (rootObject instanceof IResourceChangeEvent) {
						initialize(rootObject);
					} else {
						initialize();
					}
				}
				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}

		@Override
		public Object getParent(Object child) {
			return ((DeltaNode) child).getParent();
		}

		@Override
		public boolean hasChildren(Object parent) {
			return ((DeltaNode) parent).hasChildren();
		}

		/*
		 * We will set up a dummy model to initialize tree hierarchy.
		 */
		private void initialize() {
			invisibleRoot = new DeltaNode();
		}

		private void initialize(Object input) {
			if (!(input instanceof IResourceChangeEvent))
				return;
			IResourceChangeEvent evt = (IResourceChangeEvent) input;
			ResourceEventNode root = new ResourceEventNode(evt);
			invisibleRoot = new DeltaNode();
			invisibleRoot.addChild(root);
		}

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			invisibleRoot = null;
		}
	}

	class ViewLabelProvider extends LabelProvider {

		private String getEventTypeAsString(int eventType) {
			switch (eventType) {
				case IResourceChangeEvent.POST_BUILD :
					return "POST_BUILD"; //$NON-NLS-1$
				case IResourceChangeEvent.POST_CHANGE :
					return "POST_CHANGE"; //$NON-NLS-1$
				case IResourceChangeEvent.PRE_BUILD :
					return "PRE_BUILD"; //$NON-NLS-1$
			}
			return null;
		}

		/**
		 * Return a string representation of the change flags found
		 * within a resource change event.
		 */
		private String getFlagsAsString(int flags) {
			StringBuilder buffer = new StringBuilder();
			if ((IResourceDelta.ADDED & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("ADDED"); //$NON-NLS-1$
			}
			if ((IResourceDelta.ADDED_PHANTOM & flags) != 0 && showPhantoms) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("ADDED_PHANTOM"); //$NON-NLS-1$
			}
			if ((IResourceDelta.ALL_WITH_PHANTOMS & flags) != 0 && showPhantoms) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("ALL_WITH_PHANTOMS"); //$NON-NLS-1$
			}
			if ((IResourceDelta.CHANGED & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("CHANGED"); //$NON-NLS-1$
			}
			if ((IResourceDelta.CONTENT & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("CONTENT"); //$NON-NLS-1$
			}
			if ((IResourceDelta.DESCRIPTION & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("DESCRIPTION"); //$NON-NLS-1$
			}
			if ((IResourceDelta.MARKERS & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("MARKERS"); //$NON-NLS-1$
			}
			if ((IResourceDelta.MOVED_FROM & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("MOVED_FROM"); //$NON-NLS-1$
			}
			if ((IResourceDelta.MOVED_TO & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("MOVED_TO"); //$NON-NLS-1$
			}
			if ((IResourceDelta.NO_CHANGE & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("NO_CHANGE"); //$NON-NLS-1$
			}
			if ((IResourceDelta.OPEN & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("OPEN"); //$NON-NLS-1$
			}
			if ((IResourceDelta.REMOVED & flags) != 0) {
				buffer.append("-");
				buffer.append("REMOVED");
			}
			if ((IResourceDelta.REMOVED_PHANTOM & flags) != 0 && showPhantoms) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("REMOVED_PHANTOM"); //$NON-NLS-1$
			}
			if ((IResourceDelta.REPLACED & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("REPLACED"); //$NON-NLS-1$
			}
			if ((IResourceDelta.SYNC & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("SYNC"); //$NON-NLS-1$
			}
			if ((IResourceDelta.TYPE & flags) != 0) {
				buffer.append("-"); //$NON-NLS-1$
				buffer.append("TYPE"); //$NON-NLS-1$
			}
			return buffer.toString();
		}

		@Override
		public Image getImage(Object obj) {
			DeltaNode node = (DeltaNode) obj;
			String imageKey;
			switch (node.getResource().getType()) {
				case IResource.ROOT :
					imageKey = ISharedImages.IMG_OBJ_FOLDER;
					break;
				case IResource.PROJECT :
					imageKey = org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT;
					break;
				case IResource.FOLDER :
					imageKey = ISharedImages.IMG_OBJ_FOLDER;
					break;
				case IResource.FILE :
				default :
					imageKey = ISharedImages.IMG_OBJ_FILE;
					break;
			}
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}

		private String getKindAsString(int kind) {
			if (kind == -1)
				return ""; //$NON-NLS-1$
			return " kind(" + getFlagsAsString(kind) + ") "; //$NON-NLS-1$ //$NON-NLS-2$
		}

		@Override
		public String getText(Object obj) {
			StringBuilder buffer = new StringBuilder(obj.toString());
			if (obj instanceof ResourceEventNode) {
				buffer.append("Workspace Root - "); //$NON-NLS-1$
				buffer.append(getEventTypeAsString(((ResourceEventNode) obj).getEventType()));
			}
			buffer.append(getKindAsString(((DeltaNode) obj).getDeltaKind()));
			buffer.append(getFlagsAsString(((DeltaNode) obj).getDeltaFlags()));
			return buffer.toString();
		}
	}

	protected Set<Action> eventActions = new HashSet<>();
	private Action PHANTOMS;
	private Action POST_BUILD;

	private Action POST_CHANGE;

	private Action PRE_BUILD;

	protected Object rootObject = ResourcesPlugin.getWorkspace();

	protected boolean showPhantoms = false;

	protected int typeFilter = IResourceChangeEvent.POST_CHANGE;

	protected TreeViewer viewer;

	/**
	 * The constructor.
	 */
	public ResourceChangeView() {
		super();
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(rootObject);

		initializeActions();

		IActionBars bars = getViewSite().getActionBars();
		IMenuManager menuMgr = bars.getMenuManager();
		menuMgr.addMenuListener(this::fillPullDownBar);
		fillPullDownBar(menuMgr);

		// register for all types of events and then filter out the one that we
		// want based on the action settings.
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_BUILD);
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content
	 * (like Task List, for example).
	 */

	protected void fillPullDownBar(IMenuManager manager) {
		manager.removeAll();

		for (Action action : eventActions)
			manager.add(action);
		manager.add(new Separator("phantoms")); //$NON-NLS-1$
		manager.add(PHANTOMS);
	}

	private void initializeActions() {
		// create the actions for the drop-down menu
		POST_BUILD = new FilterAction("POST_BUILD", IResourceChangeEvent.POST_BUILD); //$NON-NLS-1$
		POST_CHANGE = new FilterAction("POST_CHANGE", IResourceChangeEvent.POST_CHANGE); //$NON-NLS-1$
		// default event type
		POST_CHANGE.setChecked(true);
		PRE_BUILD = new FilterAction("PRE_BUILD", IResourceChangeEvent.PRE_BUILD); //$NON-NLS-1$
		eventActions.add(POST_BUILD);
		eventActions.add(POST_CHANGE);
		eventActions.add(PRE_BUILD);
		PHANTOMS = new FilterPhantoms();
	}

	/**
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() != typeFilter)
			return;
		setRoot(event);
	}

	private void setRoot(IResourceChangeEvent event) {
		if (viewer == null)
			return;
		Display display = viewer.getControl().getDisplay();
		if (display == null)
			return;
		rootObject = event;
		display.asyncExec(() -> {
			viewer.getControl().setRedraw(false);
			viewer.setInput(ResourceChangeView.this.rootObject);
			viewer.expandAll();
			viewer.getControl().setRedraw(true);
		});
	}
}