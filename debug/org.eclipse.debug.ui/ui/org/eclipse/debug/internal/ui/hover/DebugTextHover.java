/****************************************************************************
* Copyright (c) 2017 Red Hat Inc. and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Mickael Istria (Red Hat Inc.) - [521958] initial implementation
*******************************************************************************/
package org.eclipse.debug.internal.ui.hover;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;


public class DebugTextHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		IVariable variable = getHoverInfo2(textViewer, new Region(offset, 0));
		if (variable == null) {
			return null;
		}
		// assumes variable.getName() is the name of the variable as used in the document
		try {
			IDocument document = textViewer.getDocument();
			for (int prefix = Math.min(offset, variable.getName().length()); prefix >= 0 && document.getLength() > offset - prefix + variable.getName().length() ; prefix--) {
				if (textViewer.getDocument().get(offset - prefix, variable.getName().length()).equals(variable.getName())) {
					return new Region(offset - prefix, variable.getName().length());
				}
			}
		} catch (DebugException | BadLocationException ex) {
			DebugUIPlugin.log(ex);
		}
		return null;
	}


	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (textViewer == null || hoverRegion == null) {
			return null;
		}
		Object object = getHoverInfo2(textViewer, hoverRegion);
		if (object instanceof IVariable) {
			IVariable var = (IVariable) object;
			return getVariableText(var);
		}
		return null;
	}

	/**
	 * Returns HTML text for the given variable
	 */
	private static String getVariableText(IVariable variable) {
		try {
			return replaceHTMLChars(variable.getValue().getValueString()) + "<br/>" + replaceHTMLChars(variable.getReferenceTypeName()); //$NON-NLS-1$
		} catch (DebugException e) {
			DebugUIPlugin.log(e);
			return null;
		}
	}

	/**
	 * Replaces reserved HTML characters in the given string with
	 * their escaped equivalents. This is to ensure that variable
	 * values containing reserved characters are correctly displayed.
	 */
	private static String replaceHTMLChars(String variableText) {
		StringBuilder buffer= new StringBuilder(variableText.length());
		for (char character : variableText.toCharArray()) {
			switch (character) {
				case '<':
					buffer.append("&lt;"); //$NON-NLS-1$
					break;
				case '>':
					buffer.append("&gt;"); //$NON-NLS-1$
					break;
				case '&':
					buffer.append("&amp;"); //$NON-NLS-1$
					break;
				case '"':
					buffer.append("&quot;"); //$NON-NLS-1$
					break;
				default:
					buffer.append(character);
			}
		}
		return buffer.toString();
	}

	/**
	 * Returns the value of this filters preference (on/off) for the given
	 * view.
	 *
	 * @param part
	 * @return boolean
	 */
	public static boolean getBooleanPreferenceValue(String id, String preference) {
		String compositeKey = id + "." + preference; //$NON-NLS-1$
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		boolean value = false;
		if (store.contains(compositeKey)) {
			value = store.getBoolean(compositeKey);
		} else {
			value = store.getBoolean(preference);
		}
		return value;
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return new ExpressionInformationControlCreator();
	}

	@Override
	public IVariable getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		if (textViewer == null || hoverRegion == null) {
			return null;
		}
		TextSelection sel = new TextSelection(textViewer.getDocument(), hoverRegion.getOffset(), hoverRegion.getLength());
		return Adapters.adapt(sel, IVariable.class);
	}

}
