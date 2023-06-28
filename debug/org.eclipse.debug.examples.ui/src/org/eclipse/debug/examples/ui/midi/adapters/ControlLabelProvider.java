/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.examples.core.midi.launcher.SequencerControl;
import org.eclipse.debug.internal.ui.model.elements.ElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.viewers.TreePath;

/**
 * Label provider for a sequencer control.
 *
 * @since 1.0
 */
public class ControlLabelProvider extends ElementLabelProvider {

	@Override
	protected String getLabel(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		SequencerControl control = (SequencerControl) elementPath.getLastSegment();
		if (SequencerColumnPresentation.COL_NAME.equals(columnId)) {
			return control.getName();
		}
		if (SequencerColumnPresentation.COL_VALUE.equals(columnId)) {
			return control.getValue();
		}
		return ""; //$NON-NLS-1$
	}

}
