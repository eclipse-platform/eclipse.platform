/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

/**
 * Common interface for objects with editable contents.
 * Typically it is implemented by objects that also implement
 * the <code>IStreamContentAccessor</code> interface.
 * <p>
 * Clients may implement this interface.
 * <p>
 * Note that implementing <code>IEditableContent</code> does not
 * automatically mean that it is editable. An object is only editable if
 * it implements <code>IEditableContent</code> and the <code>isEditable</code> method returns <code>true</code>.
 *
 * @see IStreamContentAccessor
 */
public interface IEditableContent {
	
	/**
	 * Returns <code>true</code> if this object can be modified.
	 * If it returns <code>false</code> the other methods of this API must not be called.
	 * 
	 * @return <code>true</code> if this object can be modified
	 */
	boolean isEditable();
			
	/**
	 * Replaces the current content with the given new bytes.
	 * 
	 * @param newContent this new contents replaces the old contents
	 */
	void setContent(byte[] newContent); 

	/**
	 * This method is called on a parent to add or remove a child,
	 * or to copy the contents of a child.
	 * 
	 * What to do is encoded in the two arguments as follows:
	 * <TABLE>
	 * <TR>
	 * 	<TD>add:</TD>
	 *  <TD>dest == null</TD>
	 *  <TD>src != null</TD>
	 * </TR>
	 * <TR>
	 * 	<TD>remove:</TD>
	 *  <TD>dest != null</TD>
	 *  <TD>src == null</TD>
	 * </TR>
	 * <TR>
	 * 	<TD>copy:</TD>
	 *  <TD>dest != null</TD>
	 *  <TD>src != null</TD>
	 * </TR>
	 * </TABLE>
	 * @param dest the existing child of this object to be replaced; if <code>null</code> a new child can be added.
	 * @param src the new child to be added or replaced; if <code>null</code> an existing child can be removed.
	 * @return the argument <code>dest</code>
	 */
	ITypedElement replace(ITypedElement dest, ITypedElement src);
}
