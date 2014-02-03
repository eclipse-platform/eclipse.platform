/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *		IBM Corporation - serial version id
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd;

/**
 * @author Bob Foster
 */
public class ParseError extends Exception {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor for ParseError.
	 * @param msg Message
	 */
	public ParseError(String msg) {
		super(msg);
	}
}
