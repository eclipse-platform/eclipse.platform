/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import java.io.*;
import java.net.*;
import java.util.jar.*;

/**
 * .jar file entry content reference.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.ContentReference
 * @see org.eclipse.update.core.JarContentReference
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class JarEntryContentReference extends ContentReference {

	private JarContentReference jarContentReference;
	private JarEntry entry;

	/**
	 * Create jar entry content reference.
	 * 
	 * @param id "symbolic" path identifier
	 * @param jarContentReference jar file content reference
	 * @param entry jar entry
	 * @since 2.0
	 */
	public JarEntryContentReference(
		String id,
		JarContentReference jarContentReference,
		JarEntry entry) {
		super(id, (File) null);
		this.jarContentReference = jarContentReference;
		this.entry = entry;
	}
	
	/**
	 * Creates an input stream for the reference.
	 * 
	 * @return input stream
	 * @exception IOException unable to create stream
	 * @since 2.0
	 */
	public InputStream getInputStream() throws IOException {
		return jarContentReference.asJarFile().getInputStream(entry);
	}	
	
	/**
	 * Returns the size of the referenced entry.
	 * 
	 * @return input size
	 * @since 2.0
	 */
	public long getInputSize() {
		return entry.getSize();
	}
	
	/**
	 * Indicates whether the reference is to a an entry within a local jar.
	 * 
	 * @return <code>true</code> if the reference is local, 
	 * otherwise <code>false</code>
	 * @since 2.0
	 */
	public boolean isLocalReference() {
		return jarContentReference.isLocalReference();
	}	
		
	/**
	 * Returns the content reference as a URL.
	 * 
	 * @return reference as URL
	 * @exception IOException reference cannot be returned as URL
	 * @since 2.0
	 */
	public URL asURL() throws IOException {
		String fileName =
			jarContentReference.asFile().getAbsolutePath().replace(File.separatorChar, '/');
		return new URL("jar:file:" + fileName + "!/" + entry.getName());	//$NON-NLS-1$ //$NON-NLS-2$
	}
			
	/**
	 * Return string representation of this reference.
	 * 
	 * @return string representation
	 * @since 2.0
	 */
	public String toString() {
		URL url;
		try {
			url = asURL();
		} catch (IOException e) {
			url = null;
		}
		if (url != null)
			return url.toExternalForm();
		else
			return getClass().getName() + "@" + hashCode(); //$NON-NLS-1$
	}
}
