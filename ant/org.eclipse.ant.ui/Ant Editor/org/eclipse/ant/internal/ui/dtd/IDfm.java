/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd;

/**
 * A Dfm is an IModel converted to a form more appropriate for
 * validation. The whole point of a Dfm is to run fast.
 * @author Bob Foster
 */
public interface IDfm {

	/**
	 * Return true if no further symbols are required.
	 */
	boolean isAccepting();
	
	/**
	 * If the symbol represented by name is acceptable,
	 * return the dfm to apply to the next (child) symbol.
	 * Otherwise, return null.
	 */
	IDfm advance(String name);
	
	/**
	 * If the symbol represented by the namespace,name pair is acceptable,
	 * return the dfm to apply to the next (child) symbol. Otherwise, return
	 * null.
	 */
	IDfm advance(String namespace, String localname);
	
	/**
	 * If the symbol represented by name is acceptable
	 * to <code>advance()</code>
	 * return the corresponding atom.
	 * Otherwise, return null.
	 */
	IAtom getAtom(String name);
	
	/**
	 * Return the symbols for which <code>advance()</code>
	 * will return a next dfm. If no symbols will
	 * advance this dfm or if any symbol will, returns
	 * an empty array. Use <code>isAny()</code> and
	 * <code>isEmpty()</code> to disambiguate the
	 * cases.
	 */
	String[] getAccepts();
	
	/**
	 * Return the symbols for which <code>advance()</code>
	 * will return a next dfm. If no symbols will
	 * advance this dfm or if any symbol will, returns
	 * null. Use <code>isAny()</code> and
	 * <code>isEmpty()</code> to disambiguate the
	 * cases.
	 */
	Object[] getKeys();
	
	/**
	 * Return true if dfm will accept any symbol
	 * and return itself; false otherwise.
	 * This interface keeps the dfm from needing
	 * schema knowledge, but a better way to
	 * process an ANY dfm for elements is use the 
	 * schema to look up the element and use its 
	 * dfm instead of this one.
	 */
	boolean isAny();
	
	/**
	 * Return true if dfm will reject every symbol
	 * (<code>advance()</code> returns null);
	 * false otherwise. Included for completeness,
	 * so every element can have a dfm, regardless
	 * of its type.
	 */
	boolean isEmpty();
}
