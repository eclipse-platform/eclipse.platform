/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.core.internal.expressions.util.LRUCache;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Implements the <code>IRangeComparator</code> interface for lines in a document.
 * A <code>DocLineComparator</code> is used as the input for the <code>RangeDifferencer</code>
 * engine to perform a line oriented compare on documents.
 * <p>
 * A <code>DocLineComparator</code> doesn't know anything about line separators because
 * its notion of lines is solely defined in the underlying <code>IDocument</code>.
 */
public class DocLineComparator implements ITokenComparator {

	private IDocument fDocument;
	private int fLineOffset;
	private int fLineCount;
	private int fLength;
	private boolean fIgnoreWhiteSpace;
	private ICompareFilter[] fCompareFilters;
	private char fContributor;
	private LRUCache fCompareFilterCache;

	/**
	 * Creates a <code>DocLineComparator</code> for the given document range.
	 * ignoreWhiteSpace controls whether comparing lines (in method
	 * <code>rangesEqual<code>) should ignore whitespace.
	 *
	 * @param document the document from which the lines are taken
	 * @param region if non-<code>null</code> only lines within this range are taken
	 * @param ignoreWhiteSpace if <code>true</code> white space is ignored when comparing lines
	 */
	public DocLineComparator(IDocument document, IRegion region,
			boolean ignoreWhiteSpace) {
		this(document, region, ignoreWhiteSpace, null, '?');
	}

	/**
	 * Creates a <code>DocLineComparator</code> for the given document range.
	 * ignoreWhiteSpace controls whether comparing lines (in method
	 * <code>rangesEqual<code>) should ignore whitespace. Compare filters may be used
	 * to affect the detection of line differences.
	 * 
	 * @param document
	 *            the document from which the lines are taken
	 * @param region
	 *            if non-<code>null</code> only lines within this range are
	 *            taken
	 * @param ignoreWhiteSpace
	 *            if <code>true</code> white space is ignored when comparing
	 *            lines
	 * @param compareFilters
	 *            the active compare filters for the compare
	 * @param contributor
	 *            contributor of document
	 */
	public DocLineComparator(IDocument document, IRegion region,
			boolean ignoreWhiteSpace, ICompareFilter[] compareFilters,
			char contributor) {
		fDocument = document;
		fIgnoreWhiteSpace = ignoreWhiteSpace;
		fCompareFilters = compareFilters;
		fContributor = contributor;

		boolean cacheFilteredLines = false;
		if (compareFilters != null && compareFilters.length > 0) {
			cacheFilteredLines = true;
			for (int i = 0; i < compareFilters.length; i++) {
				if (!compareFilters[i].canCacheFilteredRegions()) {
					cacheFilteredLines = false;
					break;
				}
			}
		}
		fCompareFilterCache = (cacheFilteredLines) ? new LRUCache(1024) : null;

		fLineOffset = 0;
		if (region != null) {
			fLength = region.getLength();
			int start = region.getOffset();
			try {
				fLineOffset = fDocument.getLineOfOffset(start);
			} catch (BadLocationException ex) {
				// silently ignored
			}

			if (fLength == 0) {
				// optimization, empty documents have one line
				fLineCount = 1;
			} else {
				int endLine = fDocument.getNumberOfLines();
				try {
					endLine = fDocument.getLineOfOffset(start + fLength);
				} catch (BadLocationException ex) {
					// silently ignored
				}
				fLineCount = endLine - fLineOffset + 1;
			}
		} else {
			fLength = document.getLength();
			fLineCount = fDocument.getNumberOfLines();
		}
	}

	/**
	 * Returns the number of lines in the document.
	 *
	 * @return number of lines
	 */
	public int getRangeCount() {
		return fLineCount;
	}

	/* (non Javadoc)
	 * see ITokenComparator.getTokenStart
	 */
	public int getTokenStart(int line) {
		try {
			IRegion r= fDocument.getLineInformation(fLineOffset + line);
			return r.getOffset();
		} catch (BadLocationException ex) {
			return fDocument.getLength();
		}
	}

	/* (non Javadoc)
	 * Returns the length of the given line.
	 * see ITokenComparator.getTokenLength
	 */
	public int getTokenLength(int line) {
		return getTokenStart(line+1) - getTokenStart(line);
	}

