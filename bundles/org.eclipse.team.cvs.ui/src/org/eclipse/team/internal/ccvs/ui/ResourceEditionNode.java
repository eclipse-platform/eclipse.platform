/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;
 
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.history.FileRevisionEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * A class for comparing ICVSRemoteResource objects
 */
public class ResourceEditionNode implements IStructureComparator, ITypedElement, IEncodedStreamContentAccessor, IAdaptable {
	private ICVSRemoteResource resource;
	private ResourceEditionNode[] children;
	private ISharedDocumentAdapter sharedDocumentAdapter;
	private IEditorInput editorInput;

	/**
	 * Creates a new ResourceEditionNode on the given resource edition.
	 */
	public ResourceEditionNode(ICVSRemoteResource resourceEdition) {
		this.resource = resourceEdition;
	}
		
	/**
	 * Returns true if both resources names are identical.
	 * The content is not considered.
	 * @see IComparator#equals
	 */
	public boolean equals(Object other) {
		if (other instanceof ITypedElement) {
			String otherName = ((ITypedElement)other).getName();
			return getName().equals(otherName);
		}
		return super.equals(other);
	}
	
	/**
	 * Enumerate children of this node (if any).
	 */
	public Object[] getChildren() {
		if (children == null) {
			children = new ResourceEditionNode[0];
			if (resource != null) {
				try {
					CVSUIPlugin.runWithProgress(null, true /*cancelable*/, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								ICVSRemoteResource[] members = resource.members(monitor);
								children = new ResourceEditionNode[members.length];
								for (int i = 0; i < members.length; i++) {
									children[i] = new ResourceEditionNode(members[i]);
								}
							} catch (TeamException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
				} catch (InterruptedException e) {
					// operation canceled
				} catch (InvocationTargetException e) {
					Throwable t = e.getTargetException();
					if (t instanceof TeamException) {
						CVSUIPlugin.log(((TeamException) t));
					}
				}
			}
		}
		return children;
	}

	/**
	 * @see IStreamContentAccessor#getContents()
	 */
	public InputStream getContents() throws CoreException {
		IStorage storage = getStorage();
		if (storage != null) {
			return storage.getContents();
		}
		return new ByteArrayInputStream(new byte[0]);
	}
	
	public Image getImage() {
		return CompareUI.getImage(resource);
	}
	
	/**
	 * Returns the name of this node.
	 */
	public String getName() {
		return resource == null ? "" : resource.getName(); //$NON-NLS-1$
	}
	
	public ICVSRemoteResource getRemoteResource() {
		return resource;
	}
	
	/**
	 * Returns the comparison type for this node.
	 */
	public String getType() {
		if (resource == null) {
			return UNKNOWN_TYPE;
		}
		if (resource.isContainer()) {
			return FOLDER_TYPE;
		}
		String name = resource.getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		return name.length() == 0 ? UNKNOWN_TYPE : name;
	}
	
	/**
	 * @see IComparator#equals
	 */
	public int hashCode() {
		return getName().hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEncodedStreamContentAccessor#getCharset()
	 */
	public String getCharset() throws CoreException {
		// Use the local file encoding if there is one
		IResource local = resource.getIResource();
		if (local != null && local.getType() == IResource.FILE) {
			return ((IFile)local).getCharset();
		}
		// See if the remote file has an encoding
		IStorage storage = getStorage();
		if (storage instanceof IEncodedStorage) {
			String charset = ((IEncodedStorage)storage).getCharset();
			if (charset != null) {
				return charset;
			}
		}
		return null;
	}
	
	private IStorage getStorage() throws TeamException {
		if (resource == null) {
			return null;
		}
		final IStorage[] holder = new IStorage[1];
		try {
			CVSUIPlugin.runWithProgress(null, true /*cancelable*/, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						holder[0] = ((IResourceVariant)resource).getStorage(monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			throw TeamException.asTeamException(e);
		} catch (InterruptedException e) {
			// Shouldn't happen. Ignore
		}
		return holder[0];
	}

	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			synchronized (this) {
				if (sharedDocumentAdapter == null)
					sharedDocumentAdapter = new SharedDocumentAdapter() {
						public IEditorInput getDocumentKey(Object element) {
							return ResourceEditionNode.this
									.getDocumentKey(element);
						}

						public void flushDocument(IDocumentProvider provider,
								IEditorInput documentKey, IDocument document,
								boolean overwrite) throws CoreException {
							// The document is read-only
						}
					};
				return adapter.cast(sharedDocumentAdapter);
			}
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	private IEditorInput getDocumentKey(Object element) {
		try {
			if (element == this && getStorage() != null) {
				if (editorInput == null) {
				editorInput = new FileRevisionEditorInput(resource
							.getAdapter(IFileRevision.class), getStorage());
				}
				return editorInput;
			}
		} catch (TeamException e) {
			TeamUIPlugin.log(e);
		}
		return null;
	}
}
