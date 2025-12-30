/*******************************************************************************
 *  Copyright (c) 2025 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;

import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getBuildFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.tests.ui.editor.support.TestLocationProvider;
import org.eclipse.ant.tests.ui.editor.support.TestProblemRequestor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

public class AntModelForDocument {

	private final IDocument currentDocument;

	private final AntModel antModel;

	public AntModelForDocument(String fileName) {
		this.currentDocument = loadDocument(fileName);
		this.antModel = createAntModel(fileName);
	}

	private IDocument loadDocument(String fileName) {
		File file = getBuildFile(fileName);
		try {
			String initialContent = Files.readString(file.toPath());
			return new Document(initialContent);
		} catch (IOException e) {
			return null;
		}
	}

	private AntModel createAntModel(String fileName) {
		AntModel model = new AntModel(currentDocument, new TestProblemRequestor(),
				new TestLocationProvider(getBuildFile(fileName)));
		model.reconcile();
		return model;
	}

	/**
	 * {@return the {@link AntModel} for the file name passed when initializing this
	 * object}
	 */
	public AntModel getAntModel() {
		return antModel;
	}

	/**
	 * {@return the underlying {@link IDocument} for the file name passed when
	 * initializing this object}
	 */
	public IDocument getDocument() {
		return currentDocument;
	}

}
