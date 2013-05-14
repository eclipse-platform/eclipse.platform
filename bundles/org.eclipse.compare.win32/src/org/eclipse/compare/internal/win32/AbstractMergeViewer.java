/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.win32;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

/**
 * Abstract class that caches any remote contents in local so that external 
 * tools can be used to show a comparison.
 */
public abstract class AbstractMergeViewer extends Viewer {

	private Object input;
	private File leftFile;
	private File rightFile;
	private File resultFile;
	private final CompareConfiguration configuration;

	public AbstractMergeViewer(CompareConfiguration configuration) {
		this.configuration = configuration;
	}

	public Object getInput() {
		return input;
	}

	public ISelection getSelection() {
		return StructuredSelection.EMPTY;
	}

	public void refresh() {
		// Nothing to do
	}

	public void setInput(Object input) {
		this.input = input;
		reset();
	}

	protected void reset() {
		if (leftFile != null && leftFile.exists()) {
			leftFile.delete();
		}
		if (rightFile != null && rightFile.exists()) {
			rightFile.delete();
		}
		if (resultFile != null && resultFile.exists()) {
			resultFile.delete();
		}
		leftFile = null;
		rightFile = null;
		resultFile = null;
	}

	public void setSelection(ISelection selection, boolean reveal) {
		// Nothing to do
	}

	protected boolean isOneSided() {
		if (input instanceof ICompareInput) {
			ICompareInput ci = (ICompareInput) input;
			int type = ci.getKind() & Differencer.CHANGE_TYPE_MASK;
			return type != Differencer.CHANGE;
		}
		return false;
	}
	
	protected File getFileForSingleSide() throws CoreException {
		File file = getFileForLeft();
		if (file != null && file.exists())
			return file;
		return getFileForRight();
	}
	
	protected File getFileForRight() throws CoreException {
		if (rightFile != null)
			return rightFile;
		ICompareInput ci = getCompareInput();
		if (ci != null) {
			ITypedElement right = ci.getRight();
			File file = getLocalFile(right);
			if (file != null) {
				return file;
			}
			rightFile = cacheContents(right);
			return rightFile;
		}
		return null;
	}

	protected File getFileForLeft() throws CoreException {
		if (leftFile != null)
			return leftFile;
		ICompareInput ci = getCompareInput();
		if (ci != null) {
			ITypedElement left = ci.getLeft();
			File file = getLocalFile(left);
			if (file != null) {
				return file;
			}
			leftFile = cacheContents(left);
			return leftFile;
		}
		return null;
	}
	
	protected File getResultFile() throws IOException {
		if (resultFile != null)
			return resultFile;
		resultFile = File.createTempFile("merge", ".doc"); //$NON-NLS-1$ //$NON-NLS-2$
		resultFile.deleteOnExit();
		// Need to delete the file so that clients will know that the files doesn't exist yet
		resultFile.delete();
		return resultFile;
	}
	
	protected boolean hasResultFile() {
		return resultFile != null;
	}

	private File cacheContents(ITypedElement element) throws CoreException {
		if (element instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca = (IStreamContentAccessor) element;
			InputStream contents = sca.getContents();
			if (contents != null) {
				try {
					return createTempFile(contents);
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getMessage(), e));
				} finally {
					try {
						contents.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}
		}
		return null;
	}

	private File createTempFile(InputStream contents) throws IOException {
		File file = File.createTempFile("compare", ".doc"); //$NON-NLS-1$ //$NON-NLS-2$
		file.deleteOnExit();
		OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = contents.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			return file;
		} finally {
			out.close();
		}
	}

	protected ICompareInput getCompareInput() {
		if (input instanceof ICompareInput) {
			return (ICompareInput) input;	
		}
		return null;
	}

	protected File getLocalFile(ITypedElement left) throws CoreException {
		IFile file = getEclipseFile(left);
		if (file != null) {
			URI uri = file.getLocationURI();
			IFileStore store = EFS.getStore(uri);
			if (store != null) {
				return store.toLocalFile(EFS.NONE, null);
			}
		}
		return null;
	}
	
	protected IFile getEclipseFile(Object element) {
		if (element instanceof IResourceProvider) {
			IResourceProvider rp = (IResourceProvider) element;
			IResource resource = rp.getResource();
			if (resource.getType() == IResource.FILE) {
				return (IFile)resource;
			}
		}
		if (element instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) element;
			Object result = a.getAdapter(IResource.class);
			if (result == null) {
				result = a.getAdapter(IFile.class);
			}
			if (result instanceof IFile) {
				return (IFile) result;
			}
		}
		return null;
	}
	
	protected IEditableContent getSaveTarget() {
		IEditableContent left = getEditableLeft();
		IEditableContent right = getEditableRight();
		if (left != null && right == null) {
			return left;
		}
		if (left == null && right != null) {
			return right;
		}
		return null;
	}
	
	private IEditableContent getEditableLeft() {
		ICompareInput compareInput = getCompareInput();
		if (compareInput != null) {
			ITypedElement left = compareInput.getLeft();
			if (left instanceof IEditableContent && configuration.isLeftEditable()) {
				return (IEditableContent) left;
			}	
		}	
		return null;
	}
	
	private IEditableContent getEditableRight() {
		ICompareInput compareInput = getCompareInput();
		if (compareInput != null) {
			ITypedElement right = compareInput.getRight();
			if (right instanceof IEditableContent && configuration.isRightEditable()) {
				return (IEditableContent) right;
				
			}	
		}	
		return null;
	}
	
	protected byte[] asBytes(File file) throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			out.close();
			return out.toByteArray();
		} finally {
			in.close();
		}
	}

	public CompareConfiguration getConfiguration() {
		return configuration;
	}
}
