/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal.core.patch;

import java.io.*;
import java.util.*;

import org.eclipse.compare.internal.core.Messages;
import org.eclipse.compare.patch.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

public class FileDiffResult implements IFilePatchResult {
	private FilePatch2 fDiff;
	private boolean fMatches= false;
	private boolean fDiffProblem;
	private String fErrorMessage;
	private Map<Hunk, HunkResult> fHunkResults = new HashMap<>();
	private List<String> fBeforeLines, fAfterLines;
	private final PatchConfiguration configuration;
	private String charset;

	public FileDiffResult(FilePatch2 diff, PatchConfiguration configuration) {
		super();
		this.fDiff = diff;
		this.configuration = configuration;
	}

	public PatchConfiguration getConfiguration() {
		return this.configuration;
	}

	public boolean canApplyHunk(Hunk hunk) {
		HunkResult result = getHunkResult(hunk);
		return result.isOK() && !this.fDiffProblem;
	}

	/**
	 * Refreshes the state of the diff to {no matches, no problems} and checks to see what hunks contained
	 * by this Diff can actually be applied.
	 *
	 * Checks to see:
	 * 1) if the target file specified in fNewPath exists and is patchable
	 * 2) which hunks contained by this diff can actually be applied to the file
	 * @param content the contents being patched or <code>null</code> for an addition
	 * @param monitor a progress monitor or <code>null</code> if no progress monitoring is desired
	 */
	public void refresh(ReaderCreator content, IProgressMonitor monitor) {
		this.fMatches= false;
		this.fDiffProblem= false;
		boolean create= false;
		this.charset = Utilities.getCharset(content);
		//If this diff is an addition, make sure that it doesn't already exist
		boolean exists = targetExists(content);
		if (this.fDiff.getDiffType(getConfiguration().isReversed()) == FilePatch2.ADDITION) {
			if ((!exists || isEmpty(content)) && canCreateTarget(content)) {
				this.fMatches= true;
			} else {
				// file already exists
				this.fDiffProblem= true;
				this.fErrorMessage= Messages.FileDiffResult_0;
			}
			create= true;
		} else { //This diff is not an addition, try to find a match for it
			//Ensure that the file described by the path exists and is modifiable
			if (exists) {
				this.fMatches= true;
			} else {
				// file doesn't exist
				this.fDiffProblem= true;
				this.fErrorMessage= Messages.FileDiffResult_1;
			}
		}

		if (this.fDiffProblem) {
			// We couldn't find the target file or the patch is trying to add a
			// file that already exists but we need to initialize the hunk
			// results for display
			this.fBeforeLines = new ArrayList<>(getLines(content, false));
			this.fAfterLines = this.fMatches ? new ArrayList<>() : this.fBeforeLines;
			IHunk[] hunks = this.fDiff.getHunks();
			for (IHunk h : hunks) {
				Hunk hunk = (Hunk) h;
				hunk.setCharset(getCharset());
				HunkResult result = getHunkResult(hunk);
				result.setMatches(false);
			}
		} else {
			// If this diff has no problems discovered so far, try applying the patch
			patch(getLines(content, create), monitor);
		}

		if (containsProblems()) {
			if (this.fMatches) {
				// Check to see if we have at least one hunk that matches
				this.fMatches = false;
				IHunk[] hunks = this.fDiff.getHunks();
				for (IHunk h : hunks) {
					Hunk hunk = (Hunk) h;
					HunkResult result = getHunkResult(hunk);
					if (result.isOK()) {
						this.fMatches = true;
						break;
					}
				}
			}
		}
	}

	protected boolean canCreateTarget(ReaderCreator content) {
		return true;
	}

	protected boolean targetExists(ReaderCreator content) {
		return content != null && content.canCreateReader();
	}

	protected List<String> getLines(ReaderCreator content, boolean create) {
		List<String> lines = LineReader.load(content, create);
		return lines;
	}

	protected boolean isEmpty(ReaderCreator content) {
		if (content == null)
			return true;
		return LineReader.load(content, false).isEmpty();
	}

	/*
	 * Tries to patch the given lines with the specified Diff.
	 * Any hunk that couldn't be applied is returned in the list failedHunks.
	 */
	public void patch(List<String> lines, IProgressMonitor monitor) {
		this.fBeforeLines = new ArrayList<>();
		this.fBeforeLines.addAll(lines);
		if (getConfiguration().getFuzz() != 0) {
			calculateFuzz(this.fBeforeLines, monitor);
		}
		int shift= 0;
		IHunk[] hunks = this.fDiff.getHunks();
		for (IHunk h : hunks) {
			Hunk hunk = (Hunk) h;
			hunk.setCharset(getCharset());
			HunkResult result = getHunkResult(hunk);
			result.setShift(shift);
			if (result.patch(lines)) {
				shift = result.getShift();
			}
		}
		this.fAfterLines = lines;
	}

	public boolean getDiffProblem() {
		return this.fDiffProblem;
	}

