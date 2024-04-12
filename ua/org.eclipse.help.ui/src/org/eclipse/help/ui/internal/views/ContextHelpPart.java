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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.ICommandLink;
import org.eclipse.help.IContext;
import org.eclipse.help.IContext2;
import org.eclipse.help.IContext3;
import org.eclipse.help.IContextProvider;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.internal.context.Context;
import org.eclipse.help.ui.internal.DefaultHelpUI;
import org.eclipse.help.ui.internal.ExecuteCommandAction;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.util.EscapeUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;

@SuppressWarnings("restriction")
public class ContextHelpPart extends SectionPart implements IHelpPart {
	private ReusableHelpPart parent;

	private static final String HELP_KEY = "org.eclipse.ui.help"; //$NON-NLS-1$

	private static final String MORE_HREF = "__more__"; //$NON-NLS-1$

	private final FormText text;

	private Control lastControl;

	private IContextProvider lastProvider;

	private IContext lastContext;

	private IWorkbenchPart lastPart;

	private String defaultText = ""; //$NON-NLS-1$

	private String id;

	private Font codeFont;

	private String savedDescription;

	public ContextHelpPart(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, getSectionStyle());
		Section section = getSection();
		section.marginWidth = 5;
		section.setText(Messages.ContextHelpPart_about);
		Composite container = toolkit.createComposite(section);
		section.setClient(container);
		section.addExpansionListener(new ExpansionAdapter() {

			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (e.getState()) {
					updateText(savedDescription);
				}
			}
		});
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = layout.bottomMargin = 0;
		layout.leftMargin = layout.rightMargin = 0;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		text = toolkit.createFormText(container, false);
		text.setWhitespaceNormalized(false);
		text.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		text.setColor(IFormColors.TITLE, toolkit.getColors().getColor(
				IFormColors.TITLE));
		codeFont = createCodeFont(parent.getDisplay(), parent.getFont(), JFaceResources.getTextFont());
		text.setFont("code", codeFont); //$NON-NLS-1$
		String key = IHelpUIConstants.IMAGE_FILE_F1TOPIC;
		text.setImage(key, HelpUIResources.getImage(key));
		key = IHelpUIConstants.IMAGE_COMMAND_F1TOPIC;
		text.setImage(key, HelpUIResources.getImage(key));

		String searchKey = IHelpUIConstants.IMAGE_HELP_SEARCH;
		text.setImage(searchKey, HelpUIResources.getImage(searchKey));

		text.addHyperlinkListener(new IHyperlinkListener() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				String href = (String) e.getHref();
				if (href.startsWith(MORE_HREF)) {
					doMore(href.substring(MORE_HREF.length()));
				}  else {
					doOpenLink(e.getHref());
				}
			}

			@Override
			public void linkEntered(HyperlinkEvent e) {
				ContextHelpPart.this.parent.handleLinkEntered(e);
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				ContextHelpPart.this.parent.handleLinkExited(e);
			}
		});
		text.setText(defaultText, false, false);
		text.getAccessible().addAccessibleListener(new AccessibleAdapter() {

			@Override
			public void getName(AccessibleEvent e) {
				if (e.childID == ACC.CHILDID_SELF) {
					String currentName = e.result;
					e.result = Messages.ReusableHelpPart_contextHelpPage_name
					  + ' ' + getSection().getText()+ ' ' +currentName;
				}
			}
		});

	}

	private static int getSectionStyle() {
		int style = ExpandableComposite.EXPANDED ;
		if (RelatedTopicsPart.isUseDynamicHelp()) {
			style = style | ExpandableComposite.TWISTIE;
		}
		return style;
	}

	private static Font createCodeFont(Display display, Font regularFont, Font textFont) {
		FontData[] rfontData = regularFont.getFontData();
		FontData[] tfontData = textFont.getFontData();
		int height = 0;

		for (int i=0; i<rfontData.length; i++) {
			FontData data = rfontData[i];
			height = Math.max(height, data.getHeight());
		}
		for (int i = 0; i < tfontData.length; i++) {
			tfontData[i].setHeight(height);
		}
		return new Font(display, tfontData);
	}

	@Override
	public void dispose() {
		if (codeFont!=null)
			codeFont.dispose();
		codeFont = null;
		super.dispose();
	}

	@Override
	public Control getControl() {
		return getSection();
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
		parent.hookFormText(text);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		getSection().setVisible(visible);
	}

	/**
	 * @return returns the default text
	 */
	public String getDefaultText() {
		return defaultText;
	}

	/**
	 * @param defaultText
	 *            The defaultText to set.
	 */
	public void setDefaultText(String defaultText) {
		this.defaultText = defaultText;
		if (text != null)
			text.setText(defaultText, false, false);
	}

	private void doOpenLink(Object href) {
		String sHref = (String)href;
		if (sHref.startsWith("command://")) { //$NON-NLS-1$
			doRunCommand(sHref.substring(10));
		}
		else {
			parent.showURL(sHref);
		}
	}

	private void doRunCommand(String serialization) {
		ExecuteCommandAction action = new ExecuteCommandAction();
		action.setInitializationString(serialization);
		action.run();
	}

	private void updateDescription(String helpText) {
		if (getSection().isExpanded()) {
			updateText(helpText);
		}
		savedDescription = helpText;
	}

	private void updateSearchExpression() {
		if (lastProvider != null) {
			String providerSearchExpression = lastProvider.getSearchExpression(lastControl);
				if (providerSearchExpression != null) {
					updateSearchExpression(providerSearchExpression, lastControl);
					return;
				}
		}
		if (lastContext instanceof IContext2) {
			String title = ((IContext2)lastContext).getTitle();
			if (title!=null) {
				updateSearchExpression(title, lastControl);
				return;
			}
		}
		if (lastControl != null)
			updateSearchExpression(null, lastControl);
	}

	public void handleActivation(IContextProvider provider, IContext context,
			Control c,
			IWorkbenchPart part, boolean isExplicitRequest) {
		if (text.isDisposed())
			return;
		if (DefaultHelpUI.isOpeningHelpView()) {
			return;
		}

		if (checkForRecentExplicitActivation(isExplicitRequest)) {
			return;
		}
		lastControl = c;
		lastProvider = provider;
		lastContext = context;
		lastPart = part;
		if (provider!= null && (context==null || ((context instanceof Context) && IWorkbenchHelpContextIds.MISSING.equals(((Context)context).getId())))) {
			if (HelpPlugin.DEBUG_CONTEXT) {
				System.out.println("Getting context from provider"); //$NON-NLS-1$
			}
			lastContext = provider.getContext(c);
		}
		updateSearchExpression();

		String helpText;
		if (lastContext!=null) {
			helpText = formatHelpContext(lastContext);
			if (HelpPlugin.DEBUG_CONTEXT) {
				System.out.println("Context Activation, context =  " + lastContext.getText()); //$NON-NLS-1$
			}
		} else {
			if (HelpPlugin.DEBUG_CONTEXT) {
				System.out.println("Context Activation on control"); //$NON-NLS-1$
			}
			helpText = createContextHelp(c);
		}
		updateTitle(c);
		updateDescription(helpText);
		if (RelatedTopicsPart.isUseDynamicHelp()) {
			updateDynamicHelp();
		}
	}

	private long lastUpdate = 0;

	private String[] searchTerms;

	/*
	 * If F1 was pressed within the last half second and this is a context change do not
	 * update dynamic help solely due to a focus change, Bug 159450
	 */
	private boolean checkForRecentExplicitActivation(boolean isExplicitRequest) {
		if (isExplicitRequest) {
			lastUpdate = System.currentTimeMillis();
			return false;
		}
		if (lastUpdate == 0) {
			return false;
		}
		long previousUpdate = lastUpdate;
		lastUpdate = 0;
		return System.currentTimeMillis() - previousUpdate  < 500;
	}

	private void updateTitle(Control c) {
		String title = null;
		if (lastContext != null && lastContext instanceof IContext2) {
			IContext2 c2 = (IContext2)lastContext;
			title = c2.getTitle();
		}
		if (title==null && lastPart != null)
			title = NLS.bind(Messages.ContextHelpPart_aboutP, lastPart
							.getSite().getRegisteredName());
		if (title == null) {
			String[] searchTerms = computeSearchTerms(c);
			if (searchTerms.length > 0) {
				title = NLS.bind(Messages.ContextHelpPart_aboutP, searchTerms[0]);
			}
		}
		if (title==null)
			title = Messages.ContextHelpPart_about;
		getSection().setText(EscapeUtils.escapeForLabel(title));
	}

	private void updateText(String helpText) {
		try {
			text.setText(helpText != null ? helpText : defaultText,
					helpText != null,
					false);
			getSection().layout();
			getManagedForm().reflow(true);
		} catch (Exception e) {
			ILog.of(getClass()).error("Error displaying context help text " + helpText, e); //$NON-NLS-1$
		}
	}

	private void updateSearchExpression(String expression, Control c) {
		if (expression == null) {
			searchTerms = computeSearchTerms(c);
		} else {
			searchTerms = new String[] { expression };
		}
	}

	private void updateDynamicHelp() {
		RelatedTopicsPart part = (RelatedTopicsPart) parent
				.findPart(IHelpUIConstants.HV_RELATED_TOPICS);
		if (part != null) {
			if (searchTerms != null) {
				if (HelpPlugin.DEBUG_CONTEXT) {
					System.out.println("Dynamic help - search for " + Arrays.toString(searchTerms)); //$NON-NLS-1$
				}
				part.startSearch(buildSearchExpression(searchTerms), lastContext);
			}
		}
	}

	private String buildSearchExpression(String[] searchTerms) {
		StringBuilder buff = new StringBuilder();
		for (int i = 0; i < searchTerms.length; i++) {
			if (buff.length() > 0)
				buff.append(" OR "); //$NON-NLS-1$
			buff.append('"');
			buff.append(searchTerms[i]);
			buff.append('"');
		}
		return buff.length() > 0 ? buff.toString().trim() : null;
	}

	private static class SearchTerms {

		private final List<String> terms = new ArrayList<>();
		private final Set<String> termSet = new HashSet<>();
		public void add(String term) {
			if (term == null ) return;
			String lowerCaseTerm = term.toLowerCase();
			// Do not allow duplicates
			if (!termSet.contains(lowerCaseTerm)) {
				termSet.add(lowerCaseTerm);
				terms.add(term);
			}
		}

		public String[] toArray() {
			return terms.toArray(new String[terms.size()]);
		}
	}

	/*
	 * Used for both dynamic help and to get a useful title
	 */
	private String[] computeSearchTerms(Control c) {
		// Search the control and all ancestors until we find a composite
		// which we can get a search term from
		Composite container =  c instanceof Composite ? (Composite)c : c.getParent();
		SearchTerms searchTerms = new SearchTerms();
		while (container != null) {
			Object data = container.getData();
			if (data instanceof IWizardContainer) {
				IWizardContainer wc = (IWizardContainer) data;
				searchTerms.add(wc.getCurrentPage().getTitle());
				searchTerms.add(wc.getCurrentPage().getWizard().getWindowTitle());
				break;
			} else if (data instanceof IWorkbenchWindow) {
				IWorkbenchWindow window = (IWorkbenchWindow) data;
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					IWorkbenchPart part = lastPart;
					if (part != null) {
						if (part instanceof IViewPart) {
							searchTerms.add(NLS.bind(
									Messages.ContextHelpPart_query_view, part
											.getSite().getRegisteredName()));
						} else if (part instanceof IEditorPart) {
							if (part.getSite() != null && part.getSite().getRegisteredName() != null) {
								searchTerms.add(part.getSite().getRegisteredName());
							}
						}
					}
					/*
					// Searching by perspective seems counterproductive - CG
					IPerspectiveDescriptor persp = page.getPerspective();
					if (persp != null) {
						searchTerms.add(NLS.bind(
								Messages.ContextHelpPart_query_perspective,
								persp.getLabel()));
					}
					*/
				}
				break;
			} else if (data instanceof Window) {
				Window w = (Window) data;
				if (w instanceof IPageChangeProvider) {
					Object page = ((IPageChangeProvider) w).getSelectedPage();
					String pageName = getPageName(c, page);
					if (pageName != null) {
						searchTerms.add(pageName);
					}
				}
				searchTerms.add(w.getShell().getText());
				break;
			}
			container = container.getParent();
		}
		return searchTerms.toArray();
	}

	private String getPageName(Control focusControl, Object page) {
		if (page instanceof IDialogPage)
			return ((IDialogPage) page).getTitle();
		if (focusControl == null)
			return null;

		Composite parent = focusControl.getParent();
		while (parent != null) {
			if (parent instanceof TabFolder) {
				TabItem[] selection = ((TabFolder) parent).getSelection();
				if (selection.length == 1)
					return stripMnemonic(selection[0].getText());
			} else if (parent instanceof CTabFolder) {
				CTabItem selection = ((CTabFolder) parent).getSelection();
				return stripMnemonic(selection.getText());
			}
			parent = parent.getParent();
		}
		return null;
	}

	private String stripMnemonic(String name) {
		int loc = name.indexOf('&');
		if (loc!= -1)
			return name.substring(0, loc)+name.substring(loc+1);
		return name;
	}

	private String createContextHelp(Control page) {
		String text = null;
		lastContext = null;
		if (page != null) {
			if (page != null /* && page.isVisible() */&& !page.isDisposed()) {
				IContext helpContext = findHelpContext(page);
				if (helpContext != null) {
					text = formatHelpContext(helpContext);
					lastContext = helpContext;
				}
			}
		}
		return text;
	}

	public static IContext findHelpContext(Control c) {
		String contextId = null;
		Control node = c;
		do {
			contextId = (String) node.getData(HELP_KEY);
			if (contextId != null)
				break;
			node = node.getParent();
		} while (node != null);
		if (contextId != null) {
			return HelpSystem.getContext(contextId);
		}
		return null;
	}

	private String formatHelpContext(IContext context) {
		String locale = Platform.getNL();
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("<form>"); //$NON-NLS-1$
		sbuf.append("<p>"); //$NON-NLS-1$
		sbuf.append(decodeContextBoldTags(context));
		sbuf.append("</p>"); //$NON-NLS-1$

		ICommandLink[] commands = null;
		if (context instanceof IContext3) {
			commands = ((IContext3)context).getRelatedCommands();
		}

		String category = new String();
		if (commands != null && commands.length > 0) {
			for (int i=0;i<commands.length;++i) {
				if (!UAContentFilter.isFiltered(commands[i], HelpEvaluationContext.getContext())) {
					if (category != null) {
						addCategory(sbuf, null);
					}
					category = null;
					sbuf.append("<li style=\"image\" value=\""); //$NON-NLS-1$
					sbuf.append(IHelpUIConstants.IMAGE_COMMAND_F1TOPIC);
					sbuf.append("\" indent=\"21\">"); //$NON-NLS-1$
					sbuf.append("<a href=\"command://"); //$NON-NLS-1$
					sbuf.append(commands[i].getSerialization());
					sbuf.append("\">"); //$NON-NLS-1$
					sbuf.append(EscapeUtils.escapeSpecialChars(commands[i].getLabel()));
					sbuf.append("</a>"); //$NON-NLS-1$
					sbuf.append("</li>"); //$NON-NLS-1$
				}
			}
		}

		IHelpResource[] links = context.getRelatedTopics();
		if (links != null && context instanceof IContext2) {
			ContextHelpSorter sorter = new ContextHelpSorter((IContext2)context);
			sorter.sort(null, links);
		}
		if (links != null && links.length > 0) {
			for (int i = 0; i < links.length; i++) {
				IHelpResource link = links[i];
				if (!UAContentFilter.isFiltered(link, HelpEvaluationContext.getContext())) {
					String cat = null;
					if (context instanceof IContext2) {
						cat = ((IContext2)context).getCategory(link);
					}
					if (cat == null && category != null || cat != null
							&& category == null || cat != null
							&& category != null && !cat.equals(category)) {
						addCategory(sbuf, cat);
					}
					category = cat;
					sbuf.append("<li style=\"image\" value=\""); //$NON-NLS-1$
					sbuf.append(IHelpUIConstants.IMAGE_FILE_F1TOPIC);
					sbuf.append("\" indent=\"21\">"); //$NON-NLS-1$
					sbuf.append("<a href=\""); //$NON-NLS-1$
					sbuf.append(EscapeUtils.escapeAmpersand(link.getHref()));
					String tcat = getTopicCategory(link.getHref(), locale);
					if (tcat != null && !Platform.getWS().equals(Platform.WS_GTK)) {
						sbuf.append("\" alt=\""); //$NON-NLS-1$
						sbuf.append(EscapeUtils.escapeSpecialChars(tcat));
					}
					sbuf.append("\">"); //$NON-NLS-1$
					sbuf.append(EscapeUtils.escapeSpecialChars(link.getLabel()));
					sbuf.append("</a>"); //$NON-NLS-1$
					sbuf.append("</li>"); //$NON-NLS-1$
				} else {
					if (HelpPlugin.DEBUG_CONTEXT) {
						System.out.println("Link is filtered out: " + link.getLabel()); //$NON-NLS-1$
					}
				}
			}
		}
		if (!RelatedTopicsPart.isUseDynamicHelp() && searchTerms != null && searchTerms.length > 0) {
			sbuf.append("<p><span color=\""); //$NON-NLS-1$
			sbuf.append(IFormColors.TITLE);
			sbuf.append("\">"); //$NON-NLS-1$
			sbuf.append(Messages.ContextHelpPart_more);
			sbuf.append("</span></p>"); //$NON-NLS-1$
			for (int term = 0; term < searchTerms.length; term++) {
				sbuf.append("<p><img href=\""); //$NON-NLS-1$
				sbuf.append(IHelpUIConstants.IMAGE_HELP_SEARCH);
				sbuf.append("\"/>"); //$NON-NLS-1$
				sbuf.append(" <a href=\""); //$NON-NLS-1$
				sbuf.append(MORE_HREF);
				sbuf.append(term);
				sbuf.append("\">"); //$NON-NLS-1$
				String searchForMessage = NLS.bind(Messages.ContextHelpPart_searchFor, searchTerms[term]);
				sbuf.append(EscapeUtils.escapeSpecialChars(searchForMessage));
				sbuf.append("</a></p>"); //$NON-NLS-1$
			}
		}
		sbuf.append("</form>"); //$NON-NLS-1$
		return sbuf.toString();
	}

	private void addCategory(StringBuilder sbuf, String category) {
		if (category == null)
			category = Messages.ContextHelpPart_seeAlso;
		sbuf.append("<p><span color=\""); //$NON-NLS-1$
		sbuf.append(IFormColors.TITLE);
		sbuf.append("\">"); //$NON-NLS-1$
		sbuf.append(category);
		sbuf.append("</span></p>"); //$NON-NLS-1$
	}

	private String getTopicCategory(String href, String locale) {
		IToc[] tocs = HelpPlugin.getTocManager().getTocs(locale);
		for (int i = 0; i < tocs.length; i++) {
			ITopic topic = tocs[i].getTopic(href);
			if (topic != null)
				return tocs[i].getLabel();
		}
		return null;
	}

	/**
	 * Make sure to support the Help system bold tag. The help system returns a
	 * regular string for getText(). Use internal apis for now to get bold.
	 */
	private String decodeContextBoldTags(IContext context) {
		String styledText;
		if (context instanceof IContext2) {
			styledText = ((IContext2) context).getStyledText();
			if (styledText == null) {
				styledText = context.getText();
			}
		} else {
			styledText = context.getText();
		}
		if (styledText == null) {
			return Messages.ContextHelpPart_noDescription;
		}
		String decodedString = styledText.replaceAll("<@#\\$b>", "<b>"); //$NON-NLS-1$ //$NON-NLS-2$
		decodedString = decodedString.replaceAll("</@#\\$b>", "</b>"); //$NON-NLS-1$ //$NON-NLS-2$
		decodedString = EscapeUtils.escapeSpecialCharsLeavinggBold(decodedString);
		decodedString = decodedString.replaceAll("\r\n|\n|\r", "<br/>");  //$NON-NLS-1$ //$NON-NLS-2$
		return decodedString;
	}

	@Override
	public boolean setFormInput(Object input) {
		if (input instanceof ContextHelpProviderInput) {
			ContextHelpProviderInput chinput = (ContextHelpProviderInput) input;
			handleActivation(chinput.getProvider(), chinput.getContext(), chinput.getControl(),
						chinput.getPart(), chinput.isExplicitRequest());
			return true;
		}
		return false;
	}

	@Override
	public void setFocus() {
		if (text != null)
			text.setFocus();
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return parent.fillFormContextMenu(text, manager);
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return text.equals(control);
	}

	@Override
	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.COPY.getId()))
			return parent.getCopyAction();
		return null;
	}

	@Override
	public void stop() {
	}

	@Override
	public void toggleRoleFilter() {
	}

	@Override
	public void refilter() {
	}

	@Override
	public void saveState(IMemento memento) {
	}

	private void doMore(String moreText) {
		int index = Integer.parseInt(moreText);
		parent.startSearch(searchTerms[index]);
	}
}
