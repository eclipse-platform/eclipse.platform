/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A buffer for a workspace resource.
 */
public class BufferedResourceNode extends ResourceNode {

	private boolean fDirty= false;
	private IFile fDeleteFile;

	/**
	 * Creates a <code>ResourceNode</code> for the given resource.
	 *
	 * @param resource the resource
	 */
	public BufferedResourceNode(IResource resource) {
		super(resource);
	}

	/*
	 * Returns <code>true</code> if buffer contains uncommitted changes.
	 */
	public boolean isDirty() {
		return fDirty;
	}

	@Override
	protected IStructureComparator createChild(IResource child) {
		return new BufferedResourceNode(child);
	}

	@Override
	public void setContent(byte[] contents) {
		fDirty= true;
		super.setContent(contents);
	}

	/*
	 * Commits buffered contents to resource.
	 */
	public void commit(IProgressMonitor pm) throws CoreException {
		if (fDirty) {
			if (fDeleteFile != null) {
				fDeleteFile.delete(true, true, pm);
				return;
			}
			IResource resource = getResource();
			if (resource instanceof IFile file) {
				byte[] bytes = getContent();
				file.write(bytes, false, false, true, pm);
				fDirty = false;
			}
		}
	}

	@Override
	public ITypedElement replace(ITypedElement child, ITypedElement other) {

		if (child == null) {	// add resource
			// create a node without a resource behind it!
			IResource resource= getResource();
			if (resource instanceof IFolder folder) {
				IFile file= folder.getFile(other.getName());
				child= new BufferedResourceNode(file);
			}
		}

		if (other == null) {	// delete resource
			IResource resource= getResource();
			if (resource instanceof IFolder folder) {
				IFile file= folder.getFile(child.getName());
				if (file != null && file.exists()) {
					fDeleteFile= file;
					fDirty= true;
				}
			}
			return null;
		}

		if (other instanceof IStreamContentAccessor && child instanceof IEditableContent dst) {
			try (InputStream is = ((IStreamContentAccessor) other).getContents()) {
				byte[] bytes = is.readAllBytes();
				if (bytes != null) {
					dst.setContent(bytes);
				}
			} catch (CoreException | IOException ex) {
				// NeedWork
			}
		}
		return child;
	}
}

