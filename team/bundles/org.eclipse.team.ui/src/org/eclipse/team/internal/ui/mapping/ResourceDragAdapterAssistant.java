/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
 *     Anton Leherbauer (Wind River Systems) - http://bugs.eclipse.org/247294
 ******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.eclipse.ui.part.ResourceTransfer;


/**
 * Drag adapter assistant used for the Common Navigator based viewer for use by a
 * {@link ModelSynchronizePage}.
 *
 * @since 3.6
 */
public class ResourceDragAdapterAssistant extends CommonDragAdapterAssistant {

	private static final Transfer[] SUPPORTED_TRANSFERS = new Transfer[] {
			ResourceTransfer.getInstance(),
			FileTransfer.getInstance() };

	private static final Class<IResource> IRESOURCE_TYPE = IResource.class;

	@Override
	public Transfer[] getSupportedTransferTypes() {
		return SUPPORTED_TRANSFERS;
	}

	@Override
	public boolean setDragData(DragSourceEvent anEvent,
			IStructuredSelection aSelection) {

		IResource[] resources = getSelectedResources(aSelection);
		if (resources.length > 0) {
			if (ResourceTransfer.getInstance().isSupportedType(anEvent.dataType)) {
				anEvent.data = resources;
				if (Policy.DEBUG_DND) {
					System.out
							.println("ResourceDragAdapterAssistant.dragSetData set ResourceTransfer"); //$NON-NLS-1$
				}
				return true;
			}

			if (FileTransfer.getInstance().isSupportedType(anEvent.dataType)) {
				// Get the path of each file and set as the drag data
				final int length = resources.length;
				int actualLength = 0;
				String[] fileNames = new String[length];
				for (int i = 0; i < length; i++) {
					IPath location = resources[i].getLocation();
					// location may be null. See bug 29491.
					if (location != null) {
						fileNames[actualLength++] = location.toOSString();
					}
				}
				if (actualLength > 0) {
					// was one or more of the locations null?
					if (actualLength < length) {
						String[] tempFileNames = fileNames;
						fileNames = new String[actualLength];
						System.arraycopy(tempFileNames, 0, fileNames, 0, actualLength);
					}
					anEvent.data = fileNames;

					if (Policy.DEBUG_DND)
						System.out
								.println("ResourceDragAdapterAssistant.dragSetData set FileTransfer"); //$NON-NLS-1$
					return true;
				}
			}
		}
		return false;

	}

	private IResource[] getSelectedResources(IStructuredSelection aSelection) {
		Set<IResource> resources = new LinkedHashSet<>();
		IResource resource = null;
		for (Object selected : aSelection) {
			resource = adaptToResource(selected);
			if (resource != null) {
				resources.add(resource);
		}
		}
		return resources.toArray(new IResource[resources.size()]);
	}

	private IResource adaptToResource(Object selected) {
		IResource resource;
		if (selected instanceof IResource) {
			resource = (IResource) selected;
		} else if (selected instanceof IAdaptable) {
			resource = ((IAdaptable) selected).getAdapter(IRESOURCE_TYPE);
		} else {
			resource = Platform.getAdapterManager().getAdapter(selected, IRESOURCE_TYPE);
		}
		return resource;
	}

}
