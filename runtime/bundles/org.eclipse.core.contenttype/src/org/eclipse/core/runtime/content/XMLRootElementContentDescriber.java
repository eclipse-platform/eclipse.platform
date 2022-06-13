/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.content.ContentMessages;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.InputSource;

/**
 * A content describer for detecting the name of the top-level element or the
 * DTD system identifier in an XML file.
 * <p>
 * This executable extension supports two parameters:
 * "dtd" and "element".
 * At least one of them <strong>must</strong> be provided.  If the
 * <code>":-"</code> method is used, then the value is treated as
 * "element".
 * </p>
 * <p>
 * This class is not intended to be subclassed or instantiated by clients,
 * only to be referenced by the "describer" configuration element in
 * extensions to the <code>org.eclipse.core.runtime.contentTypes</code>
 * extension point.
 * </p>
 *
 * @since 3.0
 * @deprecated Use {@link XMLRootElementContentDescriber2} instead
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@Deprecated
public final class XMLRootElementContentDescriber extends XMLContentDescriber implements IExecutableExtension {
	private static final String DTD_TO_FIND = "dtd"; //$NON-NLS-1$
	private static final String ELEMENT_TO_FIND = "element"; //$NON-NLS-1$
	/* (Intentionally not included in javadoc)
	 * The system identifier that we wish to find. This value will be
	 * initialized by the <code>setInitializationData</code> method. If no
	 * value is provided, then this means that we don't care what the system
	 * identifier will be.
	 */
	private String dtdToFind = null;
	/* (Intentionally not included in javadoc)
	 * The top-level element we are looking for. This value will be initialized
	 * by the <code>setInitializationData</code> method. If no value is
	 * provided, then this means that we don't care what the top-level element
	 * will be.
	 */
	private String elementToFind = null;

	/* (Intentionally not included in javadoc)
	 * Determines the validation status for the given contents.
	 *
	 * @param contents the contents to be evaluated
	 * @return one of the following:<ul>
	 * <li><code>VALID</code></li>,
	 * <li><code>INVALID</code></li>,
	 * <li><code>INDETERMINATE</code></li>
	 * </ul>
	 * @throws IOException
	 */
	private int checkCriteria(InputSource contents, Map<String, Object> properties) throws IOException {
		if (!XMLRootElementContentDescriber2.isProcessed(properties))
			XMLRootElementContentDescriber2.fillContentProperties(contents, properties);
		return checkCriteria(properties);
	}

	private int checkCriteria(Map<String, Object> properties) throws IOException {
		Boolean result = (Boolean) properties.get(XMLRootElementContentDescriber2.RESULT);
		if (!result.booleanValue())
			return INDETERMINATE;
		// Check to see if we matched our criteria.
		if ((dtdToFind != null) && (!dtdToFind.equals(properties.get(XMLRootElementContentDescriber2.DTD))))
			return INDETERMINATE;
		if ((elementToFind != null) && (!elementToFind.equals(properties.get(XMLRootElementContentDescriber2.ELEMENT))))
			return INDETERMINATE;
		// We must be okay then.
		return VALID;
	}

	/* (Intentionally not included in javadoc)
	 * @see IContentDescriber#describe(InputStream, IContentDescription)
	 */
	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		return describe(contents, description, new HashMap<>());
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public int describe(InputStream contents, IContentDescription description, Map<String, Object> properties) throws IOException {
		// call the basic XML describer to do basic recognition
		if (super.describe2(contents, description, properties) == INVALID)
			return INVALID;
		// super.describe will have consumed some chars, need to rewind
		contents.reset();
		// Check to see if we matched our criteria.
		return checkCriteria(new InputSource(contents), properties);
	}

	/* (Intentionally not included in javadoc)
	 * @see IContentDescriber#describe(Reader, IContentDescription)
	 */
	@Override
	public int describe(Reader contents, IContentDescription description) throws IOException {
		return describe(contents, description, new HashMap<>());
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public int describe(Reader contents, IContentDescription description, Map<String, Object> properties) throws IOException {
		// call the basic XML describer to do basic recognition
		if (super.describe2(contents, description, properties) == INVALID)
			return INVALID;
		// super.describe will have consumed some chars, need to rewind
		contents.reset();
		// Check to see if we matched our criteria.
		return checkCriteria(new InputSource(contents), properties);
	}

	/* (Intentionally not included in javadoc)
	 * @see IExecutableExtension#setInitializationData
	 */
	@Override
	public void setInitializationData(final IConfigurationElement config, final String propertyName, final Object data) throws CoreException {
		if (data instanceof String)
			elementToFind = (String) data;
		else if (data instanceof Hashtable) {
			Hashtable<?, ?> parameters = (Hashtable<?, ?>) data;
			dtdToFind = (String) parameters.get(DTD_TO_FIND);
			elementToFind = (String) parameters.get(ELEMENT_TO_FIND);
		}
		if (dtdToFind == null && elementToFind == null) {
			String message = NLS.bind(ContentMessages.content_badInitializationData, XMLRootElementContentDescriber.class.getName());
			throw new CoreException(new Status(IStatus.ERROR, ContentMessages.OWNER_NAME, 0, message, null));
		}
	}
}
