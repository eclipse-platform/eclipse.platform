/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.ColorManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public abstract class AbstractAntEditorScanner extends RuleBasedScanner {

	protected void adaptToColorChange(PropertyChangeEvent event, Token token) {
		RGB rgb= null;
		
		Object value= event.getNewValue();
		if (value instanceof RGB) {
			rgb= (RGB) value;
		} else if (value instanceof String) {
			rgb= StringConverter.asRGB((String) value);
		}
			
		if (rgb != null) {
			TextAttribute attr= (TextAttribute) token.getData();
			token.setData(new TextAttribute(ColorManager.getDefault().getColor(rgb), attr.getBackground(), attr.getStyle()));
		}
	}

	protected void adaptToStyleChange(PropertyChangeEvent event, Token token, int styleAttribute) {
	 	if (token == null) {
			return;
		}
		boolean eventValue= false;
		Object value= event.getNewValue();
		if (value instanceof Boolean) {
			eventValue= ((Boolean) value).booleanValue();
		} else if (IPreferenceStore.TRUE.equals(value)) {
			eventValue= true;
		}
		
		TextAttribute attr= (TextAttribute) token.getData();
		boolean activeValue= (attr.getStyle() & styleAttribute) == styleAttribute;
		if (activeValue != eventValue) { 
			token.setData(new TextAttribute(attr.getForeground(), attr.getBackground(), eventValue ? attr.getStyle() | styleAttribute : attr.getStyle() & ~styleAttribute));
		}
	}

	protected TextAttribute createTextAttribute(String colorID, String boldKey, String italicKey) {
		Color color= null;
		if (colorID != null) {
			color= AntUIPlugin.getPreferenceColor(colorID);
		}
		IPreferenceStore store= AntUIPlugin.getDefault().getPreferenceStore();
		int style= store.getBoolean(boldKey) ? SWT.BOLD : SWT.NORMAL;
		if (store.getBoolean(italicKey)) {
			style |= SWT.ITALIC;
		}
		
		return new TextAttribute(color, null, style);
	}
}
