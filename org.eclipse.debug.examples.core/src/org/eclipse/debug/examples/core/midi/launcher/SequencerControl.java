/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.debug.examples.core.midi.launcher;

import javax.sound.midi.Sequencer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;

/**
 * Controls some aspect of a MIDI sequencer.
 *
 * @since 1.0
 */
public abstract class SequencerControl {

	/**
	 * The launch
	 */
	private MidiLaunch fLaunch;

	/**
	 * Control name
	 */
	private String fName;

	/**
	 * Constructs a control with the given name.
	 */
	public SequencerControl(String name, MidiLaunch launch) {
		fName = name;
		fLaunch = launch;
	}

	/**
	 * Returns the launch this control is associated with.
	 *
	 * @return MIDI launch
	 */
	public MidiLaunch getLaunch() {
		return fLaunch;
	}

	/**
	 * Returns the sequencer associated with this control.
	 *
	 * @return associated sequencer
	 */
	public Sequencer getSequencer() {
		return fLaunch.getSequencer();
	}

	/**
	 * Returns the name of this control.
	 *
	 * @return control name
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Returns this controls current value.
	 *
	 * @return current value
	 */
	public abstract String getValue();

	/**
	 * Whether this contol's value can be modified.
	 *
	 * @return Whether this contol's value can be modified
	 */
	public abstract boolean isEditable();

	/**
	 * Returns a status indicating if the given value is
	 * a valid value for this control to accept.
	 *
	 * @param value new value
	 * @return whether the value is valid
	 */
	public abstract IStatus validateValue(String value);

	/**
	 * Sets the value of this control to the given value
	 * and returns a status indicating if the value was
	 * successfully set.
	 *
	 * @param newValue value
	 * @return whether successful
	 */
	public abstract IStatus setValue(String newValue);

	/**
	 * Fires a debug event.
	 *
	 * @param event debug event to fire
	 */
	public void fireEvent(DebugEvent event) {
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {event});
	}

	@SuppressWarnings("resource")
	@Override
	public boolean equals(Object obj) {
		if (obj != null) {
			if (getClass().equals(obj.getClass())) {
				return ((SequencerControl)obj).getSequencer().equals(getSequencer());

			}
		}
		return false;
	}

	@SuppressWarnings("resource")
	@Override
	public int hashCode() {
		return getSequencer().hashCode() + getClass().hashCode();
	}
}
