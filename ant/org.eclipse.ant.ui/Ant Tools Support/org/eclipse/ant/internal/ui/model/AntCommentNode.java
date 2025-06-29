/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.model;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;

public class AntCommentNode extends AntElementNode {
	public AntCommentNode() {
		super("AntComment"); //$NON-NLS-1$
	}

	@Override
	public boolean isStructuralNode() {
		return false;
	}

	@Override
	public boolean collapseProjection() {
		IPreferenceStore store = AntUIPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(AntEditorPreferenceConstants.EDITOR_FOLDING_COMMENTS)) {
			return true;
		}
		return false;
	}
}
