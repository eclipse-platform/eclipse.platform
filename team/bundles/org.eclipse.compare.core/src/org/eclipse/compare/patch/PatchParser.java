/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.compare.patch;

import java.io.BufferedReader;
import java.io.IOException;

import org.eclipse.compare.internal.core.CompareSettings;
import org.eclipse.compare.internal.core.patch.PatchReader;
import org.eclipse.core.runtime.*;

/**
 * Helper class for parsing patches.
 *
 * @since org.eclipse.compare.core 3.5
 */
public class PatchParser {

	/**
	 * Parse the given patch and return the set of file patches that it
	 * contains.
	 *
	 * @param content
	 *            a patch reader creator
	 * @return the set of file patches that the patch contains
	 * @throws CoreException
	 *             if an error occurs reading the contents
	 */
	public static IFilePatch2[] parsePatch(ReaderCreator content)
			throws CoreException {
		try (BufferedReader reader = new BufferedReader(content.createReader())) {
			PatchReader patchReader = new PatchReader();
			patchReader.parse(reader);
			return patchReader.getAdjustedDiffs();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					CompareSettings.PLUGIN_ID, 0, e.getMessage(), e));
		}
	}
}