	/**
	 * Returns <code>true</code> if a line given by the first index
	 * matches a line specified by the other <code>IRangeComparator</code> and index.
	 *
	 * @param thisIndex the number of the line within this range comparator
	 * @param otherComparator the range comparator to compare this with
	 * @param otherIndex the number of the line within the other comparator
	 * @return <code>true</code> if the lines are equal
	 */
	public boolean rangesEqual(int thisIndex, IRangeComparator otherComparator, int otherIndex) {

		if (otherComparator != null && otherComparator.getClass() == getClass()) {
			DocLineComparator other= (DocLineComparator) otherComparator;

			if (fIgnoreWhiteSpace) {
				String[] linesToCompare = extract(thisIndex, otherIndex, other, false);
				return compare(linesToCompare[0], linesToCompare[1]);
			}

			int tlen= getTokenLength(thisIndex);
			int olen= other.getTokenLength(otherIndex);
			if (fCompareFilters != null && fCompareFilters.length > 0) {
				String[] linesToCompare = extract(thisIndex, otherIndex, other, true);
				return linesToCompare[0].equals(linesToCompare[1]);
			} else if (tlen == olen) {
				String[] linesToCompare = extract(thisIndex, otherIndex, other, false);
				return linesToCompare[0].equals(linesToCompare[1]);
			}
		}
		return false;
	}

	/**
	 * Aborts the comparison if the number of tokens is too large.
	 * 
	 * @param length a number on which to base the decision whether to return
	 * 	<code>true</code> or <code>false</code>
	 * @param maxLength another number on which to base the decision whether to return
	 *	<code>true</code> or <code>false</code>
	 * @param other the other <code>IRangeComparator</code> to compare with
	 * @return <code>true</code> to avoid a too lengthy range comparison
	 */
	public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
		return false;
	}
		
	//---- private methods
	
	private String[] extract(int thisIndex, int otherIndex,
			DocLineComparator other, boolean includeSeparator) {

		String[] extracts = new String[2];
		if (fCompareFilters != null && fCompareFilters.length > 0) {
			if (fCompareFilterCache != null
					&& other.fCompareFilterCache != null) {
				extracts[0] = (String) fCompareFilterCache.get(new Integer(
						thisIndex));
				if (extracts[0] == null) {
					extracts[0] = Utilities.applyCompareFilters(
							extract(thisIndex, includeSeparator), fContributor,
							other.extract(otherIndex, includeSeparator), other.fContributor,
							fCompareFilters);
					fCompareFilterCache
							.put(new Integer(thisIndex), extracts[0]);
				}

				extracts[1] = (String) other.fCompareFilterCache
						.get(new Integer(otherIndex));
				if (extracts[1] == null) {
					extracts[1] = Utilities.applyCompareFilters(
							other.extract(otherIndex, includeSeparator), other.fContributor,
							extract(thisIndex, includeSeparator), fContributor, fCompareFilters);
					other.fCompareFilterCache.put(new Integer(otherIndex),
							extracts[1]);
				}
			} else {
				String thisLine = extract(thisIndex, includeSeparator);
				String otherLine = other.extract(otherIndex, includeSeparator);
				extracts = new String[] {
						Utilities.applyCompareFilters(thisLine, fContributor,
								otherLine, other.fContributor, fCompareFilters),
						Utilities.applyCompareFilters(otherLine,
								other.fContributor, thisLine, fContributor,
								fCompareFilters) };
			}
		} else {
			extracts = new String[] { extract(thisIndex, includeSeparator),
					other.extract(otherIndex, includeSeparator) };
		}
		return extracts;
	}

	/**
	 * Extract a single line from the underlying document.
	 *
	 * @param line the number of the line to extract
     * @param whether to include the line separator
	 * @return the contents of the line as a String
	 */
	private String extract(int line, boolean includeSeparator) {
		if (line < fLineCount) {
			try {
				if (includeSeparator)
					return fDocument.get(fDocument.getLineOffset(line),
							fDocument.getLineLength(line));

				IRegion r = fDocument.getLineInformation(fLineOffset + line);
				return fDocument.get(r.getOffset(), r.getLength());

			} catch (BadLocationException e) {
				// silently ignored
			}
		}
		return ""; //$NON-NLS-1$
	}

	private boolean compare(String s1, String s2) {
		int l1= s1.length();
		int l2= s2.length();
		int c1= 0, c2= 0;
		int i1= 0, i2= 0;
		
		while (c1 != -1) {
			
			c1= -1;
			while (i1 < l1) {
				char c= s1.charAt(i1++);
				if (! Character.isWhitespace(c)) {
					c1= c;
					break;
				}
			}
			
			c2= -1;
			while (i2 < l2) {
				char c= s2.charAt(i2++);
				if (! Character.isWhitespace(c)) {
					c2= c;
					break;
				}
			}
				
			if (c1 != c2)
				return false;
		}
		return true;
	}
}

