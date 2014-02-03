/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.actions;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * A toolbar action which toggles the {@linkplain org.eclipse.ant.internal.ui.preferences#EDITOR_RECONCILE preference}.
 * 
 * @since 3.1
 */
public class ToggleAutoReconcileAction extends TextEditorAction implements IPropertyChangeListener {
		
	private IPreferenceStore fStore;

	/**
	 * Constructs and updates the action.
	 */
	public ToggleAutoReconcileAction() {
		super(AntEditorActionMessages.getResourceBundle(), "ToggleAutoReconcileAction.", null, IAction.AS_CHECK_BOX); //$NON-NLS-1$
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_REFRESH));
		setToolTipText(AntEditorActionMessages.getString("ToggleAutoReconcileAction.tooltip"));		 //$NON-NLS-1$
		update();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		ITextEditor editor= getTextEditor();
        if (editor instanceof AntEditor) {
            AntModel model= ((AntEditor) editor).getAntModel();
            model.setShouldReconcile(isChecked());
            fStore.setValue(AntEditorPreferenceConstants.EDITOR_RECONCILE, isChecked());
        }
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		ITextEditor editor= getTextEditor();
		
		boolean checked= false;
		boolean enabled= false;
		if (editor instanceof AntEditor) {
            AntModel model= ((AntEditor)editor).getAntModel();
            enabled=  model != null;
			checked= enabled && fStore.getBoolean(AntEditorPreferenceConstants.EDITOR_RECONCILE);
            if (model != null) {
                model.setShouldReconcile(checked);
            }
		}
			
		setChecked(checked);
		setEnabled(enabled);
	}
	
	/*
	 * @see TextEditorAction#setEditor(ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		
		super.setEditor(editor);
		
		if (editor != null) {
			if (fStore == null) {
				fStore= AntUIPlugin.getDefault().getPreferenceStore();
				fStore.addPropertyChangeListener(this);
			}
		} else if (fStore != null) {
			fStore.removePropertyChangeListener(this);
			fStore= null;
		}
		
		update();
	}
	
	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(AntEditorPreferenceConstants.EDITOR_RECONCILE)) {
			setChecked(Boolean.valueOf(event.getNewValue().toString()).booleanValue());
        }
	}
}