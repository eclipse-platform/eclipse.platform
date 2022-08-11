/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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

package org.eclipse.core.internal.propertytester;

import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.*;

/**
 * A property tester for various properties of files.
 *
 * @since 3.2
 */
public class FilePropertyTester extends ResourcePropertyTester {

	/**
	 * A property indicating a content type on the selected file (value <code>"contentTypeId"</code>).
	 * <code>"kindOf"</code> indicates that the file content type should be the kind of the one given as the expected value.
	 * If <code>"kindOf"</code> is not specified, the file content type identifier should equals the expected value.
	 * @see IContentType#isKindOf(IContentType)
	 */
	private static final String CONTENT_TYPE_ID = "contentTypeId"; //$NON-NLS-1$

	/**
	 * An argument for <code>"contentTypeId"</code>.
	 * <code>"kindOf"</code> indicates that the file content type should be the kind of the one given as the expected value.
	 * If <code>"kindOf"</code> is not specified, the file content type identifier should equals the expected value.
	 * @see IContentType#isKindOf(IContentType)
	 */
	private static final String IS_KIND_OF = "kindOf"; //$NON-NLS-1$

	/**
	 * An argument for <code>"contentTypeId"</code>.
	 * Setting <code>"useFilenameOnly"</code> indicates that the file content type should be determined by the file name only.
	 * If <code>"useFilenameOnly"</code> is not specified, the file content type is determined by both, the file name and content.
	 * @see IContentTypeMatcher#findContentTypeFor(String)
	 */
	private static final String USE_FILENAME_ONLY = "useFilenameOnly"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		if ((receiver instanceof IFile) && method.equals(CONTENT_TYPE_ID))
			return testContentType((IFile) receiver, toString(expectedValue), isArgumentUsed(args, IS_KIND_OF), isArgumentUsed(args, USE_FILENAME_ONLY));
		return false;
	}

	private boolean isArgumentUsed(Object[] args, String value) {
		for (Object arg : args)
			if (value.equals(arg))
				return true;
		return false;
	}

	/**
	 * <p>
	 * Tests whether the content type for <code>file</code> matches
	 * or is a kind of <code>contentTypeId</code>.
	 * </p>
	 * <p>
	 * It is possible that this method call could
	 * cause the file to be read. It is also possible (through poor plug-in
	 * design) for this method to load plug-ins.
	 * </p>
	 *
	 * @param file
	 *            The file to test. Must not be <code>null</code>.
	 * @param contentTypeId
	 *            The content type to test. Must not be <code>null</code>.
	 * @param isKindOfUsed
	 *            Indicates whether the file content type should match <code>contentTypeId</code>
	 *            or should be a kind of <code>contentTypeId</code>.
	 * @param useFilenameOnly
	 *            Indicates to determine the file content type based on the file name only.
	 * @return <code>true</code>, if the best matching content type for <code>file</code>
	 * 		<ul>
	 *			<li>has an identifier that matches <code>contentTypeId</code>
	 *			and <code>isKindOfUsed</code> is <code>false</code>, or</li>
	 * 			<li>is a kind of <code>contentTypeId</code>
	 * 			and <code>isKindOfUsed</code> is <code>true</code>.</li>
	 * 		</ul>
	 * Otherwise it returns <code>false</code>.
	 */
	private boolean testContentType(final IFile file, String contentTypeId, boolean isKindOfUsed, boolean useFilenameOnly) {
		final String expectedValue = contentTypeId.trim();
		IContentType actualContentType = null;
		if (!useFilenameOnly) {
			if (!file.exists())
				return false;
			IContentDescription contentDescription = null;
			try {
				contentDescription = file.getContentDescription();
			} catch (CoreException e) {
				Policy.log(IStatus.ERROR, "Core exception while retrieving the content description", e);//$NON-NLS-1$
			}
			if (contentDescription != null)
				actualContentType = contentDescription.getContentType();
		} else {
			actualContentType = Platform.getContentTypeManager().findContentTypeFor(file.getName());
		}
		if (actualContentType != null) {
			if (isKindOfUsed)
				return actualContentType.isKindOf(Platform.getContentTypeManager().getContentType(expectedValue));
			return expectedValue.equals(actualContentType.getId());
		}
		return false;
	}
}
