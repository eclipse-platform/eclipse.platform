/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     John-Mason P. Shackelford - bug 34548
 *     Sudhindra Kulkarni (ETAS GmbH) - Issue 527
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import java.util.List;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.ILocationProvider;

/**
 * This class provides the Run/Debug As -&gt; Ant Build launch shortcut.
 * 
 */
public class AntLaunchShortcut extends AntLaunchShortcutCommon implements ILaunchShortcut2 {

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object object = structuredSelection.getFirstElement();
			if (object instanceof IAdaptable) {
				if (object instanceof AntElementNode) {
					// return an empty list so that the shortcut is delegated to and we can prompt
					// the user for which config to run and specify the correct target
					return new ILaunchConfiguration[0];
				}
				IResource resource = ((IAdaptable) object).getAdapter(IResource.class);
				if (resource != null) {
					if (!(AntUtil.isKnownAntFile(resource))) {
						if (!AntUtil.isKnownBuildfileName(resource.getName())) {
							if (resource.getType() == IResource.FILE) {
								resource = resource.getParent();
							}
							resource = findBuildFile((IContainer) resource);
						}
					}
					if (resource != null) {
						IPath location = ((IFile) resource).getLocation();
						if (location != null) {
							List<ILaunchConfiguration> list = collectConfigurations(location);
							return list.toArray(new ILaunchConfiguration[list.size()]);
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editor) {
		IEditorInput input = editor.getEditorInput();
		IFile file = input.getAdapter(IFile.class);
		IPath filepath = null;
		if (file != null) {
			filepath = file.getLocation();
		}
		if (filepath == null) {
			ILocationProvider locationProvider = input.getAdapter(ILocationProvider.class);
			if (locationProvider != null) {
				filepath = locationProvider.getPath(input);
			}
		}

		if (filepath != null && AntUtil.isKnownAntFileName(filepath.toString())) {
			List<ILaunchConfiguration> list = collectConfigurations(filepath);
			return list.toArray(new ILaunchConfiguration[list.size()]);
		}
		return null;
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object object = structuredSelection.getFirstElement();
			if (object instanceof IAdaptable) {
				IResource resource = ((IAdaptable) object).getAdapter(IResource.class);
				if (resource != null) {
					if (!(AntUtil.isKnownAntFile(resource))) {
						if (AntUtil.isKnownBuildfileName(resource.getName())) {
							return resource;
						}
						if (resource.getType() == IResource.FILE) {
							resource = resource.getParent();
						}
						resource = findBuildFile((IContainer) resource);
					}
					return resource;
				}
			}
		}
		return null;
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editor) {
		IEditorInput input = editor.getEditorInput();
		return input.getAdapter(IFile.class);
	}
}
