/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.universal;

import java.io.PrintWriter;

public class SeparatorData extends BaseData {

	public SeparatorData() {
	}

	public SeparatorData(String id) {
		this.id = id;
	}

	@Override
	public void write(PrintWriter writer, String indent) {
		writer.print(indent);
		writer.print("<separator id=\""); //$NON-NLS-1$
		writer.print(id);
		writer.println("\"/>"); //$NON-NLS-1$
	}
}