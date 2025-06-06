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
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.StorageDocumentProvider;

/**
 * @since 3.0
 */
public class AntStorageDocumentProvider extends StorageDocumentProvider {

	@Override
	protected void setupDocument(Object element, IDocument document) {
		if (document != null) {
			IDocumentPartitioner partitioner = createDocumentPartitioner();
			if (document instanceof IDocumentExtension3 extension3) {
				extension3.setDocumentPartitioner(AntDocumentSetupParticipant.ANT_PARTITIONING, partitioner);
			} else {
				document.setDocumentPartitioner(partitioner);
			}
			partitioner.connect(document);
		}
	}

	private IDocumentPartitioner createDocumentPartitioner() {
		return new FastPartitioner(new AntEditorPartitionScanner(), new String[] { AntEditorPartitionScanner.XML_TAG,
				AntEditorPartitionScanner.XML_COMMENT, AntEditorPartitionScanner.XML_CDATA, AntEditorPartitionScanner.XML_DTD });
	}
}
