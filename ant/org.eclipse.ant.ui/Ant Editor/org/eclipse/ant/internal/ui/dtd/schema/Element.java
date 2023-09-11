/*******************************************************************************
 * Copyright (c) 2002, 2013 Object Factory Inc.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *		IBM Corporation - fix for Bug 40951
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.schema;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.internal.ui.dtd.IAttribute;
import org.eclipse.ant.internal.ui.dtd.IDfm;
import org.eclipse.ant.internal.ui.dtd.IElement;
import org.eclipse.ant.internal.ui.dtd.IModel;
import org.eclipse.ant.internal.ui.dtd.ParseError;

public class Element extends Atom implements IElement {
	private boolean fUndefined = true;
	private boolean fText;
	private IModel fModel;
	private final Map<String, IAttribute> fMap = new HashMap<>(4);
	private Dfm fElementDfm;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            QName of element.
	 */
	public Element(String name) {
		super(ELEMENT, name);
	}

	/**
	 * Set undefined property.
	 * 
	 * @param undefined
	 *            False if defined; otherwise true (default).
	 */
	public void setUndefined(boolean undefined) {
		fUndefined = undefined;
	}

	/**
	 * Set text property.
	 * 
	 * @param text
	 *            True if text only; otherwise false (default).
	 */
	public void setText(boolean text) {
		fText = text;
	}

	/**
	 * Set model property.
	 * 
	 * @param model
	 *            Dfm describing content model.
	 */
	public void setContentModel(IModel model) {
		fModel = model;
	}

	/**
	 * Add an attribute to the attribute map.
	 * 
	 * @param attribute
	 *            Attribute to add.
	 */
	public void addAttribute(IAttribute attribute) {
		fMap.put(attribute.getName(), attribute);
	}

	@Override
	public Map<String, IAttribute> getAttributes() {
		return fMap;
	}

	@Override
	public IModel getContentModel() {
		return fModel;
	}

	@Override
	public boolean isText() {
		return fText;
	}

	@Override
	public boolean isUndefined() {
		return fUndefined;
	}

	@Override
	public IDfm getDfm() {
		Dfm dfm = fElementDfm;
		if (dfm == null) {
			dfm = parseElementDfm();
			fElementDfm = dfm;
		}
		return dfm;
	}

	private Dfm parseElementDfm() {
		Dfm dfm;
		if (fAny) {
			dfm = Dfm.dfm(true);
			dfm.any = true;
		} else if (fEmpty || fText) {
			dfm = Dfm.dfm(true);
			dfm.empty = true;
		} else {
			dfm = parseModel(fModel);
		}
		return dfm;
	}

	private Dfm parseModel(IModel model) {
		Dfm dfm;
		Nfm nfm = model.toNfm();
		if (nfm != null) {
			try {
				dfm = fNfmParser.parse(nfm);
			}
			catch (ParseError e) {
				// ??? this would be the place to log the error
				dfm = Dfm.dfm(false);
			}
		} else {
			dfm = Dfm.dfm(false);
		}
		return dfm;
	}

	private static final NfmParser fNfmParser = new NfmParser();
	private boolean fAny;
	private boolean fEmpty;

	@Override
	public boolean isAny() {
		return fAny;
	}

	@Override
	public boolean isEmpty() {
		return fEmpty;
	}

	/**
	 * Sets the any.
	 * 
	 * @param any
	 *            The any to set
	 */
	public void setAny(boolean any) {
		fAny = any;
	}

	/**
	 * Sets the empty.
	 * 
	 * @param empty
	 *            The empty to set
	 */
	public void setEmpty(boolean empty) {
		fEmpty = empty;
	}

}
