/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.core.patch;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.compare.patch.IFilePatchResult;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.patch.PatchConfiguration;

/**
 * A Hunk describes a range of changed lines and some context lines.
 */
public class Hunk implements IHunk {

	private FilePatch2 fParent;
	private int fOldStart, fOldLength;
	private int fNewStart, fNewLength;
	private String[] fLines;
	private int hunkType;
	private String charset = null;

	public static Hunk createHunk(FilePatch2 parent, int[] oldRange, int[] newRange, List lines, boolean hasLineAdditions, boolean hasLineDeletions, boolean hasContextLines) {
		int oldStart = 0;
		int oldLength = 0;
		int newStart = 0;
		int newLength = 0;
		if (oldRange[0] > 0)
			oldStart= oldRange[0]-1;	// line number start at 0!
		else
			oldStart= 0;
		oldLength= oldRange[1];
		if (newRange[0] > 0)
			newStart= newRange[0]-1;	// line number start at 0!
		else
			newStart= 0;
		newLength= newRange[1];
		int hunkType = FilePatch2.CHANGE;
		if (!hasContextLines) {
			if (hasLineAdditions && !hasLineDeletions) {
				hunkType = FilePatch2.ADDITION;
			} else if (!hasLineAdditions && hasLineDeletions) {
				hunkType = FilePatch2.DELETION;
			}
		}
		return new Hunk(parent, hunkType, oldStart, oldLength, newStart, newLength, (String[]) lines.toArray(new String[lines.size()]));
	}
	
	public Hunk(FilePatch2 parent, int hunkType, int oldStart, int oldLength,
			int newStart, int newLength, String[] lines) {
		this.fParent = parent;
        if (this.fParent != null) {
            this.fParent.add(this);
        }
		this.hunkType = hunkType;
		this.fOldLength = oldLength;
		this.fOldStart = oldStart;
		this.fNewLength = newLength;
		this.fNewStart = newStart;
		this.fLines = lines;
	}
	
    public Hunk(FilePatch2 parent, Hunk toCopy) {
    	this(parent, toCopy.hunkType, toCopy.fOldStart, toCopy.fOldLength, toCopy.fNewStart, toCopy.fNewLength, toCopy.fLines);
    }

	/*
	 * Returns the contents of this hunk.
	 * Each line starts with a control character. Their meaning is as follows:
	 * <ul>
	 * <li>
	 * '+': add the line
	 * <li>
	 * '-': delete the line
	 * <li>
	 * ' ': no change, context line
	 * </ul>
	 */
	public String getContent() {
		StringBuffer sb= new StringBuffer();
		for (int i= 0; i < this.fLines.length; i++) {
			String line= this.fLines[i];
			sb.append(line.substring(0, LineReader.length(line)));
			sb.append('\n');
		}
		return sb.toString();
	}
	
	/*
	 * Returns a descriptive String for this hunk.
	 * It is in the form old_start,old_length -> new_start,new_length.
	 */
	String getDescription() {
		StringBuffer sb= new StringBuffer();
		sb.append(Integer.toString(this.fOldStart));
		sb.append(',');
		sb.append(Integer.toString(this.fOldLength));
		sb.append(" -> "); //$NON-NLS-1$
		sb.append(Integer.toString(this.fNewStart));
		sb.append(',');
		sb.append(Integer.toString(this.fNewLength));
		return sb.toString();
	}
	
	public String getRejectedDescription() {
		StringBuffer sb= new StringBuffer();
		sb.append("@@ -"); //$NON-NLS-1$
		sb.append(Integer.toString(this.fOldStart));
		sb.append(',');
		sb.append(Integer.toString(this.fOldLength));
		sb.append(" +"); //$NON-NLS-1$
		sb.append(Integer.toString(this.fNewStart));
		sb.append(',');
		sb.append(Integer.toString(this.fNewLength));
		sb.append(" @@"); //$NON-NLS-1$
		return sb.toString();
	}
	
