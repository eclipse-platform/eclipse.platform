/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 *     Patrick Chuong (Texas Instruments) - Checkbox support for Flexible Hierachy view (Bug 286310)
 *******************************************************************************/
package org.eclipse.debug.examples.ui.midi.adapters;

import org.eclipse.debug.examples.ui.pda.views.CheckboxView;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Column presentation factory for a sequencer.
 *
 * @since 1.0
 */
public class SequencerColumnFactory implements IColumnPresentationFactory {

	@Override
	public IColumnPresentation createColumnPresentation(IPresentationContext context, Object element) {
		if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(context.getId()) || CheckboxView.ID.equals(context.getId())) {
			return new SequencerColumnPresentation();
		}
		return null;
	}

	@Override
	public String getColumnPresentationId(IPresentationContext context, Object element) {
		if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(context.getId()) || CheckboxView.ID.equals(context.getId())) {
			return SequencerColumnPresentation.ID;
		}
		return null;
	}

}
