/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client;

/**
 * The CVS Annotate Command.
 * Answers a resource with each line annotated with the revision the line
 * was added/changed and the user making the change.
 */
public class Annotate extends AbstractMessageCommand {

	public static final Object FORCE_BINARY_ANNOTATE = new LocalOption("-F"); //$NON-NLS-1$

	protected Annotate() { }
	
	protected String getRequestId() {
		return "annotate"; //$NON-NLS-1$
	}

	// Local options specific to Annotate - revision (can be tag or revision)
	public static LocalOption makeRevisionOption(String revision) {
		return new LocalOption("-r" + revision, null); //$NON-NLS-1$
	}
}