	public int getHunkType(boolean reverse) {
		if (reverse) {
			if (this.hunkType == FilePatch2.ADDITION)
				return FilePatch2.DELETION;
			if (this.hunkType == FilePatch2.DELETION)
				return FilePatch2.ADDITION;
		}
		return this.hunkType;
	}

	void setHunkType(int hunkType) {
		this.hunkType = hunkType;
	}

	public String[] getLines() {
		return this.fLines;
	}

	public String[] getUnifiedLines() {
		String[] ret = new String[this.fLines.length];
		System.arraycopy(this.fLines, 0, ret, 0, this.fLines.length);
		return ret;
	}

	/**
	 * Set the parent of this hunk. This method
	 * should only be invoked from {@link FilePatch2#add(Hunk)}
	 * @param diff the parent of this hunk
	 */
	void setParent(FilePatch2 diff) {
		if (this.fParent == diff)
			return;
		if (this.fParent != null)
			this.fParent.remove(this);
		this.fParent = diff;	
	}

	public FilePatch2 getParent() {
		return this.fParent;
	}
	
	/*
	 * Tries to apply the given hunk on the specified lines.
	 * The parameter shift is added to the line numbers given
	 * in the hunk.
	 */
	public boolean tryPatch(PatchConfiguration configuration, List lines, int shift, int fuzz) {
		boolean reverse = configuration.isReversed();
		int pos = getStart(reverse) + shift;
		List contextLines = new ArrayList();
		boolean contextLinesMatched = true;
		boolean precedingLinesChecked = false;
		for (int i= 0; i < this.fLines.length; i++) {
			String s = this.fLines[i];
			Assert.isTrue(s.length() > 0);
			String line = s.substring(1);
			char controlChar = s.charAt(0);
			
			if (controlChar == ' ') {	// context lines
				
				if (pos < 0 || pos >= lines.size())
					return false;
				contextLines.add(line);
				if (linesMatch(configuration, line, (String) lines.get(pos))) {
					pos++;
					continue;
				} else if (fuzz > 0) {
					// doesn't match, use the fuzz factor
					contextLinesMatched = false;
					pos++;
					continue;
				} 
				return false;
			} else if (isDeletedDelimeter(controlChar, reverse)) {
				// deleted lines
				
				if (precedingLinesChecked && !contextLinesMatched && contextLines.size() > 0)
					// context lines inside hunk don't match
					return false;
				
				// check following context lines if exist
				// use the fuzz factor if needed
				if (!precedingLinesChecked
						&& !contextLinesMatched
						&& contextLines.size() >= fuzz
						&& !checkPrecedingContextLines(configuration, lines,
								fuzz, pos, contextLines))
					return false;
				// else if there is less or equal context line to the fuzz
				// factor we ignore them all and treat as matching
				
				precedingLinesChecked = true;
				contextLines.clear();
				contextLinesMatched = true;
				
				if (pos < 0 || pos >= lines.size()) // out of the file
					return false;
				if (linesMatch(configuration, line, (String) lines.get(pos))) {
					pos++;
					continue; // line matched, continue with the next one
				}

				// We must remove all lines at once, return false if this
				// fails. In other words, all lines considered for deletion
				// must be found one by one.
				return false;
			} else if (isAddedDelimeter(controlChar, reverse)) {
				
				if (precedingLinesChecked && !contextLinesMatched && contextLines.size() > 0)
					return false;
				
				if (!precedingLinesChecked
						&& !contextLinesMatched
						&& contextLines.size() >= fuzz
						&& !checkPrecedingContextLines(configuration, lines,
								fuzz, pos, contextLines))
					return false;

				precedingLinesChecked = true;
				contextLines.clear();
				contextLinesMatched = true;
				
				// we don't have to do anything more for a 'try'
			} else
				Assert.isTrue(false, "tryPatch: unknown control character: " + controlChar); //$NON-NLS-1$
		}
		
		// check following context lines if exist
		if (!contextLinesMatched
				&& fuzz > 0
				&& contextLines.size() > fuzz
				&& !checkFollowingContextLines(configuration, lines, fuzz, pos,
						contextLines))
			return false;
		
		return true;
	}

