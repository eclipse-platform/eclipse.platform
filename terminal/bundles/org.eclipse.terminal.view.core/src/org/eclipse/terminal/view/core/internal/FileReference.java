/*******************************************************************************
 * Copyright (c) 2024 Fabrizio Iannetti and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.view.core.internal;

/**
 * An immutable value object representing a file reference that may include
 * a line and column position.
 *
 * @param path   the file path
 * @param line   the 1-based line number, or {@code -1} if absent
 * @param column the 1-based column number, or {@code -1} if absent
 */
public record FileReference(String path, int line, int column) {

	/**
	 * @return {@code true} if this reference contains a valid line number
	 */
	public boolean hasLine() {
		return line > 0;
	}

	/**
	 * @return {@code true} if this reference contains a valid column number
	 */
	public boolean hasColumn() {
		return column > 0;
	}
}
