/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * This class is used to read markers from disk. This is for version 2. Here
 * is the file format:
 */
public class MarkerReader_3 extends MarkerReader {

	// type constants
	public static final byte INDEX = 1;
	public static final byte QNAME = 2;

	// marker attribute types
	public static final byte ATTRIBUTE_NULL = 0;
	public static final byte ATTRIBUTE_BOOLEAN = 1;
	public static final byte ATTRIBUTE_INTEGER = 2;
	public static final byte ATTRIBUTE_STRING = 3;

	public MarkerReader_3(Workspace workspace) {
		super(workspace);
	}

	/**
	 * <pre> {@code
	 * SAVE_FILE -> VERSION_ID RESOURCE+
	 * VERSION_ID -> int
	 * RESOURCE -> RESOURCE_PATH MARKERS_SIZE MARKER+
	 * RESOURCE_PATH -> String
	 * MARKERS_SIZE -> int
	 * MARKER -> MARKER_ID TYPE ATTRIBUTES_SIZE ATTRIBUTE* CREATION_TIME
	 * MARKER_ID -> long
	 * TYPE -> INDEX | QNAME
	 * INDEX -> byte int
	 * QNAME -> byte String
	 * ATTRIBUTES_SIZE -> short
	 * ATTRIBUTE -> ATTRIBUTE_KEY ATTRIBUTE_VALUE
	 * ATTRIBUTE_KEY -> String
	 * ATTRIBUTE_VALUE -> INTEGER_VALUE | BOOLEAN_VALUE | STRING_VALUE | NULL_VALUE
	 * INTEGER_VALUE -> byte int
	 * BOOLEAN_VALUE -> byte boolean
	 * STRING_VALUE -> byte String
	 * NULL_VALUE -> byte
	 * CREATION_TIME -> long
	 * }</pre>
	 */
	@Override
	public void read(DataInputStream input, boolean generateDeltas) throws IOException, CoreException {
		try {
			List<String> readTypes = new ArrayList<>(5);
			while (true) {
				IPath path = IPath.fromOSString(input.readUTF());
				int markersSize = input.readInt();
				MarkerSet markers = new MarkerSet(markersSize);
				for (int i = 0; i < markersSize; i++)
					markers.add(readMarkerInfo(input, readTypes));
				// if the resource doesn't exist then return. ensure we do this after
				// reading the markers from the file so we don't get into an
				// inconsistent state.
				ResourceInfo info = workspace.getResourceInfo(path, false, false);
				if (info == null)
					continue;
				info.setMarkers(markers);
				if (generateDeltas) {
					// Iterate over all elements and add not null ones. This saves us from copying
					// and shrinking the array.
					Resource resource = workspace.newResource(path, info.getType());
					IMarkerSetElement[] infos = markers.elements;
					ArrayList<MarkerDelta> deltas = new ArrayList<>(infos.length);
					for (IMarkerSetElement info2 : infos)
						if (info2 != null)
							deltas.add(new MarkerDelta(IResourceDelta.ADDED, resource, (MarkerInfo) info2));
					workspace.getMarkerManager().changedMarkers(resource, deltas.toArray(new IMarkerSetElement[deltas.size()]));
				}
			}
		} catch (EOFException e) {
			// ignore end of file
		}
	}

	private Map<String, Object> readAttributes(DataInputStream input) throws IOException {
		int attributesSize = input.readShort();
		if (attributesSize == 0)
			return null;
		Map<String, Object> result = new HashMap<>(attributesSize);
		for (int j = 0; j < attributesSize; j++) {
			String key = input.readUTF();
			byte type = input.readByte();
			Object value = null;
			switch (type) {
				case ATTRIBUTE_INTEGER :
					//canonicalize well known values (marker severity, task priority)
					value = Integer.valueOf(input.readInt());
					break;
				case ATTRIBUTE_BOOLEAN :
					value = Boolean.valueOf(input.readBoolean());
					break;
				case ATTRIBUTE_STRING :
					value = input.readUTF();
					break;
				case ATTRIBUTE_NULL :
					// do nothing
					break;
			}
			if (value != null) {
				result.put(key, value);
			}
		}
		return result.isEmpty() ? null : result;
	}

	private MarkerInfo readMarkerInfo(DataInputStream input, List<String> readTypes) throws IOException, CoreException {
		long id = input.readLong();
		String type = null;
		byte constant = input.readByte();
		switch (constant) {
			case QNAME :
				type = input.readUTF();
				readTypes.add(type);
				break;
			case INDEX :
				type = readTypes.get(input.readInt());
				break;
			default :
				//if we get here the marker file is corrupt
				String msg = Messages.resources_readMarkers;
				throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, null, msg, null);
		}
		Map<String, Object> map = readAttributes(input);
		long creationTime = input.readLong();
		return new MarkerInfo(map, false, creationTime, type, id);
	}
}
