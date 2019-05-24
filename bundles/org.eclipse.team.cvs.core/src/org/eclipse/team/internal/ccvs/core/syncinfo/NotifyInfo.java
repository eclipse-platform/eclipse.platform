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
package org.eclipse.team.internal.ccvs.core.syncinfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Date;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.CVSDateFormatter;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * This class contains the information required by the server for edit/unedit.
 */
public class NotifyInfo {
	
	// constants for the notifiation type and watches
	public static final char EDIT = 'E';
	public static final char UNEDIT = 'U';
	public static final char COMMIT = 'C';
	public static final char[] ALL = new char[] {EDIT, UNEDIT, COMMIT};
	
	protected static final String TAB_SEPARATOR = "\t"; //$NON-NLS-1$
	
	private String filename;
	private char notificationType;
	private Date timeStamp;
	private char[] watches;
	
	/**
	 * Constructor for setting all variables
	 */
	public NotifyInfo(String filename, char notificationType, Date timeStamp, char[] watches) {
			
		this.filename = filename;
		this.notificationType = notificationType;
		this.timeStamp = timeStamp;
		this.watches = watches;
	}

	/**
	 * Constructor for a line from the CVS/Notify file
	 * @param line
	 */
	public NotifyInfo(IContainer parent, String line) throws CVSException {
		String[] strings = Util.parseIntoSubstrings(line, ResourceSyncInfo.SEPARATOR);
		if(strings.length != 4) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR_LINE, NLS.bind(CVSMessages.NotifyInfo_MalformedLine, new String[] { line }), parent);
			throw new CVSException(status); 
		}
		this.filename = strings[0];
		
		String type = strings[1];
		if (type.length() != 1) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR_LINE, NLS.bind(CVSMessages.NotifyInfo_MalformedNotificationType, new String[] { line }), parent);
			throw new CVSException(status);
		}
		this.notificationType = type.charAt(0);
		
		String date = strings[2];
		try {	
			this.timeStamp = CVSDateFormatter.entryLineToDate(date);
		} catch(ParseException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR_LINE, NLS.bind(CVSMessages.NotifyInfo_MalformedNotifyDate, new String[] { line }), parent);
			throw new CVSException(status);			
		}
		
		String watchesString = strings[3];
		if (watchesString.length() > 0) {
			this.watches = new char[watchesString.length()];
			for (int i = 0; i < watchesString.length(); i++) {
				watches[i] = watchesString.charAt(i);
			}
		} else {
			this.watches = null;
		}
	}
	
	/**
	 * Answer a Sting formatted to be written to the CVS/Notify file.
	 * 
	 * XXX NOTE: This is a guess at the local format. Need to obtain proper format
	 * 
	 * @return String
	 */
	public String getNotifyLine() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(getName());
		buffer.append(ResourceSyncInfo.SEPARATOR);
		buffer.append(notificationType);
		buffer.append(ResourceSyncInfo.SEPARATOR);
		buffer.append(CVSDateFormatter.dateToEntryLine(timeStamp));
		buffer.append(ResourceSyncInfo.SEPARATOR);
		if (watches != null) {
			for (int i = 0; i < watches.length; i++) {
				char c = watches[i];
				buffer.append(c);
			}
		}
		return buffer.toString();
	}

	/**
	 * Answer a Sting formatted to be sent to the server.
	 * 
	 * @return String
	 */
	public String getServerLine(ICVSFolder parent) throws CVSException {
		StringBuilder buffer = new StringBuilder();
		buffer.append(notificationType);
		buffer.append(TAB_SEPARATOR);
		buffer.append(getServerTimestamp());
		buffer.append(TAB_SEPARATOR);
		buffer.append(getHost());
		buffer.append(TAB_SEPARATOR);
		buffer.append(getWorkingDirectory(parent));
		buffer.append(TAB_SEPARATOR);
		if (watches != null) {
			for (int i = 0; i < watches.length; i++) {
				char c = watches[i];
				buffer.append(c);
			}
		}
		return buffer.toString();
	}

	/**
	 * Answer the timestamp in GMT format.
	 * @return String
	 */
	private String getServerTimestamp() {
		return CVSDateFormatter.dateToNotifyServer(timeStamp);
	}

	/**
	 * Answer the working directory for the receiver's file. The format
	 * is NOT device dependant (i.e. /'s are used as the path separator).
	 * 
	 * @return String
	 */
	private String getWorkingDirectory(ICVSFolder parent) throws CVSException {
		return parent.getIResource().getLocation().toString();
	}

	/**
	 * Answer the host name of the client machine.
	 * @return String
	 */
	private String getHost() throws CVSException {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw CVSException.wrapException(e);
		}
	}

	/**
	 * Answer the name of the file associated with the notification
	 * @return String
	 */
	public String getName() {
		return filename;
	}

	/**
	 * Answer the notification type associated with the notification
	 * @return char
	 */
	public char getNotificationType() {
		return notificationType;
	}

}
