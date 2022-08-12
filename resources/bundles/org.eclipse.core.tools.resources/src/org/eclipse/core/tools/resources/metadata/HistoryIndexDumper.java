/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.tools.resources.metadata;

import java.io.*;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.tools.metadata.AbstractDumper;
import org.eclipse.core.tools.metadata.DumpException;

public class HistoryIndexDumper extends AbstractDumper {

	private final static int LONG_LENGTH = 8;

	private final static int UUID_LENGTH = UniversalUniqueIdentifier.BYTES_SIZE;

	public final static int DATA_LENGTH = UUID_LENGTH + LONG_LENGTH;

	private static long getTimestamp(byte[] state) {
		long timestamp = 0;
		for (int j = 0; j < LONG_LENGTH; j++)
			timestamp += (state[UUID_LENGTH + j] & 0xFFL) << j * 8;
		return timestamp;
	}

	@Override
	protected void dumpContents(PushbackInputStream input, StringBuilder contents) throws IOException, Exception, DumpException {
		DataInputStream source = new DataInputStream(input);
		int version = source.readByte();
		contents.append("version: ");
		contents.append(version);
		contents.append('\n');
		int entryCount = source.readInt();
		contents.append("entries: ");
		contents.append(entryCount);
		contents.append('\n');
		// data holds a UUID (16 bytes) + timestamp (long - 8 bytes)
		byte[] data = new byte[24];
		for (int i = 0; i < entryCount; i++) {
			contents.append("Path: ");
			contents.append(source.readUTF());
			int stateCount = source.readUnsignedShort();
			contents.append('\n');
			contents.append("State count: ");
			contents.append(stateCount);
			contents.append("\n");
			for (int j = 0; j < stateCount; j++) {
				int read;
				if ((read = source.read(data)) < data.length)
					throw new DumpException("Data incomplete. Read " + read + " byte(s)");
				contents.append('\t');
				contents.append("UUID: ");
				contents.append(new UniversalUniqueIdentifier(data));
				contents.append(" | timestamp: ");
				contents.append(getTimestamp(data));
				contents.append('\n');
			}
		}

	}
}
