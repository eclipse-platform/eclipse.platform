/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;

public final class ContentDescription extends BasicDescription {

	private static final String CHARSET_UTF_16 = "UTF-16"; //$NON-NLS-1$
	private static final String CHARSET_UTF_8 = "UTF-8"; //$NON-NLS-1$
	private static final byte FLAG_ALL_OPTIONS = 0x01;
	private static final byte FLAG_IMMUTABLE = 0x02;
	private byte flags;
	private Object keys;
	private Object values;

	public ContentDescription(QualifiedName[] requested, IContentTypeInfo contentTypeInfo) {
		super(contentTypeInfo);
		if (requested == IContentDescription.ALL) {
			flags |= FLAG_ALL_OPTIONS;
			return;
		}
		if (requested.length > 1) {
			keys = requested;
			values = new Object[requested.length];
		} else if (requested.length == 1)
			keys = requested[0];
		// if requested.length == 0 then keys == null (no options to describe!)
	}

	private void assertMutable() {
		if ((flags & FLAG_IMMUTABLE) != 0)
			throw new IllegalStateException("Content description is immutable"); //$NON-NLS-1$
	}

	@Override
	public String getCharset() {
		byte[] bom = (byte[]) getProperty(BYTE_ORDER_MARK);
		if (bom == BOM_UTF_8)
			return CHARSET_UTF_8;
		else if (bom == BOM_UTF_16BE || bom == BOM_UTF_16LE)
			// UTF-16 will properly recognize the BOM
			return CHARSET_UTF_16;
		return (String) getProperty(CHARSET);
	}

	private Object getDescribedProperty(QualifiedName key) {
		// no values have been set
		if (values == null)
			return null;
		// a single property may have been set
		if (keys instanceof QualifiedName)
			return keys.equals(key) ? values : null;
		// multiple properties may have been set
		QualifiedName[] tmpKeys = (QualifiedName[]) this.keys;
		for (int i = 0; i < tmpKeys.length; i++)
			if (tmpKeys[i].equals(key))
				return ((Object[]) values)[i];
		return null;
	}

	@Override
	public Object getProperty(QualifiedName key) {
		Object describedProperty = getDescribedProperty(key);
		if (describedProperty != null)
			return describedProperty;
		return contentTypeInfo.getDefaultProperty(key);
	}

	@Override
	public boolean isRequested(QualifiedName propertyKey) {
		// all options requested
		if ((flags & FLAG_ALL_OPTIONS) != 0)
			return true;
		// no options requested
		if (keys == null)
			return false;
		// a single option requested
		if (keys instanceof QualifiedName)
			return keys.equals(propertyKey);
		// some (but not all) options requested
		QualifiedName[] tmpKeys = (QualifiedName[]) this.keys;
		for (QualifiedName tmpKey : tmpKeys)
			if (tmpKey.equals(propertyKey))
				return true;
		return false;
	}

	boolean isSet() {
		if (keys == null || values == null)
			return false;
		if (keys instanceof QualifiedName)
			return true;
		Object[] tmpValues = (Object[]) this.values;
		for (Object tmpValue : tmpValues)
			if (tmpValue != null)
				return true;
		return false;
	}

	public void markImmutable() {
		assertMutable();
		flags |= FLAG_IMMUTABLE;
	}

	/** Overrides content type info object. */
	void setContentTypeInfo(IContentTypeInfo info) {
		this.contentTypeInfo = info;
	}

	@Override
	public void setProperty(QualifiedName newKey, Object newValue) {
		assertMutable();
		if (keys == null) {
			if ((flags & FLAG_ALL_OPTIONS) != 0) {
				keys = newKey;
				values = newValue;
			}
			return;
		}
		if (keys.equals(newKey)) {
			values = newValue;
			return;
		}
		if (keys instanceof QualifiedName) {
			if ((flags & FLAG_ALL_OPTIONS) != 0) {
				keys = new QualifiedName[] {(QualifiedName) keys, newKey};
				values = new Object[] {values, newValue};
			}
			return;
		}
		QualifiedName[] tmpKeys = (QualifiedName[]) this.keys;
		for (int i = 0; i < tmpKeys.length; i++)
			if (tmpKeys[i].equals(newKey)) {
				((Object[]) values)[i] = newValue;
				return;
			}
		if ((flags & FLAG_ALL_OPTIONS) == 0)
			return;
		// need to resize arrays
		int currentSize = tmpKeys.length;
		tmpKeys = new QualifiedName[currentSize + 1];
		System.arraycopy(keys, 0, tmpKeys, 0, currentSize);
		Object[] tmpValues = new Object[currentSize + 1];
		System.arraycopy(values, 0, tmpValues, 0, currentSize);
		tmpKeys[tmpKeys.length - 1] = newKey;
		tmpValues[tmpValues.length - 1] = newValue;
		keys = tmpKeys;
		values = tmpValues;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("{"); //$NON-NLS-1$
		if (keys != null)
			if (keys instanceof QualifiedName) {
				if (values != null)
					result.append(keys + "=" + values); //$NON-NLS-1$
			} else {
				QualifiedName[] tmpKeys = (QualifiedName[]) keys;
				Object[] tmpValues = (Object[]) values;
				boolean any = false;
				for (int i = 0; i < tmpKeys.length; i++)
					if (tmpValues[i] != null) {
						result.append(tmpKeys[i] + "=" + tmpValues[i] + ","); //$NON-NLS-1$ //$NON-NLS-2$
						any = true;
					}
				if (any)
					result.deleteCharAt(result.length() - 1);
			}
		result.append("} : "); //$NON-NLS-1$
		result.append(contentTypeInfo.getContentType());
		return result.toString();
	}
}
