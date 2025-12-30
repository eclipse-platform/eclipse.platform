/*******************************************************************************
 *  Copyright (c) 2003, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;

import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.assertProject;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getBuildFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.tests.ui.editor.support.TestLocationProvider;
import org.eclipse.ant.tests.ui.editor.support.TestProblemRequestor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.Before;
import org.junit.Rule;

/**
 * Abstract Ant UI test class
 */
public abstract class AbstractAntUITest {

	public static final String ANT_EDITOR_ID = "org.eclipse.ant.ui.internal.editor.AntEditor"; //$NON-NLS-1$
	private final CloseWelcomeScreenExtension closeWelcomeScreenExtension = new CloseWelcomeScreenExtension();
	private IDocument currentDocument;

	@Rule
	public TestAgainExceptionRule testAgainRule = new TestAgainExceptionRule(5);

	@Before
	public void setUp() throws Exception {
		assertProject();
		closeWelcomeScreenExtension.assertWelcomeScreenClosed();
	}

	/**
	 * Returns the underlying {@link IDocument} for the given file name
	 *
	 * @return the underlying {@link IDocument} for the given file name
	 */
	protected IDocument getDocument(String fileName) {
		File file = getBuildFile(fileName);
		try {
			String initialContent = Files.readString(file.toPath());
			return new Document(initialContent);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns the {@link AntModel} for the given file name
	 *
	 * @return the {@link AntModel} for the given file name
	 */
	protected AntModel getAntModel(String fileName) {
		currentDocument = getDocument(fileName);
		AntModel model = new AntModel(currentDocument, new TestProblemRequestor(),
				new TestLocationProvider(getBuildFile(fileName)));
		model.reconcile();
		return model;
	}

	/**
	 * @return the current {@link IDocument} context
	 */
	public IDocument getCurrentDocument() {
		return currentDocument;
	}

	/**
	 * Allows the current {@link IDocument} context to be set. This method accepts <code>null</code>
	 */
	public void setCurrentDocument(IDocument currentDocument) {
		this.currentDocument = currentDocument;
	}


}