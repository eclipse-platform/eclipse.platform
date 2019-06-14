/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import java.io.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.ISubscriberChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

/**
 * A QuickDiff provider that provides a reference to the latest revision of a file
 * in the CVS repository. The provider notifies when the file's sync state changes
 * and the diff should be recalculated (e.g. commit, update...) or when the file
 * is changed (e.g. replace with).
 * 
 * Here are the file states and what this provider does for each:
 * 
 * 1. File is unmanaged : reference == empty document
 * 2. Unmanaged file transitions to managed : empty reference updated with new remote revision 
 * 3. A managed file has new remote (commit, refresh remote) : reference updated with new 
 * remote revision
 * 4. A managed file cleaned, remote is the same (replace with, update) : refresh diff bar 
 * with existing reference 
 * 
 * [Note: Currently an empty document must be returned for an unmanaged file. This
 * results in the entire document appearing as outgoing changes in the quickdiff bar. 
 * This is required because the quickdiff support relies on IDocument change events
 * to update the quickdiff, and returning null for the reference document doesn't
 * allow the transition to later return a IDocument.]
 * 
 * @since 3.0
 */
public class RemoteRevisionQuickDiffProvider implements IQuickDiffReferenceProvider {
	// The editor showing this quickdiff and provides access to the editor input and
	// ultimatly the IFile.
	private ITextEditor fEditor = null;	
	
	// The document containing the remote file. Can be null if the assigned editor doesn't have
	// a CVS remote resource associated with it.
	private IDocument fReference = null;
	
	// Will be true when the document has been read and initialized.
	private boolean fReferenceInitialized = false;
	
	// Document provider allows us to register/deregister the element state change listener.
	private IDocumentProvider fDocumentProvider = null;

	// Unique id for this reference provider as set via setId(). 
	private String fId;
	
	// A handle to the remote CVS file for this provider. 
	private SyncInfo fLastSyncState;

	// Job that re-creates the reference document.
	private Job fUpdateJob;
	
	private boolean DEBUG = false;
	
