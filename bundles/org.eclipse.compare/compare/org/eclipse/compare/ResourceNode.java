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
package org.eclipse.compare;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

/**
 * A <code>ResourceNode</code> wraps an <code>IResources</code> so that it can be used
 * as input for the differencing engine (interfaces <code>IStructureComparator</code> and <code>ITypedElement</code>)
 * and the <code>ReplaceWithEditionDialog</code> (interfaces <code>ITypedElement</code> and <code>IModificationDate</code>).
 * <p>
 * Clients may instantiate this class; it is not intended to be subclassed.
 * </p>
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ResourceNode extends BufferedContent
			implements IEncodedStreamContentAccessor, IStructureComparator, ITypedElement,
							IEditableContent, IModificationDate, IResourceProvider, IEditableContentExtension {

	private IResource fResource;
	private ArrayList<Object> fChildren;


	/**
	 * Creates a <code>ResourceNode</code> for the given resource.
	 *
	 * @param resource the resource
	 */
	public ResourceNode(IResource resource) {
		fResource= resource;
		Assert.isNotNull(resource);
	}

	/**
	 * Returns the corresponding resource for this object.
	 *
	 * @return the corresponding resource
	 */
	@Override
	public IResource getResource() {
		return fResource;
	}

	@Override
	public InputStream getContents() throws CoreException {
		if (fResource instanceof IStorage)
			return super.getContents();
		return null;
	}

	@Override
	public long getModificationDate() {
		return fResource.getLocalTimeStamp();
	}

	@Override
	public String getName() {
		if (fResource != null)
			return fResource.getName();
		return null;
	}

	@Override
	public String getType() {
		if (fResource instanceof IContainer)
			return ITypedElement.FOLDER_TYPE;
		if (fResource != null) {
			String s= fResource.getFileExtension();
			if (s != null)
				return s;
		}
		return ITypedElement.UNKNOWN_TYPE;
	}

	@Override
	public Image getImage() {
		return CompareUI.getImage(fResource);
	}

	/*
	 * Returns <code>true</code> if the other object is of type <code>ITypedElement</code>
	 * and their names are identical. The content is not considered.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof ITypedElement) {
			String otherName= ((ITypedElement)other).getName();
			return getName().equals(otherName);
		}
		return super.equals(other);
	}

	/**
	 * Returns the hash code of the name.
	 * @return a hash code value for this object.
	 */
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public Object[] getChildren() {
		if (fChildren == null) {
			fChildren= new ArrayList<>();
			if (fResource instanceof IContainer) {
				try {
					IResource members[]= ((IContainer)fResource).members();
					for (IResource member : members) {
						IStructureComparator child = createChild(member);
						if (child != null)
							fChildren.add(child);
					}
				} catch (CoreException ex) {
					// NeedWork
				}
			}
		}
		return fChildren.toArray();
	}

	/**
	 * This hook method is called from <code>getChildren</code> once for every
	 * member of a container resource. This implementation
	 * creates a new <code>ResourceNode</code> for the given child resource.
	 * Clients may override this method to create a different type of
	 * <code>IStructureComparator</code> or to filter children by returning <code>null</code>.
	 *
	 * @param child the child resource for which a <code>IStructureComparator</code> must be returned
	 * @return a <code>ResourceNode</code> for the given child or <code>null</code>
	 */
	protected IStructureComparator createChild(IResource child) {
		return new ResourceNode(child);
	}

	/**
	 * Returns an open stream if the corresponding resource implements the
	 * <code>IStorage</code> interface. Otherwise the value <code>null</code> is returned.
	 *
	 * @return a buffered input stream containing the contents of this storage
	 * @exception CoreException if the contents of this storage could not be accessed
	 */
	@Override
	protected InputStream createStream() throws CoreException {
		if (fResource instanceof IStorage) {
			InputStream is= null;
			IStorage storage= (IStorage) fResource;
			try {
				is= storage.getContents();
			} catch (CoreException e) {
				if (e.getStatus().getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL) {
					fResource.refreshLocal(IResource.DEPTH_INFINITE, null);
					is= storage.getContents();
				} else
					throw e;
			}
			if (is != null)
				return new BufferedInputStream(is);
		}
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public ITypedElement replace(ITypedElement child, ITypedElement other) {
		return child;
	}

	@Override
	public String getCharset() {
		return Utilities.getCharset(fResource);
	}

	@Override
	public boolean isReadOnly() {
		if (fResource.getType() == IResource.FILE) {
			ResourceAttributes attrs = fResource.getResourceAttributes();
			if (attrs != null) {
				return attrs.isReadOnly();
			}
		}
		return false;
	}

	@Override
	public IStatus validateEdit(Shell shell) {
		if (isReadOnly())
			return ResourcesPlugin.getWorkspace().validateEdit(new IFile[] { (IFile)fResource}, shell);
		return Status.OK_STATUS;
	}
}

