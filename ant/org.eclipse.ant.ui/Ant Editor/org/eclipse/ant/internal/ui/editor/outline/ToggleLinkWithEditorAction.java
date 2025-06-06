/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.editor.outline;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.jface.action.Action;

/**
 * This action toggles whether the Ant Outline page links its selection to the active editor.
 *
 * @since 3.0
 */
public class ToggleLinkWithEditorAction extends Action {

	AntEditor fEditor;

	public ToggleLinkWithEditorAction(AntEditor editor) {
		super(AntOutlineMessages.ToggleLinkWithEditorAction_0);
		boolean isLinkingEnabled = AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.OUTLINE_LINK_WITH_EDITOR);
		setChecked(isLinkingEnabled);
		fEditor = editor;
		setToolTipText(AntOutlineMessages.ToggleLinkWithEditorAction_1);
		setDescription(AntOutlineMessages.ToggleLinkWithEditorAction_2);
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_LINK_WITH_EDITOR));
	}

	@Override
	public void run() {
		AntUIPlugin.getDefault().getPreferenceStore().setValue(IAntUIPreferenceConstants.OUTLINE_LINK_WITH_EDITOR, isChecked());
		if (isChecked()) {
			fEditor.synchronizeOutlinePage(false);
		}
	}
}
