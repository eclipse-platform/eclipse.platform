/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.patch;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A representation of a file patch that can be applied to an input stream.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since org.eclipse.compare.core 3.5
 */
public interface IFilePatch2 {

	/**
	 * Special constant that will be returned from get getBeforeDate() or
	 * getAfterDate() if the date is unknown. Equal to Midnight, Jan 1, 1970
	 * GMT.
	 */
	public static long DATE_UNKNOWN = 0;

	/**
	 * Return the target path for this patch. The target path may differ
	 * depending on whether the patch is being reversed or not.
	 * 
	 * @param configuration
	 *            the patch configuration
	 * @return the target path for this patch
	 * @see PatchConfiguration#isReversed()
	 */
	public IPath getTargetPath(PatchConfiguration configuration);

	/**
	 * Apply this patch to the given contents. The result provides the
	 * original and patch contents and also indicates whether some portions of
	 * the patch (called hunks) failed to apply.
	 * 
	 * @param content
	 *            the contents
	 * @param configuration
	 *            the patch configuration
	 * @param monitor
	 *            a progress monitor
	 * @return the result of the patch application
	 */
	public IFilePatchResult apply(ReaderCreator content,
			PatchConfiguration configuration, IProgressMonitor monitor);

	/**
	 * Return the header information of the patch or <code>null</code> if there
	 * was no header text. The header may be multi-line.
	 * 
	 * @return the header information of the patch or <code>null</code>
	 */
	public String getHeader();

	/**
	 * Returns the milliseconds time value of the before date from the patch, or
	 * DATE_UNKNOWN if the date is unknown.
	 * 
	 * @return milliseconds time value of the before date from the patch
	 */
	public long getBeforeDate();

	/**
	 * Returns the milliseconds time value of the after date from the patch, or
	 * DATE_UNKNOWN if the date is unknown.
	 * 
	 * @return milliseconds time value of the after date from the patch
	 */
	public long getAfterDate();

	/**
	 * Returns all the hunks this file patch contains.
	 * 
	 * @return array of hunks
	 */
	public IHunk[] getHunks();

}
