/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IEditorInput;

/**
 * A buffered resource node with the following characteristics:
 * <ul>
 * <li>Supports the use of file buffers (see {@link ISharedDocumentAdapter}).
 * <li>Does not support file systems hierarchies (i.e. should not be used to
 * represent a folder).
 * <li>Does not allow editing when the file does not exist (see
 * {@link #isEditable()}).
 * <li>Tracks whether the file has been changed on disk since it was loaded
 * through the element (see {@link #isSynchronized()}).
 * <li>Any buffered contents must either be saved or discarded when the element
 * is no longer needed (see {@link #commit(IProgressMonitor)},
 * {@link #saveDocument(boolean, IProgressMonitor)} and {@link #discardBuffer()}
 * ).
 * </ul>
 * <p>
 * This class may be instantiated.
 * </p>
 *
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 */
public class LocalResourceTypedElement extends ResourceNode implements IAdaptable {

	private boolean fDirty = false;
	private EditableSharedDocumentAdapter sharedDocumentAdapter;
	private long timestamp;
	private boolean exists;
	private boolean useSharedDocument = true;
	private EditableSharedDocumentAdapter.ISharedDocumentAdapterListener sharedDocumentListener;
	private String author;

	/**
	 * Creates an element for the given resource.
	 * @param resource the resource
	 */
	public LocalResourceTypedElement(IResource resource) {
		super(resource);
		exists = resource.exists();
	}

	@Override
	public void setContent(byte[] contents) {
		fDirty = true;
		super.setContent(contents);
	}

	/**
	 * Commits buffered contents to the underlying resource. Note that if the
	 * element has a shared document, the commit will not succeed since the
	 * contents will be buffered in the shared document and will not be pushed
	 * to this element using {@link #setContent(byte[])}. Clients should check
	 * whether the element {@link #isConnected()} and, if it is, they should call
	 * {@link #saveDocument(boolean, IProgressMonitor)} to save the buffered contents to
	 * the underlying resource.
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	public void commit(IProgressMonitor monitor) throws CoreException {
		if (isDirty()) {
			if (isConnected()) {
				saveDocument(true, monitor);
			} else {
				IResource resource = getResource();
				if (resource instanceof IFile) {
					ByteArrayInputStream is = new ByteArrayInputStream(getContent());
					try {
						IFile file = (IFile) resource;
						if (file.exists())
							file.setContents(is, false, true, monitor);
						else
							file.create(is, false, monitor);
						fDirty = false;
					} finally {
						fireContentChanged();
						if (is != null)
							try {
								is.close();
							} catch (IOException ex) {
							}
					}
				}
				updateTimestamp();
			}
		}
	}

	@Override
	public InputStream getContents() throws CoreException {
		if (exists)
			return super.getContents();
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			if (isSharedDocumentsEnable())
				return (T) getSharedDocumentAdapter();
			else
				return null;
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/*
	 * Returned the shared document adapter for this element. If one does not exist
	 * yet, it will be created.
	 */
	private synchronized ISharedDocumentAdapter getSharedDocumentAdapter() {
		if (sharedDocumentAdapter == null)
			sharedDocumentAdapter = new EditableSharedDocumentAdapter(new EditableSharedDocumentAdapter.ISharedDocumentAdapterListener() {
				@Override
				public void handleDocumentConnected() {
					LocalResourceTypedElement.this.updateTimestamp();
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentConnected();
				}
				@Override
				public void handleDocumentFlushed() {
					LocalResourceTypedElement.this.fireContentChanged();
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentFlushed();
				}
				@Override
				public void handleDocumentDeleted() {
					LocalResourceTypedElement.this.update();
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentDeleted();
				}
				@Override
				public void handleDocumentSaved() {
					LocalResourceTypedElement.this.updateTimestamp();
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentSaved();
				}
				@Override
				public void handleDocumentDisconnected() {
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentDisconnected();
				}
			});
		return sharedDocumentAdapter;
	}

	@Override
	public boolean isEditable() {
		// Do not allow non-existent files to be edited
		IResource resource = getResource();
		return resource.getType() == IResource.FILE && exists;
	}

	/**
	 * Return whether the element is connected to a shared document.
	 * When connected, the element can be saved using {@link #saveDocument(boolean, IProgressMonitor)}.
	 * Otherwise, {@link #commit(IProgressMonitor)} should be used to save the buffered contents.
	 * @return whether the element is connected to a shared document
	 */
	public boolean isConnected() {
		return sharedDocumentAdapter != null
				&& sharedDocumentAdapter.isConnected();
	}

	/**
	 * Save the shared document for this element. The save can only be performed
	 * if the element is connected to a shared document. If the element is not
	 * connected, <code>false</code> is returned.
	 * @param overwrite indicates whether overwrite should be performed
	 * 			while saving the given element if necessary
	 * @param monitor a progress monitor
	 * @return whether the save succeeded or not
	 * @throws CoreException
	 */
	public boolean saveDocument(boolean overwrite, IProgressMonitor monitor) throws CoreException {
		if (isConnected()) {
			IEditorInput input = sharedDocumentAdapter.getDocumentKey(this);
			sharedDocumentAdapter.saveDocument(input, overwrite, monitor);
			updateTimestamp();
			return true;
		}
		return false;
	}

