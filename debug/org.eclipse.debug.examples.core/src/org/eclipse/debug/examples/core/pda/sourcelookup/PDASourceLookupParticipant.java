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
package org.eclipse.debug.examples.core.pda.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;


/**
 * The PDA source lookup participant knows how to translate a
 * PDA stack frame into a source file name
 */
public class PDASourceLookupParticipant extends AbstractSourceLookupParticipant {

	@Override
	public String getSourceName(Object object) throws CoreException {
		//#ifdef ex4
//#		// TODO: Exercise 4 - return the name of the source file for the given stack frame
//#		return null;
		//#else
		if (object instanceof PDAStackFrame) {
			return ((PDAStackFrame)object).getSourceName();
		}
		return null;
		//#endif
	}
}
