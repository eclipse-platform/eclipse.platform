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
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.model.CVSResourceElement;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A workbench adapter that can be used to view the resources that make up 
 * a tag source. It is used by the TagConfigurationDialog.
 */
public class TagSourceResourceAdapter implements IAdaptable, IWorkbenchAdapter {

	public static Object getViewerInput(TagSource tagSource) {
		return new TagSourceResourceAdapter(tagSource);
	}
	
	TagSource tagSource;

	private TagSourceResourceAdapter(TagSource tagSource) {
		this.tagSource = tagSource;
	}
	
	@Override
	public Object[] getChildren(Object o) {
		ICVSResource[] children = tagSource.getCVSResources();
		if (children.length == 0) return new Object[0];
		List<CVSResourceElement> result = new ArrayList<>();
		for (ICVSResource resource : children) {
			if (resource.isFolder()) {
				result.add(new CVSFolderElement((ICVSFolder)resource, false));
			} else {
				result.add(new CVSFileElement((ICVSFile)resource));
			}
		}
		return result.toArray(new Object[result.size()]);
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		// No image descriptor
		return null;
	}

	@Override
	public String getLabel(Object o) {
		return tagSource.getShortDescription();
	}

	@Override
	public Object getParent(Object o) {
		// No parent
		return null;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return adapter.cast(this);
		}
		return null;
	}

}
