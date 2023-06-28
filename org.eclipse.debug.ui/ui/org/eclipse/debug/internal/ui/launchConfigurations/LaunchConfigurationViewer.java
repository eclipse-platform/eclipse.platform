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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * This class allow the notion of the viewer to be abstracted from the launch configuration view, as well as allowing the over-riding of
 * selection preservation when filtering/deletion occurs
 * @since 3.3
 */
public class LaunchConfigurationViewer extends TreeViewer {

	private int fTotalCount = 0;
	private LaunchConfigurationView fView = null;

	/**
	 * Constructor
	 * @param tree the tree to create the viewer on
	 */
	public LaunchConfigurationViewer(Tree tree) {
		super(tree);
	}

	/**
	 * Constructor
	 * @param parent
	 * @param style
	 */
	public LaunchConfigurationViewer(Composite parent, int style) {
		this(new Tree(parent, style));
	}

	@Override
	protected void preservingSelection(Runnable updateCode) {
		IStructuredSelection selection = getStructuredSelection();
		if(!selection.isEmpty()) {
			int[] indices = collectIndices(selection.getFirstElement());
			updateCode.run();
			ArrayList<Object> set = new ArrayList<>();
			Object o = null;
			for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
				o = iter.next();
				if(o instanceof ILaunchConfiguration) {
					if(!((ILaunchConfiguration)o).exists()) {
						continue;
					}
				}
				if(internalGetWidgetToSelect(o) != null) {
					if(!set.contains(o)) {
						set.add(o);
					}
				}
			}
			if(set.isEmpty()) {
				//make a new selection based on the first item in the structured selection
				Tree tree = getTree();
				if(tree.getItemCount() > 0) {
					int index = selectIndex(tree.getItemCount(), indices[0]);
					if(index > -1) {
						TreeItem pitem = null;
						if(indices[0] > tree.getItemCount()-1) {
							pitem = tree.getItem(tree.getItemCount()-1);
						}
						else {
							pitem = tree.getItem(indices[0]);
						}
						if(pitem != null) {
							o = pitem.getData();
							if(indices[1] > -1) {
								index = selectIndex(pitem.getItemCount(), indices[1]);
								if(index > -1) {
									ILaunchConfiguration config = null;
									for(int i = index; i > -1; i--) {
										config = (ILaunchConfiguration) pitem.getItem(i).getData();
										if(config != null && config.exists()) {
											o = config;
											break;
										}
									}
								}
								else {
									if(pitem.getItemCount() > 0) {
										o = pitem.getItem((indices[1]-1 > -1 ? indices[1]-1 : 0)).getData();
										if(o == null) {
											o = pitem.getData();
										}
									}
								}
							}
						}
					}
					if(!set.contains(o)) {
						set.add(o);
					}
				}
			}
			setSelection(new StructuredSelection(set), true);
		}
		else {
			super.preservingSelection(updateCode);
		}
		getTree().getHorizontalBar().setSelection(0);
	}

	/**
	 * Covers the case of an outlier indice
	 * @param count the count to compare the index to
	 * @param index the index to compare against the count
	 * @return the adjusted index in the event index is an outlier, or -1 if it falls within the 'count' range
	 */
	private int selectIndex(int count, int index) {
		if(index < count) {
			return index;
		}
		if(index > count-1) {
			return count-1;
		}
		if(index < 0) {
			return 0;
		}
		return -1;
	}

	/**
	 * Returns the total count of all of the children that <i>could</i> be visible at
	 * the time the input was set to the viewer
	 * @return the total number of elements
	 */
	protected int getTotalChildCount() {
		return fTotalCount;
	}

	@Override
	public void remove(Object elementsOrTreePaths) {
		super.remove(elementsOrTreePaths);
		fTotalCount--;
	}

	@Override
	protected void internalAdd(Widget widget, Object parentElement, Object[] childElements) {
		super.internalAdd(widget, parentElement, childElements);
		fTotalCount++;
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		super.inputChanged(input, oldInput);
		//calc the total number of items that could be visible in the view
		LaunchConfigurationTreeContentProvider cp = (LaunchConfigurationTreeContentProvider) getContentProvider();
		Object[] types = cp.getElements(null);
		LaunchGroupFilter filter = new LaunchGroupFilter(((LaunchConfigurationsDialog)LaunchConfigurationsDialog.getCurrentlyVisibleLaunchConfigurationDialog()).getLaunchGroup());
		ILaunchConfiguration[] configs = null;
		for (Object type : types) {
			if (filter.select(this, type, null)) {
				fTotalCount++;
				configs = (ILaunchConfiguration[]) cp.getChildren(type);
				for (ILaunchConfiguration config : configs) {
					if (LaunchConfigurationManager.isVisible(config)) {
						fTotalCount++;
					}
				}
			}
		}
	}

	/**
	 * returns the number of children that are remaining in the view.
	 * Note that this method will force the loading of all children
	 * @return the count of all children in the viewer
	 *
	 * @since 3.3
	 */
	protected int getNonFilteredChildCount() {
		int count = 0;
		getTree().setRedraw(false);
		TreeItem[] items = getTree().getItems();
		count += items.length;
		boolean expanded = false;
		for (TreeItem item : items) {
			expanded = item.getExpanded();
			setExpandedState(item.getData(), true);
			count += item.getItems().length;
			item.setExpanded(expanded);
		}
		getTree().setRedraw(true);
		return count;
	}

	/**
	 * Collects the indices of the child and parent items for the specified element
	 * @param object the element to collect indices for
	 * @return an array of indices for the specified element
	 */
	private int[] collectIndices(Object object) {
		int[] indices = {-1, -1};
		if(object != null) {
			TreeItem item = (TreeItem) findItem(object);
			if(item != null) {
				TreePath path = getTreePathFromItem(item);
				item = (TreeItem) findItem(path.getFirstSegment());
				if(item != null) {
					indices[0] = getTree().indexOf(item);
					if(path.getSegmentCount() == 2) {
						indices[1] = indexOf(item.getItems(), path.getLastSegment());
					}
				}
			}
		}
		return indices;
	}

	/**
	 * Finds the index of the specified object in the given array of tree items
	 * @param items the items to search for the specified object
	 * @param object the object to find the index of
	 * @return the index of the specified object in the listing of tree items, or -1 if not found
	 */
	private int indexOf(TreeItem[] items, Object object) {
		if(object != null) {
			for(int i = 0; i < items.length; i++) {
				if(object.equals(items[i].getData())) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * The filter changed due to text typing - update the filter count
	 */
	protected void filterChanged() {
		if (fView != null) {
			fView.updateFilterLabel();
		}

	}

	/**
	 * @param launchConfigurationView
	 */
	protected void setLaunchConfigurationView(LaunchConfigurationView launchConfigurationView) {
		fView = launchConfigurationView;
	}
}
