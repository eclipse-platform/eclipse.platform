/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.INavigatable;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Abstract superclass for tree viewer advisors
 */
public abstract class AbstractTreeViewerAdvisor extends StructuredViewerAdvisor implements IAdaptable {

	private ICompareNavigator nav;
	private INavigatable navigatable;

	/**
	 * Interface used to implement navigation for tree viewers. This interface is used by
	 * {@link TreeViewerAdvisor#navigate(TreeViewer, boolean, boolean, boolean) to open}
	 * selections and navigate.
	 */
	public interface ITreeViewerAccessor {
		public void createChildren(TreeItem item);
		public void openSelection();
	}

	private class TreeCompareNavigator extends CompareNavigator {

		@Override
		protected INavigatable[] getNavigatables() {
			INavigatable navigatable = getNavigatable();
			return new INavigatable[] { navigatable };
		}

		@Override
		public boolean selectChange(boolean next) {
			if (getSubNavigator() != null) {
				if (getSubNavigator().hasChange(next)) {
					getSubNavigator().selectChange(next);
					return false;
				}
			}
			boolean noNextChange = super.selectChange(next);
			if (!noNextChange) {
				// Check to see if the selected element can be opened.
				// If it can't, try the next one
				Object selectedObject = AbstractTreeViewerAdvisor.this.getFirstElement(getViewer().getStructuredSelection());
				if (!hasCompareInput(selectedObject)) {
					return selectChange(next);
				}
			}
			return noNextChange;
		}

		private boolean hasCompareInput(Object selectedObject) {
			SyncInfo syncInfo = getSyncInfo(selectedObject);
			if(syncInfo != null) {
				return syncInfo.getLocal().getType() == IResource.FILE;
			}
			ISynchronizeParticipant p = getConfiguration().getParticipant();
			if (p instanceof ModelSynchronizeParticipant) {
				ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) p;
				return msp.hasCompareInputFor(selectedObject);
			}
			return true;
		}

		private SyncInfo getSyncInfo(Object obj) {
			if (obj instanceof SyncInfoModelElement) {
				return ((SyncInfoModelElement) obj).getSyncInfo();
			} else {
				return null;
			}
		}

		@Override
		public boolean hasChange(boolean next) {
			if (getSubNavigator() != null) {
				if (getSubNavigator().hasChange(next)) {
					return true;
				}
			}
			return super.hasChange(next);
		}

