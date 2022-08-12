/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.resources.metadata;

import java.io.DataInputStream;
import java.io.IOException;
import org.eclipse.core.tools.ByteUtil;
import org.eclipse.core.tools.metadata.IStringDumpingStrategy;

/**
 * A strategy for reading .syncinfo.snap files version 3. Layout:
 * <pre> {@code
 * SNAP_FILE -> [VERSION_ID RESOURCE]*
 * VERSION_ID -> int
 * RESOURCE -> RESOURCE_PATH SIZE SYNCINFO*
 * RESOURCE_PATH -> String
 * SIZE -> int
 * SYNCINFO -> QNAME BYTES
 * QNAME -> String String
 * BYTES -> byte[]
 * }</pre>
 */
class SyncInfoSnapshotDumpingStrategy_3 implements IStringDumpingStrategy {
	@Override
	public String dumpStringContents(DataInputStream dataInput) throws IOException {
		StringBuilder contents = new StringBuilder();
		String resourceName = dataInput.readUTF();

		contents.append("Resource: "); //$NON-NLS-1$
		contents.append(resourceName);
		contents.append('\n');
		dumpReadPartners(dataInput, contents);
		contents.append('\n');
		return contents.toString();
	}

	private void dumpReadPartners(DataInputStream input, StringBuilder contents) throws IOException {
		int size = input.readInt();
		for (int i = 0; i < size; i++) {
			String qualifier = input.readUTF();
			String localName = input.readUTF();

			String qualifiedName = (qualifier.length() > 0) ? (qualifier + ":" + localName) : localName; //$NON-NLS-1$

			contents.append("Read Partner: "); //$NON-NLS-1$
			contents.append(qualifiedName);
			contents.append('\n');

			// read the bytes - what to do with them???
			int length = input.readInt();
			byte[] bytes = new byte[length];
			input.readFully(bytes);
			// just show the first ones
			contents.append("Bytes ("); //$NON-NLS-1$
			contents.append(length);
			contents.append("): "); //$NON-NLS-1$
			contents.append(ByteUtil.byteArrayToString(bytes, 64));
			contents.append('\n');
		}
	}

	@Override
	public String getFormatDescription() {
		return "Sync info snapshot file version 3"; //$NON-NLS-1$
	}
}
