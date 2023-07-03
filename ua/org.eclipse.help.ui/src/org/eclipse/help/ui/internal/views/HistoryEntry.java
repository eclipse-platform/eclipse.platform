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
package org.eclipse.help.ui.internal.views;

public class HistoryEntry {
	public static final int URL = 1;
	public static final int PAGE = 2;
	private int type;
	private String target;
	private Object data;
	/**
	 *
	 */
	public HistoryEntry(int type, String target, Object data) {
		this.type = type;
		this.target = target;
		this.data = data;
	}
	public int getType() {
		return type;
	}
	public String getTarget() {
		return target;
	}
	public Object getData() {
		return data;
	}
}
