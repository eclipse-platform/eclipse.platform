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
package org.eclipse.ant.internal.ui.editor.actions;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * A toolbar action which toggles the {@linkplain org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants#EDITOR_MARK_OCCURRENCES mark
 * occurrences preference}.
 *
 * @since 3.1
 */
public class ToggleMarkOccurrencesAction extends TextEditorAction implements IPropertyChangeListener {

	private IPreferenceStore fStore;

	/**
	 * Constructs and updates the action.
	 */
	public ToggleMarkOccurrencesAction() {
		super(AntEditorActionMessages.getResourceBundle(), "ToggleMarkOccurrencesAction.", null, IAction.AS_CHECK_BOX); //$NON-NLS-1$
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_MARK_OCCURRENCES));
		setToolTipText(AntEditorActionMessages.getString("ToggleMarkOccurrencesAction.tooltip")); //$NON-NLS-1$
		update();
	}

	@Override
	public void run() {
		fStore.setValue(AntEditorPreferenceConstants.EDITOR_MARK_OCCURRENCES, isChecked());
	}

	@Override
	public void update() {
		ITextEditor editor = getTextEditor();

		boolean checked = false;
		boolean enabled = false;
		if (editor instanceof AntEditor) {
			checked = ((AntEditor) editor).isMarkingOccurrences();
			enabled = ((AntEditor) editor).getAntModel() != null;
		}

		setChecked(checked);
		setEnabled(enabled);
	}

	@Override
	public void setEditor(ITextEditor editor) {

		super.setEditor(editor);

		if (editor != null) {

			if (fStore == null) {
				fStore = AntUIPlugin.getDefault().getPreferenceStore();
				fStore.addPropertyChangeListener(this);
			}

		} else if (fStore != null) {
			fStore.removePropertyChangeListener(this);
			fStore = null;
		}

		update();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(AntEditorPreferenceConstants.EDITOR_MARK_OCCURRENCES)) {
			setChecked(Boolean.parseBoolean(event.getNewValue().toString()));
		}
	}
}