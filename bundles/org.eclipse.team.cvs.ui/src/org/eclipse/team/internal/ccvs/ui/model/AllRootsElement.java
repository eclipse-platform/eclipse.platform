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
package org.eclipse.team.internal.ccvs.ui.model;

 
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * AllRootsElement is the model element for the repositories view.
 * Its children are the array of all known repository roots.
 */
public class AllRootsElement extends CVSModelElement implements IAdaptable {
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}
	public Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		return CVSUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryRoots();
	}
	public String getLabel(Object o) {
		return null;
	}
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}
	public Object getParent(Object o) {
		return null;
	}
}

