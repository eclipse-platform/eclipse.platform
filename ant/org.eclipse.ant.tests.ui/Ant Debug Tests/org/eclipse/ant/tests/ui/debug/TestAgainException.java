/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.debug;

/**
 * Exception to indicate a test should be run again when it fails.
 * 
 * @since 3.8
 */
public class TestAgainException extends RuntimeException {

	/**
	 * Generated serial version id
	 */
	private static final long serialVersionUID = -7743450644051812955L;

	/**
	 * Constructor
	 * @param string
	 */
	public TestAgainException(String string) {
		super(string);
	}


}