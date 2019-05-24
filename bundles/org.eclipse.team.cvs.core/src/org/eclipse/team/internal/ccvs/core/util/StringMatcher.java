/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 132260 Eclipse doesn't understand negated character classes in .cvsignore
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.util;

import java.util.Vector;
import java.util.HashMap;

/**
 * A StringMatcher contains a glob and matches it against strings.
 * StringMatcher supports * and ? wildcards and character classes, possibly
 * negated by !, that contain single characters and/or ranges.
 * Note: code copied from org.eclipse.jdt.internal.core.util.StringMatcher on April 3, 2001
 * (version 0.1 - 010901H18 [rename jbl]).
 */
public class StringMatcher {

	protected static class CharacterClass {
		final boolean isNegated;
		final String text;

		CharacterClass(boolean isNegated, String text) {
			this.isNegated = isNegated;
			this.text = text;
		}

		boolean listed(char c) {
			for (int i = 0; i < text.length(); ) {
				if (i + 2 < text.length() && text.charAt(i + 1) == '-') {
					if (c >= text.charAt(i) && c <= text.charAt(i + 2))
						return true;
					i += 3;
				} else {
					if (c == text.charAt(i))
						return true;
					i++;
				}
			}
			return false;
		}
		boolean match(char c) {
			return listed(c) ^ isNegated;
		}
	}
	
	protected String fPattern;
	protected int fLength; // pattern length
	protected boolean fIgnoreWildCards;
	protected boolean fIgnoreCase;
	protected boolean fHasLeadingStar;
	protected boolean fHasTrailingStar;
	protected String fSegments[]; //the given pattern is split into * separated segments
	protected HashMap/*<Integer, CharacterClass>*/ fCharacterClassMaps[];

	/* boundary value beyond which we don't need to search in the text */
	protected int fBound = 0;
	
	/** \? in pattern becomes ? in fSegments, while ? in pattern becomes this */
	protected static final char fSingleWildCard = '\u0000';
	
	public static class Position {
		int start; //inclusive
		int end; //exclusive
		public Position(int start, int end) {
			this.start = start;
			this.end = end;
		}
		public int getStart() {
			return start;
		}
		public int getEnd() {
			return end;
		}
	}

	/**
	 * Find the first occurrence of the pattern between <code>start</code> (inclusive) 
	 * and <code>end</code> (exclusive).  
	 * @param text the String object to search in 
	 * @param start the starting index of the search range, inclusive
	 * @param end the ending index of the search range, exclusive
	 * @return a <code>StringMatcher.Position</code> object that keeps the starting 
	 * (inclusive) and ending positions (exclusive) of the first occurrence of the 
	 * pattern in the specified range of the text; return null if not found or subtext
	 * is empty (start==end). A pair of zeros is returned if pattern is empty string
	 * Note that for pattern like "*abc*" with leading and trailing stars, position of "abc"
	 * is returned. For a pattern like"*??*" in text "abcdf", (1,3) is returned
	 */
	public StringMatcher.Position find(String text, int start, int end) {
		if (fPattern  == null|| text == null)
			throw new IllegalArgumentException();
			
		int tlen = text.length();
		if (start < 0)
			start = 0;
		if (end > tlen)
			end = tlen;
		if (end < 0 ||start >= end )
			return null;
		if (fLength == 0)
			return new Position(start, start);
		if (fIgnoreWildCards) {
			int x = posIn(text, start, end);
			if (x < 0)
				return null;
			return new Position(x, x+fLength);
		}

		int segCount = fSegments.length;
		if (segCount == 0)//pattern contains only '*'(s)
			return new Position (start, end);
					
		int curPos = start;
		int matchStart = -1; 
		int i; 
		for (i = 0; i < segCount && curPos < end; ++i) {
			String current = fSegments[i];
			int nextMatch = regExpPosIn(text, curPos, end, current, fCharacterClassMaps[i]);
			if (nextMatch < 0 )
				return null;
			if(i == 0)
				matchStart = nextMatch;
			curPos = nextMatch + current.length();
		}
		if (i < segCount)
			return null;
		return new Position(matchStart, curPos);
	}

