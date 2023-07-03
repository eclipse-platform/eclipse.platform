/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.dynamic;

import org.eclipse.help.internal.Include;
import org.eclipse.help.internal.UAElement;

/*
 * The handler responsible for processing includes, where a node is pulled
 * in from another document.
 */
public class IncludeHandler extends ProcessorHandler {

	private IncludeResolver resolver;
	private DocumentReader reader;
	private String locale;

	/*
	 * Creates the handler. It needs to know which locale the current document
	 * is in in order to pull content from the correct locale.
	 */
	public IncludeHandler(DocumentReader reader, String locale) {
		this.reader = reader;
		this.locale = locale;
	}

	@Override
	public short handle(UAElement element, String id) {
		if (element instanceof Include) {
			String path = ((Include)element).getPath();
			if (path != null && path.length() > 0) {
				String bundleId = getBundleId(path);
				String relativePath = getRelativePath(path);
				String elementId = getElementId(path);
				if (bundleId != null && relativePath != null && elementId != null) {
					resolveInclude(bundleId, relativePath, elementId, element, locale);
				}
			}
			else {
				// remove invalid includes
				element.getParentElement().removeChild(element);
			}
			return HANDLED_SKIP;
		}
		return UNHANDLED;
	}

	/*
	 * Processes the include; replaces the element with the one described by
	 * the parameters.
	 */
	private void resolveInclude(String bundleId, String relativePath, String elementId, UAElement element, String locale) {
		if (resolver == null) {
			resolver = new IncludeResolver(getProcessor(), reader, locale);
		}
		UAElement parent = element.getParentElement();
		if (parent != null) {
			try {
				UAElement nodeToInclude = resolver.resolve(bundleId, relativePath, elementId);
				parent.insertBefore(nodeToInclude, element);
				parent.removeChild(element);
			}
			catch (Throwable t) {
				// remove invalid includes
				parent.removeChild(element);
			}
		}
	}

	/*
	 * Extracts the bundle ID from the given path.
	 */
	private String getBundleId(String path) {
		if (path.charAt(0) == '/') {
			int index = path.indexOf('/', 1);
			if (index > 1) {
				return path.substring(1, index);
			}
		}
		else {
			// legacy - handle no slash at beginning
			int index = path.indexOf('/');
			if (index != -1) {
				return path.substring(0, index);
			}
		}
		return null;
	}

	/*
	 * Extracts the bundle-relative path from the given full path.
	 */
	private String getRelativePath(String path) {
		int startIndex = path.indexOf('/', 1);
		int endIndex = path.indexOf('#');
		if (endIndex == -1) {
			// legacy - can use slash in place of '#'
			endIndex = path.lastIndexOf('/');
		}
		if (startIndex != -1 && endIndex > startIndex + 1) {
			return path.substring(startIndex + 1, endIndex);
		}
		return null;
	}

	/*
	 * Extracts the element id from the given path.
	 */
	private String getElementId(String path) {
		int index = path.indexOf('#');
		if (index == -1) {
			// legacy - can use slash in place of '#'
			index = path.lastIndexOf('/');
		}
		if (index != -1 && index < path.length() - 1) {
			return path.substring(index + 1);
		}
		return null;
	}
}
