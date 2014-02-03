/*******************************************************************************
 * Copyright (c) 2002, 2012 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd;

import org.eclipse.ant.internal.ui.dtd.schema.Nfm;


/**
 * Content model.
 * This is the printable version of the model.
 * The walkable version is the IDfm.
 * @author Bob Foster
 */
public interface IModel {

	public static final int UNKNOWN = 0;
	public static final int SEQUENCE = 1;
	public static final int CHOICE = 2;
	public static final int LEAF = 4;
	
	public static final int UNBOUNDED = Integer.MAX_VALUE;
	
	/**
	 * @return one of SEQUENCE, CHOICE, LEAF
	 */
	public int getKind();
	
	/**
	 * @return one of 0 or 1.
	 */
	public int getMinOccurs();
	
	/**
	 * @return one of 1 or UNBOUNDED.
	 */
	public int getMaxOccurs();
	
	/**
	 * @return if SEQUENCE or CHOICE return array of sub-models; otherwise
	 * undefined.
	 */
	public IModel[] getContents();
	
	/**
	 * @return if LEAF return atom; otherwise undefined.
	 */
	public IAtom getLeaf();
	
	/**
	 * @return if SEQUENCE or CHOICE return "," and "|", respectively; otherwise
	 * undefined. Useful when printing model.
	 */
	public String getOperator();
	
	/**
	 * @return one of "", "?", "*" or "+". Useful when printing model.
	 */
	public String getQualifier();
	
	/**
	 * Convert content model to string representation.
	 */
	public String stringRep();
	
	/**
	 * Convert the model to an Nfm on demand.
	 * @return Nfm
	 */
	public Nfm toNfm();
}
