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

package org.eclipse.ui.console;

import java.util.HashMap;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.internal.console.ConsoleDocument;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;
import org.eclipse.ui.internal.console.ConsolePatternMatcher;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * An abstract text console that supports regular expression matching and
 * hyperlinks.
 * <p>
 * Pattern match listeners can be registered with a console programmatically
 * or via the <code>org.eclipse.ui.console.consolePatternMatchListeners</code>
 * extension point.
 * </p>
 * <p>
 * Clients may subclass this class. Subclasses must provide a document partitioner.
 * </p>
 * @since 3.1
 */
public abstract class TextConsole extends AbstractConsole {

	/**
	 * The current width of the console. Used for fixed width consoles.
	 * A value of <=0 means does not have a fixed width.
	 */
	private int fConsoleWidth;
	/**
	 * The current tab width
	 */
	private int fTabWidth;
	/**
	 * The font used by this console
	 */
	private Font fFont;

	/**
	 * The background color used by this console or <code>null</code> if default
	 */
	private Color fBackground;

	/**
	 * The Console's regular expression pattern matcher
	 */
	private ConsolePatternMatcher fPatternMatcher;

	/**
	 * The Console's document
	 */
	private ConsoleDocument fDocument;

	/**
	 * indication that the console's partitioner is not expecting more input
	 */
	private boolean fPartitionerFinished = false;

	/**
	 * Indication that the console's pattern matcher has finished.
	 * (all matches have been found and all listeners notified)
	 */
	private boolean fMatcherFinished = false;

	/**
	 * indication that the console output complete property has been fired
	 */
	private boolean fCompleteFired = false;

	private boolean fConsoleAutoScrollLock = true;


	/**
	 * Map of client defined attributes
	 */
	private HashMap<String, Object> fAttributes = new HashMap<>();

	private IConsoleManager fConsoleManager = ConsolePlugin.getDefault().getConsoleManager();
	private ScopedPreferenceStore store;
	private IPropertyChangeListener propListener;

