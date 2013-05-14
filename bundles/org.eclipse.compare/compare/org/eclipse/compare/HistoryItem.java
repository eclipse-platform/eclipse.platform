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

import java.io.InputStream;
import java.io.BufferedInputStream;

import org.eclipse.swt.graphics.Image;

import org.eclipse.compare.IResourceProvider;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * A combination <code>IFileState</code> and <code>ITypedElement</code> that can be used as
 * an input to a compare viewer or other places where an <code>IStreamContentAccessor</code>
 * is needed.
 * <p>
 * <p>
 * Clients may instantiate this class; it is not intended to be subclassed.
 * </p>
 * @noextend This class is not intended to be subclassed by clients.
 */
public class HistoryItem implements IEncodedStreamContentAccessor, ITypedElement, IModificationDate, IResourceProvider {
	
	private ITypedElement fBase;
	private IFileState fFileState; 

	/**
	 * Creates a <code>HistoryItem</code> object which combines the given <code>IFileState</code>
	 * and <code>ITypedElement</code> into an object
	 * which is suitable as input for a compare viewer or <code>ReplaceWithEditionDialog</code>.
	 *
	 * @param base the implementation of the <code>ITypedElement</code> interface delegates to this base <code>ITypedElement</code>
	 * @param fileState the <code>IFileState</code> from which the streamable contents and the modification time is derived from
	 */
	public HistoryItem(ITypedElement base, IFileState fileState) {
		fBase= base;
		fFileState= fileState;
	}
	
	/* (non-Javadoc)
	 * see ITypedElement.getName
	 */
	public String getName() {
		return fBase.getName();
	}
	
	/* (non-Javadoc)
	 * see ITypedElement.getImage
	 */
	public Image getImage() {
		return fBase.getImage();
	}
	
	/* (non-Javadoc)
	 * see ITypedElement.getType
	 */
	public String getType() {
		return fBase.getType();
	}

	/* (non-Javadoc)
	 * see IModificationDate.getModificationDate
	 */
	public long getModificationDate() {
		return fFileState.getModificationTime();
	}
	
	/* (non-Javadoc)
	 * see IStreamContentAccessor.getContents
	 */
	public InputStream getContents() throws CoreException {
		return new BufferedInputStream(fFileState.getContents());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEncodedStreamContentAccessor#getCharset()
	 */
	public String getCharset() throws CoreException {
	    String charset= fFileState.getCharset(); 
	    if (charset == null) {
		    IResource resource= getResource();
		    if (resource instanceof IEncodedStorage)
		    	charset= ((IEncodedStorage)resource).getCharset();
	    }
		return charset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.internal.IResourceProvider#getResource()
	 */
	public IResource getResource() {
	    IPath fullPath= fFileState.getFullPath();
	    return ResourcesPlugin.getWorkspace().getRoot().findMember(fullPath);
	}
}

