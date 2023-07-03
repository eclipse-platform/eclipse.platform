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
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.IValue;

/**
 * Logical stucture to translate a string into its words.
 */
public class WordStructureDelegate implements ILogicalStructureTypeDelegate {

	@Override
	public boolean providesLogicalStructure(IValue value) {
		//#ifdef ex6
//#		// TODO: Exercise 6 - provide logical structures if the value has multiple words
		//#else
		try {
			String string = value.getValueString();
			String[] words = string.split("\\W+"); //$NON-NLS-1$
			return words.length > 1;
		} catch (DebugException e) {
		}
		//#endif
		return false;
	}

	@Override
	public IValue getLogicalStructure(IValue value) throws CoreException {
		//#ifdef ex6
//#		// TODO: Exercise 6 - create an array from the given value
//#		return null;
		//#else
		return new PDAArray((PDAValue)value);
		//#endif
	}

}