	@Override
	protected void dispose() {
		super.dispose();
		fFont = null;
		synchronized(fAttributes) {
			fAttributes.clear();
		}
		if (store != null) {
			store.removePropertyChangeListener(propListener);
		}
	}
	/**
	 * Constructs a console with the given name, image descriptor, and lifecycle
	 *
	 * @param name name to display for this console
	 * @param consoleType console type identifier or <code>null</code>
	 * @param imageDescriptor image to display for this console or <code>null</code>
	 * @param autoLifecycle whether lifecycle methods should be called automatically
	 *  when this console is added/removed from the console manager
	 */
	public TextConsole(String name, String consoleType, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, autoLifecycle);
		fDocument = new ConsoleDocument();
		fDocument.addPositionCategory(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
		fPatternMatcher = new ConsolePatternMatcher(this);
		fDocument.addDocumentListener(fPatternMatcher);
		fTabWidth = IConsoleConstants.DEFAULT_TAB_SIZE;
	}

	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		return new TextConsolePage(this, view);
	}

	/**
	 * Returns this console's document.
	 * <p>
	 * Note that a console may or may not support direct manipulation of its document.
	 * For example, an I/O console document and its partitions are produced from the
	 * streams connected to it, and clients are not intended to modify the document's
	 * contents.
	 * </p>
	 *
	 * @return this console's document
	 */
	public IDocument getDocument() {
		return fDocument;
	}

	/**
	 * Returns the current width of this console. A value of zero of less
	 * indicates this console has no fixed width.
	 *
	 * @return the current width of this console
	 */
	public int getConsoleWidth() {
		return fConsoleWidth;
	}

	/**
	 * Returns the user preference for enabling auto scroll lock feature.
	 *
	 * @return auto scroll lock
	 * @since 3.8
	 */
	public boolean isConsoleAutoScrollLock() {
		return fConsoleAutoScrollLock;
	}

	/**
	 * Sets the auto scroll lock preference.
	 *
	 * @param autoScrollLockPref enable auto scroll lock preference.
	 * @since 3.8
	 */
	public void setConsoleAutoScrollLock(boolean autoScrollLockPref) {
		if (fConsoleAutoScrollLock != autoScrollLockPref) {
			boolean old = fConsoleAutoScrollLock;
			fConsoleAutoScrollLock = autoScrollLockPref;

			firePropertyChange(this, IConsoleConstants.P_CONSOLE_AUTO_SCROLL_LOCK, Boolean.valueOf(old), Boolean.valueOf(fConsoleAutoScrollLock));
		}
	}

	@Override
	protected void init() {
		super.init();
		String qualifier = ConsolePlugin.getUniqueIdentifier();
		String key = IConsoleConstants.P_CONSOLE_AUTO_SCROLL_LOCK;
		setConsoleAutoScrollLock(Platform.getPreferencesService().getBoolean(qualifier, key, true, null));
		store = new ScopedPreferenceStore(InstanceScope.INSTANCE, qualifier);

		propListener = event -> {
			String property = event.getProperty();
			if (key.equals(property)) {
				setConsoleAutoScrollLock(store.getBoolean(key));
			}
		};
		store.addPropertyChangeListener(propListener);
	}

	/**
	 * Sets the width of this console in characters. Any value greater than zero
	 * will cause this console to have a fixed width.
	 *
	 * @param width the width to make this console. Values of 0 or less imply
	 *            the console does not have any fixed width.
	 */
	public void setConsoleWidth(int width) {
		if (fConsoleWidth != width) {
			int old = fConsoleWidth;
			fConsoleWidth = width;

			firePropertyChange(this, IConsoleConstants.P_CONSOLE_WIDTH, Integer.valueOf(old), Integer.valueOf(fConsoleWidth));
		}
	}

	/**
	 * Sets the tab width used in this console.
	 *
	 * @param newTabWidth the tab width
	 */
	public void setTabWidth(final int newTabWidth) {
		if (fTabWidth != newTabWidth) {
			final int oldTabWidth = fTabWidth;
			fTabWidth = newTabWidth;
			ConsolePlugin.getStandardDisplay().asyncExec(() -> firePropertyChange(TextConsole.this, IConsoleConstants.P_TAB_SIZE, Integer.valueOf(oldTabWidth), Integer.valueOf(fTabWidth)));
		}
	}

	/**
	 * Returns the tab width used in this console.
	 *
	 * @return tab width used in this console
	 */
	public int getTabWidth() {
		return fTabWidth;
	}

	/**
	 * Returns the font used by this console. Must be called in the UI thread.
	 *
	 * @return font used by this console
	 */
	public Font getFont() {
		if (fFont == null) {
			fFont = getDefaultFont();
		}
		return fFont;
	}

	/**
	 * Returns the default text font.
	 *
	 * @return the default text font
	 */
	private Font getDefaultFont() {
		return JFaceResources.getFont(JFaceResources.TEXT_FONT);
	}

	/**
	 * Sets the font used by this console. Specify <code>null</code> to use
	 * the default text font.
	 *
	 * @param newFont font, or <code>null</code> to indicate the default font
	 */
	public void setFont(Font newFont) {
		// ensure font is initialized
		getFont();
		// translate null to default font
		Font font = newFont;
		if (font == null) {
			font = getDefaultFont();
		}
		// fire property change if required
		if (!fFont.equals(font)) {
			Font old = fFont;
			fFont = font;
			firePropertyChange(this, IConsoleConstants.P_FONT, old, fFont);
		}
	}

	/**
	 * Sets the background color used by this console. Specify <code>null</code> to use
	 * the default background color.
	 *
	 * @param background background color or <code>null</code> for default
	 * @since 3.3
	 * @deprecated use setBackground(Color) instead
	 */
	@Deprecated
	public void setBackgrond(Color background) {
		setBackground(background);
	}

	/**
	 * Sets the background color used by this console. Specify <code>null</code> to use
	 * the default background color.
	 *
	 * @param background background color or <code>null</code> for default
	 * @since 3.3
	 */
	public void setBackground(Color background) {
		if (fBackground == null) {
			if (background == null) {
				return;
			}
		} else if (fBackground.equals(background)){
			return;
		}
		Color old = fBackground;
		fBackground = background;
		firePropertyChange(this, IConsoleConstants.P_BACKGROUND_COLOR, old, fBackground);
	}

	/**
	 * Returns the background color to use for this console or <code>null</code> for the
	 * default background color.
	 *
	 * @return background color or <code>null</code> for default
	 * @since 3.3
	 */
	public Color getBackground() {
		return fBackground;
	}

	/**
	 * Clears the console.
	 * <p>
	 * Since a console may or may not support direct manipulation
	 * of its document's contents, this method should be called to clear a text console's
	 * document. The default implementation sets this console's document content
	 * to the empty string directly. Subclasses should override as required.
	 * </p>
	 */
	public void clearConsole() {
		IDocument document = getDocument();
		if (document != null) {
			document.set(""); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the console's document partitioner.
	 * @return The console's document partitioner
	 */
	protected abstract IConsoleDocumentPartitioner getPartitioner();

	/**
	 * Returns all hyperlinks in this console.
	 *
	 * @return all hyperlinks in this console
	 */
	public IHyperlink[] getHyperlinks() {
		try {
			Position[] positions = getDocument().getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
			IHyperlink[] hyperlinks = new IHyperlink[positions.length];
			for (int i = 0; i < positions.length; i++) {
				ConsoleHyperlinkPosition position = (ConsoleHyperlinkPosition) positions[i];
				hyperlinks[i] = position.getHyperLink();
			}
			return hyperlinks;
		} catch (BadPositionCategoryException e) {
			return new IHyperlink[0];
		}
	}

	/**
	 * Returns the hyperlink at the given offset or <code>null</code> if none.
	 *
	 * @param offset offset for which a hyperlink is requested
	 * @return the hyperlink at the given offset or <code>null</code> if none
	 */
	public IHyperlink getHyperlink(int offset) {
		try {
			IDocument document = getDocument();
			if (document != null) {
				Position[] positions = document.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
				Position position = findPosition(offset, positions);
				if (position instanceof ConsoleHyperlinkPosition) {
					return ((ConsoleHyperlinkPosition) position).getHyperLink();
				}
			}
		} catch (BadPositionCategoryException e) {
		}
		return null;
	}

	/**
	 * Binary search for the position at a given offset.
	 *
	 * @param offset    the offset whose position should be found
	 * @param positions the positions list to search in
	 * @return the position containing the offset, or <code>null</code>
	 */
	private Position findPosition(int offset, Position[] positions) {

		if (positions.length == 0) {
			return null;
		}

		int left= 0;
		int right= positions.length -1;
		int mid= 0;
		Position position= null;

		while (left < right) {

			mid= (left + right) / 2;

			position= positions[mid];
			if (offset < position.getOffset()) {
				if (left == mid) {
					right= left;
				} else {
					right= mid -1;
				}
			} else if (offset > (position.getOffset() + position.getLength() - 1)) {
				if (right == mid) {
					left= right;
				} else {
					left= mid  +1;
				}
			} else {
				left= right= mid;
			}
		}

		position= positions[left];
		if (offset >= position.getOffset() && (offset < (position.getOffset() + position.getLength()))) {
			return position;
		}
		return null;
	}

	/**
	 * Adds the given pattern match listener to this console. The listener will
	 * be connected and receive match notifications. Has no effect if an identical
	 * listener has already been added.
	 *
	 * @param listener the listener to add
	 */
	public void addPatternMatchListener(IPatternMatchListener listener) {
		fPatternMatcher.addPatternMatchListener(listener);
	}

	/**
	 * Removes the given pattern match listener from this console. The listener will be
	 * disconnected and will no longer receive match notifications. Has no effect
	 * if the listener was not previously added.
	 *
	 * @param listener the pattern match listener to remove
	 */
	public void removePatternMatchListener(IPatternMatchListener listener) {
		fPatternMatcher.removePatternMatchListener(listener);
	}


	/**
	 * Job scheduling rule that prevent the job from running if the console's PatternMatcher
	 * is active.
	 */
	private class MatcherSchedulingRule implements ISchedulingRule {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			if (contains(rule)) {
				return true;
			}
			if (rule != this && rule instanceof MatcherSchedulingRule) {
				return (((MatcherSchedulingRule)rule).getConsole() == TextConsole.this);
			}
			return false;
		}

		public TextConsole getConsole() {
			return TextConsole.this;
		}
	}

	/**
	 * Returns a scheduling rule which can be used to prevent jobs from running
	 * while this console's pattern matcher is active.
	 * <p>
	 * Although this scheduling rule prevents jobs from running at the same time as
	 * pattern matching jobs for this console, it does not enforce any ordering of jobs.
	 * Since 3.2, pattern matching jobs belong to the job family identified by the console
	 * object that matching is occurring on. To ensure a job runs after all scheduled pattern
	 * matching is complete, clients must join on this console's job family.
	 * </p>
	 * @return a scheduling rule which can be used to prevent jobs from running
	 * while this console's pattern matcher is active
	 */
	public ISchedulingRule getSchedulingRule() {
		return new MatcherSchedulingRule();
	}

	/**
	 * This console's partitioner should call this method when it is not expecting any new data
	 * to be appended to the document.
	 */
	public void partitionerFinished() {
		fPatternMatcher.forceFinalMatching();
		fPartitionerFinished  = true;
		checkFinished();
	}

	/**
	 * Called by this console's pattern matcher when matching is complete.
	 * <p>
	 * Clients should not call this method.
	 * </p>
	 */
	public void matcherFinished() {
		fMatcherFinished = true;
		fDocument.removeDocumentListener(fPatternMatcher);
		checkFinished();
	}

	/**
	 * Fires the console output complete property change event.
	 */
	private synchronized void checkFinished() {
		if (!fCompleteFired && fPartitionerFinished && fMatcherFinished ) {
			fCompleteFired = true;
			firePropertyChange(this, IConsoleConstants.P_CONSOLE_OUTPUT_COMPLETE, null, null);
		}
	}

	/**
	 * Adds a hyperlink to this console.
	 *
	 * @param hyperlink the hyperlink to add
	 * @param offset the offset in the console document at which the hyperlink should be added
	 * @param length the length of the text which should be hyperlinked
	 * @throws BadLocationException if the specified location is not valid.
	 */
	public void addHyperlink(IHyperlink hyperlink, int offset, int length) throws BadLocationException {
		IDocument document = getDocument();
		ConsoleHyperlinkPosition hyperlinkPosition = new ConsoleHyperlinkPosition(hyperlink, offset, length);
		try {
			document.addPosition(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY, hyperlinkPosition);
			fConsoleManager.refresh(this);
		} catch (BadPositionCategoryException e) {
			ConsolePlugin.log(e);
		}
	}

	/**
	 * Returns the region associated with the given hyperlink.
	 *
	 * @param link hyperlink
	 * @return the region associated with the hyperlink or null if the hyperlink is not found.
	 */
	public IRegion getRegion(IHyperlink link) {
		try {
			IDocument doc = getDocument();
			if (doc != null) {
				Position[] positions = doc.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
				for (Position p : positions) {
					ConsoleHyperlinkPosition position = (ConsoleHyperlinkPosition) p;
					if (position.getHyperLink().equals(link)) {
						return new Region(position.getOffset(), position.getLength());
					}
				}
			}
		} catch (BadPositionCategoryException e) {
		}
		return null;
	}

	/**
	 * Returns the attribute associated with the specified key.
	 *
	 * @param key attribute key
	 * @return the attribute associated with the specified key
	 */
	public Object getAttribute(String key) {
		synchronized (fAttributes) {
			return fAttributes.get(key);
		}
	}

	/**
	 * Sets an attribute value. Intended for client data.
	 *
	 * @param key attribute key
	 * @param value attribute value
	 */
	public void setAttribute(String key, Object value) {
		synchronized(fAttributes) {
			fAttributes.put(key, value);
		}
	}
}
