/**********************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.ant.internal.ui.dtd;

import org.eclipse.osgi.util.NLS;

public class AntDTDMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.dtd.AntDTDMessages";//$NON-NLS-1$

	public static String Parser_XML_parser_does_not_support_DeclHandler_1;
	public static String Parser_Error_parsing_XML_document_or_DTD_2;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, AntDTDMessages.class);
	}
}