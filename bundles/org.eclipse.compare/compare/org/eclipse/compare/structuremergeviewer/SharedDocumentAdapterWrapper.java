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
package org.eclipse.compare.structuremergeviewer;

import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * An implementation of {@link ISharedDocumentAdapter} that wraps another
 * shared document adapter.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.3
 */
public class SharedDocumentAdapterWrapper implements ISharedDocumentAdapter {

	private ISharedDocumentAdapter wrappedAdapter;
	
	/**
	 * Helper method that returns the shared document adapter for the
	 * given typed element or <code>null</code> if there isn't one.
	 * @param element the typed element
	 * @return the shared document adapter for the given typed element 
	 *    or <code>null</code>
	 */
	public static ISharedDocumentAdapter getAdapter(Object element) {
		return (ISharedDocumentAdapter)Utilities.getAdapter(element, ISharedDocumentAdapter.class, true);
	}
	
	/**
	 * Create a shared document adapter that wraps the given adapter.
	 * @param wrappedAdapter the wrapped adapter
	 */
	public SharedDocumentAdapterWrapper(ISharedDocumentAdapter wrappedAdapter) {
		super();
		this.wrappedAdapter = wrappedAdapter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ISharedDocumentAdapter#connect(org.eclipse.ui.texteditor.IDocumentProvider, org.eclipse.ui.IEditorInput)
	 */
	public void connect(IDocumentProvider provider, IEditorInput documentKey)
			throws CoreException {
		wrappedAdapter.connect(provider, documentKey);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ISharedDocumentAdapter#disconnect(org.eclipse.ui.texteditor.IDocumentProvider, org.eclipse.ui.IEditorInput)
	 */
	public void disconnect(IDocumentProvider provider, IEditorInput documentKey) {
		wrappedAdapter.disconnect(provider, documentKey);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ISharedDocumentAdapter#getDocumentKey(java.lang.Object)
	 */
	public IEditorInput getDocumentKey(Object element) {
		return wrappedAdapter.getDocumentKey(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ISharedDocumentAdapter#saveDocument(org.eclipse.ui.texteditor.IDocumentProvider, org.eclipse.ui.IEditorInput, org.eclipse.jface.text.IDocument, boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void flushDocument(IDocumentProvider provider,
			IEditorInput documentKey, IDocument document, boolean overwrite) throws CoreException {
		wrappedAdapter.flushDocument(provider, documentKey, document, overwrite);
	}

	/**
	 * Return the wrapped adapter.
	 * @return the wrapped adapter
	 */
	public final ISharedDocumentAdapter getWrappedAdapter() {
		return wrappedAdapter;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ISharedDocumentAdapter#disconnect(java.lang.Object)
	 */
	public void disconnect(Object element) {
		IEditorInput input = getDocumentKey(element);
		if (input == null)
			return;
		IDocumentProvider provider = SharedDocumentAdapter.getDocumentProvider(input);
		if (provider == null)
			return;
		disconnect(provider, input);
	}

}
