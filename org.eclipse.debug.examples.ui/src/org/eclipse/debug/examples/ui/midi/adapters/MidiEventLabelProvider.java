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
 *     Patrick Chuong (Texas Instruments) - Checkbox support for Flexible Hierachy view (Bug 286310)
 *******************************************************************************/
package org.eclipse.debug.examples.ui.midi.adapters;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.ui.model.elements.ElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.viewers.TreePath;

/**
 * Provides labels for MIDI tracks.
 *
 * @since 1.0
 */
public class MidiEventLabelProvider extends ElementLabelProvider {

	@Override
	protected String getLabel(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		MidiEvent event = (MidiEvent) elementPath.getLastSegment();
		MidiMessage message = event.getMessage();
		if (TrackColumnPresentation.COL_TICK.equals(columnId)) {
			return Long.toString(event.getTick());
		} else if (TrackColumnPresentation.COL_BYTES.equals(columnId)) {
			byte[] bytes = message.getMessage();
			StringBuilder buffer = new StringBuilder();
			for (int i = 0; i < message.getLength(); i++) {
				buffer.append(' ');
				appendByte(buffer, bytes[i]);
			}
			return buffer.toString();
		} else if (TrackColumnPresentation.COL_COMMAND.equals(columnId)) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage) message;
				StringBuilder buf = new StringBuilder();
				appendByte(buf, (byte)sm.getCommand());
				return buf.toString();
			}
		} else if (TrackColumnPresentation.COL_CHANNEL.equals(columnId)) {
			if (message instanceof ShortMessage) {
				return Integer.toString(((ShortMessage)message).getChannel());
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean getChecked(TreePath path, IPresentationContext presentationContext) throws CoreException {
		Boolean result = MidiEventModelProxy.gChecked.get(path);
		return result == null ? false : result.booleanValue();
	}

	/**
	 * Appends a byte to the buffer with 2 hex characters.
	 *
	 * @param buffer
	 * @param b
	 */
	private void appendByte(StringBuilder buffer, byte b) {
		String hex = Integer.toHexString(b & 0xFF).toUpperCase();
		for (int i = hex.length(); i < 2; i++) {
			buffer.append('0');
		}
		buffer.append(hex);
	}

}
