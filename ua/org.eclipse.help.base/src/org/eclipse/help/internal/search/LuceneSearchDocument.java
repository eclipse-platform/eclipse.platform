/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
 *     Sopot Cela - Bug 466829
 *******************************************************************************/

package org.eclipse.help.internal.search;

import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.eclipse.help.search.ISearchDocument;

/**
 * Class which adapts a Lucene Document to ISearchDocument.
 */

public class LuceneSearchDocument implements ISearchDocument {

	private Document doc;

	public LuceneSearchDocument(Document document) {
		this.doc = document;
	}

	@Override
	public void setTitle(String title) {
		doc.add(new TextField("title", title, Field.Store.NO)); //$NON-NLS-1$
		doc.add(new TextField("exact_title", title, Field.Store.NO)); //$NON-NLS-1$
		doc.add(new StoredField("raw_title", title)); //$NON-NLS-1$
	}

	@Override
	public void setSummary(String summary) {
		doc.add(new StoredField("summary", summary)); //$NON-NLS-1$
	}

	@Override
	public void addContents(String contents) {
		doc.add(new TextField("contents", new StringReader(contents))); //$NON-NLS-1$
		doc.add(new TextField("exact_contents", new StringReader(contents))); //$NON-NLS-1$
	}

	@Override
	public void setHasFilters(boolean hasFilters) {
		doc.add(new StoredField("filters", Boolean.toString(hasFilters))); //$NON-NLS-1$
	}

	public Document getDocument() {
		return doc;
	}

	@Override
	public void addContents(Reader contents, Reader exactContents) {
		doc.add(new TextField("contents", contents)); //$NON-NLS-1$
		doc.add(new TextField("exact_contents", exactContents)); //$NON-NLS-1$
	}

}
