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
package org.eclipse.ant.internal.ui.dtd.util;

/**
 * @author Bob Foster
 */
public class MapHolder extends SortedMapFactory implements IMapHolder {

	public Object[] keys;
	public Object[] values;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.util.IKeyHolder#getKeys()
	 */
	public Object[] getKeys() {
		return keys;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.util.IKeyHolder#setKeys(java.lang.Object[])
	 */
	public void setKeys(Object[] keys) {
		this.keys = keys;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.util.IValueHolder#getValues()
	 */
	public Object[] getValues() {
		return values;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.dtd.util.IValueHolder#setValues(java.lang.Object[])
	 */
	public void setValues(Object[] values) {
		this.values = values;
	}
}
