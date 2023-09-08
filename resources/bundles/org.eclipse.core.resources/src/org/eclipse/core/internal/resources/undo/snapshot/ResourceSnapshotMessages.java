/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - Adapted from classes in org.eclipse.ui.ide.undo and org.eclipse.ui.internal.ide.undo
 *******************************************************************************/
package org.eclipse.core.internal.resources.undo.snapshot;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 *
 * @since 3.20
 *
 */
public final class ResourceSnapshotMessages extends NLS {

	private static final String BUNDLE_NAME= ResourceSnapshotMessages.class.getName();

	static {
		NLS.initializeMessages(BUNDLE_NAME, ResourceSnapshotMessages.class);
	}

	private ResourceSnapshotMessages() {
		// Do not instantiate
	}

	public static String FileDescription_NewFileProgress;
	public static String FileDescription_ContentsCouldNotBeRestored;
	public static String FolderDescription_NewFolderProgress;
	public static String FolderDescription_SavingUndoInfoProgress;
}
