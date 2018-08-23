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
 *     CSC - Intial implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;


/**
 * Instances of EditorsInfo represent information for a CVS resurce that results
 * from the cvs editors command.
 * 
 * @author <a href="mailto:gregor.kohlwes@csc.com,kohlwes@gmx.net">Gregor Kohlwes</a>
 */

public class EditorsInfo {
	public EditorsInfo() {
	}
	
	private String userName;
	private String fileName;
	private String dateString;
	private String computerName;
	

	/**
	 * Returns the userName.
	 * @return String
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Sets the userName.
	 * @param userName The userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Returns the dateString.
	 * @return String
	 */
	public String getDateString() {
		return dateString;
	}

	/**
	 * Returns the fileName.
	 * @return String
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the dateString.
	 * @param dateString The dateString to set
	 */
	public void setDateString(String dateString) {
		this.dateString = dateString;
	}

	/**
	 * Sets the fileName.
	 * @param fileName The fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Returns the computerName.
	 * @return String
	 */
	public String getComputerName() {
		return computerName;
	}

	/**
	 * Sets the computerName.
	 * @param computerName The computerName to set
	 */
	public void setComputerName(String computerName) {
		this.computerName = computerName;
	}

}
