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
package org.eclipse.debug.internal.ui.views.breakpoints;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.CompositeDebugImageDescriptor;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label provider for the breakpoints view
 */
public class BreakpointsLabelProvider extends LabelProvider implements IFontProvider {

	private WorkbenchLabelProvider fWorkbenchLabelProvider;
	private IDebugModelPresentation fPresentation;

	/**
	 * Constructs a new label provide for the breakpoints view.
	 */
	public BreakpointsLabelProvider() {
		fWorkbenchLabelProvider = new WorkbenchLabelProvider();
		fPresentation = DebugUITools.newDebugModelPresentation();
	}

	@Override
	public void dispose() {
		fWorkbenchLabelProvider.dispose();
		fPresentation.dispose();
		super.dispose();
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof IBreakpoint) {
			return fPresentation.getImage(element);
		}
		Image image = fWorkbenchLabelProvider.getImage(element);
		if (image != null) {
			int flags= computeAdornmentFlags();
			if (flags > 0) {
				CompositeDebugImageDescriptor descriptor= new CompositeDebugImageDescriptor(image, flags);
				return DebugUIPlugin.getImageDescriptorRegistry().get(descriptor);
			}
		}
		return image;
	}

	/**
	 * Computes and return common adornment flags for the given category.
	 *
	 * @return adornment flags defined in CompositeDebugImageDescriptor
	 */
	private int computeAdornmentFlags() {
		if (!DebugPlugin.getDefault().getBreakpointManager().isEnabled()) {
			return CompositeDebugImageDescriptor.SKIP_BREAKPOINT;
		}
		return 0;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IBreakpoint) {
			return fPresentation.getText(element);
		}
		return fWorkbenchLabelProvider.getText(element);
	}

	@Override
	public Font getFont(Object element) {
		if (element instanceof IBreakpoint) {
			return null;
		}
		return fWorkbenchLabelProvider.getFont(element);
	}

	/**
	 * Returns the debug model presentation used by this label provider.
	 *
	 * @return the debug model presentation used by this label provider
	 */
	public IDebugModelPresentation getPresentation() {
		return fPresentation;
	}
}
