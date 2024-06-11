/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.internal.propertytester;

import org.eclipse.core.resources.IFile;

/**
 * This class is a property tester for determining whether a given resource is a
 * zip file.
 */
public class ZipFilePropertyTester extends ResourcePropertyTester {

	private static final String PROPERTY_IS_ZIP_FILE = "zipFile"; //$NON-NLS-1$

	/** Enum representing allowed file extensions for zip files. */
	private enum ZipFileExtensions {
		ZIP("zip"); //$NON-NLS-1$

		private final String value;

		ZipFileExtensions(String value) {
			this.value = value;
		}
	}

	/**
	 * Tests whether the given receiver object satisfies the condition specified by
	 * the given property.
	 *
	 * @param receiver      The object being tested.
	 * @param property      The name of the property to test.
	 * @param args          Additional arguments to evaluate the property. Not used
	 *                      in this implementation.
	 * @param expectedValue The expected value of the property. Not used in this
	 *                      implementation.
	 * @return true if the given file is a zip file and is not a linked resource;
	 *         false otherwise.
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IFile))
			return false;

		IFile file = (IFile) receiver;

		if (property.equals(PROPERTY_IS_ZIP_FILE)) {
			String fileExtension = file.getFileExtension();
			boolean isZipFile = false;

			for (ZipFileExtensions allowedExtension : ZipFileExtensions.values()) {
				if (fileExtension.equals(allowedExtension.value)) {
					isZipFile = true;
					break;
				}
			}

			if (!file.isLinked() && isZipFile)
				return true;
		}

		return false;
	}
}
