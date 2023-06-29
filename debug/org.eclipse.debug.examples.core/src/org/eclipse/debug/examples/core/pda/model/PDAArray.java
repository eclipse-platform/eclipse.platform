/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;

public class PDAArray extends PDAValue {

	/**
	 * An array splits a value into its words
	 *
	 * @param value existing value
	 * @throws DebugException
	 */
	public PDAArray(PDAValue value) throws DebugException {
		super(value.getVariable(), value.getValueString());
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return true;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		String string = getValueString();
		String[] words = string.split("\\W+"); //$NON-NLS-1$
		IVariable[] variables = new IVariable[words.length];
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			variables[i] = new PDAArrayEntry(getPDADebugTarget(), i, new PDAValue(getVariable(), word));
		}
		return variables;
	}

}
