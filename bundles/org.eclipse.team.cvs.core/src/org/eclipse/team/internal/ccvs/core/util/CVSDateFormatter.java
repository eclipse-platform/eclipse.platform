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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.util;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class for converting timestamps used in Entry file lines. The format
 * required in the Entry file is ISO C asctime() function (Sun Apr  7 01:29:26 1996).
 * <p>
 * To be compatible with asctime(), the day field in the entryline format is
 * padded with a space and not a zero. Most other CVS clients use string comparison 
 * for timestamps based on the result of the C function asctime().
 * </p>
 */
public class CVSDateFormatter {
	
	private static final String ENTRYLINE_FORMAT = "E MMM dd HH:mm:ss yyyy"; //$NON-NLS-1$
	private static final String SERVER_FORMAT = "dd MMM yyyy HH:mm:ss";//$NON-NLS-1$
	private static final int ENTRYLINE_TENS_DAY_OFFSET = 8;
	
	private static final SimpleDateFormat serverFormat = new SimpleDateFormat(SERVER_FORMAT, Locale.US);
	private static SimpleDateFormat entryLineFormat = new SimpleDateFormat(ENTRYLINE_FORMAT, Locale.US);
	
	static {
		entryLineFormat.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
	}
	static synchronized public Date serverStampToDate(String text) throws ParseException {
		serverFormat.setTimeZone(getTimeZone(text));
		Date date = serverFormat.parse(text);
		return date;
	}
	
	static synchronized public Date entryLineToDate(String text) throws ParseException {
		try {
			if (text.charAt(ENTRYLINE_TENS_DAY_OFFSET) == ' ') {
				StringBuilder buf = new StringBuilder(text);
				buf.setCharAt(ENTRYLINE_TENS_DAY_OFFSET, '0');
				text = buf.toString();
			}
		} catch (StringIndexOutOfBoundsException e) {
			throw new ParseException(e.getMessage(), ENTRYLINE_TENS_DAY_OFFSET);
		}
		return entryLineFormat.parse(text);
	}

	static synchronized public String dateToEntryLine(Date date) {
		if (date == null) return ""; //$NON-NLS-1$
		String passOne = entryLineFormat.format(date);
		if (passOne.charAt(ENTRYLINE_TENS_DAY_OFFSET) != '0') return passOne;
		StringBuilder passTwo = new StringBuilder(passOne);
		passTwo.setCharAt(ENTRYLINE_TENS_DAY_OFFSET, ' ');
		return passTwo.toString();
	}
	
	static synchronized public String dateToNotifyServer(Date date) {
		serverFormat.setTimeZone(TimeZone.getTimeZone("GMT"));//$NON-NLS-1$
		return serverFormat.format(date) + " GMT"; //$NON-NLS-1$
	}
	
	/*
	 * Converts timezone text from date string from CVS server and
	 * returns a timezone representing the received timezone.
	 * Timezone string is of the following format: [-|+]MMSS
	 */
	static private TimeZone getTimeZone(String dateFromServer) {
		if (dateFromServer.contains("0000"))  //$NON-NLS-1$
			return TimeZone.getTimeZone("GMT");//$NON-NLS-1$ 
		String tz = null;
		StringBuilder resultTz = new StringBuilder("GMT");//$NON-NLS-1$
		if (dateFromServer.contains("-")) {//$NON-NLS-1$
			resultTz.append("-");//$NON-NLS-1$
			tz = dateFromServer.substring(dateFromServer.indexOf("-"));//$NON-NLS-1$
		} else if (dateFromServer.contains("+")) {//$NON-NLS-1$
			resultTz.append('+');
			tz = dateFromServer.substring(dateFromServer.indexOf("+"));//$NON-NLS-1$
		}
		try {
			if(tz!=null) {
				resultTz.append(tz.substring(1, 3) /*hours*/ + ":" + tz.substring(3, 5) /*minutes*/);//$NON-NLS-1$
				return TimeZone.getTimeZone(resultTz.toString());
			}
		} catch(IndexOutOfBoundsException e) {
			return TimeZone.getTimeZone("GMT");//$NON-NLS-1$
		}
		return TimeZone.getTimeZone("GMT");//$NON-NLS-1$
	}
}
