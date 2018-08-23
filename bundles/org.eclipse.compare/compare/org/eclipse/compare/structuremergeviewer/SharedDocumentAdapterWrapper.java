/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.compare.structuremergeviewer;

import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
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
		return Adapters.adapt(element, ISharedDocumentAdapter.class);
	}

	/**
	 * Creates a shared document adapter that wraps the given adapter.
	 * @param wrappedAdapter the wrapped adapter
	 */
	public SharedDocumentAdapterWrapper(ISharedDocumentAdapter wrappedAdapter) {
		super();
		this.wrappedAdapter = wrappedAdapter;
	}

	@Override
	public void connect(IDocumentProvider provider, IEditorInput documentKey)
			throws CoreException {
		wrappedAdapter.connect(provider, documentKey);
	}

	@Override
	public void disconnect(IDocumentProvider provider, IEditorInput documentKey) {
		wrappedAdapter.disconnect(provider, documentKey);
	}

	@Override
	public IEditorInput getDocumentKey(Object element) {
		return wrappedAdapter.getDocumentKey(element);
	}

	@Override
	public void flushDocument(IDocumentProvider provider,
			IEditorInput documentKey, IDocument document, boolean overwrite) throws CoreException {
		wrappedAdapter.flushDocument(provider, documentKey, document, overwrite);
	}

	/**
	 * Returns the wrapped adapter.
	 * @return the wrapped adapter
	 */
	public final ISharedDocumentAdapter getWrappedAdapter() {
		return wrappedAdapter;
	}

	@Override
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
