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

/**
 * This class is used to represent a particular id mapping or ordered entry in the XML Compare preference page
 */
public class Mapping {

	private String fElement;
	private String fSignature;
	private String fIdAttribute;

	public Mapping() {
		this("", "", ""); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
	}

	public Mapping(String element, String signature) {
		this(element, signature, ""); //$NON-NLS-1$
	}

	public Mapping(String element, String signature, String idattribute) {
		fElement = element;
		fSignature = signature;
		fIdAttribute = idattribute;
	}
	
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Mapping))
			return false;
			
		Mapping mapping= (Mapping) object;

		if (mapping == this)
			return true;		

		return
			mapping.fElement.equals(fElement) &&
			mapping.fSignature.equals(fSignature) &&
			mapping.fIdAttribute.equals(fIdAttribute);
	}
	
	@Override
	public int hashCode() {
		return fElement.hashCode() ^ fSignature.hashCode();
	}

	public void setElement(String element) {
		fElement = element;
	}
	public String getElement() {
		return fElement;
	}	

	public void setSignature(String signature) {
		fSignature = signature;
	}
	public String getSignature() {
		return fSignature;
	}
	
	public void setIdAttribute(String idattribute) {
		fIdAttribute = idattribute;
	}
	public String getIdAttribute() {
		return fIdAttribute;
	}
	
	public String getKey() {
		return getKey(fSignature, fElement);
	}
	
	public static String getKey(String signature, String element) {
		if (signature == "") //$NON-NLS-1$
			return XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + element + XMLStructureCreator.SIGN_SEPARATOR;
		return XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + signature + XMLStructureCreator.SIGN_SEPARATOR + element + XMLStructureCreator.SIGN_SEPARATOR;
	}
}
