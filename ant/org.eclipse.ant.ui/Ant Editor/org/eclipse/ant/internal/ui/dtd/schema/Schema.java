/*******************************************************************************
 * Copyright (c) 2002, 2019 Object Factory Inc.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.schema;

import java.util.HashMap;

import org.eclipse.ant.internal.ui.dtd.IElement;
import org.eclipse.ant.internal.ui.dtd.ISchema;

/**
 * This is a very simple schema suitable for DTDs. Once constructed, a schema is immutable and could be used by multiple threads. However, since in
 * general the schema will reflect the internal DTD subset, re-use for multiple documents is problematic.
 * 
 * @author Bob Foster
 */
public class Schema implements ISchema {
	private HashMap<String, IElement> fElementMap = new HashMap<>();
	private Exception fErrorException;

	@Override
	public IElement getElement(String qname) {
		if (fErrorException != null) {
			throw new RuntimeException(fErrorException);
		}
		return fElementMap.get(qname);
	}

	@Override
	public IElement[] getElements() {
		if (fErrorException != null) {
			throw new RuntimeException(fErrorException);
		}
		return fElementMap.values().toArray(new IElement[fElementMap.size()]);
	}

	/**
	 * Add a visible element to the schema.
	 * 
	 * @param element
	 *            Element to add.
	 */
	public void addElement(IElement element) {
		fElementMap.put(element.getName(), element);
	}

	/**
	 * Sets the exception thrown by then parser when the schema was built. Note that the exception does not necessarily mean the schema is incomplete.
	 * 
	 * @param e
	 *            the Exception
	 */
	public void setErrorException(Exception e) {
		fErrorException = e;
	}

	@Override
	public Exception getErrorException() {
		return fErrorException;
	}
}
