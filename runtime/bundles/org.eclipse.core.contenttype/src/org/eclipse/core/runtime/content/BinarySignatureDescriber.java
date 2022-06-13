/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.eclipse.core.internal.content.ContentMessages;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * A content describer for binary formats that present some
 * simple signature at a known, fixed offset.
 * <p>
 * This executable extension supports three parameters:
 * "signature", "offset" and "required", the first one being mandatory.
 * If the
 * <code>":-"</code> method is used, then the value is treated as the
 * "signature".
 * </p>
 * <p>
 * The "signature" parameter is a sequence of hex codes, one for each byte in
 * the signature. For example, "CA FE BA BE" would be a signature for Java
 * class files.
 * </p>
 * <p>
 * The "offset" parameter is an integer indicating the offset where the
 * signature's first byte is found.
 * </p>
 * <p>
 * The "required" parameter is a boolean (default is " true") indicating whether
 * the absence of a signature should deem the contents validity status as
 * IContentDescriber.INVALID or IContentDescriber.INDETERMINATE.
 * </p>
 * <p>
 * This class is not intended to be subclassed or instantiated by clients,
 * only to be referenced by the "describer" configuration element in
 * extensions to the <code>org.eclipse.core.runtime.contentTypes</code>
 * extension point.
 * </p>
 *
 * @since 3.0
 */
public final class BinarySignatureDescriber implements IContentDescriber, IExecutableExtension {
	private final static String SIGNATURE = "signature"; //$NON-NLS-1$
	private final static String OFFSET = "offset"; //$NON-NLS-1$
	private static final Object REQUIRED = "required"; //$NON-NLS-1$
	private byte[] signature;
	private int offset;
	private boolean required = true;

	/* (Intentionally not included in javadoc)
	 * @see IContentDescriber#describe(InputStream, IContentDescription)
	 */
	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		byte[] buffer = new byte[signature.length];
		int notValid = required ? INVALID : INDETERMINATE;
		if (contents.skip(offset) < offset)
			return notValid;
		if (contents.read(buffer) != buffer.length)
			return notValid;
		for (int i = 0; i < signature.length; i++)
			if (signature[i] != buffer[i])
				return notValid;
		return VALID;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return new QualifiedName[0];
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		try {
			if (data instanceof String)
				signature = parseSignature((String) data);
			else if (data instanceof Hashtable) {
				Hashtable<?,?> parameters = (Hashtable<?,?>) data;
				if (!parameters.containsKey(SIGNATURE)) {
					String message = NLS.bind(ContentMessages.content_badInitializationData, BinarySignatureDescriber.class.getName());
					throw new CoreException(new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, null));
				}
				signature = parseSignature((String) parameters.get(SIGNATURE));
				if (parameters.containsKey(OFFSET))
					offset = Integer.parseInt((String) parameters.get(OFFSET));
				if (parameters.containsKey(REQUIRED))
					required = Boolean.parseBoolean((String) parameters.get(REQUIRED));
			}
		} catch (NumberFormatException nfe) {
			String message = NLS.bind(ContentMessages.content_badInitializationData, BinarySignatureDescriber.class.getName());
			throw new CoreException(new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, nfe));
		}
	}

	private static byte[] parseSignature(String data) {
		List<Byte> bytes = new ArrayList<>();
		StringTokenizer tokenizer = new StringTokenizer(data, " \t\n\r\f,"); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens())
			bytes.add(Byte.valueOf((byte) Integer.parseInt(tokenizer.nextToken().trim(), 16)));
		byte[] signature = new byte[bytes.size()];
		for (int i = 0; i < signature.length; i++)
			signature[i] = bytes.get(i).byteValue();
		return signature;
	}
}
