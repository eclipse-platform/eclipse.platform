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

public interface IDiffPartitioning {
	public static final String DIFF_PARTITIONINING = "__DIFF_PARTITIONING"; //$NON-NLS-1$
	public static final String PARTITION_BODY = "__PARTITION_BODY"; //$NON-NLS-1$
	public static final String PARTITION_HEADER = "__PARTITION_HEADER"; //$NON-NLS-1$
	public static final String[] LEGAL_PARTITION_TYPES = new String[] {PARTITION_HEADER,PARTITION_BODY, IDocument.DEFAULT_CONTENT_TYPE };
}
