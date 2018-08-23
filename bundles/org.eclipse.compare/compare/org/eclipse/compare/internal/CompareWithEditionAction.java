/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.compare.internal;

public class CompareWithEditionAction extends EditionAction {

	public CompareWithEditionAction() {
		super(false, "org.eclipse.compare.internal.CompareWithEditionAction"); //$NON-NLS-1$
		this.fHelpContextId= ICompareContextIds.COMPARE_WITH_EDITION_DIALOG;
	}
}

