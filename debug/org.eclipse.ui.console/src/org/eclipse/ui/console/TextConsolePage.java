/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *	   Livar Cunha (livarcocc@gmail.com) - Bug 236049
 *******************************************************************************/

package org.eclipse.ui.console;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.actions.ClearOutputAction;
import org.eclipse.ui.console.actions.TextViewerAction;
import org.eclipse.ui.internal.console.ConsoleMessages;
import org.eclipse.ui.internal.console.ConsoleResourceBundleMessages;
import org.eclipse.ui.internal.console.FollowHyperlinkAction;
import org.eclipse.ui.internal.console.IConsoleHelpContextIds;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.FindNextAction;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

/**
 * A page for a text console.
 * <p>
 * Clients may contribute actions to the context menu of a text console page
 * using the <code>org.eclipse.ui.popupMenus</code> extension point. The context
 * menu identifier for a text console page is the associated console's type
 * suffixed with <code>.#ContextMenu</code>. When a console does not specify
 * a type, the context menu id is <code>#ContextMenu</code>.
 * </p>
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.1
 */
public class TextConsolePage implements IPageBookViewPage, IPropertyChangeListener, IAdaptable {
	private IPageSite fSite;
	private final TextConsole fConsole;
	private final IConsoleView fConsoleView;
	private TextConsoleViewer fViewer;
	private MenuManager fMenuManager;
	// Font created from persisted preferences; disposed by this page to avoid leaks
	private Font fPersistedFont;
	protected Map<String, IAction> fGlobalActions = new HashMap<>();
	protected ArrayList<String> fSelectionActions = new ArrayList<>();
	protected ClearOutputAction fClearOutputAction;
	/**
	 * Data key used to store a custom zoom font on a StyledText widget. The font
	 * stored under this key is managed per-widget and disposed via a
	 * DisposeListener on that widget.
	 */
	private static final String ZOOM_FONT_KEY = TextConsolePage.class.getName() + ".zoomFont"; //$NON-NLS-1$

	/**
	 * Minimum font size for console zoom.
	 */
	private static final int MIN_FONT_SIZE = 6;

	/**
	 * Maximum font size for console zoom.
	 */
	private static final int MAX_FONT_SIZE = 72;

	/**
	 * Font size change step for zoom in/out.
	 */
	private static final int FONT_SIZE_STEP = 1;

	/**
	 * Preference key prefix used to store per-console-type font settings.
	 * The full key is: console.font.<consoleType>
	 * The value format is: name|height|style
	 */
	private static final String FONT_PREF_KEY_PREFIX = "console.font."; //$NON-NLS-1$

	// text selection listener, used to update selection dependent actions on selection changes
	private final ISelectionChangedListener selectionChangedListener =  event -> updateSelectionDependentActions();

	// updates the find actions and the clear action if the document length is > 0
	private final ITextListener textListener = event -> {
		Stream.of(ActionFactory.FIND.getId(), ITextEditorActionConstants.FIND_NEXT,
				ITextEditorActionConstants.FIND_PREVIOUS)
				.map(id -> fGlobalActions.get(id)).filter(Objects::nonNull).map(IUpdate.class::cast)
				.forEach(IUpdate::update);

		if (fClearOutputAction != null) {
			IDocument doc = fViewer.getDocument();
			if(doc != null) {
				fClearOutputAction.setEnabled(doc.getLength() > 0);
			}
		}
	};

	/**
	 * Constructs a text console page for the given console in the given view.
	 *
	 * @param console text console
	 * @param view console view the page is contained in
	 */
	public TextConsolePage(TextConsole console, IConsoleView view) {
		fConsole = console;
		fConsoleView = view;
	}

	/**
	 * Returns a viewer used to display the contents of this page's console.
	 *
	 * @param parent container for the viewer
	 * @return a viewer used to display the contents of this page's console
	 */
	protected TextConsoleViewer createViewer(Composite parent) {
		return new TextConsoleViewer(parent, fConsole, fConsoleView);
	}

	@Override
	public IPageSite getSite() {
		return fSite;
	}

	@Override
	public void init(IPageSite pageSite) throws PartInitException {
		fSite = pageSite;
	}

	/**
	 * Updates selection dependent actions.
	 */
	protected void updateSelectionDependentActions() {
		for (String string : fSelectionActions) {
			updateAction(string);
		}
	}

