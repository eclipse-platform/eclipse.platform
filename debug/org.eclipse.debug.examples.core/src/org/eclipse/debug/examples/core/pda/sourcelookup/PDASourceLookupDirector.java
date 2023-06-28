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

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

/**
 * PDA source lookup director. For PDA source lookup there is one source
 * lookup participant.
 */
public class PDASourceLookupDirector extends AbstractSourceLookupDirector {

	@Override
	public void initializeParticipants() {
		//#ifdef ex4
//#		// TODO: Exercise 4 - add our participant to this director
		//#else
		addParticipants(new ISourceLookupParticipant[]{new PDASourceLookupParticipant()});
		//#endif
	}
}
