/*******************************************************************************
 * Copyright (c) 2002, 2012 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *		IBM Corporation - fix for Bug 40951
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd;

import java.util.Map;

/**
 * IElement interface.
 * @author Bob Foster
 */
public interface IElement extends IAtom {

	/**
	 * @return true if the element has been referenced in a schema but is not
	 * yet defined.
	 */
	public boolean isUndefined();
	
	/**
	 * @return true if element model is <code>"(#PCDATA)"</code>.
	 */
	public boolean isText();
	
	/**
	 * @return true if element model is EMPTY.
	 */
	public boolean isEmpty();
	
	/**
	 * @return true if element model is ANY.
	 */
	public boolean isAny();
	
	/**
	 * @return the element's content model. The content model will be empty
	 * if <code>isText()</code>, <code>isAny()</code> or <code>isEmpty()</code>.
	 * Note that the content model deals with child elements only; use
	 * <code>isMixed()</code> to see if text is also allowed.
	 */
	public IModel getContentModel();
	
	/**
	 * @return the DFM corresponding to the content model. Every element
	 * has a DFM.
	 */
	public IDfm getDfm();
	
	/**
	 * @return Map with attribute qname keys and IAttr values.
	 * If element has no attributes, the map is empty.
	 * Map must not be modified by caller; for performance reasons,
	 * it is not a copy.
	 */
	public Map getAttributes();
}
