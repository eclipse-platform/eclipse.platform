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
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getLaunchConfiguration;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.launchAndTerminate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.tests.ui.editor.support.TestLocationProvider;
import org.eclipse.ant.tests.ui.editor.support.TestProblemRequestor;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;
import org.eclipse.ui.internal.console.IOConsolePartition;
import org.junit.Before;
import org.junit.Rule;

/**
 * Abstract Ant UI test class
 */
@SuppressWarnings("restriction")
public abstract class AbstractAntUITest {

	public static final String ANT_EDITOR_ID = "org.eclipse.ant.ui.internal.editor.AntEditor"; //$NON-NLS-1$
	private final CloseWelcomeScreenExtension closeWelcomeScreenExtension = new CloseWelcomeScreenExtension();
	private IDocument currentDocument;

	@Rule
	public TestAgainExceptionRule testAgainRule = new TestAgainExceptionRule(5);

	/**
	 * Returns the {@link IFile} for the given build file name
	 *
	 * @return the associated {@link IFile} for the given build file name
	 */
	protected IFile getIFile(String buildFileName) {
		return AntUITestUtil.getProject().getFolder("buildfiles").getFile(buildFileName); //$NON-NLS-1$
	}

	/**
	 * Returns the {@link File} for the given build file name
	 *
	 * @return the {@link File} for the given build file name
	 */
	protected File getBuildFile(String buildFileName) {
		IFile file = getIFile(buildFileName);
		assertTrue("Could not find build file named: " + buildFileName, file.exists()); //$NON-NLS-1$
		return file.getLocation().toFile();
	}

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
	 * Returns the contents of the given {@link BufferedReader} as a {@link String}
	 *
	 * @return the contents of the given {@link BufferedReader} as a {@link String}
	 */
	protected String getReaderContentAsString(BufferedReader bufferedReader) {
		StringBuilder result = new StringBuilder();
		try {
			String line = bufferedReader.readLine();

			while (line != null) {
				if (result.length() != 0) {
					result.append(System.lineSeparator());
				}
				result.append(line);
				line = bufferedReader.readLine();
			}
		}
		catch (IOException e) {
			AntUIPlugin.log(e);
			return null;
		}

		return result.toString();
	}

	/**
	 * Returns the {@link AntModel} for the given file name
	 *
	 * @return the {@link AntModel} for the given file name
	 */
	protected AntModel getAntModel(String fileName) {
		currentDocument = getDocument(fileName);
		AntModel model = new AntModel(currentDocument, new TestProblemRequestor(), new TestLocationProvider(getBuildFile(fileName)));
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

	/**
	 * Launches the Ant build with the build file name (no extension).
	 *
	 * @param buildFileName
	 *            the ant build file name
	 */
	protected void launch(String buildFileName) throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
		launchAndTerminate(config, 20000);
	}

	/**
	 * Launches the Ant build with the build file name (no extension).
	 *
	 * @param buildFileName
	 *            the build file
	 * @param arguments
	 *            the ant arguments
	 */
	protected void launch(String buildFileName, String arguments) throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		copy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, arguments);
		launchAndTerminate(copy, 20000);
	}

	/**
	 * Returns the {@link IHyperlink} at the given offset on the given document, or <code>null</code> if there is no {@link IHyperlink} at that offset
	 * on the document.
	 *
	 * @return the {@link IHyperlink} at the given offset on the given document or <code>null</code>
	 */
	protected IHyperlink getHyperlink(int offset, IDocument doc) {
		if (offset >= 0 && doc != null) {
			Position[] positions = null;
			try {
				positions = doc.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
			}
			catch (BadPositionCategoryException ex) {
				// no links have been added
				return null;
			}
			for (Position position : positions) {
				if (offset >= position.getOffset() && offset <= (position.getOffset() + position.getLength())) {
					return ((ConsoleHyperlinkPosition) position).getHyperLink();
				}
			}
		}
		return null;
	}

	/**
	 * Returns the {@link Color} at the given offset on the given document, or <code>null</code> if there is no {@link Color} at that offset on the
	 * document.
	 *
	 * @return the {@link Color} at the given offset on the given document or <code>null</code>
	 */
	protected Color getColorAtOffset(int offset, IDocument document) throws BadLocationException {
		if (document != null) {
			IDocumentPartitioner partitioner = document.getDocumentPartitioner();
			if (partitioner != null) {
				ITypedRegion[] regions = partitioner.computePartitioning(offset, document.getLineInformationOfOffset(offset).getLength());
				if (regions.length > 0) {
					IOConsolePartition partition = (IOConsolePartition) regions[0];
					return partition.getColor();
				}
			}
		}
		return null;
	}

	/**
	 * This is to help in increasing the test coverage by enabling access to fields and execution of methods irrespective of their Java language
	 * access permissions.
	 *
	 * More accessor methods can be added to this on a need basis
	 */
	protected static abstract class TypeProxy {

		Object master = null;

		protected TypeProxy(Object obj) {
			master = obj;
		}

		/**
		 * Gets the method with the given method name and argument types.
		 *
		 * @param methodName
		 *            the method name
		 * @param types
		 *            the argument types
		 * @return the method
		 */
		protected Method get(String methodName, Class<?>[] types) {
			Method method = null;
			try {
				method = master.getClass().getDeclaredMethod(methodName, types);
			}
			catch (SecurityException | NoSuchMethodException e) {
				fail();
			}
			Assert.isNotNull(method);
			method.setAccessible(true);
			return method;
		}

		/**
		 * Invokes the given method with the given arguments.
		 *
		 * @param method
		 *            the given method
		 * @param arguments
		 *            the method arguments
		 * @return the method return value
		 */
		protected Object invoke(Method method, Object[] arguments) {
			try {
				return method.invoke(master, arguments);
			}
			catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
				fail();
			}
			return null;
		}
	}
}