/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
package org.eclipse.team.examples.model.ui;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.ModelFolder;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.team.examples.model.ModelObjectElementFile;
import org.eclipse.team.examples.model.ModelProject;
import org.eclipse.team.examples.model.ModelWorkspace;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * The workbench adapter for ModelObjects.
 */
public class ModelWorkbenchAdapter implements IWorkbenchAdapter {

	// image path
	private static final String ICON_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

	@Override
	public Object[] getChildren(Object o) {
		if (o instanceof ModelObject) {
			ModelObject mo = (ModelObject) o;
			try {
				return mo.getChildren();
			} catch (CoreException e) {
				FileSystemPlugin.log(e);
			}
		}
		return new Object[0];
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		if (object instanceof ModelProject) {
			return createImageDescriptor("obj/prj_obj.gif");
		}
		if (object instanceof ModelWorkspace) {
			return createImageDescriptor("obj/root_obj.gif");
		}
		if (object instanceof ModelFolder) {
			return createImageDescriptor("obj/fldr_obj.gif");
		}
		if (object instanceof ModelObjectDefinitionFile) {
			return createImageDescriptor("obj/mod_obj.gif");
		}
		if (object instanceof ModelObjectElementFile) {
			return createImageDescriptor("obj/moe_obj.gif");
		}
		return null;
	}

	@Override
	public String getLabel(Object o) {
		if (o instanceof ModelObject) {
			ModelObject mo = (ModelObject) o;
			return mo.getName();
		}
		return null;
	}

	@Override
	public Object getParent(Object o) {
		if (o instanceof ModelObject) {
			ModelObject mo = (ModelObject) o;
			return mo.getParent();
		}
		return null;
	}

	/**
	 * Creates an image descriptor.
	 */
	public static ImageDescriptor createImageDescriptor(String id) {
		URL url = FileLocator.find(FileSystemPlugin.getPlugin().getBundle(), new Path(ICON_PATH + id), null);
		return ImageDescriptor.createFromURL(url);
	}

}
