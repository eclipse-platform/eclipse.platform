/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.debug.examples.internal.memory.launchconfig;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.debug.ui.memory.MemoryRenderingElement;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * Model presentation for sample debug adapter
 */

public class SampleModelPresentation implements IDebugModelPresentation, IColorProvider {

	private static SampleModelPresentation fPresentation;
	private static Color blue;

	public static SampleModelPresentation getSampleModelPresentation() {
		if (fPresentation == null) {
			fPresentation = new SampleModelPresentation();

			UIJob job = new UIJob("get colors") { //$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					Display display = PlatformUI.getWorkbench().getDisplay();
					blue = display.getSystemColor(SWT.COLOR_BLUE);
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}

		return fPresentation;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public void computeDetail(IValue value, IValueDetailListener listener) {

	}

	@Override
	public void dispose() {

	}

	@Override
	public Color getBackground(Object element) {
		return null;
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		return null;
	}

	@Override
	public IEditorInput getEditorInput(Object element) {

		return null;
	}

	@Override
	public Color getForeground(Object element) {

		if (element instanceof MemoryRenderingElement) {
			MemoryRenderingElement elm = (MemoryRenderingElement) element;
			MemoryByte[] bytes = elm.getBytes();
			if (!bytes[0].isWritable()) {
				return blue;
			}
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		return null;
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public void setAttribute(String attribute, Object value) {
	}
}
