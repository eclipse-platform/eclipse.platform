/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.preferences;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;

class MinimizedFileSystemElement implements IWorkbenchAdapter, IAdaptable {
	private boolean populated = false;
	private List folders = null;
	private List files = null;
	private String name;
	private boolean isDirectory = false;
	private MinimizedFileSystemElement parent;
	private Object fileSystemObject;
	
	/**
	 * Create a <code>MinimizedFileSystemElement</code> with the supplied name and parent.
	 * @param name the name of the file element this represents
	 * @param parent the containing parent
	 * @param isDirectory indicated if this could have children or not
	 */
	public MinimizedFileSystemElement(String name, MinimizedFileSystemElement parent, boolean isDirectory) {
		this.name = name;
		this.parent = parent;
		this.isDirectory = isDirectory;
		if (parent != null) {
			parent.addChild(this);
		}
	}
	
	/**
	 * Returns the adapter
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return this;
		}
		//defer to the platform
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
	
	/**
	 * Returns true if this element represents a directory, and false
	 * otherwise.
	 */
	public boolean isDirectory() {
		return isDirectory;
	}
	
	/**
	 * Adds the passed child to this object's collection of children.
	 *
	 * @param child MinimizedFileSystemElement
	 */
	private void addChild(MinimizedFileSystemElement child) {
		if (child.isDirectory()) {
			if (folders == null) {
				 folders = new ArrayList(1);
			}
			folders.add(child);
		} else {
			if (files == null) {
				 files = new ArrayList(1);
			}
			files.add(child);
		}
	}
	/**
	 * Returns a list of the files that are immediate children. Use the supplied provider
	 * if it needs to be populated.
	 */
	protected List getFiles(IImportStructureProvider provider) {
		if (!populated) {
			populate(provider);
		}

		if (files == null) {
			 return Collections.EMPTY_LIST;
		}
		return files;

	}
	/**
	 * Returns a list of the folders that are immediate children. Use the supplied provider
	 * if it needs to be populated.
	 */
	protected List getFolders(IImportStructureProvider provider) {
		if (!populated) {
			populate(provider);
		}

		return getFolders();

	}

	protected List getFolders() {
		if (folders == null){
			 return Collections.EMPTY_LIST;
		}
		return folders;
	}
	/**
	 * Return whether or not population has happened for the receiver.
	 */
	protected boolean isPopulated() {
		return this.populated;
	}
	/**
	 * Return whether or not population has not happened for the receiver.
	 */
	protected boolean notPopulated() {
		return !this.populated;
	}
	/**
	 * Populate the files and folders of the receiver using the supplied
	 * structure provider.
	 * @param provider org.eclipse.ui.wizards.datatransfer.IImportStructureProvider
	 */
	private void populate(IImportStructureProvider provider) {

		List children = provider.getChildren(fileSystemObject);
		if (children == null) {
			children = new ArrayList(1);
		}
		Iterator childrenEnum = children.iterator();
		while (childrenEnum.hasNext()) {
			Object child = childrenEnum.next();

			String elementLabel = provider.getLabel(child);
			boolean isFolder= provider.isFolder(child);
			if (!isFolder && !elementLabel.endsWith(".class")) { //$NON-NLS-1$
				continue;
			}
			//Create one level below
			MinimizedFileSystemElement result = new MinimizedFileSystemElement(elementLabel, this, isFolder);
			result.setFileSystemObject(child);
		}
		setPopulated();
	}
	
	/**
	 *	Returns the file system object property of this element
	 *
	 *	@return the file system object
	 */
	protected Object getFileSystemObject() {
		return fileSystemObject;
	}
	
	/**
	 *	Set the file system object property of this element
	 *
	 *	@param value the file system object
	 */
	protected void setFileSystemObject(Object value) {
		fileSystemObject = value;
	}
	/**
	 * Set whether or not population has happened for the receiver to true.
	 */
	protected void setPopulated() {
		this.populated = true;
	}
	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object o) {
		return getFolders().toArray();
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		if (isDirectory()) {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
		}
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(name);
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return name;
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return parent;
	}

}

