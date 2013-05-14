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

import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * An implementation of {@link ISharedDocumentAdapter} that provides default behavior for the
 * methods of that interface.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.3
 */
public abstract class SharedDocumentAdapter implements ISharedDocumentAdapter {

	/**
	 * Return the document provider for the given editor input.
	 * @param input the editor input
	 * @return the document provider for the given editor input
	 */
	public static IDocumentProvider getDocumentProvider(IEditorInput input) {
		return DocumentProviderRegistry.getDefault().getDocumentProvider(input);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ISharedDocumentAdapter#connect(org.eclipse.ui.texteditor.IDocumentProvider, org.eclipse.ui.IEditorInput)
	 */
	public void connect(IDocumentProvider provider, IEditorInput documentKey)
			throws CoreException {
		provider.connect(documentKey);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ISharedDocumentAdapter#disconnect(org.eclipse.ui.texteditor.IDocumentProvider, org.eclipse.ui.IEditorInput)
	 */
	public void disconnect(IDocumentProvider provider, IEditorInput documentKey) {
		provider.disconnect(documentKey);
	}

	/**
	 * Default implementation of {@link #getDocumentKey(Object)} that returns a 
	 * {@link FileEditorInput} for the element if the element adapts to {@link IFile}.
	 * @see org.eclipse.compare.ISharedDocumentAdapter#getDocumentKey(java.lang.Object)
	 */
	public IEditorInput getDocumentKey(Object element) {
		IFile file = getFile(element);
		if (file != null && file.exists()) {
			return new FileEditorInput(file);
		}
		return null;
	}
	
	private IFile getFile(Object element) {
		if (element instanceof IResourceProvider) {
			IResourceProvider rp = (IResourceProvider) element;
			IResource resource = rp.getResource();
			if (resource instanceof IFile) {
				return (IFile) resource;
			}
		}
		IFile file = (IFile)Utilities.getAdapter(element, IFile.class);
		if (file != null) {
			return file;
		}
		IResource resource = (IResource)Utilities.getAdapter(element, IResource.class);
		if (resource instanceof IFile) {
			return (IFile) resource;
		}
		return null;
	}

	/**
	 * A helper method to save a document.
	 * 
	 * @param provider the document provider
	 * @param documentKey the document key
	 * @param document the document
	 * @param overwrite indicates whether overwrite should be performed
	 * 			while saving the given element if necessary
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	protected void saveDocument(IDocumentProvider provider,
			IEditorInput documentKey, IDocument document, boolean overwrite,
			IProgressMonitor monitor) throws CoreException {
		try {
			provider.aboutToChange(documentKey);
			provider.saveDocument(monitor, documentKey, document, overwrite);
		} finally {
			provider.changed(documentKey);
		}
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
