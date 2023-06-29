/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;

/**
 * A console document. Requires synchronization for multi-threaded access.
 */
public class ConsoleDocument extends Document {

	@Override
	public synchronized String get(int pos, int length) throws BadLocationException {
		return super.get(pos, length);
	}

	@Override
	public synchronized int getLength() {
		return super.getLength();
	}

	@Override
	public synchronized String getLineDelimiter(int line) throws BadLocationException {
		return super.getLineDelimiter(line);
	}

	@Override
	public synchronized IRegion getLineInformation(int line) throws BadLocationException {
		return super.getLineInformation(line);
	}

	@Override
	public synchronized IRegion getLineInformationOfOffset(int offset) throws BadLocationException {
		return super.getLineInformationOfOffset(offset);
	}

	@Override
	public synchronized int getLineLength(int line) throws BadLocationException {
		return super.getLineLength(line);
	}

	@Override
	public synchronized int getLineOffset(int line) throws BadLocationException {
		return super.getLineOffset(line);
	}

	@Override
	public int getLineOfOffset(int pos) throws BadLocationException {
		return super.getLineOfOffset(pos);
	}

	@Override
	public synchronized int getNumberOfLines() {
		return super.getNumberOfLines();
	}

	@Override
	public synchronized void replace(int pos, int length, String text) throws BadLocationException {
		super.replace(pos, length, text);
	}

	@Override
	public synchronized void set(String text) {
		super.set(text);
	}

	@Override
	protected void completeInitialization() {
		super.completeInitialization();
		addPositionUpdater(new HyperlinkUpdater());
	}

	@Override
	public synchronized void addPosition(String category, Position position) throws BadLocationException, BadPositionCategoryException {
		super.addPosition(category, position);
	}

	@Override
	public synchronized void removePosition(String category, Position position) throws BadPositionCategoryException {
		super.removePosition(category, position);
	}

	@Override
	public synchronized Position[] getPositions(String category) throws BadPositionCategoryException {
		return super.getPositions(category);
	}

	/** for debug only **/
	@Override
	public synchronized String toString() {
		return get();
	}
}
