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

import java.io.BufferedInputStream;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;

/**
 * Creates and starts a MIDI sequencer.
 *
 * @since 1.0
 */
public class MidiLaunchDelegate extends LaunchConfigurationDelegate {

	/**
	 * Identifier for the MIDI launch configuration type
	 * (value <code>midi.launchType</code>)
	 */
	public static final String ID_MIDI_LAUNCH_CONFIGURATION_TYPE = "midi.launchType"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute for the MIDI file to play
	 * (value <code>midi.file</code>)
	 */
	public static final String ATTR_MIDI_FILE = "midi.file"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute for the MIDI launcher. Specifies whether to throw
	 * an exception when present. Value is one of <code>HANDLED</code> or <code>UNHANDLED</code>.
	 */
	public static final String ATTR_THROW_EXCEPTION = "throw.exception"; //$NON-NLS-1$

	/**
	 * Possible values for the <code>ATTR_THROW_EXCEPTION</code>.
	 */
	public static final String HANDLED = "HANDLED"; //$NON-NLS-1$
	public static final String UNHANDLED = "UNHANDLED"; //$NON-NLS-1$

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String excep = configuration.getAttribute(ATTR_THROW_EXCEPTION, (String)null);
		if (excep != null) {
			if (HANDLED.equals(excep)) {
				throw new CoreException(new Status(IStatus.ERROR, DebugCorePlugin.PLUGIN_ID, 303, "Test handled exception during launch", null)); //$NON-NLS-1$
			} else {
				throw new CoreException(new Status(IStatus.ERROR, DebugCorePlugin.PLUGIN_ID, "Test unhandled exception during launch", new Error("Test unhandled exception during launch"))); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		String fileName = configuration.getAttribute(ATTR_MIDI_FILE, (String)null);
		if (fileName == null) {
			abort("MIDI file not specified.", null); //$NON-NLS-1$
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile file = root.getFile(IPath.fromOSString(fileName));
		if (!file.exists()) {
			abort("MIDI file does not exist.", null); //$NON-NLS-1$
		}
		Sequencer sequencer = null;
		MidiFileFormat fileFormat = null;
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			IPath location = file.getLocation();
			if (location != null) {
				fileFormat = MidiSystem.getMidiFileFormat(location.toFile());
			}
		} catch (MidiUnavailableException e) {
			abort("Cannot initialize sequencer.", e); //$NON-NLS-1$
		} catch (InvalidMidiDataException e) {
			abort("Invalid MIDI file.", e); //$NON-NLS-1$
		} catch (IOException e) {
			abort("Error reading MIDI file.", e); //$NON-NLS-1$
		}
		if(sequencer != null) {
			BufferedInputStream stream = new BufferedInputStream(file.getContents());
			try {
				sequencer.setSequence(stream);
			} catch (IOException e) {
				abort("Error reading MIDI file", e); //$NON-NLS-1$
			} catch (InvalidMidiDataException e) {
				abort("Inavlid MIDI file.", e); //$NON-NLS-1$
			}
			MidiLaunch midiLaunch = (MidiLaunch)launch;
			midiLaunch.setSequencer(sequencer);
			midiLaunch.setFormat(fileFormat);
			sequencer.start();
		}
		else {
			abort("Could not create the sequencer", null); //$NON-NLS-1$
		}
	}

	/**
	 * Throws an exception with a new status containing the given
	 * message and optional exception.
	 *
	 * @param message error message
	 * @param e underlying exception
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, DebugCorePlugin.PLUGIN_ID, 0, message, e));
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return new MidiLaunch(configuration, mode);
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		return false;
	}



}
