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
 * A strategy for reading .markers files version 1. Layout:
 * <pre> {@code
 * SAVE_FILE -> VERSION_ID RESOURCE+
 * VERSION_ID ->
 * RESOURCE -> RESOURCE_PATH MARKERS_SIZE MARKER*
 * RESOURCE_PATH -> String
 * MARKERS_SIZE -> int
 * MARKER -> MARKER_ID TYPE ATTRIBUTES_SIZE ATTRIBUTE*
 * MARKER_ID -> long
 * TYPE -> INDEX | QNAME
 * INDEX -> int int
 * QNAME -> int String
 * ATTRIBUTES_SIZE -> int
 * ATTRIBUTE -> ATTRIBUTE_KEY ATTRIBUTE_VALUE
 * ATTRIBUTE_KEY -> String
 * ATTRIBUTE_VALUE -> INTEGER_VALUE | BOOLEAN_VALUE | STRING_VALUE | NULL_VALUE
 * INTEGER_VALUE -> int int
 * BOOLEAN_VALUE -> int boolean
 * STRING_VALUE -> int String
 * NULL_VALUE -> int
 * }</pre>
 */
@SuppressWarnings("restriction")
 public class MarkersDumpingStrategy_1 implements IStringDumpingStrategy {
	 @Override
	 public String dumpStringContents(DataInputStream dataInput) throws IOException, DumpException {
		 StringBuilder contents = new StringBuilder();
		 List<String> markerTypes = new ArrayList<>();
		 while (dataInput.available() > 0) {
			 String resourceName = dataInput.readUTF();
			 contents.append("Resource: "); //$NON-NLS-1$
			 contents.append(resourceName);
			 contents.append('\n');
			 dumpMarkers(dataInput, contents, markerTypes);
			 contents.append('\n');
		 }
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
			 contents.append('\n');
		 }
	 }

	 private void dumpAttributes(DataInputStream input, StringBuilder contents) throws IOException, DumpException {
		 int attributesSize = input.readInt();
		 contents.append("Attributes ["); //$NON-NLS-1$
		 contents.append(attributesSize);
		 contents.append("]:"); //$NON-NLS-1$
		 contents.append('\n');
		 for (int j = 0; j < attributesSize; j++) {
			 contents.append(input.readUTF());
			 int type = input.readInt();
			 Object value = null;
			 switch (type) {
				 case MarkersDumper.ATTRIBUTE_INTEGER :
					 value = Integer.valueOf(input.readInt());
					 break;
				 case MarkersDumper.ATTRIBUTE_BOOLEAN :
					 value = input.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
					 break;
				 case MarkersDumper.ATTRIBUTE_STRING :
					 value = "\"" + input.readUTF() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
					 break;
				 case MarkersDumper.ATTRIBUTE_NULL :
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
		 int constant = input.readInt();
		 switch (constant) {
			 case MarkersDumper.QNAME :
				 markerType = input.readUTF();
				 markerTypes.add(markerType);
				 break;
			 case MarkersDumper.INDEX :
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
		 return "Markers snapshot file version 1"; //$NON-NLS-1$
	 }
 }
