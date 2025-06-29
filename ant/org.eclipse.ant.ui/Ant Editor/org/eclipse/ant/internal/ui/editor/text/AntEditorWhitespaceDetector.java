/*******************************************************************************
 * Copyright (c) 2002, 2005 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH,
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug 29148
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

/*
 * This file originates from an internal package of Eclipse's
 * Manifest Editor. It has been copied by GEBIT to here in order to
 * permanently use those features. It has been renamed by GEBIT
 * after copying.
 */

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class AntEditorWhitespaceDetector implements IWhitespaceDetector {

	@Override
	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
