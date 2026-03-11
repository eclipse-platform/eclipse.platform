package org.eclipse.compare.unifieddiff;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.compare.contentmergeviewer.IIgnoreWhitespaceContributor;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

public final class UnifiedDiff {

	private UnifiedDiff() {
	}

	public static enum UnifiedDiffMode {
		/**
		 * Diffs are directly applied in the editor. Users have the possibility to keep
		 * or undo the applied diffs.
		 */
		REPLACE_MODE,
		/**
		 * The source in the editor is not modified. Diffs are shown as code mining and
		 * users have the possibility to apply or cancel individual diffs.
		 */
		OVERLAY_MODE,
		/**
		 * The source in the editor is not modified. Diffs are shown as code mining and
		 * users cannot apply or dismiss the diffs (read-only mode).
		 */
		OVERLAY_READ_ONLY_MODE
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

		public void open() {
			UnifiedDiffManager.open(editor, source, mode, additionalActions, tokenComparatorFactory,
					ignoreWhitespaceContributorFactory, ignoreWhiteSpace);
		}
	}
}
