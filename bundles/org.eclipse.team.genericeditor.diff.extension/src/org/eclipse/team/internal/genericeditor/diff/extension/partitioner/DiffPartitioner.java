/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.team.internal.genericeditor.diff.extension.partitioner;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;

public class DiffPartitioner implements IDocumentSetupParticipant{
	@Override
	public void setup(IDocument document) {
		IPartitionTokenScanner scanner = new DiffPartitionScanner();
		FastPartitioner partitioner = new FastPartitioner(scanner, IDiffPartitioning.LEGAL_PARTITION_TYPES);
	    if (document instanceof IDocumentExtension3) {
	        final IDocumentExtension3 extension3 = (IDocumentExtension3) document;
	        extension3.setDocumentPartitioner(IDiffPartitioning.DIFF_PARTITIONINING, partitioner);
	    } else {
	    	document.setDocumentPartitioner(partitioner);
	    }
	    partitioner.connect(document);
	}
}
