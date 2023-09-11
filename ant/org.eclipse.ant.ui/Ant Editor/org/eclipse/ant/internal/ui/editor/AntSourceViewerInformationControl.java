/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.editor;

import org.eclipse.ant.internal.ui.AntSourceViewerConfiguration;
import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class AntSourceViewerInformationControl implements IInformationControl, IInformationControlExtension, DisposeListener {
	/** The control's shell */
	private Shell fShell;

	/** The control's source viewer */
	private final SourceViewer fViewer;

	/** The control's text widget */
	private StyledText fText;

	public AntSourceViewerInformationControl(Shell parent) {
		GridLayout layout;
		GridData gd;

		fShell = new Shell(parent, SWT.ON_TOP | SWT.TOOL);
		Display display = fShell.getDisplay();
		fShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		Composite composite = fShell;
		layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(gd);
		fViewer = createViewer(composite);

		fText = fViewer.getTextWidget();
		gd = new GridData(GridData.BEGINNING | GridData.FILL_BOTH);
		fText.setLayoutData(gd);
		fText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		fText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		fText.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == 0x1B) // ESC
					fShell.dispose();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});
	}

	private SourceViewer createViewer(Composite parent) {
		SourceViewer viewer = new SourceViewer(parent, null, SWT.NONE);

		SourceViewerConfiguration configuration = new AntSourceViewerConfiguration();
		viewer.configure(configuration);
		viewer.setEditable(false);
		Font font = JFaceResources.getFont(JFaceResources.TEXT_FONT);
		viewer.getTextWidget().setFont(font);

		return viewer;
	}

	@Override
	public void setInformation(String content) {
		if (content == null) {
			fViewer.setInput(null);
			return;
		}
		IDocument document = new Document(content);
		new AntDocumentSetupParticipant().setup(document);
		fViewer.setDocument(document);
	}

	@Override
	public void setSizeConstraints(int maxWidth, int maxHeight) {
		// do nothing
	}

	@Override
	public Point computeSizeHint() {
		return fShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	@Override
	public void setVisible(boolean visible) {
		fShell.setVisible(visible);
	}

	@Override
	public void setSize(int width, int height) {
		fShell.setSize(width, height);
	}

	@Override
	public void setLocation(Point location) {
		Rectangle trim = fShell.computeTrim(0, 0, 0, 0);
		Point textLocation = fText.getLocation();
		location.x += trim.x - textLocation.x;
		location.y += trim.y - textLocation.y;
		fShell.setLocation(location);
	}

	@Override
	public void dispose() {
		if (fShell != null && !fShell.isDisposed()) {
			fShell.dispose();
		} else {
			widgetDisposed(null);
		}

	}

	@Override
	public void addDisposeListener(DisposeListener listener) {
		fShell.addDisposeListener(listener);

	}

	@Override
	public void removeDisposeListener(DisposeListener listener) {
		fShell.removeDisposeListener(listener);
	}

	@Override
	public void setForegroundColor(Color foreground) {
		fText.setForeground(foreground);
	}

	@Override
	public void setBackgroundColor(Color background) {
		fText.setBackground(background);
	}

	@Override
	public boolean isFocusControl() {
		return fText.isFocusControl();
	}

	@Override
	public void setFocus() {
		fShell.forceFocus();
		fText.setFocus();

	}

	@Override
	public void addFocusListener(FocusListener listener) {
		fText.addFocusListener(listener);
	}

	@Override
	public void removeFocusListener(FocusListener listener) {
		fText.removeFocusListener(listener);

	}

	@Override
	public boolean hasContents() {
		return fText.getCharCount() > 0;
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		fShell = null;
		fText = null;
	}
}