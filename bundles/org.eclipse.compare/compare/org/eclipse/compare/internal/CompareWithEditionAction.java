/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

public class CompareWithEditionAction extends EditionAction {
	
	public CompareWithEditionAction() {
		super(false, "org.eclipse.compare.internal.CompareWithEditionAction"); //$NON-NLS-1$
		this.fHelpContextId= ICompareContextIds.COMPARE_WITH_EDITION_DIALOG;
	}
}

