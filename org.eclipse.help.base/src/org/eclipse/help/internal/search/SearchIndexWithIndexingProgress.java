/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

import org.eclipse.help.internal.toc.*;

public class SearchIndexWithIndexingProgress extends SearchIndex {
	private ProgressDistributor progressDistributor;
	/**
	 * @param locale
	 * @param analyzerDesc
	 * @param tocManager
	 */
	public SearchIndexWithIndexingProgress(String locale,
			AnalyzerDescriptor analyzerDesc, TocManager tocManager) {
		super(locale, analyzerDesc, tocManager);
		progressDistributor = new ProgressDistributor();
	}
	/**
	 * @return Returns the progressDistributor.
	 */
	public ProgressDistributor getProgressDistributor() {
		return progressDistributor;
	}
}
