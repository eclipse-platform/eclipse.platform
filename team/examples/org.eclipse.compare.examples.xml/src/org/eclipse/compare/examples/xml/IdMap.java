/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml;

import java.util.ArrayList;
import java.util.Vector;

/** This class is used to represent a id mapping scheme in the XML Compare preference page
 */
public class IdMap {

	private String fName;
	private boolean fInternal;
	private Vector fMappings;
	private String fExtension;
	private ArrayList fOrdered;//contains Mapping elements for list of ordered entries (the children of these elements will be compared in ordered fashion)

	/**
	 * Creates an IdMap which represents an Id Mapping Scheme
	 * @param internal true if the IdMap is internal
	 */
	public IdMap(boolean internal) {
		this("", internal); //$NON-NLS-1$
	}

	/**
	 * Creates an IdMap which represents an Id Mapping Scheme
	 * @param name The name of the mapping, as in fIdMaps/fIdMapsInternal HashMaps and fOrderedElements/fOrderedElementsInternal HashMaps
	 * @param internal true if the IdMap is internal
	 */
	public IdMap(String name, boolean internal) {
		this(name, internal, new Vector());
	}

	/**
	 * Creates an IdMap which represents an Id Mapping Scheme
	 * @param name The name of the mapping, as in fIdMaps/fIdMapsInternal HashMaps and fOrderedElements/fOrderedElementsInternal HashMaps
	 * @param internal true if the IdMap is internal
	 * @param mappings Vector of Mapping elements which represent the id mappings of this id mapping scheme
	 */
	public IdMap(String name, boolean internal, Vector mappings) {
		this(name, internal, mappings, ""); //$NON-NLS-1$
	}

	/**
	 * Creates an IdMap which represents an Id Mapping Scheme.
	 * @param name The name of the mapping, as in fIdMaps/fIdMapsInternal HashMaps and fOrderedElements/fOrderedElementsInternal HashMaps.
	 * @param internal true if the IdMap is internal.
	 * @param mappings Vector of Mapping elements which represent the id mappings of this id mapping scheme.
	 * @param extension Optional extension to be associated with this id mapping scheme.
	 */
	public IdMap(String name, boolean internal, Vector mappings, String extension) {
		this(name, internal, mappings, extension, null);
	}

	/**
	 * Creates an IdMap which represents an Id Mapping Scheme.
	 * @param name The name of the mapping, as in fIdMaps/fIdMapsInternal HashMaps and fOrderedElements/fOrderedElementsInternal HashMaps.
	 * @param internal true if the IdMap is internal.
	 * @param mappings Vector of Mapping elements which represent the id mappings of this id mapping scheme.
	 * @param extension Optional extension to be associated with this id mapping scheme.
	 * @param ordered Optional ArrayList of Mapping elements representing ordered entries.
	 */
	public IdMap(String name, boolean internal, Vector mappings, String extension, ArrayList ordered) {
		fName = name;
		fInternal = internal;
		fMappings = mappings;
		fExtension= extension.toLowerCase();
		fOrdered= ordered;
	}
	
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof IdMap))
			return false;
			
		IdMap idmap= (IdMap) object;

		if (idmap == this)
			return true;		

		return
			idmap.getName().equals(fName) &&
			idmap.getMappings().equals(fMappings);
	}
	
	@Override
	public int hashCode() {
		return fName.hashCode() ^ fMappings.hashCode();
	}

	public void setName(String name) {
		fName = name;
	}
	
	public String getName() {
		return fName;
	}

	public void setMappings(Vector mappings) {
		fMappings = mappings;
	}
	
	public Vector getMappings() {
		return fMappings;
	}

	public void setInternal(boolean bool) {
		fInternal = bool;
	}
	
	public boolean isInternal() {
		return fInternal;
	}
	
	public void setExtension(String extension) {
		fExtension= extension;
	}
	
	public String getExtension() {
		return fExtension;
	}
	public void setOrdered(ArrayList ordered) {
		fOrdered= ordered;
	}
	public ArrayList getOrdered() {
		return fOrdered;
	}

}