	@Override
	public void createControl(Composite parent) {
		fViewer = createViewer(parent);
		// Restore font for this console type (if saved)
		Font prefFont = loadFontFromPreferences();
		if (prefFont != null) {
			// keep a reference so we can dispose it when the page is disposed
			fPersistedFont = prefFont;
			fViewer.setFont(prefFont);
		}
		fViewer.setConsoleWidth(fConsole.getConsoleWidth());
		fViewer.setTabWidth(fConsole.getTabWidth());
		fConsole.addPropertyChangeListener(this);
		JFaceResources.getFontRegistry().addListener(this);

		String id = "#ContextMenu"; //$NON-NLS-1$
		if (getConsole().getType() != null) {
			id = getConsole().getType() + "." + id; //$NON-NLS-1$
		}
		fMenuManager= new MenuManager("#ContextMenu", id);  //$NON-NLS-1$
		fMenuManager.setRemoveAllWhenShown(true);
		fMenuManager.addMenuListener(this::contextMenuAboutToShow);
		Menu menu = fMenuManager.createContextMenu(getControl());
		getControl().setMenu(menu);

		createActions();
		configureToolBar(getSite().getActionBars().getToolBarManager());

		getSite().registerContextMenu(id, fMenuManager, fViewer);
		getSite().setSelectionProvider(fViewer);

		fViewer.getSelectionProvider().addSelectionChangedListener(selectionChangedListener);
		fViewer.addTextListener(textListener);

		// Install font zoom key listener on the StyledText widget
		StyledText textWidget = fViewer.getTextWidget();
		if (textWidget != null) {
			installFontZoomKeyListener(textWidget);
		}
	}

	@Override
	public void dispose() {
		fConsole.removePropertyChangeListener(this);
		JFaceResources.getFontRegistry().removeListener(this);

		if (fMenuManager != null) {
			fMenuManager.dispose();
		}
		fClearOutputAction = null;
		fSelectionActions.clear();
		fGlobalActions.clear();

		fViewer.getSelectionProvider().removeSelectionChangedListener(selectionChangedListener);
		fViewer.removeTextListener(textListener);
		fViewer = null;
		// Dispose any font created from persisted preferences to avoid resource leaks
		if (fPersistedFont != null && !fPersistedFont.isDisposed()) {
			fPersistedFont.dispose();
			fPersistedFont = null;
		}
	}

	@Override
	public Control getControl() {
		return fViewer != null ? fViewer.getControl() : null;
	}

	@Override
	public void setActionBars(IActionBars actionBars) {
	}

	@Override
	public void setFocus() {
		if (fViewer != null) {
			fViewer.getTextWidget().setFocus();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (fViewer != null) {
			Object source = event.getSource();
			String property = event.getProperty();

			if (source.equals(fConsole) && IConsoleConstants.P_FONT.equals(property)) {
				fViewer.setFont(fConsole.getFont());
			} else if (IConsoleConstants.P_FONT_STYLE.equals(property)) {
				fViewer.getTextWidget().redraw();
			} else if (property.equals(IConsoleConstants.P_STREAM_COLOR)) {
				fViewer.getTextWidget().redraw();
			} else if (source.equals(fConsole) && property.equals(IConsoleConstants.P_TAB_SIZE)) {
				Integer tabSize = (Integer)event.getNewValue();
				fViewer.setTabWidth(tabSize.intValue());
			} else if (source.equals(fConsole) && property.equals(IConsoleConstants.P_CONSOLE_AUTO_SCROLL_LOCK)) {
				fViewer.setConsoleAutoScrollLock(fConsole.isConsoleAutoScrollLock());
			} else if (source.equals(fConsole) && property.equals(IConsoleConstants.P_CONSOLE_WIDTH)) {
				fViewer.setConsoleWidth(fConsole.getConsoleWidth());
			} else if (IConsoleConstants.P_BACKGROUND_COLOR.equals(property)) {
				fViewer.getTextWidget().setBackground(fConsole.getBackground());
			}
		}
	}

	/**
	 * Creates actions.
	 */
	protected void createActions() {
		IActionBars actionBars= getSite().getActionBars();
		TextViewerAction action= new TextViewerAction(fViewer, ITextOperationTarget.SELECT_ALL);
		action.configureAction(ConsoleMessages.TextConsolePage_SelectAllText, ConsoleMessages.TextConsolePage_SelectAllDescrip, ConsoleMessages.TextConsolePage_SelectAllDescrip);
		action.setActionDefinitionId(ActionFactory.SELECT_ALL.getCommandId());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IConsoleHelpContextIds.CONSOLE_SELECT_ALL_ACTION);
		setGlobalAction(actionBars, ActionFactory.SELECT_ALL.getId(), action);

		action= new TextViewerAction(fViewer, ITextOperationTarget.CUT);
		action.configureAction(ConsoleMessages.TextConsolePage_CutText, ConsoleMessages.TextConsolePage_CutDescrip, ConsoleMessages.TextConsolePage_CutDescrip);
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		action.setActionDefinitionId(ActionFactory.CUT.getCommandId());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IConsoleHelpContextIds.CONSOLE_CUT_ACTION);
		setGlobalAction(actionBars, ActionFactory.CUT.getId(), action);

