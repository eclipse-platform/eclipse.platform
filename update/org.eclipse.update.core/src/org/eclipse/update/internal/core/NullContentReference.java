/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.update.core.ContentReference;

/**
 * NullContentReference implements a general access wrapper 
 * to feature and site content -- but, for which, there is no 
 * content actually found. This way, it can "keep" the ID that was 
 * requested, and still hold a place in lists and arrays, without a 
 * change to other program logic. It does, how ever require the internal
 * algorithms to be more careful about assumptions made ... for example, 
 * just because asFile is null, it does not follow that asURL will not be null. 
 * <p>
 * This class may not be instantiated or subclassed by clients. 
 * </p>
 * @see org.eclipse.update.core.ContentReference
 * @see org.eclipse.update.core.JarContentReference
 * @see org.eclipse.update.core.JarEntryContentReference
 */
public class NullContentReference extends ContentReference {


	/**
	 * Contructor for the "missing jar" case. 
	 * 
	 * @param id
	 */
	public NullContentReference(String id) {
		super(id, (File) null);
	}
	/**
	 * A factory method to create a content reference of
	 * the same type.
	 * 
	 * @param id "symbolic" path identifier
	 */
	public ContentReference createContentReference(String id, File file) {
		return new NullContentReference(id);
	}

	/**
	 * Overrides super class implementation to avoid throwing a FileNotFound exception.
	 * 
	 * @return null
	 */
	public InputStream getInputStream() throws IOException {
		return null;
	}
	/**
	 * Overrides super class implementation to avoid throwing a FileNotFound exception.
	 * 
	 * @return null
	 */
	public File asFile() throws IOException {
		return null; 
	}

	/**
	 * Overrides super class implementation to avoid throwing URL exceptions.
	 * 
	 * @return null
	 */
	public URL asURL() throws IOException {
		return null;
	}

	/**
	 * Return string representation of this reference.
	 * 
	 * @return string representation
	 */
	public String toString() {
			return "Missing archive file: " + '(' + getIdentifier() + ')'; //$NON-NLS-1$
	}
}
