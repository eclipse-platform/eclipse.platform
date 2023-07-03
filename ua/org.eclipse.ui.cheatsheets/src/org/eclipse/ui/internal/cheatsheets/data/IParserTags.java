/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 559885
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.data;

/**
 * Interface containing the constants used by the cheatsheet parser
 * to identify the tags used in the cheatsheet file.
 */
public interface IParserTags {

	/*
	 * <!ELEMENT cheatsheet (intro, item+)>
	 * <!ATTLIST cheatsheet
	 *   title               CDATA #REQUIRED
	 * >
	 */
	public static final String CHEATSHEET = "cheatsheet"; //$NON-NLS-1$
	public static final String TITLE = "title"; //$NON-NLS-1$

	/*
	 * <!ELEMENT intro (description)>
	 * <!ATTLIST intro
	 *   contextId           CDATA #IMPLIED
	 *   href                CDATA #IMPLIED
	 * >
	 */
	public static final String INTRO = "intro"; //$NON-NLS-1$
	public static final String CONTEXTID = "contextId"; //$NON-NLS-1$
	public static final String HREF = "href"; //$NON-NLS-1$

	/*
	 * <!ELEMENT description ()>
	 * <!ATTLIST description
	 * >
	 */
	public static final String DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String BOLD = "b"; //$NON-NLS-1$
	public static final String BREAK = "br"; //$NON-NLS-1$
	public static final String HYPERLINK = "a"; //$NON-NLS-1$
	public static final String BOLD_START_TAG = "<b>"; //$NON-NLS-1$
	public static final String BOLD_END_TAG = "</b>"; //$NON-NLS-1$
	public static final String BREAK_TAG = "<br/>"; //$NON-NLS-1$
	public static final String HYPERLINK_START_TAG = "<a"; //$NON-NLS-1$
	public static final String HYPERLINK_PLACEHOLDER = "<a href=\"{0}\">"; //$NON-NLS-1$
	public static final String HYPERLINK_END_TAG = "</a>"; //$NON-NLS-1$
	public static final String FORM_START_TAG = "<form><p>"; //$NON-NLS-1$
	public static final String FORM_END_TAG = "</p></form>"; //$NON-NLS-1$


	/*
	 * <!ELEMENT item (description [action|perform-when] | (subitem|repeated-subitem|conditional-subitem)*)>
	 * <!ATTLIST item
	 *   title               CDATA #REQUIRED
	 *   dialog              ("true" | "false") "false"
	 *   skip                ("true" | "false") "false"
	 *   contextId           CDATA #IMPLIED
	 *   href                CDATA #IMPLIED
	 * >
	 */
	public static final String ITEM = "item"; //$NON-NLS-1$
	public static final String DIALOG = "dialog"; //$NON-NLS-1$
	public static final String SKIP = "skip"; //$NON-NLS-1$

	/*
	 * <!ELEMENT subitem ( [action|perform-when] )>
	 * <!ATTLIST subitem
	 *   label               CDATA #REQUIRED
	 *   skip                ("true" | "false") "false"
	 *   when                CDATA #IMPLIED
	 * >
	 */
	public static final String SUBITEM = "subitem"; //$NON-NLS-1$
	public static final String LABEL = "label"; //$NON-NLS-1$
	public static final String WHEN = "when"; //$NON-NLS-1$

	/*
	 * <!ELEMENT conditional-subitem (subitem+)>
	 * <!ATTLIST conditional-subitem
	 *   condition               CDATA #REQUIRED
	 * >
	 */
	public static final String CONDITIONALSUBITEM = "conditional-subitem"; //$NON-NLS-1$
	public static final String CONDITION = "condition"; //$NON-NLS-1$

	/*
	 * <!ELEMENT repeated-subitem (subitem)>
	 * <!ATTLIST repeated-subitem
	 *   values               CDATA #REQUIRED
	 * >
	 */
	public static final String REPEATEDSUBITM = "repeated-subitem"; //$NON-NLS-1$
	public static final String VALUES = "values"; //$NON-NLS-1$

	/*
	 * <!ELEMENT action EMPTY>
	 * <!ATTLIST action
	 *   class               CDATA #REQUIRED
	 *   pluginId            CDATA #REQUIRED
	 *   param1              CDATA #IMPLIED
	 *   ...
	 *   param9              CDATA #IMPLIED
	 *   confirm             ("true" | "false") "false"
	 *   when                CDATA #IMPLIED
	 * >
	 */
	public static final String ACTION = "action"; //$NON-NLS-1$
	public static final String CLASS = "class"; //$NON-NLS-1$
	public static final String PLUGINID = "pluginId"; //$NON-NLS-1$
	public static final String PARAM = "param"; //$NON-NLS-1$
	public static final String CONFIRM = "confirm"; //$NON-NLS-1$

	/*
	 * <!ELEMENT perform-when (action+)>
	 * <!ATTLIST perform-when
	 *   condition               CDATA #REQUIRED
	 * >
	 */
	public static final String PERFORMWHEN = "perform-when"; //$NON-NLS-1$

	// Miscellaneous tags
	public static final String COMMAND = "command"; //$NON-NLS-1$
	public static final String SERIALIZATION = "serialization"; //$NON-NLS-1$
	public static final String RETURNS = "returns"; //$NON-NLS-1$
	public static final String ON_COMPLETION = "onCompletion"; //$NON-NLS-1$
	public static final String CONTENT_URL = "contentURL"; //$NON-NLS-1$
	public static final String REQUIRED = "required"; //$NON-NLS-1$
	public static final String TRANSLATE = "translate"; //$NON-NLS-1$

	// Constants for re-escaping XML characters
	public static final String LESS_THAN = "&lt;"; //$NON-NLS-1$
	public static final String GREATER_THAN = "&gt;"; //$NON-NLS-1$
	public static final String AMPERSAND = "&amp;"; //$NON-NLS-1$
	public static final String APOSTROPHE = "&apos;"; //$NON-NLS-1$
	public static final String QUOTE = "&quot;"; //$NON-NLS-1$


	// Cheat Sheet state data key
	public static final String ID = "id";//$NON-NLS-1$
	public static final String MANAGERDATA = "CSMData";//$NON-NLS-1$
	public static final String MANAGERDATAKEY = "key"; //$NON-NLS-1$
	public static final String MANAGERDATAVALUE = "value"; //$NON-NLS-1$
	public static final String COMPLETED ="completed"; //$NON-NLS-1$
	public static final String CURRENT = "current"; //$NON-NLS-1$
	public static final String SUBITEMCOMPLETED ="subitemcompleted"; //$NON-NLS-1$
	public static final String SUBITEMSKIPPED ="subitemskipped"; //$NON-NLS-1$
	public static final String EXPANDED = "expanded"; //$NON-NLS-1$
	public static final String EXPANDRESTORE = "expandRestore"; //$NON-NLS-1$
	public static final String BUTTON = "button"; //$NON-NLS-1$
	public static final String BUTTONSTATE = "buttonstate"; //$NON-NLS-1$
	public static final String PATH = "path"; //$NON-NLS-1$
	public static final String CHEATSHEET_STATE_REFERENCE = "CHEATSHEET_STATE_REFERENCE"; //$NON-NLS-1$
	public static final String CHEATSHEET_STATE = "cheatsheetState"; //$NON-NLS-1$

}
