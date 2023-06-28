/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.elements.adapters;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.LazyModelPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.AsynchronousLabelAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.ILabelRequestMonitor;
import org.eclipse.debug.internal.ui.views.launch.DebugElementHelper;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

/**
 * Asynchronous label adapter for debug elements.
 *
 * @since 3.2
 */
public class AsynchronousDebugLabelAdapter extends AsynchronousLabelAdapter {

	@Override
	protected void computeLabels(Object element, IPresentationContext context, ILabelRequestMonitor monitor) {
		DelegatingModelPresentation presentation = DebugElementHelper.getPresentation();
		// Honor view specific settings in a debug view by copying model presentation settings
		// into the debug element helper's presentation before we get the label. This allows
		// for qualified name and type name settings to remain in tact.
		if (element instanceof IDebugElement && context.getPart() instanceof IDebugView) {
			IDebugView debugView = (IDebugView)context.getPart();
			IDebugModelPresentation pres = debugView.getPresentation(((IDebugElement)element).getModelIdentifier());
			Map<String, Object> settings = null;
			synchronized (presentation) {
				if (pres instanceof DelegatingModelPresentation) {
					settings = ((DelegatingModelPresentation)pres).getAttributes();
				} else if (pres instanceof LazyModelPresentation) {
					settings = ((LazyModelPresentation)pres).getAttributes();
				}
				if (settings != null) {
					for (Entry<String, Object> entry : settings.entrySet()) {
						presentation.setAttribute(entry.getKey(), entry.getValue());
					}
					super.computeLabels(element, context, monitor);
					return;
				}
			}
		}
		super.computeLabels(element, context, monitor);
	}

	@Override
	protected String[] getLabels(Object element, IPresentationContext context) throws CoreException {
		return new String[] {DebugElementHelper.getLabel(element)};
	}

	@Override
	protected ImageDescriptor[] getImageDescriptors(Object element, IPresentationContext context) throws CoreException {
		return new ImageDescriptor[] {DebugElementHelper.getImageDescriptor(element)};
	}

	@Override
	protected FontData[] getFontDatas(Object element, IPresentationContext context) throws CoreException {
		FontData[] datas = new FontData[getNumElements(context)];
		Arrays.fill(datas, DebugElementHelper.getFont(element));
		return datas;
	}

	@Override
	protected RGB[] getForegrounds(Object element, IPresentationContext context) throws CoreException {
		RGB[] rgbs = new RGB[getNumElements(context)];
		Arrays.fill(rgbs, DebugElementHelper.getForeground(element));
		return rgbs;
	}

	@Override
	protected RGB[] getBackgrounds(Object element, IPresentationContext context) throws CoreException {
		RGB[] rgbs = new RGB[getNumElements(context)];
		Arrays.fill(rgbs, DebugElementHelper.getBackground(element));
		return rgbs;
	}

	/**
	 * Returns the number of columns in the given presentation context, or 1
	 * if there are no columns.
	 *
	 * @param context presentation context
	 * @return number of columns or 1 if none
	 */
	protected int getNumElements(IPresentationContext context) {
		String[] columns = context.getColumns();
		if (columns == null) {
			return 1;
		}
		return columns.length;
	}

}
