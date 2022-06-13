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
package org.eclipse.core.runtime.content;

import org.eclipse.core.internal.content.IContentConstants;
import org.eclipse.core.runtime.QualifiedName;

/**
 * A content description object contains information about the nature of
 * arbitrary data.
 * <p>
 * A content description object will always include the content type for the
 * examined contents, and may also include information on:
 * </p>
 * <ol>
 * <li>charset;</li>
 * <li>byte order mark;</li>
 * <li>other custom properties provided by third-party plug-ins.</li>
 * </ol>
 * <p>
 * <cite>Content describers</cite> provided by plug-ins will fill in most of the
 * properties in a content description object, except for the content type,
 * what is done by the platform. After a content
 * description is filled in by a content interpreter, it is marked as immutable
 * by the platform, so calling any of the mutator methods defined in this
 * interface will cause an <code>IllegalStateException</code> to be thrown.
 * </p>
 * <p>
 * Default values for properties can be contributed by plug-ins as part of
 * the content type definition markup.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see IContentDescriber
 * @since 3.0
 */
public interface IContentDescription {
	/**
	 * Key for the "charset" property.
	 */
	QualifiedName CHARSET = new QualifiedName(IContentConstants.RUNTIME_NAME, "charset"); //$NON-NLS-1$
	/**
	 * Key for the "byte order mark" property. This property is only meaningful
	 * when describing byte streams.
	 */
	QualifiedName BYTE_ORDER_MARK = new QualifiedName(IContentConstants.RUNTIME_NAME, "bom"); //$NON-NLS-1$
	/**
	 * Options constant meaning that all properties should be described.
	 */
	QualifiedName[] ALL = null;
	/**
	 * Constant that identifies the Byte-Order-Mark for contents encoded with
	 * the UTF-8 character encoding scheme.
	 */
	byte[] BOM_UTF_8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
	/**
	 * Constant that identifies the Byte-Order-Mark for contents encoded with
	 * the UTF-16 Big Endian character encoding scheme.
	 */
	byte[] BOM_UTF_16BE = {(byte) 0xFE, (byte) 0xFF};
	/**
	 * Constant that identifies the Byte-Order-Mark for contents encoded with
	 * the UTF-16 Little Endian character encoding scheme.
	 */
	byte[] BOM_UTF_16LE = {(byte) 0xFF, (byte) 0xFE};

	/**
	 * Returns whether the given property is requested to be described. This
	 * method is intended to allow content describers to determine  which
	 * properties should be described.
	 *
	 * @param key a key for the property to be verified
	 * @return <code>true</code> if the property is to be described,
	 * <code>false</code> otherwise
	 */
	boolean isRequested(QualifiedName key);

	/**
	 * Returns the charset name to be used when reading the contents
	 * described by this object.
	 * <p>
	 * If a Unicode byte order mark has been found (the
	 * <code>BYTE_ORDER_MARK</code> property has been set),
	 * a corresponding charset name will be returned (e.g. "UTF-8",
	 * "UTF-16"). Otherwise, the value of the <code>CHARSET</code>
	 * property will be returned.
	 * </p>
	 * @return a charset name, or <code>null</code>
	 */
	String getCharset();

	/**
	 * Returns the content type detected. Returns <code>null</code> if the
	 * content type could not be determined.
	 *
	 * @return the corresponding content type, or <code>null</code>
	 */
	IContentType getContentType();

	/**
	 * Returns the value of custom property set by the content describer,
	 * or the default value for the property, if one has been defined.
	 * <p>
	 * The qualifier part of the property name must be the unique identifier
	 * of the declaring plug-in (e.g. <code>"com.example.plugin"</code>).
	 * </p>
	 *
	 * @param key the property key
	 * @return the property value, or <code>null</code>, if the property is not
	 * found
	 */
	Object getProperty(QualifiedName key);

	/**
	 * Sets the given property to the given value.
	 * <p>
	 * The qualifier part of the property name must be the unique identifier
	 * of the declaring plug-in (e.g. <code>"com.example.plugin"</code>).
	 * </p>
	 * <p>
	 * This method should not be called by clients other than content
	 * describers. An attempt to set a property from other contexts will cause
	 * an <code>IllegalStateException</code> to be thrown.
	 * </p>
	 *
	 * @param key the qualified name of the property
	 * @param value the property value, or <code>null</code>,
	 * if the property is to be removed
	 * @throws IllegalStateException if called after this description has been
	 * filled in
	 */
	void setProperty(QualifiedName key, Object value);
}
