/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.team.internal.genericeditor.diff.extension.rules;

import org.eclipse.jface.text.rules.*;

public class StartOfLineRule extends SingleLineRule {

	public StartOfLineRule(String startSequence, String endSequence, IToken token) {
		super(startSequence, endSequence, token);
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		scanner.unread();
		int c = scanner.read();
		if ((c == '\n') || (c == -1)) {
			return super.evaluate(scanner);
		}
		return Token.UNDEFINED;
	}
}
