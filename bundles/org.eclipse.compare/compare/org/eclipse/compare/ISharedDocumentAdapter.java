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

import org.eclipse.compare.structuremergeviewer.SharedDocumentAdapterWrapper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * An <code>ISharedDocumentAdapter</code> is used to map an
 * {@link ITypedElement} to a shared document for the purposes of editing.
 * 
 * @noimplement Clients are not expected to implement this interface but instead
 *              should subclass {@link SharedDocumentAdapter} or
 *              {@link SharedDocumentAdapterWrapper}.
 * @since 3.3
 */
public interface ISharedDocumentAdapter {

	/**
	 * Return the object that is to be used as the key for retrieving the
	 * appropriate {@link IDocumentProvider} from the
	 * <code>DocumentProviderRegistry</code> and for obtaining the shared
	 * {@link IDocument} from the document provider. Returns <code>null</code>
	 * if the element does not have a shared document.
	 * 
	 * @param element
	 *            the element being queried for a shared document
	 * @return the object that acts as the key to obtain a document provider and
	 *         document or <code>null</code>
	 */
	IEditorInput getDocumentKey(Object element);
	
	/**
	 * Connect the given element to its document provider. All connections must be performed
	 * through this adapter so that the adapter can track whether it is connected or not.
	 * @param provider the document provider
	 * @param documentKey the element's key returned from {@link #getDocumentKey(Object)}
	 * @throws CoreException if connection was not possible
	 * @see IDocumentProvider#connect(Object)
	 */
	void connect(IDocumentProvider provider, IEditorInput documentKey) throws CoreException;
	
	/**
	 * Disconnect the element from the document provider. All connects and
	 * disconnects must occur through the adapter so that the adapter can 
	 * track whether it is connected or not.
	 * @param provider the document provider
	 * @param documentKey the element's key returned from {@link #getDocumentKey(Object)}
	 * @see IDocumentProvider#disconnect(Object)
	 */
	void disconnect(IDocumentProvider provider, IEditorInput documentKey);
	
	/**
	 * A helper disconnect method that looks up the appropriate key (using {@link #getDocumentKey(Object)}
	 * and the appropriate provider and calls {@link #disconnect(IDocumentProvider, IEditorInput)}.
	 * @param element the element that was used to previously connect to a document
	 * @see IDocumentProvider#disconnect(Object)
	 */
	void disconnect(Object element);
	
	/**
	 * Flush the contents of the given document into the typed element that provided the
	 * document. This method is invoked by the Compare framework classes
	 * when a request to flush the viewers has been made. It is up to the implementor to decide
	 * whether the changes in the buffer should be saved to disk at the time of the flush or
	 * buffered to be saved at a later time.
	 * 
	 * @param provider the document provider
	 * @param documentKey the element's key returned from {@link #getDocumentKey(Object)}
	 * @param document the document
	 * @param overwrite indicates whether overwrite should be performed
	 * 			while saving the given element if necessary
	 * @exception CoreException if document could not be stored to the given element
	 * @see IDocumentProvider#saveDocument(IProgressMonitor, Object, IDocument, boolean)
	 */
	void flushDocument(IDocumentProvider provider, IEditorInput documentKey, IDocument document, boolean overwrite) throws CoreException;

}
