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

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.examples.ui.pda.views.CheckboxView;
import org.eclipse.debug.internal.ui.model.elements.ElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Content provider for track in the variables view. Returns MIDI events
 * in the track.
 *
 * @since 1.0
 */
public class TrackContentProvider extends ElementContentProvider {

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		Track track = (Track) element;
		if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(context.getId()) || CheckboxView.ID.equals(context.getId()) ) {
			return track.size();
		}
		return 0;
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(context.getId()) || CheckboxView.ID.equals(context.getId())) {
			Track track = (Track) parent;
			MidiEvent[] events= new MidiEvent[length];
			for (int i = 0; i < length; i++) {
				events[i] = track.get(i+index);
			}
			return events;
		}
		return EMPTY;
	}

	@Override
	protected boolean supportsContextId(String id) {
		return IDebugUIConstants.ID_VARIABLE_VIEW.equals(id) || CheckboxView.ID.equals(id);
	}

}
