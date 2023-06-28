/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.debug.examples.ui.midi.adapters;

import org.eclipse.debug.examples.core.midi.launcher.SequencerControl;
import org.eclipse.jface.viewers.ICellModifier;

/**
 * A cell modifier for a sequencer control. Provides current
 * values of controls and updates control values in the sequencer
 * as they are changed in the UI.
 *
 * @since 1.0
 */
public class ControlCellModifier implements ICellModifier {

	@Override
	public boolean canModify(Object element, String property) {
		if (SequencerColumnPresentation.COL_VALUE.equals(property)) {
			if (element instanceof SequencerControl) {
				return ((SequencerControl) element).isEditable();
			}
		}
		return false;
	}

	@Override
	public Object getValue(Object element, String property) {
		if (SequencerColumnPresentation.COL_VALUE.equals(property)) {
			if (element instanceof SequencerControl) {
				SequencerControl control = (SequencerControl) element;
				return control.getValue();
			}
		}
		return null;
	}

	@Override
	public void modify(Object element, String property, Object value) {
		Object oldValue = getValue(element, property);
		if (!value.equals(oldValue)) {
			if (SequencerColumnPresentation.COL_VALUE.equals(property)) {
				if (element instanceof SequencerControl) {
					if (value instanceof String) {
						SequencerControl control = (SequencerControl) element;
						control.setValue((String) value);
					}
				}
			}
		}
	}

}