		action= new TextViewerAction(fViewer, ITextOperationTarget.COPY);
		action.configureAction(ConsoleMessages.TextConsolePage_CopyText, ConsoleMessages.TextConsolePage_CopyDescrip, ConsoleMessages.TextConsolePage_CopyDescrip);
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		action.setActionDefinitionId(ActionFactory.COPY.getCommandId());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IConsoleHelpContextIds.CONSOLE_COPY_ACTION);
		setGlobalAction(actionBars, ActionFactory.COPY.getId(), action);

		action= new TextViewerAction(fViewer, ITextOperationTarget.PASTE);
		action.configureAction(ConsoleMessages.TextConsolePage_PasteText, ConsoleMessages.TextConsolePage_PasteDescrip, ConsoleMessages.TextConsolePage_PasteDescrip);
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		action.setActionDefinitionId(ActionFactory.PASTE.getCommandId());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IConsoleHelpContextIds.CONSOLE_PASTE_ACTION);
		setGlobalAction(actionBars, ActionFactory.PASTE.getId(), action);

		fClearOutputAction = new ClearOutputAction(fConsole);
		fClearOutputAction.setActionDefinitionId(IConsoleConstants.COMMAND_ID_CLEAR_CONSOLE);
		setGlobalAction(actionBars, IConsoleConstants.COMMAND_ID_CLEAR_CONSOLE, fClearOutputAction);

		ResourceBundle bundle = ConsoleResourceBundleMessages.getBundle();
		FindReplaceAction fraction = new FindReplaceAction(bundle, "find_replace_action_", fConsoleView); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(fraction, IConsoleHelpContextIds.CONSOLE_FIND_REPLACE_ACTION);
		setGlobalAction(actionBars, ActionFactory.FIND.getId(), fraction);

		FindNextAction findNextAction = new FindNextAction(bundle, "find_next_action_", fConsoleView, true); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(findNextAction, IConsoleHelpContextIds.CONSOLE_FIND_NEXT_ACTION);
		findNextAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_NEXT);
		setGlobalAction(actionBars, ITextEditorActionConstants.FIND_NEXT, findNextAction);

		FindNextAction findPreviousAction = new FindNextAction(bundle, "find_previous_action_", fConsoleView, false); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(findPreviousAction,
				IConsoleHelpContextIds.CONSOLE_FIND_PREVIOUS_ACTION);
		findPreviousAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_PREVIOUS);
		setGlobalAction(actionBars, ITextEditorActionConstants.FIND_PREVIOUS, findPreviousAction);

		fSelectionActions.add(ActionFactory.CUT.getId());
		fSelectionActions.add(ActionFactory.COPY.getId());
		fSelectionActions.add(ActionFactory.PASTE.getId());
		fSelectionActions.add(ActionFactory.FIND.getId());
		fSelectionActions.add(ITextEditorActionConstants.FIND_NEXT);
		fSelectionActions.add(ITextEditorActionConstants.FIND_PREVIOUS);

		actionBars.updateActionBars();
	}

	/**
	 * Configures an action for key bindings.
	 *
	 * @param actionBars action bars for this page
	 * @param actionID action definition id
	 * @param action associated action
	 */
	protected void setGlobalAction(IActionBars actionBars, String actionID, IAction action) {
		fGlobalActions.put(actionID, action);
		actionBars.setGlobalActionHandler(actionID, action);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> required) {
		if (IFindReplaceTarget.class.equals(required)) {
			return (T) fViewer.getFindReplaceTarget();
		}
		if (Widget.class.equals(required)) {
			return (T) fViewer.getTextWidget();
		}
		return null;
	}

	/**
	 * Returns the view this page is contained in.
	 *
	 * @return the view this page is contained in
	 */
	protected IConsoleView getConsoleView() {
		return fConsoleView;
	}

	/**
	 * Returns the console this page is displaying.
	 *
	 * @return the console this page is displaying
	 */
	protected IConsole getConsole() {
		return fConsole;
	}

	/**
	 * Updates the global action with the given id
	 *
	 * @param actionId action definition id
	 */
	protected void updateAction(String actionId) {
		IAction action= fGlobalActions.get(actionId);
		if (action instanceof IUpdate) {
			((IUpdate) action).update();
		}
	}


	/**
	 * Fill the context menu
	 *
	 * @param menuManager menu
	 */
	protected void contextMenuAboutToShow(IMenuManager menuManager) {
		IDocument doc= fViewer.getDocument();
		if (doc == null) {
			return;
		}

		menuManager.add(fGlobalActions.get(ActionFactory.CUT.getId()));
		menuManager.add(fGlobalActions.get(ActionFactory.COPY.getId()));
		menuManager.add(fGlobalActions.get(ActionFactory.PASTE.getId()));
		menuManager.add(fGlobalActions.get(ActionFactory.SELECT_ALL.getId()));

		menuManager.add(new Separator("FIND")); //$NON-NLS-1$
		menuManager.add(fGlobalActions.get(ActionFactory.FIND.getId()));
		menuManager.add(fGlobalActions.get(ITextEditorActionConstants.FIND_NEXT));
		menuManager.add(fGlobalActions.get(ITextEditorActionConstants.FIND_PREVIOUS));
		menuManager.add(new FollowHyperlinkAction(fViewer.getHyperlink()));
		menuManager.add(fClearOutputAction);

		menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected void configureToolBar(IToolBarManager mgr) {
		mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, fClearOutputAction);
	}


	/**
	 * Returns the viewer contained in this page.
	 *
	 * @return the viewer contained in this page
	 */
	public TextConsoleViewer getViewer() {
		return fViewer;
	}

	/**
	 * Sets the viewer contained in this page.
	 *
	 * @param viewer text viewer
	 */
	public void setViewer(TextConsoleViewer viewer) {
		this.fViewer = viewer;
	}

	/**
	 * Installs the font zoom key listener on the given control.
	 *
	 * @param control the control to install the key listener on
	 */
	private void installFontZoomKeyListener(Control control) {
		control.addKeyListener(KeyListener.keyPressedAdapter(event -> {
			// Check for Ctrl key first.
			if ((event.stateMask & SWT.MOD1) == 0) {
				return;
			}

			// Check for + or - keys (including numpad)
			boolean isPlus = event.character == '=' || event.keyCode == SWT.KEYPAD_ADD
					|| (event.character == '+' && (event.stateMask & SWT.SHIFT) != 0);
			boolean isMinus = event.character == '-' || event.keyCode == SWT.KEYPAD_SUBTRACT;

			if (isPlus) {
				increaseFontSize();
				event.doit = false;
			} else if (isMinus) {
				decreaseFontSize();
				event.doit = false;
			}
		}));
	}

	/**
	 * Increases the font size of the console by one step.
	 */
	private void increaseFontSize() {
		changeFontSize(FONT_SIZE_STEP);
	}

	/**
	 * Decreases the font size of the console by one step.
	 */
	private void decreaseFontSize() {
		changeFontSize(-FONT_SIZE_STEP);
	}

	/**
	 * Changes the font size of the console by the given delta.
	 *
	 * @param delta the amount to change the font size (positive to increase,
	 *              negative to decrease)
	 */
	private void changeFontSize(int delta) {
		StyledText styledText = fViewer.getTextWidget();
		if (styledText == null || styledText.isDisposed()) {
			return;
		}

		Font currentFont = styledText.getFont();
		if (currentFont == null || currentFont.isDisposed()) {
			return;
		}

		FontData[] fontData = currentFont.getFontData();
		if (fontData == null || fontData.length == 0) {
			return;
		}

		int currentHeight = fontData[0].getHeight();
		int newHeight = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, currentHeight + delta));

		if (newHeight == currentHeight) {
			return;
		}

		// Copy the existing FontData array and set the new height on each element.
		FontData[] newFontData = fontData.clone();
		for (FontData fd : newFontData) {
			if (fd != null) {
				fd.setHeight(newHeight);
			}
		}

		// Get any existing zoom font for this specific StyledText
		Font oldZoomFont = (Font) styledText.getData(ZOOM_FONT_KEY);

		// Create and set the new font
		Font newZoomFont = new Font(styledText.getDisplay(), newFontData);
		styledText.setFont(newZoomFont);

		// Store the new zoom font on this specific StyledText
		styledText.setData(ZOOM_FONT_KEY, newZoomFont);

		// Install DisposeListener if this is the first zoom font for this widget
		if (oldZoomFont == null) {
			styledText.addDisposeListener(e -> {
				Font zoomFont = (Font) styledText.getData(ZOOM_FONT_KEY);
				if (zoomFont != null && !zoomFont.isDisposed()) {
					zoomFont.dispose();
				}
			});
		}

		// Dispose the old zoom font for this widget after setting the new one
		if (oldZoomFont != null && !oldZoomFont.isDisposed()) {
			oldZoomFont.dispose();
		}

		// Persist the changed font for this console type
		saveFontToPreferences(newZoomFont);
	}

	/**
	 * Returns the preference key for the font for this console's type.
	 */
	private String getFontPrefKey() {
		String type = getConsole().getType();
		if (type == null || type.isEmpty()) {
			return FONT_PREF_KEY_PREFIX + "default"; //$NON-NLS-1$
		}
		return FONT_PREF_KEY_PREFIX + type;
	}

	/**
	 * Loads a Font from the preference store for this console type, or null if none.
	 */
	private Font loadFontFromPreferences() {
		IPreferenceStore store = ConsolePlugin.getDefault().getPreferenceStore();
		String value = store.getString(getFontPrefKey());
		if (value == null || value.isEmpty()) {
			return null;
		}
		// Stored as a comma-separated list of Base64 encoded tokens.
		// Each token is either a platform FontData(String) (starting with "1|")
		// or our compact format: name|height|style|locale
		try {
			String[] parts = value.split(","); //$NON-NLS-1$
			FontData[] fds = new FontData[parts.length];
			for (int i = 0; i < parts.length; i++) {
				String decoded = new String(Base64.getDecoder().decode(parts[i]), StandardCharsets.UTF_8);

				if (decoded.startsWith("1|")) { //$NON-NLS-1$
					// platform-native FontData string
					fds[i] = new FontData(decoded);
				} else {
					// our compact format: name|height|style|locale
					String[] fields = decoded.split("\\|", -1); //$NON-NLS-1$
					if (fields.length >= 4) {
						String name = fields[0];
						int height = Integer.parseInt(fields[1]);
						int style = Integer.parseInt(fields[2]);
						String locale = fields[3];
						FontData fd = new FontData(name, height, style);
						if (locale != null && !locale.isEmpty()) {
							fd.setLocale(locale);
						}
						fds[i] = fd;
					} else {
						fds[i] = new FontData(decoded);
					}
				}
			}
			return new Font(ConsolePlugin.getStandardDisplay(), fds);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/**
	 * Saves the given font into the preference store for this console type.
	 */
	private void saveFontToPreferences(Font font) {
		if (font == null)
			return;
		FontData[] fds = font.getFontData();
		if (fds == null || fds.length == 0)
			return;
		// Persist only name, height, style and locale in a compact token and Base64-encode it.
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fds.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			FontData fd = fds[i];
			String name = fd.getName();
			int height = fd.getHeight();
			int style = fd.getStyle();
			String locale = fd.getLocale();
			if (locale == null) {
				locale = ""; //$NON-NLS-1$
			}
			String token = name + "|" + height + "|" + style + "|" + locale; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String enc = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
			sb.append(enc);
		}
		IPreferenceStore store = ConsolePlugin.getDefault().getPreferenceStore();
		store.setValue(getFontPrefKey(), sb.toString());
	}
}
