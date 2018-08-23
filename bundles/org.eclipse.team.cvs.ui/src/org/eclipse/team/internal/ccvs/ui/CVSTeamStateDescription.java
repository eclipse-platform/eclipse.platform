/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.team.ui.synchronize.TeamStateDescription;

/**
 * A state description for the CVS decorations. We only need to 
 * enumerate the states that can be associated with model
 * elements that don't have a one-to-one mapping to resources
 */
public class CVSTeamStateDescription extends TeamStateDescription {

	/**
	 * Property representing the image overlay
	 */
	public static final String PROP_RESOURCE_STATE = "resourceState"; //$NON-NLS-1$	
	public static final String PROP_TAG = "tag"; //$NON-NLS-1$
	
	public CVSTeamStateDescription(int state) {
		super(state);
	}
}
