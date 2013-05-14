/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;

/**
 * Extends the {@link IEditableContent} interface to support validate edit.
 * Clients should only use this interface if they obtained the content
 * from an {@link IStreamContentAccessor}. If content was obtained through an
 * {@link ISharedDocumentAdapter} then validation should be performed through
 * the {@link IDocumentProviderExtension} interface.
 * @see IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], Object)
 * @since 3.3
 */
public interface IEditableContentExtension {
	
	/**
	 * Return whether the typed element being displayed
	 * is read-only. a read-only element will require a
	 * call to validateEdit before the element can be modified on disk.
	 * @return whether the typed element is read-only
	 */
	boolean isReadOnly();
	
	/**
	 * If the element is read-only, this method should be called
	 * to attempt to make it writable.
	 * @param shell a shell used to prompt the user if required.
	 * @return a status object that is <code>OK</code> if things are fine, 
	 * otherwise a status describing reasons why modifying the given files is not 
	 * reasonable. A status with a severity of <code>CANCEL</code> is returned
	 * if the validation was canceled, indicating the edit should not proceed.
	 */
	IStatus validateEdit(Shell shell);

}
