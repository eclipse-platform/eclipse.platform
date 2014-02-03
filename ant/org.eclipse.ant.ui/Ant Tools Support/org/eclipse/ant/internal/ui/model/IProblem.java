/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.model;

import org.eclipse.jface.text.IRegion;


public interface IProblem extends IRegion {

	/**
	 * Answer a localized, human-readable message string which describes the problem.
	 * The message has been "escaped" to handle special characters.
	 *
	 * @return a localized, human-readable message string which describes the problem
	 */
	String getMessage();
	
	/**
	 * Answer a localized, human-readable message string which describes the problem.
	 * The message is in its original form; special characters have not been escaped.
	 *
	 * @return localized, human-readable message string which describes the problem
	 */
	String getUnmodifiedMessage();

	/**
	 * Checks the severity to see if the Error bit is set.
	 *
	 * @return true if the Error bit is set for the severity, false otherwise
	 */
	boolean isError();

	/**
	 * Checks the severity to see if the Error bit is not set.
	 *
	 * @return true if the Error bit is not set for the severity, false otherwise
	 */
	boolean isWarning();
	
	/**
	 * Returns the line number of this problem.
	 *
	 * @return the line number of this problem
	 */
	int getLineNumber();
}
