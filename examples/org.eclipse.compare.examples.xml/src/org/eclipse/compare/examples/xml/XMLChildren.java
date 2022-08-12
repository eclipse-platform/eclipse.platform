/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml;

import java.util.HashMap;

import org.eclipse.jface.text.IDocument;

/** XMLNode that has children elements */
public class XMLChildren extends XMLNode {
	
	public int children;	// counts the number of children
	public HashMap<String,Integer> childElements;	// maps the name of XML child elements to their # of occurence
	
	public XMLChildren(String XMLType, String id, String value, String signature, IDocument doc, int start, int length) {
		super(XMLType, id, value, signature, doc, start, length);
		children= 0;
		childElements = new HashMap<>();
	}
}

