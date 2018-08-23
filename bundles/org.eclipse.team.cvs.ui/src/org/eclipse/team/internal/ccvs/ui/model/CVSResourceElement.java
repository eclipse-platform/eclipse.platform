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
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.ui.model.IWorkbenchAdapter;

public abstract class CVSResourceElement extends CVSModelElement implements IAdaptable {
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
		return null;
	}
	/**
	 * Initial implementation: return the resource's name
	 */
	public String getLabel(Object o) {
		if (!(o instanceof ICVSResource)) return null;
		return ((ICVSResource)o).getName();
	}
	/**
	 * Return null.
	 */
	public Object getParent(Object o) {
		if (!(o instanceof ICVSResource)) return null;
		return null;
	}
	
	abstract public ICVSResource getCVSResource();
}
