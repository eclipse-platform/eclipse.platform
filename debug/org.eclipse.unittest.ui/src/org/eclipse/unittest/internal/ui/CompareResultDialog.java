/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.osgi.framework.FrameworkUtil;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.PlatformUI;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;

/**
 * A Compare result dialog
 */
public class CompareResultDialog extends TrayDialog {
	private static final String PREFIX_SUFFIX_PROPERTY = UnitTestPlugin.PLUGIN_ID + ".CompareResultDialog.prefixSuffix"; //$NON-NLS-1$

	private static class CompareResultMergeViewer extends TextMergeViewer {
		private CompareResultMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
			super(parent, style, configuration);
		}

		@Override
		protected void createControls(Composite composite) {
			super.createControls(composite);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IUnitTestHelpContextIds.RESULT_COMPARE_DIALOG);
		}

//        protected void createToolItems(ToolBarManager tbm) {
//    		ResourceBundle bundle= CompareUI.getResourceBundle();
//    		tbm.add(new IgnoreWhiteSpaceAction(bundle, getCompareConfiguration()));
//    		super.createToolItems(tbm);
//        }

		@Override
		protected void configureTextViewer(TextViewer textViewer) {
			if (textViewer instanceof SourceViewer) {
				int[] prefixSuffixOffsets = (int[]) getCompareConfiguration().getProperty(PREFIX_SUFFIX_PROPERTY);
				((SourceViewer) textViewer).configure(new CompareResultViewerConfiguration(prefixSuffixOffsets));
			}
		}
	}

	private static class CompareResultViewerConfiguration extends SourceViewerConfiguration {
		private static class SimpleDamagerRepairer implements IPresentationDamager, IPresentationRepairer {
			private IDocument fDocument;
			private final int[] fPrefixSuffixOffsets2;

			public SimpleDamagerRepairer(int[] prefixSuffixOffsets) {
				fPrefixSuffixOffsets2 = prefixSuffixOffsets;
			}

			@Override
			public void setDocument(IDocument document) {
				fDocument = document;
			}

			@Override
			public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent event, boolean changed) {
				return new Region(0, fDocument.getLength());
			}

			@Override
			public void createPresentation(TextPresentation presentation, ITypedRegion damage) {
				presentation.setDefaultStyleRange(new StyleRange(0, fDocument.getLength(), null, null));
				int prefix = fPrefixSuffixOffsets2[0];
				int suffix = fPrefixSuffixOffsets2[1];
				TextAttribute attr = new TextAttribute(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				presentation.addStyleRange(new StyleRange(prefix, fDocument.getLength() - suffix - prefix,
						attr.getForeground(), attr.getBackground(), attr.getStyle()));
			}
		}

		private final int[] fPrefixSuffixOffsets;

		public CompareResultViewerConfiguration(int[] prefixSuffixOffsets) {
			fPrefixSuffixOffsets = prefixSuffixOffsets;
		}

		@Override
		public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
			PresentationReconciler reconciler = new PresentationReconciler();
			SimpleDamagerRepairer dr = new SimpleDamagerRepairer(fPrefixSuffixOffsets);
			reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
			reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
			return reconciler;
		}
	}

	private static class CompareElement implements ITypedElement, IEncodedStreamContentAccessor {
		private String fContent;

		public CompareElement(String content) {
			fContent = content;
		}

		@Override
		public String getName() {
			return "<no name>"; //$NON-NLS-1$
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getType() {
			return "txt"; //$NON-NLS-1$
		}

		@Override
		public InputStream getContents() {
			return new ByteArrayInputStream(fContent.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public String getCharset() throws CoreException {
			return "UTF-8"; //$NON-NLS-1$
		}
	}

	private TextMergeViewer fViewer;
	private FailureTrace trace;
	private String fTestName;

	/**
	 * Lengths of common prefix and suffix. Note: this array is passed to the
	 * DamagerRepairer and the lengths are updated on content change.
	 */
	private final int[] fPrefixSuffix = new int[2];

	private CompareViewerPane fCompareViewerPane;

	/**
	 * Constructs a compare results dialog
	 *
	 * @param parentShell a parent shell object
	 * @param element     a {@link TestElement} object
	 */
	public CompareResultDialog(Shell parentShell, TestElement element) {
		super(parentShell);
		setShellStyle((getShellStyle() & ~SWT.APPLICATION_MODAL) | SWT.TOOL);
		setFailedTest(element);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private void setFailedTest(TestElement failedTest) {
		fTestName = failedTest.getTestName();
		trace = failedTest.getFailureTrace();
		computePrefixSuffix();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return getDialogSettingsSection(getClass().getName());
	}

	/**
	 * Returns the section with the given name in this dialog settings.
	 *
	 * @param name the key
	 * @return {@link IDialogSettings} (the section)
	 */
	private IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(getClass()))
				.getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(name);
		if (section == null) {
			section = dialogSettings.addNewSection(name);
		}
		return section;
	}

	private void computePrefixSuffix() {
		String expected = trace.getExpected();
		String actual = trace.getActual();
		int end = Math.min(expected.length(), actual.length());
		int i = 0;
		for (; i < end; i++)
			if (expected.charAt(i) != actual.charAt(i))
				break;
		fPrefixSuffix[0] = i;

		int j = expected.length() - 1;
		int k = actual.length() - 1;
		int l = 0;
		for (; k >= i && j >= i; k--, j--) {
			if (expected.charAt(j) != actual.charAt(k))
				break;
			l++;
		}
		fPrefixSuffix[1] = l;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.CompareResultDialog_title);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IUnitTestHelpContextIds.RESULT_COMPARE_DIALOG);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);

		fCompareViewerPane = new CompareViewerPane(composite, SWT.BORDER | SWT.FLAT);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		data.widthHint = convertWidthInCharsToPixels(120);
		data.heightHint = convertHeightInCharsToPixels(13);
		fCompareViewerPane.setLayoutData(data);

		Control previewer = createPreviewer(fCompareViewerPane);
		fCompareViewerPane.setContent(previewer);
		GridData gd = new GridData(GridData.FILL_BOTH);
		previewer.setLayoutData(gd);
		applyDialogFont(parent);
		return composite;
	}

	private Control createPreviewer(Composite parent) {
		final CompareConfiguration compareConfiguration = new CompareConfiguration();
		compareConfiguration.setLeftLabel(Messages.CompareResultDialog_expectedLabel);
		compareConfiguration.setLeftEditable(false);
		compareConfiguration.setRightLabel(Messages.CompareResultDialog_actualLabel);
		compareConfiguration.setRightEditable(false);
		compareConfiguration.setProperty(CompareConfiguration.IGNORE_WHITESPACE, Boolean.FALSE);
		compareConfiguration.setProperty(PREFIX_SUFFIX_PROPERTY, fPrefixSuffix);

		fViewer = new CompareResultMergeViewer(parent, SWT.NONE, compareConfiguration);
		setCompareViewerInput();

		Control control = fViewer.getControl();
		control.addDisposeListener(e -> compareConfiguration.dispose());
		return control;
	}

	private void setCompareViewerInput() {
		if (!fViewer.getControl().isDisposed()) {
			fViewer.setInput(
					new DiffNode(new CompareElement(trace.getExpected()), new CompareElement(trace.getActual())));
			fCompareViewerPane.setText(fTestName);
		}
	}

	/**
	 * Sets a failed {@link ITestElement} as input for the CompareResultDialog
	 *
	 * @param failedTest a failed test element
	 */
	public void setInput(TestElement failedTest) {
		setFailedTest(failedTest);
		setCompareViewerInput();
	}
}