	@Override
	protected InputStream createStream() throws CoreException {
		InputStream inputStream = super.createStream();
		updateTimestamp();
		return inputStream;
	}

	/**
	 * Update the cached timestamp of the resource.
	 */
	void updateTimestamp() {
		if (getResource().exists())
			timestamp = getResource().getLocalTimeStamp();
		else
			exists = false;
	}

	/**
	 * Return the cached timestamp of the resource.
	 * @return the cached timestamp of the resource
	 */
	private long getTimestamp() {
		return timestamp;
	}

	@Override
	public int hashCode() {
		return getResource().hashCode();
	}

	/*
	 * Returns <code>true</code> if the other object is of type
	 * <code>LocalResourceTypedElement</code> and their corresponding resources
	 * are identical. The content is not considered.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof LocalResourceTypedElement) {
			LocalResourceTypedElement otherElement = (LocalResourceTypedElement) obj;
			return otherElement.getResource().equals(getResource())
					&& exists == otherElement.exists;
		}
		return false;
	}

	/**
	 * Method called to update the state of this element when the compare input that
	 * contains this element is issuing a change event. It is not necessarily the
	 * case that the {@link #isSynchronized()} will return <code>true</code> after this
	 * call.
	 */
	public void update() {
		exists = getResource().exists();
	}

	/**
	 * Return whether the contents provided by this typed element are in-sync with what is on
	 * disk. This method will return <code>false</code> if the file has been changed
	 * externally since the last time the contents were obtained or saved through this
	 * element.
	 * @return whether the contents provided by this typed element are in-sync with what is on
	 * disk
	 */
	public boolean isSynchronized() {
		long current = getResource().getLocalTimeStamp();
		return current == getTimestamp();
	}

	/**
	 * Return whether the resource of this element existed at the last time the typed
	 * element was updated.
	 * @return whether the resource of this element existed at the last time the typed
	 * element was updated
	 */
	public boolean exists() {
		return exists;
	}

	@Override
	protected void fireContentChanged() {
		super.fireContentChanged();
	}

	/**
	 * Discard of any buffered contents. This must be called
	 * when the local element is no longer needed but is dirty since a
	 * the element will connect to a shared document when a merge viewer
	 * flushes its contents to the element and it must be disconnected or the
	 * buffer will remain.
	 * #see {@link #isDirty()}
	 */
	@Override
	public void discardBuffer() {
		if (sharedDocumentAdapter != null)
			sharedDocumentAdapter.releaseBuffer();
		super.discardBuffer();
	}

	/**
	 * Return whether this element can use a shared document.
	 * @return whether this element can use a shared document
	 */
	public boolean isSharedDocumentsEnable() {
		return useSharedDocument && getResource().getType() == IResource.FILE && exists;
	}

	/**
	 * Set whether this element can use shared documents. The enablement
	 * will only apply to files (i.e. shared documents never apply to folders).
	 * @param enablement whether this element can use shared documents
	 */
	public void enableSharedDocument(boolean enablement) {
		this.useSharedDocument = enablement;
	}

	/**
	 * Return whether this element is dirty. The element is
	 * dirty if a merge viewer has flushed it's contents
	 * to the element and the contents have not been saved.
	 * @return whether this element is dirty
	 * @see #commit(IProgressMonitor)
	 * @see #saveDocument(boolean, IProgressMonitor)
	 * @see #discardBuffer()
	 */
	public boolean isDirty() {
		return fDirty || (sharedDocumentAdapter != null && sharedDocumentAdapter.hasBufferedContents());
	}

	public void setSharedDocumentListener(
			EditableSharedDocumentAdapter.ISharedDocumentAdapterListener sharedDocumentListener) {
		this.sharedDocumentListener = sharedDocumentListener;
	}

	/**
	 * Returns the author of the workspace file revision if any.
	 *
	 * @return the author or <code>null</code> if the author has not been fetched or is not
	 *         available
	 * @since 3.7
	 * @see #fetchAuthor(IProgressMonitor)
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Fetches the author from the repository.
	 *
	 * @param monitor the progress monitor
	 * @throws CoreException if fetching the revision properties fails
	 * @since 3.7
	 */
	public void fetchAuthor(IProgressMonitor monitor) throws CoreException {
		author = null;

		IFileHistoryProvider fileHistoryProvider= Utils.getHistoryProvider(getResource());
		if (fileHistoryProvider == null)
			return;

		IFileRevision revision= fileHistoryProvider.getWorkspaceFileRevision(getResource());
		if (revision == null)
			return;

		author = revision.getAuthor();

		if (author == null && revision.isPropertyMissing()) {
			IFileRevision other = revision.withAllProperties(monitor);
			author = other.getAuthor();
		}
	}

	/**
	 * Sets the author.
	 *
	 * @param author the author
	 * @since 3.7
	 */
	public void setAuthor(String author) {
		this.author= author;
	}

}
