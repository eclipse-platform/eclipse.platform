/*******************************************************************************
 * Copyright (c) 2002, 2012 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.schema;

import java.util.HashMap;

import org.eclipse.ant.internal.ui.dtd.IElement;
import org.eclipse.ant.internal.ui.dtd.ISchema;

/**
 * This is a very simple schema suitable for DTDs.
 * Once constructed, a schema is immutable and could be
 * used by multiple threads. However, since in general
 * the schema will reflect the internal DTD subset,
 * re-use for multiple documents is problematic.
 * @author Bob Foster
 */
public class Schema implements ISchema {
	private HashMap fElementMap = new HashMap();
	private Exception fErrorException;
	/**
	 * @see org.eclipse.ant.internal.ui.dtd.ISchema#getElement(java.lang.String)
	 */
	public IElement getElement(String qname) {
		return (IElement) fElementMap.get(qname);
	}

	/**
	 * @see org.eclipse.ant.internal.ui.dtd.ISchema#getElements()
	 */
	public IElement[] getElements() {
		return (IElement[]) fElementMap.entrySet().toArray(new IElement[fElementMap.entrySet().size()]);
	}
	
	/**
	 * Add a visible element to the schema.
	 * @param element Element to add.
	 */
	public void addElement(IElement element) {
		fElementMap.put(element.getName(), element);
	}
	
	/**
	 * Sets the exception thrown by then parser when the schema was built.
	 * Note that the exception does not necessarily mean the schema is incomplete.
	 * 
	 * @param e the Exception
	 */
	public void setErrorException(Exception e) {
		fErrorException = e;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.ISchema#getErrorException()
	 */
	public Exception getErrorException() {
		return fErrorException;
	}
}
