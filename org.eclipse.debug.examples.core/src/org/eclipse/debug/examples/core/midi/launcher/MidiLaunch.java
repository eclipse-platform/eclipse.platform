/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.Sequencer;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ISuspendResume;

/**
 * A launch containing a MIDI sequencer.
 *
 * @since 1.0
 */
public class MidiLaunch extends Launch implements ISuspendResume {

	/**
	 * MIDI Sequencer
	 */
	private Sequencer fSequencer;

	/**
	 * MIDI file format
	 */
	private MidiFileFormat fFormat;

	/**
	 * Constructs a new MIDI launch.
	 *
	 * @param launchConfiguration configuration to play
	 * @param mode mode to play in
	 */
	public MidiLaunch(ILaunchConfiguration launchConfiguration, String mode) {
		super(launchConfiguration, mode, null);
	}

	/**
	 * Sets the sequencer used to play MIDI files.
	 *
	 * @param sequencer
	 */
	public void setSequencer(Sequencer sequencer) {
		fSequencer = sequencer;
		fireChanged();
	}

	/**
	 * Sets the format of the sequence
	 * @param format
	 */
	public void setFormat(MidiFileFormat format) {
		fFormat = format;
	}

	/**
	 * Returns the file format of the sequence.
	 *
	 * @return file format
	 */
	public MidiFileFormat getFormat() {
		return fFormat;
	}
	/**
	 * Returns the sequencer used to play MIDI files.
	 *
	 * @return the sequencer used to play MIDI files
	 */
	public Sequencer getSequencer() {
		return fSequencer;
	}

	@Override
	public boolean canTerminate() {
		return fSequencer.isOpen();
	}

	@Override
	public boolean isTerminated() {
		if (fSequencer != null) {
			return !fSequencer.isOpen();
		}
		return false;
	}

	@Override
	public void terminate() throws DebugException {
		fSequencer.stop();
		fSequencer.close();
		fireTerminate();
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
				new DebugEvent(fSequencer, DebugEvent.TERMINATE) });
	}

	@Override
	public boolean canResume() {
		return isSuspended();
	}

	@Override
	public boolean canSuspend() {
		if (fSequencer != null) {
			return fSequencer.isRunning();
		}
		return false;
	}

	@Override
	public boolean isSuspended() {
		if (fSequencer != null) {
			return fSequencer.isOpen() && !fSequencer.isRunning();
		}
		return false;
	}

	@Override
	public void resume() throws DebugException {
		fSequencer.start();
		fireChanged();
		fireEvent(new DebugEvent(fSequencer, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST));
	}

	@Override
	public void suspend() throws DebugException {
		fSequencer.stop();
		fireChanged();
		fireEvent(new DebugEvent(fSequencer, DebugEvent.SUSPEND, DebugEvent.CLIENT_REQUEST));
	}

	/**
	 * Fires a debug event.
	 *
	 * @param event debug event to fire
	 */
	protected void fireEvent(DebugEvent event) {
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {event});
	}
}
