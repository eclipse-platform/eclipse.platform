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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * This class acts as a resource list that can be used in table widgets.
 */
public class AdaptableResourceList implements IAdaptable, IWorkbenchAdapter {

	IResource[] resources;
	
	public AdaptableResourceList(IResource[] resources) {
		this.resources = resources;
	}
	
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}

	public Object[] getChildren(Object o) {
		return resources;
	}
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}
	
	public String getLabel(Object o) {
		return o == null ? "" : o.toString();//$NON-NLS-1$
	}
	
	public Object getParent(Object o) {
		return null;
	}
	
}
