/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * This class sorts <code>SyncInfoModelElement</code> instances.
 * It is not thread safe so it should not be reused between views.
 */
public class SynchronizeModelElementSorter extends ResourceSorter {

	public SynchronizeModelElementSorter() {
		super(ResourceSorter.NAME);
	}

	@Override
	public int compare(Viewer viewer, Object o1, Object o2) {
		IResource resource1 = getResource(o1);
		IResource resource2 = getResource(o2);
		Object objectToCompare1 = resource1==null ? o1 : resource1;
		Object objectToCompare2 = resource2==null ? o2 : resource2;

		return super.compare(viewer, objectToCompare1, objectToCompare2);
	}

	protected IResource getResource(Object obj) {
		IResource[] resources = Utils.getResources(new Object[] {obj});
		return resources.length == 1 ? resources[0] : null;
	}
}