	private boolean checkPrecedingContextLines(
			PatchConfiguration configuration, List lines, int fuzz, int pos,
			List contextLines) {
		
		// ignore from the beginning
		for (int j = fuzz; j < contextLines.size(); j++) {
			if (!linesMatch(configuration, (String) contextLines.get(j),
							(String) lines.get(pos - contextLines.size() + j)))
				return false;
		}
		return true;
	}
	
	private boolean checkFollowingContextLines(
			PatchConfiguration configuration, List lines, int fuzz, int pos,
			List contextLines) {
		if (!contextLines.isEmpty()) {
			// ignore from the end
			for (int j = 0; j < contextLines.size() - fuzz; j++) {
				if (!linesMatch(configuration, (String) contextLines.get(j),
						(String) lines.get(pos - contextLines.size() + j)))
					return false;
			}
		}
		return true;
	}
	
	public int getStart(boolean after) {
		if (after) {
			return this.fNewStart;
		}
		return this.fOldStart;
	}

	public void setStart(int start, boolean after) {
		if (after) {
			this.fNewStart = start;
		} else {
			this.fOldStart = start;
		}
	}

	public int getLength(boolean after) {
		if (after) {
			return this.fNewLength;
		}
		return this.fOldLength;
	}
	
	private int getShift(boolean reverse) {
		if (reverse) {
			return this.fOldLength - this.fNewLength;
		}
		return this.fNewLength - this.fOldLength;
	}
	
	int doPatch(PatchConfiguration configuration, List lines, int shift, int fuzz) {
		boolean reverse = configuration.isReversed();
		int pos = getStart(reverse) + shift;
		List contextLines = new ArrayList();
		boolean contextLinesMatched = true;
		boolean precedingLinesChecked = false;
		String lineDelimiter = getLineDelimiter(lines);

		for (int i= 0; i < this.fLines.length; i++) {
			String s= this.fLines[i];
			Assert.isTrue(s.length() > 0);
			String line= s.substring(1);
			char controlChar= s.charAt(0);
			if (controlChar == ' ') {	
				// context lines
					Assert.isTrue(pos < lines.size(), "doPatch: inconsistency in context"); //$NON-NLS-1$
					contextLines.add(line);
					if (linesMatch(configuration, line, (String) lines.get(pos))) {
						pos++;
						continue;
					} else if (fuzz > 0) {
						// doesn't match, use the fuzz factor
						contextLinesMatched = false;
						pos++;
						continue;
					}
					Assert.isTrue(false, "doPatch: context doesn't match"); //$NON-NLS-1$
//					pos++;
			} else if (isDeletedDelimeter(controlChar, reverse)) {
				// deleted lines	
				if (precedingLinesChecked && !contextLinesMatched && contextLines.size() > 0)
					// context lines inside hunk don't match
					Assert.isTrue(false, "doPatch: context lines inside hunk don't match"); //$NON-NLS-1$
				
				// check following context lines if exist
				// use the fuzz factor if needed
				if (!precedingLinesChecked
						&& !contextLinesMatched
						&& contextLines.size() >= fuzz
						&& !checkPrecedingContextLines(configuration, lines,
								fuzz, pos, contextLines))
					Assert.isTrue(false, "doPatch: preceding context lines don't match, even though fuzz factor has been used"); //$NON-NLS-1$;
				// else if there is less or equal context line to the fuzz
				// factor we ignore them all and treat as matching
				
				precedingLinesChecked = true;
				contextLines.clear();
				contextLinesMatched = true;
				
				lines.remove(pos);
			} else if (isAddedDelimeter(controlChar, reverse)) {
				// added lines
				if (precedingLinesChecked && !contextLinesMatched && contextLines.size() > 0)
					Assert.isTrue(false, "doPatch: context lines inside hunk don't match"); //$NON-NLS-1$
				
				if (!precedingLinesChecked
						&& !contextLinesMatched
						&& contextLines.size() >= fuzz
						&& !checkPrecedingContextLines(configuration, lines,
								fuzz, pos, contextLines))
					Assert.isTrue(false, "doPatch: preceding context lines don't match, even though fuzz factor has been used"); //$NON-NLS-1$;

				precedingLinesChecked = true;
				contextLines.clear();
				contextLinesMatched = true;
				
				// if the line contains a delimiter, use a proper one
				if (line.length() > LineReader.length(line))
					line = line.substring(0, LineReader.length(line)) + lineDelimiter;
				
				if (getLength(reverse) == 0 && pos+1 < lines.size())
					lines.add(pos+1, line);
				else
					lines.add(pos, line);
				pos++;
			} else
				Assert.isTrue(false, "doPatch: unknown control character: " + controlChar); //$NON-NLS-1$
		}
		return getShift(reverse);
	}

