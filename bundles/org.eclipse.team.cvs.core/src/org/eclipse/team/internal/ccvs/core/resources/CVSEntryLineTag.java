/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.resources;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;

public class CVSEntryLineTag extends CVSTag {
	
	/*
	 * This is the format of a date as it appears in the entry line. The date in an entry
	 * line is always in GMT.
	 */
	private static final String ENTRY_LINE_DATE_TAG_FORMAT = "yyyy.MM.dd.HH.mm.ss"; //$NON-NLS-1$
	
	/*
	 * This is a formatter that will translate dates to and from text as it appears in the entry line 
	 */
	private static SimpleDateFormat entryLineDateTagFormatter = new SimpleDateFormat(ENTRY_LINE_DATE_TAG_FORMAT, Locale.US);
	
	/*
	 * Convert the tag name as it appears as an argument to a command
	 * into the format that appears in the entry line of a folder or file
	 */
	private static String getNameInInternalFormat(CVSTag tag) {
		if(tag.getType() == DATE){
			String s = ensureEntryLineFormat(tag.getName());
			if(s != null){
				return s;
			}
		}
		return tag.getName();
	}
	
	/*
	 * Helper for converting the tag name as it appears as an argument to a command
	 * into the format that appears in the entry line of a folder or file
	 */
	private static synchronized String ensureEntryLineFormat(String text){
		if(text.length() == ENTRY_LINE_DATE_TAG_FORMAT.length()) return text;
		Date date = tagNameToDate(text);
		if (date == null) return text;
		entryLineDateTagFormatter.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
		return entryLineDateTagFormatter.format(date);
	}
	
	static synchronized public Date entryLineToDate(String text){
		try {
			entryLineDateTagFormatter.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
			return entryLineDateTagFormatter.parse(text);
		} catch (ParseException e) {
			CVSProviderPlugin.log(new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, "Tag name " + text + " is not of the expected format " + ENTRY_LINE_DATE_TAG_FORMAT, e)); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}		
	}
	
	/*
	 * The parameter tag must not be null.
	 */
	public CVSEntryLineTag(CVSTag tag) {
		super(getNameInInternalFormat(tag), tag.getType());
	}
	
	public CVSEntryLineTag(String entryLineTag) {
		switch (entryLineTag.charAt(0)) {
			case 'T' : type = BRANCH; break;
			case 'N' : type = VERSION; break;
			case 'D' : type = DATE; break;
			default: type = HEAD;
		}
		name = entryLineTag.substring(1);
	}
	/*
	 * Returns the tag name
	 */
	public String getName() {
		if (getType() == DATE) {
			// Use same format as CVSTag when the name is requested
			Date date = asDate();
			if(date != null){
				return dateToTagName(date);
			}
		}
		return name;
	}
	/*
	 * Returns the tag type
	 */
	public int getType() {
		return type;
	}
	
	public String toEntryLineFormat(boolean useSamePrefixForBranchAndTag) {
		if (type == BRANCH || (type == VERSION && useSamePrefixForBranchAndTag))
			return "T" + name;//$NON-NLS-1$
		else if (type == VERSION)
			return "N" + name;//$NON-NLS-1$
		else if (type == DATE)
			return "D" + name;//$NON-NLS-1$
		return "";//$NON-NLS-1$
	}

	/*
	 * For debugging purposes.
	 */
	public String toString() {
		return toEntryLineFormat(false);
	}
	
	@Override
	public Date asDate() {
		return entryLineToDate(name);
	}
}

