/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import java.util.Optional;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Factory for creating a cell editor with content assist for the Expressions
 * view and for configuring the source viewer in the Add/Edit Watch Expression
 * dialog.
 * <p>
 * Clients may implement this interface. Debug-context elements that support
 * language-aware watch expression editing should adapt to this interface.
 * </p>
 *
 * @since 3.23
 */
public interface IWatchExpressionCellEditorFactory {

	/**
	 * Creates and returns a new cell editor suitable for editing a watch expression
	 * in the Expressions view. The returned editor may provide language-specific
	 * content assist and must use {@link String} as its value type.
	 *
	 * @param parent the composite to use as the parent of the cell editor's control
	 * @return an {@link Optional} containing the cell editor, or an empty {@link Optional}
	 *         if this factory cannot provide one in the current state
	 */
	Optional<CellEditor> createCellEditor(Composite parent);

	/**
	 * Configures the given source viewer for language-aware editing of a watch
	 * expression, typically inside the Add/Edit Watch Expression dialog.
	 *
	 * @param viewer the source viewer to configure; never {@code null}
	 * @since 3.23
	 */
	default void configureSourceViewer(SourceViewer viewer) {
		viewer.configure(new SourceViewerConfiguration());
	}

	/**
	 * Prepares the given document for language-aware editing
	 *
	 * @param document the document to prepare; never {@code null}
	 * @since 3.23
	 */
	default void prepareDocument(IDocument document) {
		// nothing by default
	}
}
