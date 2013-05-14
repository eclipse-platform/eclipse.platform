/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.core.patch;

import java.util.List;

import org.eclipse.compare.patch.IHunkFilter;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

public class HunkResult {

	private static final boolean DEBUG= false;

	/**
	 * Default maximum fuzz factor equals 2. This is related to the default
	 * number of context lines, which is 3.
	 */
	private static final int MAXIMUM_FUZZ_FACTOR = 2;
	
	private Hunk fHunk;
	private boolean fMatches;
	private int fShift;
	private int fFuzz = -1; // not set or couldn't be found

	private final FileDiffResult fDiffResult;

	/**
	 * Create a hunk result for the given hunk
	 * @param diffResult the parent diff result
	 * @param hunk the hunk
	 */
	public HunkResult(FileDiffResult diffResult, Hunk hunk) {
		this.fDiffResult = diffResult;
		this.fHunk = hunk;
	}

	/**
	 * Try to apply the specified hunk to the given lines.
	 * If the hunk cannot be applied at the original position
	 * the method tries shift lines up and down.
	 * @param lines the lines to be patched
	 * @return whether the hunk could be applied
	 */
	public boolean patch(List lines) {
		this.fMatches = false;
		PatchConfiguration configuration = getConfiguration();
		// if the fuzz is not set for the current hunk use the one from fDiffResult
		int fuzz = this.fFuzz != -1 ? this.fFuzz : configuration.getFuzz();
		if (isEnabled(configuration)) {
			if (this.fHunk.tryPatch(configuration, lines, this.fShift, fuzz)) {
				// it's a perfect match, no shifting is needed
				this.fShift += this.fHunk.doPatch(configuration, lines, this.fShift, fuzz);
				this.fMatches = true;
			} else {
				boolean found= false;
				int oldShift= this.fShift;
				
				int hugeShift = lines.size();
				for (int i = 1; i <= hugeShift; i++) {
					if (this.fHunk.tryPatch(configuration, lines, this.fShift - i, fuzz)) {
						if (isAdjustShift())
							this.fShift -= i;
						found = true;
						break;
					}
				}
				
				if (!found) {
					for (int i = 1; i <= hugeShift; i++) {
						if (this.fHunk.tryPatch(configuration, lines, this.fShift + i, fuzz)) {
							if (isAdjustShift())
								this.fShift += i;
							found = true;
							break;
						}
					}
				}
				
				if (found) {
					if (DEBUG) System.out.println("patched hunk at offset: " + (this.fShift-oldShift)); //$NON-NLS-1$
					this.fShift+= this.fHunk.doPatch(configuration, lines, this.fShift, fuzz);
					this.fMatches = true;
				}
			}
		}
		return this.fMatches;
	}

	private boolean isAdjustShift() {
		return true;
	}

	private PatchConfiguration getConfiguration() {
		return getDiffResult().getConfiguration();
	}

	/**
	 * Calculate the fuzz that will allow the most hunks to be matched. Even
	 * though we're interested only in the value of the fuzz, the shifting is
	 * done anyway.
	 * 
	 * @param lines
	 *            the lines of the target file
	 * @param monitor
	 *            a progress monitor
	 * @return the fuzz factor or -1 if the hunk could not be matched
	 */
	public int calculateFuzz(List lines, IProgressMonitor monitor) {
		this.fMatches = false;
		PatchConfiguration configuration = getConfiguration();
		int fuzz = 0;
		int maxFuzz = configuration.getFuzz() == -1 ? MAXIMUM_FUZZ_FACTOR
				: configuration.getFuzz();
		for (; fuzz <= maxFuzz; fuzz++) {
			// try to apply using lines coordinates from the patch
			if (this.fHunk.tryPatch(configuration, lines, this.fShift, fuzz)) {
				// it's a perfect match, no adjustment is needed
				this.fShift += this.fHunk.doPatch(configuration, lines, this.fShift, fuzz);
				this.fMatches = true;
				break;
			}
			
			// TODO (tzarna): hugeShift=lines.size() is more than we need.
			// Lines to the beg/end of a file would be enough but this can still
			// in matching hunks out of order. Try to shift using only lines
			// available "between" hunks.
			int hugeShift = lines.size(); 
			
			// shift up 
			for (int i = 1; i <= hugeShift; i++) {
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				if (this.fHunk.tryPatch(configuration, lines, this.fShift - i, fuzz)) {
					if (isAdjustShift())
						this.fShift -= i;
					this.fMatches = true;
					break;
				}
			}

			// shift down
			if (!this.fMatches) {
				for (int i = 1; i <= hugeShift; i++) {
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					if (this.fHunk.tryPatch(configuration, lines, this.fShift + i, fuzz)) {
						if (isAdjustShift())
							this.fShift += i;
						this.fMatches = true;
						break;
					}
				}
			}

			if (this.fMatches) {
				this.fShift += this.fHunk.doPatch(configuration, lines, this.fShift, fuzz);
				break;
			}
		}
		// set fuzz for the current hunk
		this.fFuzz = this.fMatches ? fuzz : -1;
		return this.fFuzz;
	}

	/**
	 * Return the amount that this hunk should be shifted when a match with the file
	 * is attempted. The shift is needed to compensate for previous hunks that have
	 * been applied.
	 * @return the amount that this hunk should be shifted when applied
	 */
	public int getShift() {
		return this.fShift;
	}

	/**
	 * Set the amount that this hunk should be shifted when a match with the file
	 * is attempted. The shift is needed to compensate for previous hunks that have
	 * been applied.
	 * @param shift the amount to shift this hunk
	 */
	public void setShift(int shift) {
		this.fShift = shift;
	}

	/**
	 * Return the hunk to which this result applies.
	 * @return the hunk to which this result applies
	 */
	public Hunk getHunk() {
		return this.fHunk;
	}

	/**
	 * Return the parent diff result.
	 * @return the parent diff result
	 */
	public FileDiffResult getDiffResult() {
		return this.fDiffResult;
	}

	/**
	 * Return whether the hunk was matched with the target file.
	 * @return whether the hunk was matched with the target file
	 */
	public boolean isOK() {
		return this.fMatches;
	}

	/**
	 * Return the contents that should be displayed for the hunk result.
	 * @param afterState whether the after state or before state of the hunk is desired
	 * @param fullContext whether the hunk should be displayed with the entire file or
	 * only the lines in the hunk itself
	 * @return the contents to be display
	 */
	public String getContents(boolean afterState, boolean fullContext) {
		if (fullContext) {
			boolean problemFound = false;
			List lines = getDiffResult().getBeforeLines();
			if (afterState) {
				if (isOK()) {
					int oldShift = this.fShift;
					try {
						this.fShift = 0;
						problemFound = !patch(lines);
					} finally {
						this.fShift = oldShift;
					}
				} else {
					problemFound = true;
				}
			}
			// Only return the full context if we could apply the hunk
			if (!problemFound)
				return LineReader.createString(this.fDiffResult.isPreserveLineDelimeters(), lines);
		}
		return getHunk().getContents(afterState, getConfiguration().isReversed());
	}

	private boolean isEnabled(PatchConfiguration configuration) {
		IHunkFilter[] filters = configuration.getHunkFilters();
		for (int i = 0; i < filters.length; i++) {
			if (!filters[i].select(this.fHunk)) {
				return false;
			}
		}
		return true;
	}

	public void setMatches(boolean matches) {
		this.fMatches = matches;
	}

	public String getCharset() {
		return this.fDiffResult.getCharset();
	}
	
	public int getFuzz() {
		return this.fFuzz;
	}
}
