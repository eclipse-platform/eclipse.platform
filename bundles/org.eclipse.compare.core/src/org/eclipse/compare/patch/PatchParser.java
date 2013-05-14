/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.patch;

import java.io.BufferedReader;
import java.io.IOException;

import org.eclipse.compare.internal.core.ComparePlugin;
import org.eclipse.compare.internal.core.patch.PatchReader;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

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
		BufferedReader reader = new BufferedReader(content.createReader());
		try {
			PatchReader patchReader = new PatchReader();
			patchReader.parse(reader);
			return patchReader.getAdjustedDiffs();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					ComparePlugin.PLUGIN_ID, 0, e.getMessage(), e));
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignored
			}
		}
	}
}
