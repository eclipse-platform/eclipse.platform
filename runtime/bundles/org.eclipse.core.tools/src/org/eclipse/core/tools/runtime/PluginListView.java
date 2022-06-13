/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
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
package org.eclipse.core.tools.runtime;

import java.util.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tools.Messages;
import org.eclipse.core.tools.SpyView;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.*;

public class PluginListView extends SpyView implements IStructuredContentProvider {

	// cache the plug-in list
	private Object[] bundles = null;

	/** The id by which this view is known in the plug-in registry */
	public static final String VIEW_ID = PluginListView.class.getName();

	/**
	 * Class which provides the text labels for the view.
	 */
	static class PluginListLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return element == null ? Messages.depend_badPluginId : ((BundleDescription) element).getSymbolicName();
		}
	}

	@Override
	public Object[] getElements(Object arg0) {
		if (bundles == null) {
			// before caching the array of descriptors, sort them.
			// we have to use a comparator here because plug-in
			// descriptors cannot be compared against each other
			// in a tree set.
			Comparator<BundleDescription> comparator = (obj1, obj2) -> {
				String id1 = obj1.getSymbolicName();
				String id2 = obj2.getSymbolicName();
				return id1.compareTo(id2);
			};
			Set<BundleDescription> set = new TreeSet<>(comparator);
			BundleContext context = FrameworkUtil.getBundle(PluginListView.class).getBundleContext();
			Bundle[] allBundles = context.getBundles();
			State state = Platform.getPlatformAdmin().getState(false);
			for (Bundle allBundle : allBundles)
				set.add(state.getBundle(allBundle.getBundleId()));
			bundles = set.toArray();
		}
		return bundles;
	}

	@Override
	public void dispose() {
		bundles = null;
	}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		// do nothing
	}

	@Override
	public void createPartControl(Composite parent) {
		// Create viewer.
		ListViewer viewer = new ListViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(this);
		viewer.setLabelProvider(new PluginListLabelProvider());
		viewer.setInput(""); //$NON-NLS-1$
		getSite().setSelectionProvider(viewer);
	}
}
