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

package org.eclipse.update.internal.security;
 
 import java.security.cert.*;
 
/**
 *
 */
public class CertificatePair {
	private Certificate root;
	private Certificate issuer;
	
	

	/**
	 * Gets the root.
	 * @return Returns a Certificate
	 */
	public Certificate getRoot() {
		return root;
	}

	/**
	 * Sets the root.
	 * @param root The root to set
	 */
	public void setRoot(Certificate root) {
		this.root = root;
	}

	/**
	 * Gets the issuer.
	 * @return Returns a Certificate
	 */
	public Certificate getIssuer() {
		return issuer;
	}

	/**
	 * Sets the issuer.
	 * @param issuer The issuer to set
	 */
	public void setIssuer(Certificate issuer) {
		this.issuer = issuer;
	}

	/*
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		
		if (obj==null) return false;
		
		if (!(obj instanceof CertificatePair)) return false;
		
		if (root==null || issuer==null) return false;
		
		CertificatePair pair = (CertificatePair)obj;
		
		return (root.equals(pair.getRoot()) && issuer.equals(pair.getIssuer()));
	}

}
