/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.extension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.dynamic.DocumentProcessor;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.eclipse.help.internal.dynamic.ValidationHandler;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Parses content extension XML files into extension model elements.
 */
public class ContentExtensionFileParser extends DefaultHandler {

	private DocumentReader reader;
	private DocumentProcessor processor;
	private Map<String, String[]> requiredAttributes;
	private Map<String, String> deprecatedElements;

	/*
	 * Parses the specified content extension XML file into model elements.
	 */
	public ContentExtension[] parse(Bundle bundle, String path) throws IOException, SAXException, ParserConfigurationException {
		if (reader == null) {
			reader = new DocumentReader();
		}
		URL url = FileLocator.find(bundle, IPath.fromOSString(path), null);
		if (url != null) {
			InputStream in = url.openStream();
			UAElement extension = reader.read(in);
			if (processor == null) {
				processor = new DocumentProcessor(new ProcessorHandler[] {
					new ValidationHandler(getRequiredAttributes(), getDeprecatedElements())
				});
			}
			processor.process(extension, '/' + bundle.getSymbolicName() + '/' + path);
			IUAElement[] children = extension.getChildren();
			ContentExtension[] result = new ContentExtension[children.length];
			System.arraycopy(children, 0, result, 0, children.length);
			return result;
		}
		else {
			throw new FileNotFoundException();
		}
	}

	private Map<String, String[]> getRequiredAttributes() {
		if (requiredAttributes == null) {
			requiredAttributes = new HashMap<>();
			requiredAttributes.put(ContentExtension.NAME_CONTRIBUTION, new String[] { ContentExtension.ATTRIBUTE_CONTENT, ContentExtension.ATTRIBUTE_PATH });
			requiredAttributes.put(ContentExtension.NAME_CONTRIBUTION_LEGACY, new String[] { ContentExtension.ATTRIBUTE_CONTENT, ContentExtension.ATTRIBUTE_PATH });
			requiredAttributes.put(ContentExtension.NAME_REPLACEMENT, new String[] { ContentExtension.ATTRIBUTE_CONTENT, ContentExtension.ATTRIBUTE_PATH });
			requiredAttributes.put(ContentExtension.NAME_REPLACEMENT_LEGACY, new String[] { ContentExtension.ATTRIBUTE_CONTENT, ContentExtension.ATTRIBUTE_PATH });
		}
		return requiredAttributes;
	}

	private Map<String, String> getDeprecatedElements() {
		if (deprecatedElements == null) {
			deprecatedElements = new HashMap<>();
			deprecatedElements.put(ContentExtension.NAME_CONTRIBUTION_LEGACY, ContentExtension.NAME_CONTRIBUTION);
			deprecatedElements.put(ContentExtension.NAME_REPLACEMENT_LEGACY, ContentExtension.NAME_REPLACEMENT);
		}
		return deprecatedElements;
	}
}
