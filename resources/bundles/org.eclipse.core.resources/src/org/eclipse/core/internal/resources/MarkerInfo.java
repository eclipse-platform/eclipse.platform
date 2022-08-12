/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - Bug 488938, 488937
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.internal.utils.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;

public class MarkerInfo implements IMarkerSetElement, Cloneable, IStringPoolParticipant {
	// this class is used concurrently => all members have to be final or volatile
	/**
	 * The store of attributes for this marker. Can not be modified since that could
	 * remove concurrently added entries while the last entry is removed.
	 */
	private final MarkerAttributeMap attributes;

	/** The creation time for this marker. */
	protected final long creationTime;

	/** Marker identifier. */
	protected final long id;

	/** The type of this marker. */
	protected volatile String type;

	/**
	 * Returns whether the given object is a valid attribute value. Returns
	 * either the attribute or an equal canonical substitute.
	 */
	protected static Object checkValidAttribute(Object value) {
		if (value == null)
			return null;
		if (value instanceof String) {
			//we cannot write attributes whose UTF encoding exceeds 65535 bytes.
			String valueString = (String) value;
			//optimized test based on maximum 3 bytes per character
			if (valueString.length() < 21000)
				return value;
			byte[] bytes = valueString.getBytes(StandardCharsets.UTF_8);
			if (bytes.length > 65535) {
				String msg = "Marker property value is too long: " + valueString.substring(0, 10000); //$NON-NLS-1$
				Assert.isTrue(false, msg);
			}
			return value;
		}
		if (value instanceof Boolean) {
			//return canonical boolean
			return Boolean.valueOf(((Boolean) value));
		}
		if (value instanceof Integer) {
			//replace common integers with canonical values
			return Integer.valueOf(((Integer) value));
		}
		//if we got here, it's an invalid attribute value type
		throw new IllegalArgumentException(NLS.bind(Messages.resources_wrongMarkerAttributeValueType, value.getClass().getName()));
	}

	public MarkerInfo(String type, long id) {
		this(null, false, type, id);
	}

	public MarkerInfo(MarkerAttributeMap map, long creationTime, String type, long id) {
		super();
		attributes = map;
		this.id = id;
		this.creationTime = creationTime;
		this.type = type;
	}

	/** clone constructor **/
	public MarkerInfo(MarkerInfo markerInfo) {
		this(new MarkerAttributeMap(markerInfo.attributes), markerInfo.creationTime, markerInfo.type, markerInfo.id);
	}

	public MarkerInfo(Map<String, ? extends Object> attributes, boolean validate, long creationTime, String type,
			long id) {
		this(attributes == null ? new MarkerAttributeMap() : new MarkerAttributeMap(attributes, validate), creationTime,
				type, id);

	}

	public MarkerInfo(Map<String, ? extends Object> attributes, boolean validate, String type, long id) {
		this(attributes, validate, System.currentTimeMillis(), type, id);
	}

	/**
	 * See Object#clone.
	 */
	@Override
	public Object clone() {
		return new MarkerInfo(this);
	}

	public Object getAttribute(String attributeName) {
		return attributes.get(attributeName);
	}

	public Map<String, Object> getAttributes() {
		if (attributes.isEmpty())
			return null;
		return attributes.toMap();
	}

	public MarkerAttributeMap getAttributes(boolean makeCopy) {
		if (attributes.isEmpty())
			return null;
		return makeCopy ? new MarkerAttributeMap(attributes) : attributes;
	}

	public Object[] getAttributes(String[] attributeNames) {
		Object[] result = new Object[attributeNames.length];
		for (int i = 0; i < attributeNames.length; i++)
			result[i] = getAttribute(attributeNames[i]);
		return result;
	}

	public long getCreationTime() {
		return creationTime;
	}

	@Override
	public long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public void setAttribute(String attributeName, Object value, boolean validate) {
		if (validate) {
			value = checkValidAttribute(value);
		}
		if (value == null) {
			attributes.remove(attributeName);
		} else {
			attributes.put(attributeName, value);
		}
	}

	/** deletes previous Attributes **/
	public void setAttributes(Map<String, ? extends Object> map, boolean validate) {
		attributes.setAttributes(map, validate);
	}

	/** keeps previous Attributes **/
	public void addAttributes(String[] attributeNames, Object[] values, boolean validate) {
		Assert.isTrue(attributeNames.length == values.length);
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < attributeNames.length; i++) {
			map.put(attributeNames[i], values[i]);
		}
		attributes.putAll(map, validate);
	}

	/* (non-Javadoc
	 * Method declared on IStringPoolParticipant
	 */
	@Override
	public void shareStrings(StringPool set) {
		type = set.add(type);
		attributes.shareStrings(set);
	}
}