	/**
	 * Updates the document if a sync changes occurs to the associated CVS file.
	 */
	private ISubscriberChangeListener teamChangeListener = new ISubscriberChangeListener() {
		@Override
		public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
			if(fReferenceInitialized) {
				for (ISubscriberChangeEvent delta : deltas) {
					IResource resource = delta.getResource();
					if(resource.getType() == IResource.FILE &&
							fLastSyncState != null && resource.equals(fLastSyncState.getLocal())) {
						if(delta.getFlags() == ISubscriberChangeEvent.SYNC_CHANGED) {
							fetchContentsInJob();
						}
					} 
				}
			}
		}
	};

	/**
	 * Updates the document if the document is changed (e.g. replace with)
	 */
	private IElementStateListener documentListener = new IElementStateListener() {
		@Override
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
		}

		@Override
		public void elementContentAboutToBeReplaced(Object element) {
		}

		@Override
		public void elementContentReplaced(Object element) {
			if(fEditor != null && fEditor.getEditorInput() == element) {
				fetchContentsInJob();
			}
		}

		@Override
		public void elementDeleted(Object element) {
		}

		@Override
		public void elementMoved(Object originalElement, Object movedElement) {
		}
	};

	@Override
	public IDocument getReference(IProgressMonitor monitor) throws CoreException {
		if(! fReferenceInitialized) return null;
		if (fReference == null) {
			readDocument(monitor);
		}
		return fReference;
	}

	@Override
	public void setActiveEditor(ITextEditor targetEditor) {
		IEditorInput editorInput = targetEditor.getEditorInput();
		if (editorInput == null || ResourceUtil.getFile(editorInput) == null) return;
		fEditor = targetEditor;
		fDocumentProvider= fEditor.getDocumentProvider();
		
		if(fDocumentProvider != null) {
			CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().addListener(teamChangeListener);
			fDocumentProvider.addElementStateListener(documentListener);
		}	
		fReferenceInitialized= true;
	}

	@Override
	public boolean isEnabled() {
		if (! fReferenceInitialized)
			return false;
		try {
			return getManagedCVSFile() != null;
		} catch (CVSException e) {
			return false;
		}
	}

	@Override
	public void dispose() {
		fReferenceInitialized = false;
		// stop update job
		if(fUpdateJob != null && fUpdateJob.getState() != Job.NONE) {
			fUpdateJob.cancel();
		}
		
		// remove listeners
		if(fDocumentProvider != null) {
			fDocumentProvider.removeElementStateListener(documentListener);
		}
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().removeListener(teamChangeListener);				
	}

	@Override
	public void setId(String id) {
		fId= id;
	}

	@Override
	public String getId() {
		return fId;
	}
	
	/**
	 * Determine if the file represented by this quickdiff provider has changed with
	 * respect to it's remote state. Return true if the remote contents should be
	 * refreshed, and false if not.
	 */
	private boolean computeChange(IProgressMonitor monitor) throws TeamException {
		boolean needToUpdateReferenceDocument = false;
		if(fReferenceInitialized) {
			SyncInfo info = getSyncState(getFileFromEditor());	
			if(info == null && fLastSyncState != null) {
				return true;
			} else if(info == null) {
				return false;
			}
					
			if(fLastSyncState == null) {
				needToUpdateReferenceDocument = true;
			} else if(! fLastSyncState.equals(info)) {
				needToUpdateReferenceDocument = true; 
			}
			if(DEBUG) debug(fLastSyncState, info);
			fLastSyncState = info;
		}		
		return needToUpdateReferenceDocument;		
	}
	
	private void debug(SyncInfo lastSyncState, SyncInfo info) {
		String last = "[none]"; //$NON-NLS-1$
		if(lastSyncState != null) {
			last = lastSyncState.toString();
		}
		System.out.println("+ CVSQuickDiff: was " + last + " is " + info.toString()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private SyncInfo getSyncState(IResource resource) throws TeamException {
		if (resource == null) return null;
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().getSyncInfo(resource);
	}
	
	/**
	 * Creates a document and initializes it with the contents of a CVS remote
	 * resource.
	 * @param monitor the progress monitor
	 * @throws CoreException
	 */
	private void readDocument(IProgressMonitor monitor) throws CoreException {
		if(! fReferenceInitialized) return;
		if(fReference == null)
			fReference = new Document();
		if(computeChange(monitor)) {
			ICVSRemoteFile remoteFile= (ICVSRemoteFile)fLastSyncState.getBase();
			if (remoteFile != null && fDocumentProvider instanceof IStorageDocumentProvider) {
				IStorageDocumentProvider provider= (IStorageDocumentProvider) fDocumentProvider;			
				String encoding= provider.getEncoding(fEditor.getEditorInput());
				if (encoding == null) {
					encoding= provider.getDefaultEncoding();
				}
				if(monitor.isCanceled()) return;
				InputStream stream= remoteFile.getContents(monitor);
				if (stream == null || monitor.isCanceled() || ! fReferenceInitialized) {
					return;
				}
				setDocumentContent(fReference, stream, encoding);
			} else {
				// the remote is null, so ensure that the document is null
				if(monitor.isCanceled()) return;
				fReference.set(""); //$NON-NLS-1$
			}
			if(DEBUG) System.out.println("+ CVSQuickDiff: updating document " + (fReference!=null ? "remote found" : "remote empty")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	/**
	 * Intitializes the given document with the given stream using the given encoding.
	 *
	 * @param document the document to be initialized
	 * @param contentStream the stream which delivers the document content
	 * @param encoding the character encoding for reading the given stream
	 * @exception CoreException if the given stream can not be read
	 */
	private static void setDocumentContent(IDocument document, InputStream contentStream, String encoding) throws CoreException {
		Reader in= null;
		try {
			final int DEFAULT_FILE_SIZE= 15 * 1024;

			in= new BufferedReader(new InputStreamReader(contentStream, encoding), DEFAULT_FILE_SIZE);
			CharArrayWriter caw= new CharArrayWriter(DEFAULT_FILE_SIZE);
			char[] readBuffer= new char[2048];
			int n= in.read(readBuffer);
			while (n > 0) {
				caw.write(readBuffer, 0, n);
				n= in.read(readBuffer);
			}
			document.set(caw.toString());
			//System.out.println("+ CVSQuickDiff: updating document : " + caw.toString());
		} catch (IOException x) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, CVSUIMessages.RemoteRevisionQuickDiffProvider_readingFile, x);
			throw new CVSException(status); 
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException x) {
					IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, CVSUIMessages.RemoteRevisionQuickDiffProvider_closingFile, x);
					throw new CVSException(status); 
				}
			}
		}
	}
	
	/**
	 * Returns the ICVSFile associated with he active editor or <code>null</code> 
	 * if the provider doesn't not have access to a CVS managed file.
	 * @return the handle to a CVS file
	 */
	private ICVSFile getManagedCVSFile() throws CVSException {
		if(fEditor != null) {
			IFile file = getFileFromEditor();
				if(file != null && CVSWorkspaceRoot.isSharedWithCVS(file)) {
					return CVSWorkspaceRoot.getCVSFileFor(file);
				}
		}
		return null;
	}

	private IFile getFileFromEditor() {
		if(fEditor != null) {
			IEditorInput input= fEditor.getEditorInput();
			if (input != null) {
				IFile file = ResourceUtil.getFile(input);
				return file;
			}
		}
		return null;
	}
	
	/**
	 * Runs a job that updates the document. If a previous job is already running it
	 * is stopped before the new job can start.
	 */
	private void fetchContentsInJob() {
		if(! fReferenceInitialized) return;
		if(fUpdateJob != null && fUpdateJob.getState() != Job.NONE) {
			fUpdateJob.cancel();
		}
		fUpdateJob = new Job(CVSUIMessages.RemoteRevisionQuickDiffProvider_fetchingFile) { 
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					readDocument(monitor);
				} catch (CoreException e) {
					// continue and return ok for now. The error will be reported
					// when the quick diff supports calls getReference() again.
					// continue
				}
				return Status.OK_STATUS;
			}
		};
		fUpdateJob.schedule();
	}
}
