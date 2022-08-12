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
package org.eclipse.team.internal.genericeditor.diff.extension.partitioner;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.*;

public class DiffPartitionScanner implements IDiffPartitioning, IPartitionTokenScanner {

	private int headerEnd;
	private int currentOffset;
	private int end;
	private int tokenStart;

	@Override
	public void setRange(IDocument document, int offset, int length) {
		headerEnd = document.get().indexOf("diff --git");//$NON-NLS-1$
		currentOffset = offset;
		end = offset + length;
		tokenStart = -1;
	}

	@Override
	public IToken nextToken() {
		tokenStart = currentOffset;
		if (currentOffset < end) {
			if (currentOffset < headerEnd) {
				currentOffset = Math.min(headerEnd, end);
				return new Token(PARTITION_HEADER);
			} else {
				currentOffset = end;
				return new Token(PARTITION_BODY);
			}
		}
		return Token.EOF;
	}

	@Override
	public int getTokenOffset() {
		return tokenStart;
	}

	@Override
	public int getTokenLength() {
		return currentOffset - tokenStart;
	}

	@Override
	public void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset) {
		setRange(document, offset, length);
	}
}
