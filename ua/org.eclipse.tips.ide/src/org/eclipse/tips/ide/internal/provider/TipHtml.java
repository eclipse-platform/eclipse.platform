/*******************************************************************************
 * Copyright (c) 2023 ArSysOp
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Nikifor Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ide.internal.provider;

import java.util.function.Supplier;

final class TipHtml implements Supplier<String> {

	private final String header;
	private final String body;
	private final String footer;

	TipHtml(String header, String body, String footer) {
		this.header = header;
		this.body = body;
		this.footer = footer;
	}

	TipHtml(String header, String body) {
		this(header, body, new String());
	}

	@Override
	public String get() {
		return String.format("<h2>%s</h2>%s%s", header, body, footer()); //$NON-NLS-1$
	}

	private String footer() {
		if (footer.isEmpty()) {
			return "<br><br><br>"; //$NON-NLS-1$
		}
		return String.format("<br><br>%s<br><br>", footer); //$NON-NLS-1$
	}

}
