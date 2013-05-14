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

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.core.runtime.IPath;

/**
 * Builder for creating IFilePatch2 and IHunk objects as well as building
 * relationship between them.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * 
 * @since org.eclipse.compare.core 3.5
 */
public class PatchBuilder {

	/**
	 * Line prefix used to mark context lines.
	 */
	public static final char CONTEXT_PREFIX = ' ';
	/**
	 * Line prefix used to mark an added lines.
	 */
	public static final char ADDITION_PREFIX = '+';
	/**
	 * Line prefix used to mark an removed lines.
	 */
	public static final char REMOVAL_PREFIX = '-';

	/**
	 * Creates an IHunk instance.
	 * 
	 * @param start
	 *            the start position in the before file
	 * @param lines
	 *            content of the hunk. Each line starts with a control
	 *            character. Their meaning is as follows:
	 *            <ul>
	 *            <li>
	 *            '+': add the line
	 *            <li>
	 *            '-': delete the line
	 *            <li>
	 *            ' ': no change, context line
	 *            </ul>
	 * @return IHunk instance
	 */
	public static IHunk createHunk(int start, String[] lines) {
		int type = getHunkType(lines);
		int oldLength = getHunkLength(lines, true);
		int newLength = getHunkLength(lines, false);
		return new Hunk(null, type, start, oldLength, start, newLength, lines);
	}

	/**
	 * Creates an IFilePatch2 instance and performs recalculation of all hunks'
	 * after positions. Hunk's after position is position in the file state
	 * after applying a patch. It is affected by all the hunks that are to be
	 * applied before a given one. This recalculation is necessary to keep
	 * IFilePatch2's state coherent.
	 * 
	 * @param oldPath
	 *            the path of the before state of the file
	 * @param oldDate
	 *            the timestamp of the before state of the file, see also
	 *            {@link IFilePatch2#DATE_UNKNOWN}
	 * @param newPath
	 *            the path of the after state of the file
	 * @param newDate
	 *            the timestamp of the after state of the file, see also
	 *            {@link IFilePatch2#DATE_UNKNOWN}
	 * @param hunks
	 *            a set of hunks to insert into IFilePatch2
	 * @return IFilePatch2 instance
	 */
	public static IFilePatch2 createFilePatch(IPath oldPath, long oldDate,
			IPath newPath, long newDate, IHunk[] hunks) {
		reorder(hunks);
		FilePatch2 fileDiff = new FilePatch2(oldPath, oldDate, newPath, newDate);
		for (int i = 0; i < hunks.length; i++) {
			fileDiff.add((Hunk) hunks[i]);
		}
		return fileDiff;
	}

	/**
	 * Adds IHunks to a given IFilePatch2 and performs recalculation of all
	 * hunks' after positions. Hunk's after position is position in the file
	 * state after applying a patch. It is affected by all the hunks that are to
	 * be applied before a given one. This recalculation is necessary to keep
	 * IFilePatch2's state coherent.
	 * 
	 * @param filePatch
	 *            a file patch to add hunks to
	 * @param toAdd
	 *            a set of IHunks to add
	 * @return newly created file patch with added hunks
	 */
	public static IFilePatch2 addHunks(IFilePatch2 filePatch, IHunk[] toAdd) {
		IHunk[] result = addHunks(filePatch.getHunks(), toAdd);
		reorder(result);
		return createFilePatch(filePatch, result);
	}

	/**
	 * Removes IHunks from a given IFilePatch2 and performs recalculation of all
	 * hunks' after positions. Hunk's after position is position in the file
	 * state after applying a patch. It is affected by all the hunks that are to
	 * be applied before a given one. This recalculation is necessary to keep
	 * IFilePatch2's state coherent.
	 * 
	 * @param filePatch
	 *            a file patch to add hunks to
	 * @param toRemove
	 *            a set of IHunks to add
	 * @return newly created file patch with removed hunks
	 */
	public static IFilePatch2 removeHunks(IFilePatch2 filePatch,
			IHunk[] toRemove) {
		IHunk[] result = removeHunks(filePatch.getHunks(), toRemove);
		reorder(result);
		return createFilePatch(filePatch, result);
	}

	private static IFilePatch2 createFilePatch(IFilePatch2 filePatch,
			IHunk[] hunks) {
		PatchConfiguration config = new PatchConfiguration();
		IPath beforePath = filePatch.getTargetPath(config);
		config.setReversed(true);
		IPath afterPath = filePatch.getTargetPath(config);
		return createFilePatch(beforePath, filePatch.getBeforeDate(),
				afterPath, filePatch.getAfterDate(), hunks);
	}

	private static int getHunkType(String[] lines) {
		boolean hasContextLines = checkForPrefix(CONTEXT_PREFIX, lines);
		if (!hasContextLines) {
			boolean hasLineAdditions = checkForPrefix(ADDITION_PREFIX, lines);
			boolean hasLineDeletions = checkForPrefix(REMOVAL_PREFIX, lines);
			if (hasLineAdditions && !hasLineDeletions) {
				return FilePatch2.ADDITION;
			} else if (!hasLineAdditions && hasLineDeletions) {
				return FilePatch2.DELETION;
			}
		}
		return FilePatch2.CHANGE;
	}

	private static int getHunkLength(String[] lines, boolean old) {
		int length = 0;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].length() > 0) {
				switch (lines[i].charAt(0)) {
				case ' ':
					length++;
					break;
				case '+':
					if (!old) {
						length++;
					}
					break;
				case '-':
					if (old) {
						length++;
					}
					break;
				default:
					throw new IllegalArgumentException(""); //$NON-NLS-1$
				}
			}
		}
		return length;
	}

	private static boolean checkForPrefix(char prefix, String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].length() > 0) {
				if (lines[i].charAt(0) == prefix) {
					return true;
				}
			}
		}
		return false;
	}

	private static IHunk[] addHunks(IHunk[] hunks, IHunk[] toAdd) {
		IHunk[] ret = new IHunk[hunks.length + toAdd.length];
		System.arraycopy(hunks, 0, ret, 0, hunks.length);
		System.arraycopy(toAdd, 0, ret, hunks.length, toAdd.length);
		return ret;
	}

	private static IHunk[] removeHunks(IHunk[] hunks, IHunk[] toRemove) {
		int removed = 0;
		for (int i = 0; i < hunks.length; i++) {
			for (int j = 0; j < toRemove.length; j++) {
				if (toRemove[j] == hunks[i]) {
					hunks[i] = null;
					removed++;
				}
			}
		}
		IHunk[] ret = new IHunk[hunks.length - removed];
		for (int i = 0, j = 0; i < hunks.length; i++) {
			if (hunks[i] != null) {
				ret[j++] = hunks[i];
			}
		}
		return ret;
	}

	private static void reorder(IHunk[] hunks) {
		Arrays.sort(hunks, new HunkComparator());
		int shift = 0;
		for (int i = 0; i < hunks.length; i++) {
			Hunk hunk = (Hunk) hunks[i];
			int start = hunk.getStart(false) + shift;
			hunk.setStart(start, true);
			shift += hunk.getLength(true) - hunk.getLength(false);
		}
	}

	static class HunkComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			if ((arg0 != null && arg0 instanceof Hunk)
					&& (arg1 != null && arg1 instanceof Hunk)) {
				Hunk hunk0 = (Hunk) arg0;
				Hunk hunk1 = (Hunk) arg1;
				int shift = hunk0.getStart(true) - hunk1.getStart(true);
				return shift;
			}
			return 0;
		}
	}

}