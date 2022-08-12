/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.resources;

import java.util.*;
import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.resources.FileState;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

/**
 * Tree viewer class used to construct a view on the local history store. The
 * tree navigates over the actual contents of the history store rather than
 * going to the navigator and asking each file for its list of file states. This allows
 * the browser to see the deleted files for containers.
 */
public class LocalHistoryBrowserView extends ViewPart {
	TreeViewer viewer;
	DrillDownAdapter drillDownAdapter;
	Action refreshAction;
	Action doubleClickAction;

	static class NameSorter extends ViewerComparator {
		// empty impl
	}

	static class FileStateEditorInput implements IStorageEditorInput {
		private IFileState state;

		public FileStateEditorInput(IFileState state) {
			super();
			this.state = state;
		}

		@Override
		public IStorage getStorage() {
			return state;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public String getName() {
			if (state instanceof FileState)
				return ((FileState) state).getUUID() + " (" + state.getFullPath() + ')'; //$NON-NLS-1$
			return state.getFullPath().toString();
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return null;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}
	}

	static class Node {

		Node parent;
		String name;
		ArrayList<Object> children;

		public Node(Node parent, String name) {
			super();
			this.parent = parent;
			this.name = name;
			this.children = new ArrayList<>();
		}

		public void addChild(Object child) {
			children.add(child);
		}

		public String getName() {
			return name;
		}

		public Node getParent() {
			return parent;
		}

		public Object[] getChildren() {
			return children.toArray(new Object[0]);
		}

		public Object getChild(String childName) {
			for (Object child : children) {
				if (child instanceof Node) {
					if (((Node) child).getName().equals(childName))
						return child;
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
		private IHistoryStore store = ((Workspace) ResourcesPlugin.getWorkspace()).getFileSystemManager().getHistoryStore();

		private Node invisibleRoot;

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			// do nothing
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				if (invisibleRoot == null)
					initialize();
				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}

		@Override
		public Object getParent(Object child) {
			return child instanceof Node ? ((Node) child).getParent() : null;
		}

		@Override
		public Object[] getChildren(Object parent) {
			return parent instanceof Node ? ((Node) parent).getChildren() : new Object[0];
		}

		@Override
		public boolean hasChildren(Object parent) {
			return parent instanceof Node ? ((Node) parent).getChildren().length != 0 : false;
		}

		public void initialize() {
			invisibleRoot = new Node(null, "/"); //$NON-NLS-1$
			Set<IPath> allFiles = store.allFiles(Path.ROOT, IResource.DEPTH_INFINITE, null);
			for (IPath path : allFiles) {
				Node current = invisibleRoot;
				String[] segments = path.segments();
				for (int i = 0; i < segments.length; i++) {
					Object child = current.getChild(segments[i]);
					if (child == null) {
						child = new Node(current, segments[i]);
						current.addChild(child);
					}
					current = (Node) child;
				}
				IFileState[] states = store.getStates(path, null);
				for (int i = 0; i < states.length; i++)
					current.addChild(states[i]);
			}
		}
	}

	static class ViewLabelProvider extends LabelProvider {

		@Override
		public String getText(Object obj) {
			return obj.toString();
		}

		@Override
		public Image getImage(Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_FOLDER;
			if (obj instanceof IFileState)
				imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
	}

	public LocalHistoryBrowserView() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(getViewSite());
		viewer.setComparator(new NameSorter());
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(LocalHistoryBrowserView.this::fillContextMenu);
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(refreshAction);
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(refreshAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(refreshAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		refreshAction = new Action() {
			@Override
			public void run() {
				((ViewContentProvider) viewer.getContentProvider()).initialize();
				showMessage("View refreshed."); //$NON-NLS-1$
			}
		};
		refreshAction.setText("Refresh View"); //$NON-NLS-1$
		refreshAction.setToolTipText("Refresh View"); //$NON-NLS-1$
		refreshAction.setImageDescriptor(ImageDescriptor.createFromURLSupplier(true, () -> LocalHistoryBrowserView.class.getResource("/icons/refresh.gif"))); //$NON-NLS-1$

		doubleClickAction = new Action() {
			@Override
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				if (obj instanceof IFileState) {
					// Show the file contents
					IFileState state = (IFileState) obj;
					IWorkbench workbench = PlatformUI.getWorkbench();
					IEditorRegistry editorRegistry = workbench.getEditorRegistry();
					IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(state.getName());
					String editorID = descriptor == null ? EditorsUI.DEFAULT_TEXT_EDITOR_ID : descriptor.getId();
					IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
					try {
						page.openEditor(new FileStateEditorInput(state), editorID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				} else if (obj instanceof Node) {
					// TODO expand/collapse tree node
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(event -> doubleClickAction.run());
	}

	protected void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "Local History Browser", message); //$NON-NLS-1$
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
