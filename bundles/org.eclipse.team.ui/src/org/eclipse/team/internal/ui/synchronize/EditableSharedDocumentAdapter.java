/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;

/**
 * A shared document adapter that tracks whether the element is connected to a
 * shared document and whether the contents have been flushed from a compare
 * viewer. When contents are flushed, this adapter will connect to the document
 * provider to ensure that the changes are not lost (see
 * {@link #hasBufferedContents()}). In order to avoid a leak, the buffer must
 * either be saved (see
 * {@link #saveDocument(IEditorInput, boolean, IProgressMonitor)}) or released
 * (see {@link #releaseBuffer()}).
 * <p>
 * This adapter must have a one-to-one correspondence to a typed element.
 */
public class EditableSharedDocumentAdapter extends
		SharedDocumentAdapter implements IElementStateListener {

	private int connectionCount;
	private final ISharedDocumentAdapterListener listener;
	private IEditorInput bufferedKey;

	/**
	 * Interface that provides this adapter with the state of the typed element
	 * and supports call backs to the element when the adapter state changes.
	 */
	public interface ISharedDocumentAdapterListener {

		/**
		 * Method that is invoked when the adapter connects to the
		 * document provider. This method is only invoked when the
		 * adapter first connects to the document.
		 */
		void handleDocumentConnected();

		/**
		 * Method that is invoked when the adapter disconnects from the
		 * document provider. This method is only invoked when the
		 * adapter no longer has any connection to the document provider.
		 */
		void handleDocumentDisconnected();

		/**
		 * Method invoked when changes in the document are flushed to the adapter.
		 */
		void handleDocumentFlushed();

		/**
		 * Method invoked when the file behind the shared document is deleted.
		 */
		void handleDocumentDeleted();

		/**
		 * Method invoked when the document dirty state changes from dirty to clean.
		 */
		void handleDocumentSaved();
	}

	/**
	 * Create the shared document adapter for the given element.
	 * @param listener access to element internals
	 */
	public EditableSharedDocumentAdapter(ISharedDocumentAdapterListener listener) {
		super();
		this.listener = listener;
	}

	@Override
	public void connect(IDocumentProvider provider, IEditorInput documentKey)
			throws CoreException {
		super.connect(provider, documentKey);
		connectionCount++;
		if (connectionCount == 1) {
			provider.addElementStateListener(this);
			listener.handleDocumentConnected();
		}
	}

	@Override
	public void disconnect(IDocumentProvider provider,
			IEditorInput documentKey) {
		try {
			super.disconnect(provider, documentKey);
		} finally {
			if (connectionCount > 0)
				connectionCount--;
			if (connectionCount == 0) {
				provider.removeElementStateListener(this);
				listener.handleDocumentDisconnected();
			}
		}
	}

	/**
	 * Return whether the element is connected to a shared document.
	 * @return whether the element is connected to a shared document
	 */
	public boolean isConnected() {
		return connectionCount > 0;
	}

	/**
	 * Save the shared document of the element of this adapter.
	 * @param input the document key of the element.
	 * @param overwrite indicates whether overwrite should be performed
	 * 			while saving the given element if necessary
	 * @param monitor a progress monitor
	 * @return whether the save succeeded or not
	 * @throws CoreException
	 */
	public boolean saveDocument(IEditorInput input, boolean overwrite, IProgressMonitor monitor) throws CoreException {
		if (isConnected()) {
			IDocumentProvider provider = SharedDocumentAdapter.getDocumentProvider(input);
			try {
				saveDocument(provider, input, provider.getDocument(input), overwrite, monitor);
			} finally {
				// When we write the document, remove out hold on the buffer
				releaseBuffer();
			}
			return true;
		}
		return false;
	}

	/**
	 * Release the buffer if this adapter has buffered the contents in response to
	 * a {@link #flushDocument(IDocumentProvider, IEditorInput, IDocument, boolean)}.
	 */
	public void releaseBuffer() {
		if (bufferedKey != null) {
			IDocumentProvider provider = SharedDocumentAdapter.getDocumentProvider(bufferedKey);
			provider.disconnect(bufferedKey);
			bufferedKey = null;
		}
	}

	@Override
	public void flushDocument(IDocumentProvider provider,
			IEditorInput documentKey, IDocument document,
			boolean overwrite)
			throws CoreException {
		if (!hasBufferedContents()) {
			// On a flush, make an extra connection to the shared document so it will be kept even
			// if it is no longer being viewed.
			bufferedKey = documentKey;
			provider.connect(bufferedKey);
		}
		this.listener.handleDocumentFlushed();
	}

	@Override
	public void elementContentAboutToBeReplaced(Object element) {
		// Nothing to do
	}

	@Override
	public void elementContentReplaced(Object element) {
		// Nothing to do
	}

	@Override
	public void elementDeleted(Object element) {
		listener.handleDocumentDeleted();
	}

	@Override
	public void elementDirtyStateChanged(Object element, boolean isDirty) {
		if (!isDirty) {
			this.listener.handleDocumentSaved();
		}
	}

	@Override
	public void elementMoved(Object originalElement, Object movedElement) {
		// Nothing to do
	}

	/**
	 * Return whether the adapter has buffered contents. The adapter
	 * buffers contents by connecting to the document through the document
	 * provider. This means that the adapter must be disconnected either by saving
	 * or discarding the buffer.
	 * @return whether the adapter has buffered contents
	 */
	public boolean hasBufferedContents() {
		return bufferedKey != null;
	}
}