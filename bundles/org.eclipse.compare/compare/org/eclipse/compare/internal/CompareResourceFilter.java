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

import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;

import com.ibm.icu.text.MessageFormat;


public class CompareResourceFilter {
	private static final char[][] NO_CHAR_CHAR= new char[0][];
	
	private char[][] fExtraResourceFileFilters;
	private String[] fExtraResourceFolderFilters;

	
	public CompareResourceFilter() {
		// nothing to do
	}

	/*
	 * Returns true if path matches filter, that is if path should be filtered.
	 */
	public boolean filter(String path0, boolean folder, boolean isArchive) {
		if (!folder && fExtraResourceFileFilters != null) {
			char[] name= path0.toCharArray();
			for (int i= 0, l= fExtraResourceFileFilters.length; i < l; i++)
				if (match(fExtraResourceFileFilters[i], name, true))
					return true;
		}
		if (folder && fExtraResourceFolderFilters != null) {
			for (int i= 0, l= fExtraResourceFolderFilters.length; i < l; i++)
				if (fExtraResourceFolderFilters[i].equals(path0))
					return true;
		}
		return false;
	}

	public static String validateResourceFilters(String text) {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		String[] filters= getTokens(text, ","); //$NON-NLS-1$
		for (int i= 0; i < filters.length; i++) {
			String fileName= filters[i].replace('*', 'x');
			int resourceType= IResource.FILE;
			int lastCharacter= fileName.length() - 1;
			if (lastCharacter >= 0 && fileName.charAt(lastCharacter) == '/') {
				fileName= fileName.substring(0, lastCharacter);
				resourceType= IResource.FOLDER;
			}
			IStatus status= workspace.validateName(fileName, resourceType);
			if (status.matches(IStatus.ERROR)) {		
				String format= Utilities.getString("ComparePreferencePage.filter.invalidsegment.error"); //$NON-NLS-1$
				return MessageFormat.format(format, new String[] { status.getMessage() } );
			}
		}
		return null;
	}
	
	public void setFilters(String filterSequence) {
		char[][] filters= filterSequence != null && filterSequence.length() > 0
		? splitAndTrimOn(',', filterSequence.toCharArray())
		: null;
		if (filters == null) {
			fExtraResourceFileFilters= null;
			fExtraResourceFolderFilters= null;
		} else {
			int fileCount= 0, folderCount= 0;
			for (int i= 0, l= filters.length; i < l; i++) {
				char[] f= filters[i];
				if (f.length == 0)
					continue;
				if (f[f.length - 1] == '/')
					folderCount++;
				else
					fileCount++;
			}
			fExtraResourceFileFilters= new char[fileCount][];
			fExtraResourceFolderFilters= new String[folderCount];
			for (int i= 0, l= filters.length; i < l; i++) {
				char[] f= filters[i];
				if (f.length == 0)
					continue;
				if (f[f.length - 1] == '/')
					fExtraResourceFolderFilters[--folderCount]= new String(subarray(f, 0, f.length - 1));
				else
					fExtraResourceFileFilters[--fileCount]= f;
			}
		}
	}

	/////////
	
