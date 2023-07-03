/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Alexander Kurtakov - Bug 460787
 *     Sopot Cela - Bug 466829
 *******************************************************************************/
package org.eclipse.help.internal.search;

import org.apache.lucene.analysis.util.CharTokenizer;

/**
 * Tokenizer breaking words around letters or digits. Also normalizes to lower
 * case.
 */
public class CharAndDigitsTokenizer extends CharTokenizer {

	@Override
	protected boolean isTokenChar(int c) {
		return Character.isLetterOrDigit(c);
	}
}
