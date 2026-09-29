/*******************************************************************************
 * Copyright (c) 2026 SAP
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * SAP - initial implementation
 *******************************************************************************/
package org.eclipse.compare.unifieddiff;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.compare.contentmergeviewer.IIgnoreWhitespaceContributor;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.unifieddiff.internal.ToolbarActionPresentations;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

public final class UnifiedDiff {

	private UnifiedDiff() {
	}

	@FunctionalInterface
	public static interface TokenComparatorFactory extends Function<String, ITokenComparator> {
	}

	@FunctionalInterface
	public static interface IgnoreWhitespaceContributorFactory
			extends Function<IDocument, Optional<IIgnoreWhitespaceContributor>> {
	}

	/**
	 * Shows the unified diff in the given editor.
	 * @param editor the text editor where the diff will be shown
	 * @param source the source content to compare
	 * @param mode the mode in which the diff will be displayed
	 * @return a builder to configure and open the unified diff
	 */
	public static Builder create(ITextEditor editor, String source, UnifiedDiffMode mode) {
		return new Builder(editor, source, mode);
	}

	/**
	 * Identifies a built-in action of a unified diff toolbar, so that its text or
	 * image can be replaced. Which actions are shown depends on the
	 * {@link UnifiedDiffMode}; unused constants are simply ignored.
	 *
	 * @see Builder#toolbarActionText(ToolbarAction, String)
	 * @see Builder#toolbarActionImage(ToolbarAction, ImageDescriptor)
	 */
	public enum ToolbarAction {
		/** Applies every diff, in the toolbar shown for all diffs. */
		ACCEPT_ALL,
		/** Discards every diff, in the toolbar shown for all diffs. */
		HIDE_ALL,
		/** Reverts every diff, in the toolbar shown for all diffs. */
		REVERT_ALL,
		/** Keeps every diff, in the toolbar shown for all diffs. */
		KEEP_ALL,
		/** Undoes every diff, in the toolbar shown for all diffs. */
		UNDO_ALL,
		/** Reveals the previous diff, in the toolbar shown for all diffs. */
		PREVIOUS,
		/** Reveals the next diff, in the toolbar shown for all diffs. */
		NEXT,
		/** Applies the hovered diff, in the toolbar shown for a single diff. */
		ACCEPT,
		/** Discards the hovered diff, in the toolbar shown for a single diff. */
		HIDE,
		/** Reverts the hovered diff, in the toolbar shown for a single diff. */
		REVERT,
		/** Keeps the hovered diff, in the toolbar shown for a single diff. */
		KEEP,
		/** Undoes the hovered diff, in the toolbar shown for a single diff. */
		UNDO
	}

	public static final class Builder {
		// Required parameters
		private final ITextEditor editor;
		private final String source;
		private final UnifiedDiffMode mode;
		private boolean ignoreWhiteSpace = true;

		// Optional parameters
		private List<Action> additionalActions;
		private TokenComparatorFactory tokenComparatorFactory;
		private IgnoreWhitespaceContributorFactory ignoreWhitespaceContributorFactory;
		private int foldContextLines = -1;
		private final Map<ToolbarAction, String> toolbarActionTexts = new EnumMap<>(ToolbarAction.class);
		private final Map<ToolbarAction, ImageDescriptor> toolbarActionImages = new EnumMap<>(ToolbarAction.class);

		private Builder(ITextEditor editor, String source, UnifiedDiffMode mode) {
			this.editor = Objects.requireNonNull(editor, "Editor cannot be null"); //$NON-NLS-1$
			this.source = Objects.requireNonNull(source, "Source cannot be null"); //$NON-NLS-1$
			this.mode = Objects.requireNonNull(mode, "Mode cannot be null"); //$NON-NLS-1$
		}

		public Builder additionalActions(List<Action> actions) {
			this.additionalActions = actions;
			return this;
		}

		public Builder ignoreWhitespaceContributorFactory(IgnoreWhitespaceContributorFactory factory) {
			this.ignoreWhitespaceContributorFactory = factory;
			return this;
		}

		public Builder tokenComparatorFactory(TokenComparatorFactory factory) {
			this.tokenComparatorFactory = factory;
			return this;
		}

		public Builder ignoreWhiteSpace(boolean value) {
			ignoreWhiteSpace = value;
			return this;
		}

		/**
		 * Overrides the text, including the tooltip text, for an action in a unified
		 * diff toolbar.
		 *
		 * @param action the action whose text is overridden
		 * @param text the replacement text
		 * @return this builder
		 */
		public Builder toolbarActionText(ToolbarAction action, String text) {
			toolbarActionTexts.put(Objects.requireNonNull(action, "Action cannot be null"), //$NON-NLS-1$
					Objects.requireNonNull(text, "Action text cannot be null")); //$NON-NLS-1$
			return this;
		}

		/**
		 * Overrides the image for an action in a unified diff toolbar. Passing
		 * <code>null</code> removes the action's default image and displays its text
		 * instead.
		 *
		 * @param action the action whose image is overridden
		 * @param image the replacement image, or <code>null</code> to show text
		 * @return this builder
		 */
		public Builder toolbarActionImage(ToolbarAction action, ImageDescriptor image) {
			toolbarActionImages.put(Objects.requireNonNull(action, "Action cannot be null"), image); //$NON-NLS-1$
			return this;
		}

		/**
		 * Collapses unchanged regions between diffs, keeping the given number of
		 * context lines (at least one) around each change. A negative value disables
		 * folding.
		 * <p>
		 * The folding of the editor is reused, so this has no effect in an editor
		 * without folding support, such as the default text editor, or in an editor
		 * whose folding the user turned off.
		 */
		public Builder foldUnchanged(int contextLines) {
			this.foldContextLines = contextLines;
			return this;
		}

		public IStatus open() {
			return UnifiedDiffManager.open(editor, source, mode, additionalActions, tokenComparatorFactory,
					ignoreWhitespaceContributorFactory, ignoreWhiteSpace, foldContextLines,
					new ToolbarActionPresentations(toolbarActionTexts, toolbarActionImages));
		}
	}
}
