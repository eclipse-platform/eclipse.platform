/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
package org.eclipse.core.tools.runtime;

import java.util.ArrayList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.tools.CollapseAllAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.osgi.service.prefs.BackingStoreException;

public class EclipsePreferencesView extends ViewPart {

	private TreeViewer viewer;
	private IAction collapseAllAction;

	class ViewContentProvider implements ITreeContentProvider {

		private IEclipsePreferences invisibleRoot;

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			// do nothing
		}

		@Override
		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				if (invisibleRoot == null)
					invisibleRoot = Platform.getPreferencesService().getRootNode();
				return new Object[] {invisibleRoot};
			}
			return getChildren(parent);
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public Object getParent(Object child) {
			if (child instanceof IEclipsePreferences)
				return ((IEclipsePreferences) child).parent();
			return null;
		}

		@Override
		public Object[] getChildren(Object parent) {
			ArrayList<Object> result = new ArrayList<>();
			if (parent instanceof IEclipsePreferences) {
				IEclipsePreferences node = (IEclipsePreferences) parent;
				try {
					String[] childrenNames = node.childrenNames();
					for (int i = 0; childrenNames != null && i < childrenNames.length; i++)
						result.add(node.node(childrenNames[i]));
					String[] keys = node.keys();
					for (String key : keys)
						result.add(key + '=' + node.get(key, "")); //$NON-NLS-1$
				} catch (BackingStoreException e) {
					e.printStackTrace();
				}
			}
			return result.toArray(new Object[result.size()]);
		}

		@Override
		public boolean hasChildren(Object parent) {
			if (parent instanceof IEclipsePreferences)
				try {
					IEclipsePreferences node = (IEclipsePreferences) parent;
					return node.childrenNames().length != 0 || node.keys().length != 0;
				} catch (BackingStoreException e) {
					e.printStackTrace();
				}
			return false;
		}
	}

	static class ViewLabelProvider extends LabelProvider {

		@Override
		public String getText(Object obj) {
			String result = obj.toString();
			if (obj instanceof IEclipsePreferences) {
				IEclipsePreferences node = (IEclipsePreferences) obj;
				result = node.name();
				if (result.isEmpty())
					result = "<root>"; //$NON-NLS-1$
			}
			return result;
		}

		@Override
		public Image getImage(Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			if (obj instanceof IEclipsePreferences)
				imageKey = ISharedImages.IMG_OBJ_FOLDER;
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
	}

	public EclipsePreferencesView() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setComparator(new ViewerComparator());
		viewer.setInput(getViewSite());
		getViewSite().setSelectionProvider(viewer);
		IActionBars bars = getViewSite().getActionBars();
		collapseAllAction = new CollapseAllAction(viewer);
		bars.getToolBarManager().add(collapseAllAction);
		bars.updateActionBars();
	}

	@Override
	public void dispose() {
		super.dispose();
		collapseAllAction = null;
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
