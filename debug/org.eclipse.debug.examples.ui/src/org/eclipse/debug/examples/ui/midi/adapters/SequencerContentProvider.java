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

import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.examples.core.midi.launcher.ClockControl;
import org.eclipse.debug.examples.core.midi.launcher.LengthControl;
import org.eclipse.debug.examples.core.midi.launcher.MidiLaunch;
import org.eclipse.debug.examples.core.midi.launcher.SequencerControl;
import org.eclipse.debug.examples.core.midi.launcher.TempoControl;
import org.eclipse.debug.internal.ui.model.elements.ElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Provides content for a MIDI sequencer element (MIDI launch).
 *
 * @since 1.0
 */
public class SequencerContentProvider extends ElementContentProvider {

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		if (IDebugUIConstants.ID_DEBUG_VIEW.equals(context.getId())) {
			return getTracks((MidiLaunch) element).length;
		} else if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(context.getId())) {
			if (((MidiLaunch)element).isTerminated()) {
				return 0;
			}
			return 3;
		}
		return 0;
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		if (IDebugUIConstants.ID_DEBUG_VIEW.equals(context.getId())) {
			return getElements(getTracks((MidiLaunch) parent), index, length);
		} else if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(context.getId())) {
			return getElements(getControls((MidiLaunch) parent), index, length);
		}
		return EMPTY;
	}

	/**
	 * Returns the controls for this sequencer.
	 *
	 * @param launch midi launch
	 * @return controls
	 */
	public SequencerControl[] getControls(MidiLaunch launch) {
		return new SequencerControl[]{
				new TempoControl(launch),
				new ClockControl(launch),
				new LengthControl(launch)
		};
	}

	/**
	 * Returns all tracks in the sequence.
	 *
	 * @param launch MIDI launch
	 *@return tracks
	 */
	public Track[] getTracks(MidiLaunch launch) {
		Sequencer sequencer = launch.getSequencer();
		if (sequencer != null && sequencer.isOpen()) {
			return sequencer.getSequence().getTracks();
		}
		return new Track[0];
	}

	@Override
	protected boolean supportsContextId(String id) {
		return IDebugUIConstants.ID_DEBUG_VIEW.equals(id)
			|| IDebugUIConstants.ID_VARIABLE_VIEW.equals(id);
	}

}
