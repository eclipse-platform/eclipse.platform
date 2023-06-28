/*****************************************************************
 * Copyright (c) 2009, 2011 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 238956)
 *****************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.CompositeDebugImageDescriptor;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.model.IWorkbenchAdapter2;

/**
 * Breakpoint container label provider.
 *
 * @since 3.6
 */
public class BreakpointContainerLabelProvider extends DebugElementLabelProvider {
	@Override
	protected ImageDescriptor getImageDescriptor(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		ImageDescriptor desc = super.getImageDescriptor(elementPath, presentationContext, columnId);
		int flags = computeAdornmentFlags();

		if (flags > 0) {
			Image image = DebugUIPlugin.getImageDescriptorRegistry().get(desc);
			CompositeDebugImageDescriptor compDesc = new CompositeDebugImageDescriptor(image, flags);
			return compDesc;
		}
		return desc;
	}

	@Override
	public boolean getChecked(TreePath path, IPresentationContext presentationContext) throws CoreException {
		Object lastSegment = path.getLastSegment();
		if (lastSegment instanceof IBreakpointContainer) {
			IBreakpointContainer container = (IBreakpointContainer) lastSegment;
			for (IBreakpoint breakpoint : container.getBreakpoints()) {
				if (breakpoint.isEnabled()) {
					return true;
				}
			}

			return false;
		}

		return super.getChecked(path, presentationContext);
	}

	@Override
	public boolean getGrayed(TreePath path, IPresentationContext presentationContext) throws CoreException {
		Object lastSegment = path.getLastSegment();
		if (lastSegment instanceof IBreakpointContainer) {
			IBreakpointContainer container = (IBreakpointContainer) lastSegment;

			// Return true, gray if some breakpoints are enabled and some are disabled.
			// return false if all breakpoints are either disabled or all are enabled.
			boolean hasEnabled = false;
			boolean hasDisabled = false;

			for (IBreakpoint breakpoint : container.getBreakpoints()) {
				if (breakpoint.isEnabled()) {
					hasEnabled = true;
				} else {
					hasDisabled = true;
				}

				if (hasEnabled && hasDisabled) {
					return true;
				}
			}

			return false;
		}

		return super.getGrayed(path, presentationContext);
	}

	// Forward font data requests to the workbench adapter
	@Override
	protected FontData getFontData(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		Object element = elementPath.getLastSegment();
		FontData fontData = super.getFontData(elementPath, presentationContext, columnId);
		if (fontData == null && element instanceof IAdaptable) {

			IWorkbenchAdapter2 adapter = ((IAdaptable)element).getAdapter(IWorkbenchAdapter2.class);
			if (adapter != null) {
				fontData = adapter.getFont(element);
			}
		}
		return fontData;
	}

	// Forward foreground color requests to the workbench adapter
	@Override
	protected RGB getForeground(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		Object element = elementPath.getLastSegment();
		RGB rgb = super.getForeground(elementPath, presentationContext, columnId);
		if (rgb == null && element instanceof IAdaptable) {

			IWorkbenchAdapter2 adapter = ((IAdaptable)element).getAdapter(IWorkbenchAdapter2.class);
			if (adapter != null) {
				rgb = adapter.getForeground(element);
			}
		}
		return rgb;
	}

	// Forward background color requests to the workbench adapter
	@Override
	protected RGB getBackground(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		Object element = elementPath.getLastSegment();
		RGB rgb = super.getBackground(elementPath, presentationContext, columnId);
		if (rgb == null && element instanceof IAdaptable) {

			IWorkbenchAdapter2 adapter = ((IAdaptable)element).getAdapter(IWorkbenchAdapter2.class);
			if (adapter != null) {
				rgb = adapter.getBackground(element);
			}
		}
		return rgb;
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
}
