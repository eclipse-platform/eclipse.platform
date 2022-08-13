/*******************************************************************************
 * Copyright (c) 2002, 2018 IBM Corporation and others.
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
import org.eclipse.core.tools.metadata.*;

/**
 * A strategy for reading .markers.snap files version 2. Layout:
 * <pre> {@code
 * SNAP_FILE -> [VERSION_ID RESOURCE]*
 * VERSION_ID -> int (used for backwards compatibiliy)
 * RESOURCE -> RESOURCE_PATH MARKER_SIZE MARKER+
 * RESOURCE_PATH -> String
 * MARKER_SIZE -> int
 * MARKER -> MARKER_ID TYPE ATTRIBUTES_SIZE ATTRIBUTE* CREATION_TIME
 * MARKER_ID -> long
 * TYPE -> INDEX | QNAME
 * INDEX -> byte int
 * QNAME -> byte String
 * ATTRIBUTES_SIZE -> short
 * ATTRIBUTE -> ATTRIBUTE_KEY ATTRIBUTE_VALUE
 * ATTRIBUTE_KEY -> String
 * ATTRIBUTE_VALUE -> BOOLEAN_VALUE | INTEGER_VALUE | STRING_VALUE | NULL_VALUE
 * BOOLEAN_VALUE -> byte boolean
 * INTEGER_VALUE -> byte int
 * STRING_VALUE -> byte String
 * NULL_VALUE -> byte
 * CREATION_TIME -> long
 * }</pre>
 */
@SuppressWarnings("restriction")
class MarkersSnapshotDumpingStrategy_2 implements IStringDumpingStrategy {
	@Override
	public String dumpStringContents(DataInputStream input) throws IOException, DumpException {
		StringBuilder contents = new StringBuilder();
		DataInputStream dataInput = new DataInputStream(input);
		List<String> markerTypes = new ArrayList<>();
		String resourceName = dataInput.readUTF();
		contents.append("Resource: "); //$NON-NLS-1$
		contents.append(resourceName);
		contents.append('\n');
		dumpMarkers(dataInput, contents, markerTypes);
		return contents.toString();
	}

	private void dumpMarkers(DataInputStream input, StringBuilder contents, List<String> markerTypes) throws IOException, DumpException {
		int markersSize = input.readInt();
		contents.append("Markers ["); //$NON-NLS-1$
		contents.append(markersSize);
		contents.append("]:"); //$NON-NLS-1$
		contents.append('\n');
		for (int i = 0; i < markersSize; i++) {
			contents.append("ID: "); //$NON-NLS-1$
			contents.append(input.readLong());
			contents.append('\n');
			dumpMarkerType(input, contents, markerTypes);
			dumpAttributes(input, contents);
			contents.append("Creation time: "); //$NON-NLS-1$
			contents.append(input.readLong());
			contents.append('\n');
		}
	}

	private void dumpAttributes(DataInputStream input, StringBuilder contents) throws IOException, DumpException {
		int attributesSize = input.readShort();
		contents.append("Attributes ["); //$NON-NLS-1$
		contents.append(attributesSize);
		contents.append("]:"); //$NON-NLS-1$
		contents.append('\n');
		for (int j = 0; j < attributesSize; j++) {
			contents.append(input.readUTF());
			byte type = input.readByte();
			Object value = null;
			switch (type) {
				case MarkersSnapshotDumper.ATTRIBUTE_INTEGER :
					value = Integer.valueOf(input.readInt());
					break;
				case MarkersSnapshotDumper.ATTRIBUTE_BOOLEAN :
					value = input.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
					break;
				case MarkersSnapshotDumper.ATTRIBUTE_STRING :
					value = "\"" + input.readUTF() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case MarkersSnapshotDumper.ATTRIBUTE_NULL :
					break;
				default :
					throw new PartialDumpException("Invalid marker attribute type found: " + type, contents); //$NON-NLS-1$

			}
			contents.append('=');
			contents.append(value);
			contents.append('\n');
		}
	}

	private void dumpMarkerType(DataInputStream input, StringBuilder contents, List<String> markerTypes) throws IOException, DumpException {

		String markerType;
		byte constant = input.readByte();
		switch (constant) {
			case MarkersSnapshotDumper.QNAME :
				markerType = input.readUTF();
				markerTypes.add(markerType);
				break;
			case MarkersSnapshotDumper.INDEX :
				markerType = markerTypes.get(input.readInt());
				break;
			default :
				throw new PartialDumpException("Invalid marker type constant found: " + constant, contents); //$NON-NLS-1$
		}
		contents.append("Marker Type: "); //$NON-NLS-1$
		contents.append(markerType);
		contents.append('\n');
	}

	@Override
	public String getFormatDescription() {
		return "Markers file version 2"; //$NON-NLS-1$
	}
}
