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

import org.eclipse.core.runtime.IStatus;

/**
 * Displays a time value based on underlying microsecond value
 *
 * @since 1.0
 */
public abstract class TimeControl extends SequencerControl {

	/**
	 * Constructs a time control with the given name for the
	 * given launch.
	 *
	 * @param name
	 * @param launch
	 */
	public TimeControl(String name, MidiLaunch launch) {
		super(name, launch);
	}

	@Override
	public String getValue() {
		long position = getTimeValue();
		int milli = (int) (position & 0x3F);
		int sec = (int) (position / 1000000);
		int min = sec / 60;
		sec = sec % 60;
		StringBuilder clock = new StringBuilder();
		clock.append(min);
		while (clock.length() < 2) {
			clock.insert(0, 0);
		}
		clock.append(':');
		clock.append(sec);
		while (clock.length() < 5) {
			clock.insert(3, 0);
		}
		clock.append(':');
		clock.append(milli);
		while (clock.length() < 8) {
			clock.insert(6, 0);
		}
		return clock.toString();
	}

	/**
	 * Provided by subclasses for the control.
	 *
	 * @return time in microseconds
	 */
	protected abstract long getTimeValue();

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public IStatus setValue(String newValue) {
		return null;
	}

	@Override
	public IStatus validateValue(String value) {
		return null;
	}

}
