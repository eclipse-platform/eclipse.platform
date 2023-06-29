/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IPathEditorInput;

/**
 * ResourceExtender provides property testers for the XML expression language
 * evaluation. We provide a copy in Debug so that launch shortcuts can add
 * contextual launch enablement that does not require their plugins to be
 * loaded. Copied from
 * org.eclipse.jdt.internal.corext.refactoring.participants.xml.ResourceExtender
 */
public class ResourceExtender extends PropertyTester {

	private static final String PROPERTY_MATCHES_PATTERN = "matchesPattern"; //$NON-NLS-1$

	private static final String PROJECT_NATURE = "projectNature"; //$NON-NLS-1$

	private static final String PROPERTY_MATCHES_CONTENT_TYPE = "matchesContentType"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IResource resource = ((IAdaptable) receiver).getAdapter(IResource.class);
		if (resource == null) {
			if (PROPERTY_MATCHES_CONTENT_TYPE.equals(method)) {
				IPathEditorInput editorInput = ((IAdaptable) receiver).getAdapter(IPathEditorInput.class);
				if (editorInput != null) {
					IPath path= editorInput.getPath();
					File file= path.toFile();
					if (file.exists()) {
						try (FileReader reader = new FileReader(file)) {
							IContentType contentType= Platform.getContentTypeManager().getContentType((String)expectedValue);
							IContentDescription description= contentType.getDescriptionFor(reader, IContentDescription.ALL);
							if (description != null) {
								return matchesContentType(description.getContentType(), (String)expectedValue);
							}
						} catch (FileNotFoundException e) {
							return false;
						} catch (IOException e) {
							return false;
						}
					}
				}
			}
		} else {
			if (PROPERTY_MATCHES_PATTERN.equals(method)) {
				String fileName = resource.getName();
				String expected = (String) expectedValue;
				expected = expected.replaceAll("\\.", "\\\\.");  //$NON-NLS-1$//$NON-NLS-2$
				expected = expected.replaceAll("\\*", "\\.\\*");  //$NON-NLS-1$//$NON-NLS-2$
				Pattern pattern = Pattern.compile(expected);
				boolean retVal = pattern.matcher(fileName).find();
				return retVal;
			} else if (PROJECT_NATURE.equals(method)) {
				try {
					IProject proj = resource.getProject();
					return proj != null && proj.isAccessible() && proj.hasNature((String) expectedValue);
				} catch (CoreException e) {
					return false;
				}
			} else if (PROPERTY_MATCHES_CONTENT_TYPE.equals(method)) {
				return matchesContentType(resource, (String) expectedValue);
			}
		}
		return false;
	}

	/**
	 * Returns whether the given type or one of its base types matches the
	 * given content type identifier.
	 *
	 * @param type content type or <code>null</code>
	 * @param typeId content type identifier
	 * @return
	 */
	private boolean matchesContentType(IContentType type, String typeId) {
		while (type != null) {
			if (typeId.equals(type.getId())) {
				return true;
			}
			type = type.getBaseType();
		}
		return false;
	}

	/**
	 * Returns whether or not the given file's content type matches the
	 * specified content type.
	 *
	 * Content types are looked up in the content type registry.
	 *
	 * @return whether or not the given resource has the given content type
	 */
	private boolean matchesContentType(IResource resource, String contentType) {
		if (resource == null || !(resource instanceof IFile) || !resource.exists()) {
			return false;
		}
		IFile file = (IFile) resource;
		IContentDescription description;
		try {
			description = file.getContentDescription();
		} catch (CoreException e) {
			return false;
		}
		if (description != null) {
			return matchesContentType(description.getContentType(), contentType);
		}
		return false;
	}

}