		private CompareNavigator getSubNavigator() {
			IWorkbenchSite ws = AbstractTreeViewerAdvisor.this.getConfiguration().getSite().getWorkbenchSite();
			if (ws instanceof IWorkbenchPartSite) {
				Object selectedObject = AbstractTreeViewerAdvisor.this.getFirstElement(getViewer().getStructuredSelection());
				IEditorPart editor = OpenInCompareAction.findOpenCompareEditor((IWorkbenchPartSite)ws, selectedObject, getConfiguration().getParticipant());
				if(editor != null) {
					// if an existing editor is open on the current selection, use it
					CompareEditorInput input = (CompareEditorInput)editor.getEditorInput();
					ICompareNavigator navigator = input.getNavigator();
					if (navigator instanceof TreeCompareNavigator) {
						// The input knows to use the global navigator.
						// Assume it set the input navigator property
						navigator = (ICompareNavigator)AbstractTreeViewerAdvisor.this.getConfiguration().getProperty(SynchronizePageConfiguration.P_INPUT_NAVIGATOR);
					}
					if (navigator instanceof CompareNavigator) {
						return (CompareNavigator) navigator;

					}
				}
			}
			return null;
		}

	}

	private static boolean hasNextPrev(TreeViewer viewer, TreeItem item, boolean next) {
		if (item == null || !(viewer instanceof ITreeViewerAccessor))
			return false;
		TreeItem children[] = null;
		if (next) {
			if (viewer.isExpandable(item.getData()))
				return true;
			while(item != null) {
				TreeItem parent = item.getParentItem();
				if (parent != null)
					children = parent.getItems();
				else
					children = item.getParent().getItems();
				if (children != null && children.length > 0) {
					if (children[children.length - 1] != item) {
						// The item is not the last so there must be a next
						return true;
					} else {
						// Set the parent as the item and go up one more level
						item = parent;
					}
				}
			}
		} else {
			while(item != null) {
				TreeItem parent = item.getParentItem();
				if (parent != null)
					children = parent.getItems();
				else
					children = item.getParent().getItems();
				if (children != null && children.length > 0) {
					if (children[0] != item) {
						// The item is not the first so there must be a previous
						return true;
					} else {
						// Set the parent as the item and go up one more level
						item = parent;
					}
				}
			}
		}
		return false;
	}

	private static TreeItem findNextPrev(TreeViewer viewer, TreeItem item, boolean next) {
		if (item == null || !(viewer instanceof ITreeViewerAccessor))
			return null;
		TreeItem children[] = null;
		ITreeViewerAccessor treeAccessor = (ITreeViewerAccessor) viewer;
		if (!next) {
			TreeItem parent = item.getParentItem();
			if (parent != null)
				children = parent.getItems();
			else
				children = item.getParent().getItems();
			if (children != null && children.length > 0) {
				// goto previous child
				int index = 0;
				for (; index < children.length; index++)
					if (children[index] == item)
						break;
				if (index > 0) {
					item = children[index - 1];
					while (true) {
						treeAccessor.createChildren(item);
						int n = item.getItemCount();
						if (n <= 0)
							break;
						item.setExpanded(true);
						item = item.getItems()[n - 1];
					}
					// previous
					return item;
				}
			}
			// go up
			return parent;
		} else {
			item.setExpanded(true);
			treeAccessor.createChildren(item);
			if (item.getItemCount() > 0) {
				// has children: go down
				children = item.getItems();
				return children[0];
			}
			while (item != null) {
				children = null;
				TreeItem parent = item.getParentItem();
				if (parent != null)
					children = parent.getItems();
				else
					children = item.getParent().getItems();
				if (children != null && children.length > 0) {
					// goto next child
					int index = 0;
					for (; index < children.length; index++)
						if (children[index] == item)
							break;
					if (index < children.length - 1) {
						// next
						return children[index + 1];
					}
				}
				// go up
				item = parent;
			}
		}
		return item;
	}

	private static void setSelection(TreeViewer viewer, TreeItem ti, boolean fireOpen, boolean expandOnly) {
		if (ti != null) {
			Object data= ti.getData();
			if (data != null) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
				ISelection selection = new StructuredSelection(data);
				if (expandOnly) {
					viewer.expandToLevel(data, 0);
				} else {
					viewer.setSelection(selection, true);
					ISelection currentSelection = viewer.getSelection();
					if (fireOpen && currentSelection != null && selection.equals(currentSelection)) {
						if (viewer instanceof ITreeViewerAccessor) {
							((ITreeViewerAccessor) viewer).openSelection();
						}
					}
				}
			}
		}
	}

	/**
	 * Selects the next (or previous) node of the current selection.
	 * If there is no current selection the first (last) node in the tree is selected.
	 * Wraps around at end or beginning.
	 * Clients may not override.
	 * @param viewer
	 *
	 * @param next if <code>true</code> the next node is selected, otherwise the previous node
	 * @param fireOpen
	 * @param expandOnly
	 * @return <code>true</code> if at end (or beginning)
	 */
	public static boolean navigate(TreeViewer viewer, boolean next, boolean fireOpen, boolean expandOnly) {
		Tree tree = viewer.getTree();
		if (tree == null)
			return false;
		TreeItem item = getNextItem(viewer, next);
		if (item != null)
			setSelection(viewer, item, fireOpen, expandOnly);
		return item == null;
	}

	private static TreeItem getNextItem(TreeViewer viewer, boolean next) {
		TreeItem item = getCurrentItem(viewer);
		if (item != null) {
			while (true) {
				item = findNextPrev(viewer, item, next);
				if (item == null)
					break;
				if (item.getItemCount() <= 0)
					break;
			}
		}
		return item;
	}

	private static TreeItem getCurrentItem(TreeViewer viewer) {
		Tree tree = viewer.getTree();
		if (tree == null)
			return null;
		TreeItem item = null;
		TreeItem children[] = tree.getSelection();
		if (children != null && children.length > 0)
			item = children[0];
		if (item == null) {
			children = tree.getItems();
			if (children != null && children.length > 0) {
				item = children[0];
			}
		}
		return item;
	}

	private static boolean hasChange(TreeViewer viewer, boolean next) {
		TreeItem item = getCurrentItem(viewer);
		if (item != null) {
			return hasNextPrev(viewer, item, next);
		}
		return false;
	}

	public AbstractTreeViewerAdvisor(ISynchronizePageConfiguration configuration) {
		super(configuration);
		ICompareNavigator nav = (ICompareNavigator)configuration.getProperty(SynchronizePageConfiguration.P_NAVIGATOR);
		if (nav == null) {
			configuration.setProperty(SynchronizePageConfiguration.P_NAVIGATOR, getAdapter(ICompareNavigator.class));
		}
		configuration.addActionContribution(new NavigationActionGroup());
	}

	/**
	 * Allow navigation in tree viewers.
	 *
	 * @param next if <code>true</code> then navigate forwards, otherwise navigate
	 * backwards.
	 * @return <code>true</code> if the end is reached, and <code>false</code> otherwise.
	 */
	public boolean navigate(boolean next) {
		return navigate((TreeViewer)getViewer(), next, false, false);
	}

	protected boolean hasChange(boolean next) {
		return hasChange((TreeViewer)getViewer(), next);
	}

	/*
	 * Allow adding an advisor to the PartNavigator and support coordinated
	 * navigation between several objects.
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if(adapter == ICompareNavigator.class) {
			if(nav == null) {
				nav = new TreeCompareNavigator();
			}
			return (T) nav;
		}
		if(adapter == INavigatable.class) {
			return (T) getNavigatable();
		}
		return null;
	}

	private synchronized INavigatable getNavigatable() {
		if(navigatable == null) {
			navigatable = new INavigatable() {
				@Override
				public boolean selectChange(int flag) {
					if (flag == INavigatable.FIRST_CHANGE) {
						getViewer().setSelection(StructuredSelection.EMPTY);
						flag = INavigatable.NEXT_CHANGE;
					} else if (flag == INavigatable.LAST_CHANGE) {
						getViewer().setSelection(StructuredSelection.EMPTY);
						flag = INavigatable.PREVIOUS_CHANGE;
					}
					return navigate((TreeViewer)getViewer(), flag == INavigatable.NEXT_CHANGE, true, false);
				}

				@Override
				public boolean openSelectedChange() {
					Viewer v = getViewer();
					if (v instanceof ITreeViewerAccessor && !v.getControl().isDisposed()) {
						ITreeViewerAccessor tva = (ITreeViewerAccessor) v;
						tva.openSelection();
						return true;
					}
					return false;
				}
				@Override
				public boolean hasChange(int changeFlag) {
					return AbstractTreeViewerAdvisor.this.hasChange(changeFlag == INavigatable.NEXT_CHANGE);
				}
				@Override
				public Object getInput() {
					return getViewer().getInput();
				}

			};
		}
		return navigatable;
	}

	/**
	 * Handles a double-click event from the viewer. Expands or collapses a folder when double-clicked.
	 *
	 * @param viewer the viewer
	 * @param event the double-click event
	 */
	@Override
	protected boolean handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		if (super.handleDoubleClick(viewer, event)) return true;
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		Object element = getFirstElementOrPath(selection);
		AbstractTreeViewer treeViewer = (AbstractTreeViewer) getViewer();
		if(element != null) {
			if (treeViewer.getExpandedState(element)) {
				treeViewer.collapseToLevel(element, AbstractTreeViewer.ALL_LEVELS);
			} else {
				expandToNextDiff(element);
			}
		}
		return true;
	}

	private Object getFirstElementOrPath(IStructuredSelection selection) {
		if (selection instanceof TreeSelection) {
			TreeSelection ts = (TreeSelection) selection;
			TreePath[] paths = ts.getPaths();
			if (paths.length > 0)
				return paths[0];
		}
		Object element = selection.getFirstElement();
		return element;
	}

	private Object getFirstElement(IStructuredSelection selection) {
		Object element = getFirstElementOrPath(selection);
		if (element instanceof TreePath) {
			TreePath path = (TreePath) element;
			element = path.getLastSegment();
		}
		return element;
	}

	protected void expandToNextDiff(Object elementOrPath) {
		AbstractTreeViewerAdvisor.navigate((TreeViewer)getViewer(), true /* next */, false /* no-open */, true /* only-expand */);
	}
}
