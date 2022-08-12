/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class PatchWorkbenchAdapter implements IWorkbenchAdapter {

	@Override
	public Object[] getChildren(Object o) {
		if (o instanceof DiffNode) {
			return ((DiffNode) o).getChildren();
		}
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		if (object instanceof DiffNode) {
			Image image = ((DiffNode) object).getImage();
			return ImageDescriptor.createFromImage(image);
		}
		return null;
	}

	@Override
	public String getLabel(Object o) {
		if (o instanceof DiffNode) {
			return ((DiffNode) o).getName();
		}
		return null;
	}

	@Override
	public Object getParent(Object o) {
		if (o instanceof DiffNode) {
			return ((DiffNode) o).getParent();
		}
		return null;
	}
}
