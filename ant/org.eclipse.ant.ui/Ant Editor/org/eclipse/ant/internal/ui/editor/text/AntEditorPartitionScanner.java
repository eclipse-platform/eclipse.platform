/*******************************************************************************
 * Copyright (c) 2002, 2006 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug 32890, bug 24108, bug 111740
 *     John-Mason P. Shackelford - bug 57379
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

/*
 * This file originates from an internal package of Eclipse's 
 * Manifest Editor. It has been copied by GEBIT to here in order to
 * permanently use those features. It has been renamed and edited by GEBIT 
 * after copying.
 */

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * Scanner that scans the document and partitions the document into the four 
 * supported content types:
 * <ul>
 * <li>XML_COMMENT</li>
 * <li>XML_TAG</li>
 * <li>XML_CDATA</li>
 * <li>XML_DTD</li>
 * </ul>
 */
public class AntEditorPartitionScanner extends RuleBasedPartitionScanner {

	public final static String XML_COMMENT = "__xml_comment"; //$NON-NLS-1$
	public final static String XML_TAG = "__xml_tag"; //$NON-NLS-1$
	public final static String XML_CDATA = "__xml_cdata"; //$NON-NLS-1$
	public final static String XML_DTD = "__xml_dtd"; //$NON-NLS-1$
	
    /**
     * Creates an instance.
     */
	public AntEditorPartitionScanner() {

		IPredicateRule[] rules =new IPredicateRule[4];

        IToken xmlCDATA = new Token(XML_CDATA);
		rules[0]= new MultiLineRule("<![CDATA[", "]]>", xmlCDATA); //$NON-NLS-1$ //$NON-NLS-2$
		
        IToken xmlComment = new Token(XML_COMMENT);
		rules[1]= new MultiLineRule("<!--", "-->", xmlComment, '\\', true); //$NON-NLS-1$ //$NON-NLS-2$

        IToken tag = new Token(XML_TAG);
		rules[2]= new TagRule(tag);
	
		IToken xmlDTD = new Token(XML_DTD);
		rules[3]= new DocTypeRule(xmlDTD);
		
		setPredicateRules(rules);
	}
}