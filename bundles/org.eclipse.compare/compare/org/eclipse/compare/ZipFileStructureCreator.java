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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

/**
 * This implementation of the <code>IStructureCreator</code> interface
 * makes the contents of a zip archive available as a
 * hierarchical structure of <code>IStructureComparator</code>s.
 * <p>
 * It is used when comparing the internal structure of a zip archive.
 *
 * @since 2.0
 */
public class ZipFileStructureCreator implements IStructureCreator {

	/**
	 * Common base class for ZipFolder and ZipFile
	 */
	static abstract class ZipResource implements IStructureComparator, ITypedElement {

		private String fName;

		ZipResource(String name) {
			fName= name;
		}

		public String getName() {
			return fName;
		}

		public Image getImage() {
			return CompareUI.getImage(getType());
		}

		/*
		 * Returns true if other is ITypedElement and names are equal.
		 * @see IComparator#equals
		 */
		public boolean equals(Object other) {
			if (other instanceof ITypedElement)
				return fName.equals(((ITypedElement) other).getName());
			return super.equals(other);
		}

		public int hashCode() {
			return fName.hashCode();
		}
	}

	static class ZipFolder extends ZipResource {

		private HashMap fChildren= new HashMap(10);

		ZipFolder(String name) {
			super(name);
		}

		public String getType() {
			return ITypedElement.FOLDER_TYPE;
		}

		public Object[] getChildren() {
			Object[] children= new Object[fChildren.size()];
			Iterator iter= fChildren.values().iterator();
			for (int i= 0; iter.hasNext(); i++)
				children[i]= iter.next();
			return children;
		}

		ZipFile createContainer(String path) {
			String entry= path;
			int pos= path.indexOf('/');
			if (pos < 0)
				pos= path.indexOf('\\');
			if (pos >= 0) {
				entry= path.substring(0, pos);
				path= path.substring(pos + 1);
			} else if (entry.length() > 0) {
				if (CompareUIPlugin.getDefault().filter(path, false, true))
					return null;
				ZipFile ze= new ZipFile(entry);
				fChildren.put(entry, ze);
				return ze;
			} else
				return null;

			ZipFolder folder= null;
			if (fChildren != null) {
				Object o= fChildren.get(entry);
				if (o instanceof ZipFolder)
					folder= (ZipFolder) o;
			}

			if (folder == null) {
				if (path.length() > 0 && CompareUIPlugin.getDefault().filter(path, true, true))
					return null;
				folder= new ZipFolder(entry);
				fChildren.put(entry, folder);
			}

			return folder.createContainer(path);
		}
	}

	static class ZipFile extends ZipResource implements IStreamContentAccessor {

		private byte[] fContents;

		ZipFile(String name) {
			super(name);
		}

		public String getType() {
			String s= this.getName();
			int pos= s.lastIndexOf('.');
			if (pos >= 0)
				return s.substring(pos + 1);
			return ITypedElement.UNKNOWN_TYPE;
		}

		public Object[] getChildren() {
			return null;
		}
		
		public InputStream getContents() {
			if (fContents == null)
				fContents= new byte[0];
			return new ByteArrayInputStream(fContents);
		}

		byte[] getBytes() {
			return fContents;
		}

		void setBytes(byte[] buffer) {
			fContents= buffer;
		}

		void appendBytes(byte[] buffer, int length) {
	    	if (length > 0) {
				int oldLen= 0;
				if (fContents != null)
					oldLen= fContents.length;
				byte[] newBuf= new byte[oldLen + length];
				if (oldLen > 0)
		    		System.arraycopy(fContents, 0, newBuf, 0, oldLen);
	    		System.arraycopy(buffer, 0, newBuf, oldLen, length);
	    		fContents= newBuf;
	    	}
		}
	}
	
	private String fTitle;

	/**
	 * Create a new ZipFileStructureCreator.
	 */
	public ZipFileStructureCreator() {
		this(Utilities.getString("ZipStructureCreator.name")); //$NON-NLS-1$
	}
	
	/**
	 * Create a new ZipFileStructureCreator with the given title.
	 * The title is returned by the method <code>getName()</code>.
	 * @param title the title of this structure creator
	 */
	public ZipFileStructureCreator(String title) {
		fTitle= title;
	}

	public String getName() {
		return fTitle;
	}

	public IStructureComparator getStructure(Object input) {

		InputStream is= null;
		
		if (input instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca= (IStreamContentAccessor) input;
			try {
				is= sca.getContents();
			} catch (CoreException ex) {
				// NeedWork
			}
		}

		if (is == null)
			return null;

		ZipInputStream zip= new ZipInputStream(is);
		ZipFolder root= new ZipFolder(""); //$NON-NLS-1$
		try {
			for (;;) {
				ZipEntry entry= zip.getNextEntry();
				if (entry == null)
					break;

				ZipFile ze= root.createContainer(entry.getName());
				if (ze != null) {
					int length= (int) entry.getSize();
					if (length >= 0) {
						byte[] buffer= new byte[length];
						int offset= 0;
	
						do {
							int n= zip.read(buffer, offset, length);
							offset += n;
							length -= n;
						} while (length > 0);
	
						ze.setBytes(buffer);
					} else {
						byte[] buffer= new byte[1024];		
						int n;
						do {
							n= zip.read(buffer, 0, 1024);
							ze.appendBytes(buffer, n);
						} while (n >= 0);
					}
				}
				zip.closeEntry();
			}
		} catch (IOException ex) {
			return null;
		} finally {
			try {
				zip.close();
			} catch (IOException ex) {
				// silently ignored
			}
		}

		if (root.fChildren.size() == 1) {
			Iterator iter= root.fChildren.values().iterator();
			return (IStructureComparator) iter.next();
		}
		return root;
	}

	public String getContents(Object o, boolean ignoreWhitespace) {
		if (o instanceof ZipFile) {
			byte[] bytes= ((ZipFile)o).getBytes();
			if (bytes != null)
				return new String(bytes);
			return ""; //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Returns <code>false</code> since we cannot update a zip archive.
	 * @return <code>false</code>
	 */
	public boolean canSave() {
		return false;
	}

	/**
	 * Called whenever a copy operation has been performed on a tree node.
	 * This implementation throws an <code>AssertionFailedException</code>
	 * since we cannot update a zip archive.
	 *
	 * @param structure the node for which to save the new content
	 * @param input the object from which the structure tree was created in <code>getStructure</code>
	 */
	public void save(IStructureComparator structure, Object input) {
		Assert.isTrue(false); // Cannot update zip archive
	}
	
	public IStructureComparator locate(Object path, Object source) {
		return null;
	}
		
	/**
	 * Returns <code>false</code> since this <code>IStructureCreator</code>
	 * cannot rewrite the diff tree in order to fold certain combinations of
	 * additions and deletions.
	 * <p>
	 * Note: this method is for internal use only. Clients should not call this method. 
	 * @return <code>false</code>
	 */
	public boolean canRewriteTree() {
		return false;
	}
	
	/**
	 * Empty implementation since this <code>IStructureCreator</code>
	 * cannot rewrite the diff tree in order to fold certain combinations of
	 * additions and deletions.
	 * <p>
	 * Note: this method is for internal use only. Clients should not call this method. 
	 * @param differencer
	 * @param root
	 */
	public void rewriteTree(Differencer differencer, IDiffContainer root) {
		// empty default implementation
	}
}