	private static String[] getTokens(String text, String separator) {
		StringTokenizer tok= new StringTokenizer(text, separator);
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < res.length; i++)
			res[i]= tok.nextToken().trim();
		return res;
	}	
	
	/**
	 * Answers true if the pattern matches the given name, false otherwise.
	 * This char[] pattern matching accepts wild-cards '*' and '?'.
	 * 
	 * When not case sensitive, the pattern is assumed to already be
	 * lowercased, the name will be lowercased character per character as
	 * comparing. If name is null, the answer is false. If pattern is null, the
	 * answer is true if name is not null. <br><br>For example:
	 * <ol>
	 * <li>
	 * 
	 * <pre>
	 *  pattern = { '?', 'b', '*' } name = { 'a', 'b', 'c' , 'd' } isCaseSensitive = true result => true
	 * </pre>
	 * 
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 *  pattern = { '?', 'b', '?' } name = { 'a', 'b', 'c' , 'd' } isCaseSensitive = true result => false
	 * </pre>
	 * 
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 *  pattern = { 'b', '*' } name = { 'a', 'b', 'c' , 'd' } isCaseSensitive = true result => false
	 * </pre>
	 * 
	 * 
	 * </li>
	 * </ol>
	 * 
	 * @param pattern
	 *            the given pattern
	 * @param name
	 *            the given name
	 * @param isCaseSensitive
	 *            flag to know whether or not the matching should be case
	 *            sensitive
	 * @return true if the pattern matches the given name, false otherwise
	 */
	private boolean match(char[] pattern, char[] name, boolean isCaseSensitive) {
		if (name == null)
			return false; // null name cannot match
		if (pattern == null)
			return true; // null pattern is equivalent to '*'
		return match(pattern, 0, pattern.length, name, 0, name.length, isCaseSensitive);
	}

	/**
	 * Answers true if the a sub-pattern matches the subpart of the given name,
	 * false otherwise. char[] pattern matching, accepting wild-cards '*' and
	 * '?'. Can match only subset of name/pattern. end positions are
	 * non-inclusive. The subpattern is defined by the patternStart and
	 * pattternEnd positions. When not case sensitive, the pattern is assumed
	 * to already be lowercased, the name will be lowercased character per
	 * character as comparing. <br><br>For example:
	 * <ol>
	 * <li>
	 * 
	 * <pre>
	 *  pattern = { '?', 'b', '*' } patternStart = 1 patternEnd = 3 name = { 'a', 'b', 'c' , 'd' } nameStart = 1 nameEnd = 4 isCaseSensitive = true result => true
	 * </pre>
	 * 
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 *  pattern = { '?', 'b', '*' } patternStart = 1 patternEnd = 2 name = { 'a', 'b', 'c' , 'd' } nameStart = 1 nameEnd = 2 isCaseSensitive = true result => false
	 * </pre>
	 * 
	 * 
	 * </li>
	 * </ol>
	 * 
	 * @param pattern
	 *            the given pattern
	 * @param patternStart
	 *            the given pattern start
	 * @param patternEnd
	 *            the given pattern end
	 * @param name
	 *            the given name
	 * @param nameStart
	 *            the given name start
	 * @param nameEnd
	 *            the given name end
	 * @param isCaseSensitive
	 *            flag to know if the matching should be case sensitive
	 * @return true if the a sub-pattern matches the subpart of the given name,
	 *         false otherwise
	 */
	private boolean match(char[] pattern, int patternStart, int patternEnd, char[] name, int nameStart, int nameEnd,
			boolean isCaseSensitive) {
		if (name == null)
			return false; // null name cannot match
		if (pattern == null)
			return true; // null pattern is equivalent to '*'
		int iPattern= patternStart;
		int iName= nameStart;
		if (patternEnd < 0)
			patternEnd= pattern.length;
		if (nameEnd < 0)
			nameEnd= name.length;
		/* check first segment */
		char patternChar= 0;
		while ((iPattern < patternEnd) && (patternChar= pattern[iPattern]) != '*') {
			if (iName == nameEnd)
				return false;
			if (patternChar != (isCaseSensitive ? name[iName] : Character.toLowerCase(name[iName])) && patternChar != '?') {
				return false;
			}
			iName++;
			iPattern++;
		}
		/* check sequence of star+segment */
		int segmentStart;
		if (patternChar == '*') {
			segmentStart= ++iPattern; // skip star
		} else {
			segmentStart= 0; // force iName check
		}
		int prefixStart= iName;
		checkSegment : while (iName < nameEnd) {
			if (iPattern == patternEnd) {
				iPattern= segmentStart; // mismatch - restart current segment
				iName= ++prefixStart;
				continue checkSegment;
			}
			/* segment is ending */
			if ((patternChar= pattern[iPattern]) == '*') {
				segmentStart= ++iPattern; // skip start
				if (segmentStart == patternEnd) {
					return true;
				}
				prefixStart= iName;
				continue checkSegment;
			}
			/* check current name character */
			if ((isCaseSensitive ? name[iName] : Character.toLowerCase(name[iName])) != patternChar && patternChar != '?') {
				iPattern= segmentStart; // mismatch - restart current segment
				iName= ++prefixStart;
				continue checkSegment;
			}
			iName++;
			iPattern++;
		}
		return (segmentStart == patternEnd) || (iName == nameEnd && iPattern == patternEnd)
				|| (iPattern == patternEnd - 1 && pattern[iPattern] == '*');
	}

	/**
	 * Return a new array which is the split of the given array using the given
	 * divider and triming each subarray to remove whitespaces equals to ' '.
	 * <br><br>For example:
	 * <ol>
	 * <li>
	 * 
	 * <pre>
	 *  divider = 'b' array = { 'a' , 'b', 'b', 'a', 'b', 'a' } result => { { 'a' }, { }, { 'a' }, { 'a' } }
	 * </pre>
	 * 
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 *  divider = 'c' array = { 'a' , 'b', 'b', 'a', 'b', 'a' } result => { { 'a', 'b', 'b', 'a', 'b', 'a' } }
	 * </pre>
	 * 
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 *  divider = 'b' array = { 'a' , ' ', 'b', 'b', 'a', 'b', 'a' } result => { { 'a' }, { }, { 'a' }, { 'a' } }
	 * </pre>
	 * 
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 *  divider = 'c' array = { ' ', ' ', 'a' , 'b', 'b', 'a', 'b', 'a', ' ' } result => { { 'a', 'b', 'b', 'a', 'b', 'a' } }
	 * </pre>
	 * 
	 * 
	 * </li>
	 * </ol>
	 * 
	 * @param divider
	 *            the given divider
	 * @param array
	 *            the given array
	 * @return a new array which is the split of the given array using the
	 *         given divider and triming each subarray to remove whitespaces
	 *         equals to ' '
	 */
	private char[][] splitAndTrimOn(char divider, char[] array) {
		int length= array == null ? 0 : array.length;
		if (length == 0)
			return NO_CHAR_CHAR;
		int wordCount= 1;
		for (int i= 0; i < length; i++)
			if (array[i] == divider)
				wordCount++;
		char[][] split= new char[wordCount][];
		int last= 0, currentWord= 0;
		for (int i= 0; i < length; i++) {
			if (array[i] == divider) {
				int start= last, end= i - 1;
				while (start < i && array[start] == ' ')
					start++;
				while (end > start && array[end] == ' ')
					end--;
				split[currentWord]= new char[end - start + 1];
				System.arraycopy(array, start, split[currentWord++], 0, end - start + 1);
				last= i + 1;
			}
		}
		int start= last, end= length - 1;
		while (start < length && array[start] == ' ')
			start++;
		while (end > start && array[end] == ' ')
			end--;
		split[currentWord]= new char[end - start + 1];
		System.arraycopy(array, start, split[currentWord++], 0, end - start + 1);
		return split;
	}

	/**
	 * Answers a new array which is a copy of the given array starting at the
	 * given start and ending at the given end. The given start is inclusive
	 * and the given end is exclusive. Answers null if start is greater than
	 * end, if start is lower than 0 or if end is greater than the length of
	 * the given array. If end equals -1, it is converted to the array length.
	 * <br><br>For example:
	 * <ol>
	 * <li>
	 * 
	 * <pre>
	 *  array = { 'a' , 'b' } start = 0 end = 1 result => { 'a' }
	 * </pre>
	 * 
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 *  array = { 'a', 'b' } start = 0 end = -1 result => { 'a' , 'b' }
	 * </pre>
	 * 
	 * 
	 * </li>
	 * </ol>
	 * 
	 * @param array
	 *            the given array
	 * @param start
	 *            the given starting index
	 * @param end
	 *            the given ending index
	 * @return a new array which is a copy of the given array starting at the
	 *         given start and ending at the given end
	 * @exception NullPointerException
	 *                if the given array is null
	 */
	private char[] subarray(char[] array, int start, int end) {
		if (end == -1)
			end= array.length;
		if (start > end)
			return null;
		if (start < 0)
			return null;
		if (end > array.length)
			return null;
		char[] result= new char[end - start];
		System.arraycopy(array, start, result, 0, end - start);
		return result;
	}
}