	/**
	 * Returns whether this Diff has any problems
	 * @return true if this Diff or any of its children Hunks have a problem, false if it doesn't
	 */
	public boolean containsProblems() {
		if (this.fDiffProblem)
			return true;
		for (HunkResult result : this.fHunkResults.values()) {
			if (!result.isOK())
				return true;
		}
		return false;
	}

	public String getLabel() {
		String label= getTargetPath().toString();
		if (this.fDiffProblem)
			return NLS.bind(Messages.FileDiffResult_2, new String[] {label, this.fErrorMessage});
		return label;
	}

	@Override
	public boolean hasMatches() {
		return this.fMatches;
	}

	/**
	 * Return the lines of the target file with all matched hunks applied.
	 * @return the lines of the target file with all matched hunks applied
	 */
	public List<String> getLines() {
		return this.fAfterLines;
	}

	/**
	 * Calculate the fuzz factor that will allow the most hunks to be matched.
	 * @param lines the lines of the target file
	 * @param monitor a progress monitor
	 * @return the fuzz factor or <code>-1</code> if no hunks could be matched
	 */
	public int calculateFuzz(List<String> lines, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		this.fBeforeLines = new ArrayList<>(lines);
		// TODO: What about deletions?
		if (this.fDiff.getDiffType(getConfiguration().isReversed()) == FilePatch2.ADDITION) {
			// Additions don't need to adjust the fuzz factor
			// TODO: What about the after lines?
			return -1;
		}
		int shift= 0;
		int highestFuzz = -1; // the maximum fuzz factor for all hunks
		String name = getTargetPath() != null ? getTargetPath().lastSegment() : ""; //$NON-NLS-1$
		IHunk[] hunks = this.fDiff.getHunks();
		for (int j = 0; j < hunks.length; j++) {
			Hunk h = (Hunk) hunks[j];
			monitor.subTask(NLS.bind(Messages.FileDiffResult_3, new String[] {name, Integer.toString(j + 1)}));
			HunkResult result = getHunkResult(h);
			result.setShift(shift);
			int fuzz = result.calculateFuzz(lines, monitor);
			shift = result.getShift();
			if (fuzz > highestFuzz)
				highestFuzz = fuzz;
			monitor.worked(1);
		}
		this.fAfterLines = lines;
		return highestFuzz;
	}

	public IPath getTargetPath() {
		return this.fDiff.getStrippedPath(getConfiguration().getPrefixSegmentStripCount(), getConfiguration().isReversed());
	}

	private HunkResult getHunkResult(Hunk hunk) {
		HunkResult result = this.fHunkResults.get(hunk);
		if (result == null) {
			result = new HunkResult(this, hunk);
			this.fHunkResults.put(hunk, result);
		}
		return result;
	}

	public List<Hunk> getFailedHunks() {
		List<Hunk> failedHunks = new ArrayList<>();
		IHunk[] hunks = this.fDiff.getHunks();
		for (IHunk hunk : hunks) {
			HunkResult result = this.fHunkResults.get(hunk);
			if (result != null && !result.isOK())
				failedHunks.add(result.getHunk());
		}
		return failedHunks;
	}

	public FilePatch2 getDiff() {
		return this.fDiff;
	}

	public List<String> getBeforeLines() {
		return this.fBeforeLines;
	}

	public List<String> getAfterLines() {
		return this.fAfterLines;
	}

	public HunkResult[] getHunkResults() {
		// return hunk results in the same order as hunks are placed in file diff
		List<HunkResult> results = new ArrayList<>();
		IHunk[] hunks = this.fDiff.getHunks();
		for (IHunk hunk : hunks) {
			HunkResult result = this.fHunkResults.get(hunk);
			if (result != null) {
				results.add(result);
			}
		}
		return results.toArray(new HunkResult[results.size()]);
	}

	@Override
	public InputStream getOriginalContents() {
		String contents = LineReader.createString(isPreserveLineDelimeters(), getBeforeLines());
		return asInputStream(contents, getCharset());
	}

	@Override
	public InputStream getPatchedContents() {
		String contents = LineReader.createString(isPreserveLineDelimeters(), getLines());
		return asInputStream(contents, getCharset());
	}

	@Override
	public String getCharset() {
		return this.charset;
	}

	public boolean isPreserveLineDelimeters() {
		return false;
	}

	@Override
	public IHunk[] getRejects() {
		List<Hunk> failedHunks = getFailedHunks();
		return failedHunks.toArray(new IHunk[failedHunks.size()]);
	}

	@Override
	public boolean hasRejects() {
		return getFailedHunks().size() > 0;
	}

	public static InputStream asInputStream(String contents, String charSet) {
		byte[] bytes = null;
		if (charSet != null) {
			try {
				bytes = contents.getBytes(charSet);
			} catch (UnsupportedEncodingException e) {
				Platform.getLog(FileDiffResult.class).error(Messages.Activator_1, e);
			}
		}
		if (bytes == null) {
			bytes = contents.getBytes();
		}
		return new ByteArrayInputStream(bytes);
	}

}