	/**
	 * Constructs a StringMatcher that matches strings against the glob
	 * <code>aPattern</code>.
	 * 
	 * <code>aPattern</code> may contain "?"s, which match single characters,
	 * "*"s, which match zero or more characters, and character classes in
	 * "[...]".  All characters other than "*", "?", and "[" match themselves,
	 * except for "\", which escapes the following character.  For example,
	 * "\*" matches "*" and "\a" matches "a", while "\\" matches "\".  Remember
	 * that Java string literals have an additional level of escaping, so a
	 * string literal for a glob matching a single backslash is written "\\\\".
	 * 
	 * "[" begins a character class, which may contain characters and/or ranges;
	 * "]" ends the class.  A character class matches any single character in
	 * it; for example, "[ac-e]" matches an "a", a "c", a "d", or an "e".  A
	 * negated character class begins with "[!" and matches any single character
	 * not listed.  Inside a character class, "\" loses its special meaning as
	 * an escape character.  The fancier POSIX requirements for character
	 * classes are not supported: ranges use Unicode character numbers, and
	 * (for example) [:alpha:], [.ch.], and [=a=] are not recognized.
	 * 
	 * @param aPattern the glob to match text with
	 * @param ignoreCase if true, case is ignored
	 * @param ignoreWildCards if true, the pattern is taken literally instead of
	 * as a glob
	 */
	public StringMatcher(String aPattern, boolean ignoreCase, boolean ignoreWildCards) {
		fIgnoreCase = ignoreCase;
		fIgnoreWildCards = ignoreWildCards;
		fLength = aPattern.length();

		/* convert case */
		if (fIgnoreCase) {
			fPattern = aPattern.toUpperCase();
		} else {
			fPattern = aPattern;
		}
		
		if (fIgnoreWildCards) {
			parseNoWildCards();
		} else {
			parseWildCards();
		}
	}
	/**
	 * Given the starting (inclusive) and the ending (exclusive) poisitions in the   
	 * <code>text</code>, determine if the given substring matches with aPattern  
	 * @return true if the specified portion of the text matches the pattern
	 * @param text a String object that contains the substring to match 
	 * @param start marks the starting position (inclusive) of the substring
	 * @param end marks the ending index (exclusive) of the substring 
	 */
	public boolean match(String text, int start, int end) {
		if (null == text)
			throw new IllegalArgumentException();

		if (start > end)
			return false;

		if (fIgnoreWildCards)
			return (end - start == fLength) && fPattern.regionMatches(fIgnoreCase, 0, text, start, fLength);
		int segCount= fSegments.length;
		if (segCount == 0 && (fHasLeadingStar || fHasTrailingStar))  // pattern contains only '*'(s)
			return true;
		if (start == end)
			return fLength == 0;
		if (fLength == 0)
			return start == end;

		int tlen= text.length();
		if (start < 0)
			start= 0;
		if (end > tlen)
			end= tlen;

		int tCurPos= start;
		int bound= end - fBound;
		if ( bound < 0)
			return false;
		int i=0;
		String current= fSegments[i];
		HashMap/*<Integer, CharacterClass>*/ curCharClassMap= fCharacterClassMaps[i];
		int segLength= current.length();

		/* process first segment */
		if (!fHasLeadingStar){
			if(!regExpRegionMatches(text, start, current, 0, segLength, curCharClassMap)) {
				return false;
			} else {
				++i;
				tCurPos= tCurPos + segLength;
			}
		}
		if ((fSegments.length == 1) && (!fHasLeadingStar) && (!fHasTrailingStar)) {
			// only one segment to match, no wildcards specified
			return tCurPos == end;
		}
		/* process middle segments */
		while (i < segCount) {
			current= fSegments[i];
			curCharClassMap= fCharacterClassMaps[i];
			int currentMatch;
			int k= current.indexOf(fSingleWildCard);
			if (k < 0) {
				currentMatch= textPosIn(text, tCurPos, end, current);
				if (currentMatch < 0)
					return false;
			} else {
				currentMatch= regExpPosIn(text, tCurPos, end, current, curCharClassMap);
				if (currentMatch < 0)
					return false;
			}
			tCurPos= currentMatch + current.length();
			i++;
		}

		/* process final segment */
		if (!fHasTrailingStar && tCurPos != end) {
			int clen= current.length();
			return regExpRegionMatches(text, end - clen, current, 0, clen, curCharClassMap);
		}
		return i == segCount ;
	}
	/**
	 * match the given <code>text</code> with the pattern 
	 * @return true if matched eitherwise false
	 * @param text the String object to match against the pattern 
	 */
	public boolean  match(String text) {
		return match(text, 0, text.length());
	}
	/**
	 * This method parses the given pattern into segments separated by wildcard '*' characters.
	 * Since wildcards are not being used in this case, the pattern consists of a single segment.
	 */
	private void parseNoWildCards() {
		fSegments = new String[1];
		fSegments[0] = fPattern;
		fBound = fLength;
	}
	/**
	 * This method parses the given pattern into segments separated by wildcard '*' characters.
	 * @param p a String object that is a simple regular expression with *  and/or  ? 
	 */
	private void parseWildCards() {
		if(fPattern.startsWith("*"))//$NON-NLS-1$
			fHasLeadingStar = true;

		Vector temp = new Vector();
		HashMap/*<Integer, CharacterClass>*/ segmentCCs = null;
		Vector/*<HashMap<Integer, CharacterClass>>*/ allCCs = new Vector();

		int pos = 0;
		StringBuilder buf = new StringBuilder();
		while (pos < fLength) {
			char c = fPattern.charAt(pos++);
			fHasTrailingStar = false;
			switch (c) {
				case '\\':
					if (pos >= fLength) {
						buf.append(c);
					} else {
						c = fPattern.charAt(pos++);
						buf.append(c);
					}
				break;
				case '*':
					fHasTrailingStar = true;
					if (buf.length() > 0) {
						/* new segment */
						temp.addElement(buf.toString());
						allCCs.addElement(segmentCCs);
						fBound += buf.length();
						buf.setLength(0);
						segmentCCs = null;
					}
				break;
				case '[':
					if (segmentCCs == null)
						segmentCCs = new HashMap/*<Integer, CharacterClass>*/();
					if (pos >= fLength) {
						// Unterminated; take [ literally for lack of anything better to do
						buf.append(c);
						break;
					}
					boolean negated = (fPattern.charAt(pos) == '!');
					int beginPos = (negated ? pos + 1 : pos);
					int endPos = fPattern.indexOf(']', beginPos + 1);
					if (endPos == -1) {
						// Unterminated; take [ literally for lack of anything better to do
						buf.append(c);
						break;
					}
					CharacterClass cc = new CharacterClass(negated, fPattern.substring(beginPos, endPos));
					segmentCCs.put(Integer.valueOf(buf.length()), cc);
					pos = endPos + 1;
					/* fall through; fSingleWildCard can also represent a character class */
				case '?':
					/* append special character representing single match wildcard */
					buf.append(fSingleWildCard);
				break;
				default:
					buf.append(c);
			}
		}

		/* add last buffer to segment list */
		if (buf.length() > 0) {
			temp.addElement(buf.toString());
			allCCs.addElement(segmentCCs);
			fBound += buf.length();
		}
			
		fSegments = new String[temp.size()];
		temp.copyInto(fSegments);
		fCharacterClassMaps = new HashMap[allCCs.size()];
		allCCs.copyInto(fCharacterClassMaps);
	}
	/** 
	 * @param text a string which contains no wildcard
	 * @param start the starting index in the text for search, inclusive
	 * @param end the stopping point of search, exclusive
	 * @return the starting index in the text of the pattern , or -1 if not found 
	 */
	protected int posIn(String text, int start, int end) {//no wild card in pattern
		return textPosIn(text, start, end, fPattern);
	}
	/** 
	 * @param text a simple regular expression that may only contain '?'(s)
	 * @param start the starting index in the text for search, inclusive
	 * @param end the stopping point of search, exclusive
	 * @param p a simple regular expression that may contains '?'
	 * @param caseIgnored whether the pattern is not casesensitive
	 * @return the starting index in the text of the pattern , or -1 if not found 
	 */
	protected int regExpPosIn(String text, int start, int end, String p, HashMap/*<Integer, CharacterClass>*/ ccMap) {
		int plen = p.length();
		
		int max = end - plen;
		for (int i = start; i <= max; ++i) {
			if (regExpRegionMatches(text, i, p, 0, plen, ccMap))
				return i;
		}
		return -1;
	}
	/**
	 * 
	 * @return boolean
	 * @param text a String to match
	 * @param start int that indicates the starting index of match, inclusive
	 * @param end int that indicates the ending index of match, exclusive
	 * @param p String, a simple regular expression that may contain '?'
	 * @param ignoreCase boolean indicating whether <code>p</code> is case sensitive
	 * @param ccMap maps each index of p at which fSingleWildCard occurs (as an 
	 * Integer) to CharacterClass data, or null for a plain old ?
	 */
	protected boolean regExpRegionMatches(String text, int tStart, String p, int pStart, int plen, HashMap/*<Integer, CharacterClass>*/ ccMap) {
		for (int ppos = 0; plen-- > 0; ppos++) {
			char tchar = text.charAt(tStart++);
			char pchar = p.charAt(pStart++);

			/* process wild cards */
			if (!fIgnoreWildCards) {
				/* skip single wild cards */
				if (pchar == fSingleWildCard) {
					if (ccMap == null)
						continue;
					CharacterClass cc = (CharacterClass) ccMap.get(Integer.valueOf(ppos));
					if (cc == null || cc.match(tchar))
						continue;
					else
						return false;
				}
			}
			if (pchar == tchar)
				continue;
			if (fIgnoreCase) {
				char tc = Character.toUpperCase(tchar);
				if (tc == pchar)
					continue;
			}
			return false;
		}
		return true;
	}
	/** 
	 * @param text the string to match
	 * @param start the starting index in the text for search, inclusive
	 * @param end the stopping point of search, exclusive
	 * @param p a string that has no wildcard
	 * @param ignoreCase boolean indicating whether p is case sensitive
	 * @return the starting index in the text of the pattern , or -1 if not found 
	 */
	protected int textPosIn(String text, int start, int end, String p) { 
		
		int plen = p.length();
		int max = end - plen;
		
		if (!fIgnoreCase) {
			int i = text.indexOf(p, start);
			if (i == -1 || i > max)
				return -1;
			return i;
		}
		
		for (int i = start; i <= max; ++i) {
			if (text.regionMatches(true, i, p, 0, plen))
				return i;
		}
		
		return -1;
	}
}
