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
 *     IBM Corporation - Initial implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

public abstract class AbstractAntEditorScanner extends RuleBasedScanner {

	protected TextAttribute createTextAttribute(String colorId) {
		return new TextAttribute(colorId == null ? null : AntUIPlugin.getThemeColor(colorId));
	}

	protected void adaptToColorChange(Token token, String colorId) {
		token.setData(createTextAttribute(colorId));
	}
}
