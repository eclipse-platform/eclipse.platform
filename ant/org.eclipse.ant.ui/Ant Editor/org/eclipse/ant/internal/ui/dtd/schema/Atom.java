/*******************************************************************************
 * Copyright (c) 2002, 2011 Object Factory Inc.
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
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.schema;

import org.eclipse.ant.internal.ui.dtd.IAtom;

/**
 * Atom contains information common to elements and attributes.
 *
 * @author Bob Foster
 */
public class Atom implements IAtom {

	protected String fName;
	protected int fKind;

	protected Atom(int kind, String name) {
		fKind = kind;
		fName = name;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String toString() {
		return fName;
	}
}
