/*******************************************************************************
 * Copyright (c) 2021 Fabrizio Iannetti.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.tabs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.terminal.connector.Logger;
import org.eclipse.terminal.control.ITerminalMouseListener;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.model.ITerminalTextDataReadOnly;
import org.eclipse.terminal.view.core.internal.FileReference;
import org.eclipse.terminal.view.core.internal.FileReferenceParser;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.dialogs.OpenResourceDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Bundle;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
public class OpenFileMouseHandler implements ITerminalMouseListener {
	private static final boolean DEBUG_HOVER = Platform.getDebugBoolean(Logger.TRACE_DEBUG_LOG_HOVER);
	private static final List<String> NEEDED_BUNDLES = //
			List.of("org.eclipse.core.resources", //$NON-NLS-1$
					"org.eclipse.ui.ide", //$NON-NLS-1$
					"org.eclipse.ui.editors", //$NON-NLS-1$
					"org.eclipse.text"); //$NON-NLS-1$

	private final ITerminalViewControl terminal;
	private final IWorkbenchPartSite site;

	/**
	 * Check if we have the bundles needed.
	 */
	private boolean neededBundlesAvailable;

	OpenFileMouseHandler(IWorkbenchPartSite site, ITerminalViewControl terminal) {
		this.site = site;
		this.terminal = terminal;
		neededBundlesAvailable = true;
		for (String bundleName : NEEDED_BUNDLES) {
			if (!bundleAvailable(bundleName)) {
				this.neededBundlesAvailable = false;
				if (DEBUG_HOVER) {
					System.out.format(
							"hover: the %s bundle is not present, therefore full ctrl-click functionality is not available\n", //$NON-NLS-1$
							bundleName);
				}
			}
		}
		if (neededBundlesAvailable && DEBUG_HOVER) {
			System.out.format("hover: the bundles needed for full ctrl-click functionality are available\n"); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("restriction")
	@Override
	public void mouseUp(ITerminalTextDataReadOnly terminalText, int line, int column, int button, int stateMask) {
		if ((stateMask & SWT.MODIFIER_MASK) != SWT.MOD1) {
			// Only handle Ctrl-click
			return;
		}
		String textToOpen = terminal.getHoverSelection();
		if (textToOpen.length() > 0) {
			try {
				// if the selection looks like a web URL, open using the browser
				if (textToOpen.startsWith("http://") || textToOpen.startsWith("https://")) { //$NON-NLS-1$//$NON-NLS-2$
					try {
						PlatformUI.getWorkbench().getBrowserSupport().createBrowser(null).openURL(new URL(textToOpen));
						return;
					} catch (MalformedURLException e) {
						// not a valid URL, continue
					}
				}

				// After this we need Eclipse IDE features. If we don't have them then we stop here.
				if (!neededBundlesAvailable) {
					return;
				}

				// extract the path from file:// URLs
				if (textToOpen.startsWith("file://")) { //$NON-NLS-1$
					textToOpen = textToOpen.substring(7);
				}
				// Parse file reference to extract path and optional line:column
				FileReference fileRef = FileReferenceParser.parse(textToOpen);
				textToOpen = fileRef.path();

				Optional<String> fullPath = Optional.empty();
				if (!textToOpen.startsWith("/")) { //$NON-NLS-1$
					// relative path: try to append to the working directory
					Optional<String> workingDirectory = terminal.getTerminalConnector().getWorkingDirectory();
					if (workingDirectory.isPresent()) {
						fullPath = Optional.of(workingDirectory.get() + "/" + textToOpen); //$NON-NLS-1$
					}
				}
				// if the selection is a file location that maps to a resource
				// open the resource
				IFile fileForLocation = ResourcesPlugin.getWorkspace().getRoot()
						.getFileForLocation(new Path(fullPath.orElse(textToOpen)));
				if (fileForLocation != null && fileForLocation.exists()) {
					IEditorPart editor = IDE.openEditor(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), fileForLocation,
							true);
					goToLine(fileRef, editor);
					return;
				}
				// try an external file, if it exists
				File file = new File(fullPath.orElse(textToOpen));
				if (file.exists() && !file.isDirectory()) {
					try {
						IEditorPart editor = IDE.openEditor(site.getPage(), file.toURI(),
								IDE.getEditorDescriptor(file.getName(), true, true).getId(), true);
						goToLine(fileRef, editor);
						return;
					} catch (Exception e) {
						// continue
					}
				}
				ResourcesPlugin.getPlugin();
				OpenResourceDialog openResourceDialog = new OpenResourceDialog(site.getShell(),
						ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);
				openResourceDialog.setInitialPattern(textToOpen);
				if (openResourceDialog.open() != Window.OK) {
					return;
				}
				Object[] results = openResourceDialog.getResult();
				List<IFile> files = new ArrayList<>();
				for (Object result : results) {
					if (result instanceof IFile) {
						files.add((IFile) result);
					}
				}
				if (files.size() > 0) {

					final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if (window == null) {
						throw new ExecutionException("no active workbench window"); //$NON-NLS-1$
					}

					final IWorkbenchPage page = window.getActivePage();
					if (page == null) {
						throw new ExecutionException("no active workbench page"); //$NON-NLS-1$
					}

					try {
						for (IFile iFile : files) {
							IEditorPart editor = IDE.openEditor(page, iFile, true);
							goToLine(fileRef, editor);
						}
					} catch (final PartInitException e) {
						throw new ExecutionException("error opening file in editor", e); //$NON-NLS-1$
					}
				}
			} catch (IllegalArgumentException | NullPointerException | ExecutionException | PartInitException e) {
				ILog.of(getClass()).error("Failed to activate OpenResourceDialog", e); //$NON-NLS-1$
			}

		}

	}

	private boolean bundleAvailable(String symbolicName) {
		Bundle bundle = Platform.getBundle(symbolicName);
		boolean available = bundle != null && bundle.getState() != Bundle.UNINSTALLED
				&& bundle.getState() != Bundle.STOPPING;
		return available;
	}

	private void goToLine(FileReference fileRef, IEditorPart editor) {
		if (!fileRef.hasLine()) {
			return;
		}
		ITextEditor textEditor = Adapters.adapt(editor, ITextEditor.class);
		if (textEditor != null) {
			Optional<Integer> optionalOffset = getOffsetFromFileRef(textEditor, fileRef);
			optionalOffset.ifPresent(offset -> textEditor.selectAndReveal(offset, 0));
		}
	}

	private static Optional<Integer> getOffsetFromFileRef(ITextEditor editor, FileReference fileRef) {
		int line = fileRef.line();
		IDocumentProvider provider = editor.getDocumentProvider();
		IEditorInput input = editor.getEditorInput();
		try {
			provider.connect(input);
		} catch (CoreException e) {
			return Optional.empty();
		}
		try {
			IDocument document = provider.getDocument(input);
			if (document == null) {
				return Optional.empty();
			}
			// document lines are 0-based, input lines are 1-based
			int zeroBasedLine = line - 1;
			int lineOffset = document.getLineOffset(zeroBasedLine);
			if (fileRef.hasColumn()) {
				int lineLength = document.getLineLength(zeroBasedLine);
				int col = Math.min(fileRef.column(), lineLength);
				lineOffset += col;
			}
			return Optional.of(lineOffset);
		} catch (BadLocationException e) {
			return Optional.empty();
		} finally {
			provider.disconnect(input);
		}
	}
}