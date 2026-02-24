/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.console;

/**
 * Processes character sequences split into chunks. Modifies long lines in the
 * input. Lines can span multiple chunks, chunks can contain multiple lines.
 */
public interface IConsoleOutputModifier {

	/**
	 * Resets state to handle a new set of chunks.
	 */
	void reset();

	/**
	 * Processes a chunk, modifying long lines.
	 *
	 * @param chunk the current chunk of input
	 * @return the potentially modified chunk
	 */
	CharSequence modify(CharSequence chunk);
}
