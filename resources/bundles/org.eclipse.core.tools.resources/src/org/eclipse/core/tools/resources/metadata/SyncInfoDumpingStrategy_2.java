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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.tools.ByteUtil;
import org.eclipse.core.tools.metadata.*;

/**
 * A strategy for reading .syncinfo files version 2. Layout:
 * <pre> {@code
 * SAVE_FILE -> VERSION_ID RESOURCE*
 * VERSION_ID -> int
 * RESOURCE -> RESOURCE_PATH SIZE SYNCINFO*
 * RESOURCE_PATH -> String
 * SIZE -> int
 * SYNCINFO -> TYPE BYTES
 * TYPE -> INDEX | QNAME
 * INDEX -> int int
 * QNAME -> int String
 * BYTES -> byte[]
 * }</pre>
 */
@SuppressWarnings("restriction")
class SyncInfoDumpingStrategy_2 implements IStringDumpingStrategy {
	@Override
	public String dumpStringContents(DataInputStream dataInput) throws IOException, DumpException {
		StringBuilder contents = new StringBuilder();
		List<String> readPartners = new ArrayList<>();
		String resourceName;
		while (dataInput.available() > 0) {
			resourceName = dataInput.readUTF();

			contents.append("Resource: "); //$NON-NLS-1$
			contents.append(resourceName);
			contents.append('\n');
			dumpReadPartners(dataInput, contents, readPartners);
			contents.append('\n');
		}
		return contents.toString();
	}

	private void dumpReadPartners(DataInputStream input, StringBuilder contents, List<String> readPartners) throws DumpException, IOException {
		int size = input.readInt();
		for (int i = 0; i < size; i++) {
			String qualifiedName;
			int type = input.readInt();
			switch (type) {
				case SyncInfoDumper.QNAME :
					String qualifier = input.readUTF();
					String localName = input.readUTF();
					if (qualifier.length() > 0) {
						qualifiedName = qualifier + ":" + localName; //$NON-NLS-1$
					} else {
						qualifiedName = localName;
					}
					readPartners.add(qualifiedName);
					break;
				case SyncInfoDumper.INDEX :
					qualifiedName = readPartners.get(input.readInt());
					break;
				default :
					//if we get here then the sync info file is corrupt
					throw new PartialDumpException("Invalid read partner type found: " + type, contents); //$NON-NLS-1$
			}
			contents.append("Read Partner: "); //$NON-NLS-1$
			contents.append(qualifiedName);
			contents.append('\n');

			// read the bytes
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
		return "Sync info file version 2"; //$NON-NLS-1$
	}
}