	private boolean isDeletedDelimeter(char controlChar, boolean reverse) {
		return (!reverse && controlChar == '-') || (reverse && controlChar == '+');
	}
	
	private boolean isAddedDelimeter(char controlChar, boolean reverse) {
		return (reverse && controlChar == '-') || (!reverse && controlChar == '+');
	}
	
	/*
	 * Compares two strings.
	 * If fIgnoreWhitespace is true whitespace is ignored.
	 */
	private boolean linesMatch(PatchConfiguration configuration, String line1, String line2) {
		if (configuration.isIgnoreWhitespace())
			return stripWhiteSpace(line1).equals(stripWhiteSpace(line2));
		if (isIgnoreLineDelimiter()) {
			int l1= LineReader.length(line1);
			int l2= LineReader.length(line2);
			if (l1 != l2)
				return false;
			return line1.regionMatches(0, line2, 0, l1);
		}
		return line1.equals(line2);
	}
	
	private boolean isIgnoreLineDelimiter() {
		return true;
	}

	private String getLineDelimiter(List lines) {
		if (lines.size() > 0) {
			// get a line separator from the file being patched
			String line0 = (String) lines.get(0);
			return line0.substring(LineReader.length(line0));
		} else if (this.fLines.length > 0) {
			// if the file doesn't exist use a line separator from the patch
			return this.fLines[0].substring(LineReader.length(this.fLines[0]));
		}
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	/*
	 * Returns the given string with all whitespace characters removed.
	 * Whitespace is defined by <code>Character.isWhitespace(...)</code>.
	 */
	private String stripWhiteSpace(String s) {
		StringBuffer sb= new StringBuffer();
		int l= s.length();
		for (int i= 0; i < l; i++) {
			char c= s.charAt(i);
			if (!Character.isWhitespace(c))
				sb.append(c);
		}
		return sb.toString();
	}
	
	public String getContents(boolean isAfterState, boolean reverse) {
		StringBuffer result= new StringBuffer();
		for (int i= 0; i<this.fLines.length; i++) {
			String line= this.fLines[i];
			String rest= line.substring(1);
			char c = line.charAt(0);
			if (c == ' ') {
				result.append(rest);
			} else if (isDeletedDelimeter(c, reverse) && !isAfterState) {
				result.append(rest);	
			} else if (isAddedDelimeter(c, reverse) && isAfterState) {
				result.append(rest);
			}
		}
		return result.toString();
	}

	public String getLabel() {
		return getDescription();
	}

	public int getStartPosition() {
		return getStart(false);
	}

	public InputStream getOriginalContents() {
		String contents = getContents(false, false);
		return asInputStream(contents);
	}

	public InputStream getPatchedContents() {
		String contents = getContents(true, false);
		return asInputStream(contents);
	}

	private InputStream asInputStream(String contents) {
		String charSet = getCharset();
		return FileDiffResult.asInputStream(contents, charSet);
	}

	void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * {@inheritDoc}
	 * @deprecated This method can be called before the first attempt to apply
	 *             the hunk when it is impossible to determine the encoding and
	 *             in this case it always returns null. Please see
	 *             {@link IFilePatchResult#getCharset()} as a proper way to
	 *             obtain charset.
	 */
	public String getCharset() {
		return this.charset;
	}

}
