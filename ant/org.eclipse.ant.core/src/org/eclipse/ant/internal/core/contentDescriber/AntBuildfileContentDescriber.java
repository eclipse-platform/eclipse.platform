/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
 *     Philippe Ombredanne (pombredanne@nexb.com) - bug 125367
 *******************************************************************************/
package org.eclipse.ant.internal.core.contentDescriber;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.XMLContentDescriber;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A content describer for Ant buildfiles.
 * <p>
 * If project top level element is found then if: target sub-elements are found returns VALID default attribute is found returns VALID some other
 * likely Ant element is found (classpath, import, macrodef, path, property, taskdef, typedef) returns VALID else: returns INDETERMINATE else returns
 * INDETERMINATE
 * </p>
 *
 * @since 3.1
 */
public final class AntBuildfileContentDescriber extends XMLContentDescriber implements IExecutableExtension {

	/*
	 * (Intentionally not included in javadoc) Determines the validation status for the given contents.
	 *
	 * @param contents the contents to be evaluated
	 *
	 * @return one of the following:<ul> <li><code>VALID</code></li>, <li><code>INVALID</code></li>, <li><code>INDETERMINATE</code></li> </ul>
	 */
	private int checkCriteria(InputSource contents) throws IOException {
		AntHandler antHandler = new AntHandler();
		try {
			if (!antHandler.parseContents(contents)) {
				return INDETERMINATE;
			}
		}
		catch (SAXException e) {
			// we may be handed any kind of contents... it is normal we fail to parse
			// so we must not log any error here.
			return INDETERMINATE;
		}
		catch (ParserConfigurationException e) {
			// some bad thing happened - force this describer to be disabled
			String message = "Internal Error: XML parser configuration error during content description for Ant buildfiles"; //$NON-NLS-1$
			throw new RuntimeException(message, e);
		}
		// Check to see if we matched our criteria.
		if (antHandler.hasRootProjectElement()) {
			if (antHandler.hasProjectDefaultAttribute() || antHandler.hasTargetElement() || antHandler.hasAntElement()) {
				// project and default attribute or project and target element(s)
				// or project and top level ant element(s) (classpath, import, macrodef, path, property, taskdef, typedef)
				return VALID;
			}
			// only a top level project element...maybe an Ant buildfile
			return INDETERMINATE;
		}

		return INDETERMINATE;
	}

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		// call the basic XML describer to do basic recognition
		if (super.describe(contents, description) == INVALID) {
			return INVALID;
		}
		// super.describe will have consumed some chars, need to rewind
		contents.reset();
		// Check to see if we matched our criteria.
		return checkCriteria(new InputSource(contents));
	}

	@Override
	public int describe(Reader contents, IContentDescription description) throws IOException {
		// call the basic XML describer to do basic recognition
		if (super.describe(contents, description) == INVALID) {
			return INVALID;
		}
		// super.describe will have consumed some chars, need to rewind
		contents.reset();
		// Check to see if we matched our criteria.
		return checkCriteria(new InputSource(contents));
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		// do nothing
	}
}