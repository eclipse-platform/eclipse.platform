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
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.search.ISearchEngineResult;
import org.eclipse.help.search.ISearchEngineResult2;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.util.EscapeUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.osgi.framework.Bundle;

public class EngineResultSection {

	private static final String KEY_PREFIX_GRAYED = "grayed:"; //$NON-NLS-1$

	private static final String CAT_HEADING_PREFIX = "catheading:"; //$NON-NLS-1$

	private final SearchResultsPart part;

	private final EngineDescriptor desc;

	private IStatus errorStatus;

	private final ArrayList<ISearchEngineResult> hits;

	private Section section;

	private Composite container;

	private FormText searchResults;

	private ImageHyperlink prevLink;

	private ImageHyperlink nextLink;

	private boolean needsUpdating;

	private final FederatedSearchSorter sorter;

	private final int HITS_PER_PAGE = 10;

	private static final String HREF_PROGRESS = "__progress__"; //$NON-NLS-1$

	private static final String PROGRESS_VIEW = "org.eclipse.ui.views.ProgressView"; //$NON-NLS-1$

	private int resultOffset = 0;

	public EngineResultSection(SearchResultsPart part, EngineDescriptor desc) {
		this.part = part;
		this.desc = desc;
		hits = new ArrayList<>();
		sorter = new FederatedSearchSorter();
	}

	public boolean hasControl(Control control) {
		return searchResults.equals(control);
	}

	public boolean matches(EngineDescriptor desc) {
		return this.desc == desc;
	}

	public Control createControl(Composite parent, final FormToolkit toolkit) {
		section = toolkit.createSection(parent, ExpandableComposite.SHORT_TITLE_BAR | ExpandableComposite.COMPACT | ExpandableComposite.TWISTIE
				| ExpandableComposite.EXPANDED | ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT);
		// section.marginHeight = 10;
		container = toolkit.createComposite(section);
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.bottomMargin = 0;
		layout.leftMargin = 0;
		layout.rightMargin = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		createFormText(container, toolkit);
		searchResults.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		searchResults.setColor("summary", parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW)); //$NON-NLS-1$
		section.setClient(container);
		updateSectionTitle(0);
		section.addExpansionListener(new ExpansionAdapter() {

			@Override
			public void expansionStateChanging(ExpansionEvent e) {
				if (needsUpdating)
					asyncUpdateResults(true, false);
			}
		});
		return section;
	}

	private void createFormText(Composite parent, FormToolkit toolkit) {
		searchResults = toolkit.createFormText(parent, false);
		searchResults.setColor(IFormColors.TITLE, toolkit.getColors().getColor(IFormColors.TITLE));
		searchResults.marginHeight = 5;
		String topicKey = IHelpUIConstants.IMAGE_FILE_F1TOPIC;
		String searchKey = IHelpUIConstants.IMAGE_HELP_SEARCH;
		searchResults.setImage(topicKey, HelpUIResources.getImage(topicKey));
		searchResults.setImage(searchKey, HelpUIResources.getImage(searchKey));
		searchResults.setColor("summary", parent.getDisplay().getSystemColor( //$NON-NLS-1$
				SWT.COLOR_WIDGET_DARK_SHADOW));
		searchResults.setImage(ISharedImages.IMG_TOOL_FORWARD, PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_TOOL_FORWARD));
		searchResults.setImage(ISharedImages.IMG_TOOL_BACK, PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_TOOL_BACK));
		searchResults.setImage(ISharedImages.IMG_OBJS_ERROR_TSK, PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		searchResults.setImage(desc.getId(), desc.getIconImage());
		Image grayedImage = getGrayedImage(desc.getIconImage());
		searchResults.setImage(KEY_PREFIX_GRAYED + desc.getId(), grayedImage);
		searchResults.addDisposeListener(e -> grayedImage.dispose());
		searchResults.addHyperlinkListener(new IHyperlinkListener() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				Object href = e.getHref();
				String shref = (String) href;
				if (HREF_PROGRESS.equals(href)) {
					showProgressView();
				} else if (shref.startsWith("bmk:")) { //$NON-NLS-1$
					doBookmark(e.getLabel(), shref);
				} else if (shref.startsWith(CAT_HEADING_PREFIX)) {
					part.doCategoryLink(shref.substring(CAT_HEADING_PREFIX.length()));
				} else
					part.doOpenLink(e.getHref());
			}

			@Override
			public void linkEntered(HyperlinkEvent e) {
				part.parent.handleLinkEntered(e);
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				part.parent.handleLinkExited(e);
			}
		});
		initializeText();
		part.parent.hookFormText(searchResults);
		needsUpdating = true;
	}

	private void initializeText() {
		Bundle bundle = Platform.getBundle("org.eclipse.ui.views"); //$NON-NLS-1$
		if (bundle != null) {
			StringBuilder buff = new StringBuilder();
			buff.append("<form>"); //$NON-NLS-1$
			buff.append("<p><a href=\""); //$NON-NLS-1$
			buff.append(HREF_PROGRESS);
			buff.append("\""); //$NON-NLS-1$
			if (!Platform.getWS().equals(Platform.WS_GTK)) {
				buff.append(" alt=\""); //$NON-NLS-1$
				buff.append(Messages.EngineResultSection_progressTooltip);
				buff.append("\""); //$NON-NLS-1$
			}
			buff.append(">"); //$NON-NLS-1$
			buff.append(Messages.EngineResultSection_searchInProgress);
			buff.append("</a></p></form>"); //$NON-NLS-1$
			searchResults.setText(buff.toString(), true, false);
		} else {
			searchResults.setText(Messages.EngineResultSection_progress2, false, false);
		}
	}

	private void showProgressView() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				try {
					page.showView(PROGRESS_VIEW);
				} catch (PartInitException e) {
					ILog.of(getClass()).error(Messages.EngineResultSection_progressError, e);
				}
			}
		}
	}

	public synchronized void add(ISearchEngineResult match) {
		hits.add(match);
		asyncUpdateResults(false, false);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.help.internal.search.federated.ISearchEngineResultCollector#add(org.eclipse.help.internal.search.federated.ISearchEngineResult[])
	 */
	public synchronized void add(ISearchEngineResult[] matches) {
		Collections.addAll(hits, matches);
		asyncUpdateResults(false, false);
	}

	public synchronized void error(IStatus status) {
		errorStatus = status;
		asyncUpdateResults(false, false);
	}

	public synchronized void completed() {
		if (hits.isEmpty() && !searchResults.isDisposed())
			asyncUpdateResults(false, false);
	}

	public synchronized void canceling() {
		if (hits.isEmpty() && !searchResults.isDisposed()) {
			StringBuilder buff = new StringBuilder();
			buff.append("<form>"); //$NON-NLS-1$
			buff.append("<p><span color=\"summary\">");//$NON-NLS-1$
			buff.append(Messages.EngineResultSection_canceling);
			buff.append("</span></p>"); //$NON-NLS-1$
			buff.append("</form>"); //$NON-NLS-1$
			searchResults.setText(buff.toString(), true, false);
		}
	}

	private void asyncUpdateResults(boolean now, final boolean scrollToBeginning) {
		Runnable runnable = () -> BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), () -> {
			if (section.isDisposed()) {
				return;
			}
			updateResults(true);
			if (scrollToBeginning) {
				searchResults.setFocus();
				FormToolkit.setControlVisible(section, true);
				part.updateSeparatorVisibility();
			}
		});
		if (section.isDisposed()) {
			return;
		}
		if (now) {
			PlatformUI.getWorkbench().getDisplay().syncExec(runnable);
		} else {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	private ISearchEngineResult[] getResults() {
		ArrayList<ISearchEngineResult> list = hits;
		if (desc.getEngineTypeId().equals(IHelpUIConstants.INTERNAL_HELP_ID)) {
			if (part.parent.isFilteredByRoles()) {
				list = new ArrayList<>();
				for (int i = 0; i < hits.size(); i++) {
					ISearchEngineResult hit = hits.get(i);
					if (HelpBasePlugin.getActivitySupport().isEnabled(hit.getHref()))
						list.add(hit);
				}
			}
		}
		ISearchEngineResult[] results = list.toArray(new ISearchEngineResult[list.size()]);
		if (part.getShowCategories())
			sorter.sort(null, results);
		return results;
	}

	/**
	 * Returns a copy of the given image but grayed and half transparent.
	 * This gives the icon a grayed/disabled look.
	 *
	 * @param image the image to gray
	 * @return the grayed image
	 */
	private Image getGrayedImage(Image image) {
		// first gray the image
		Image temp = new Image(image.getDevice(), image, SWT.IMAGE_GRAY);
		// then add alpha to blend it 50/50 with the background
		ImageData data = temp.getImageData();
		ImageData maskData = data.getTransparencyMask();
		if (maskData != null) {
			for (int y=0;y<maskData.height;++y) {
				for (int x=0;x<maskData.width;++x) {
					if (maskData.getPixel(x, y) == 0) {
						// masked; set to transparent
						data.setAlpha(x, y, 0);
					}
					else {
						// not masked; set to translucent
						data.setAlpha(x, y, 128);
					}
				}
			}
			data.maskData = null;
		}
		Image grayed = new Image(image.getDevice(), data);
		temp.dispose();
		return grayed;
	}

	void updateResults(boolean reflow) {
		ISearchEngineResult[] results = getResults();
		updateSectionTitle(results.length);
		StringBuilder buff = new StringBuilder();
		buff.append("<form>"); //$NON-NLS-1$
		IHelpResource oldCat = null;

		for (int i = resultOffset; i < results.length; i++) {
			if (i - resultOffset == HITS_PER_PAGE) {
				break;
			}
			ISearchEngineResult hit = results[i];
			IHelpResource cat = hit.getCategory();
			if (part.getShowCategories() && cat != null
					&& (oldCat == null || !oldCat.getLabel().equals(cat.getLabel()))) {
				buff.append("<p>"); //$NON-NLS-1$
				if (cat.getHref() != null) {
					buff.append("<a bold=\"true\" href=\""); //$NON-NLS-1$
					String absoluteHref = ""; //$NON-NLS-1$
					if (cat.getHref().endsWith(".xml")) { //$NON-NLS-1$
						absoluteHref = absoluteHref + CAT_HEADING_PREFIX;
					}
					absoluteHref = absoluteHref + hit.toAbsoluteHref(cat.getHref(), true);
					buff.append(EscapeUtils.escapeSpecialChars(absoluteHref));
					buff.append("\">"); //$NON-NLS-1$
					buff.append(cat.getLabel());
					buff.append("</a>"); //$NON-NLS-1$
				} else {
					buff.append("<b>"); //$NON-NLS-1$
					buff.append(cat.getLabel());
					buff.append("</b>"); //$NON-NLS-1$
				}
				buff.append("</p>"); //$NON-NLS-1$
				oldCat = cat;
			}
			int indent = part.getShowCategories() && cat != null ? 26 : 21;
			int bindent = part.getShowCategories() && cat != null ? 5 : 0;
			buff.append("<li indent=\"" + indent + "\" bindent=\"" + bindent + "\" style=\"image\" value=\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String imageId = desc.getId();
			boolean isPotentialHit = (hit instanceof SearchHit && ((SearchHit)hit).isPotentialHit());
			if (hit instanceof ISearchEngineResult2) {
				URL iconURL = ((ISearchEngineResult2) hit).getIconURL();
				if (iconURL != null) {
					String id = null;
					if (isPotentialHit) {
						id = registerGrayedHitIcon(iconURL);
					}
					else {
						id = registerHitIcon(iconURL);
					}
					if (id != null)
						imageId = id;
				}
			}

			if (isPotentialHit) {
				imageId = KEY_PREFIX_GRAYED + imageId;
			}

			buff.append(imageId);
			buff.append("\">"); //$NON-NLS-1$
			buff.append("<a href=\""); //$NON-NLS-1$
			String href=null;
			if (hit instanceof ISearchEngineResult2) {
				ISearchEngineResult2 hit2 = (ISearchEngineResult2)hit;
				if (((ISearchEngineResult2)hit).canOpen()) {
					href = "open:"+desc.getId()+"?id="+hit2.getId(); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			if (href==null) {
				if (hit.getForceExternalWindow())
					href = "nw:";//$NON-NLS-1$
				href = EscapeUtils.escapeSpecialChars(hit.toAbsoluteHref(hit.getHref(), false));
			}
			buff.append(href);
			buff.append("\""); //$NON-NLS-1$
			if (hit.getCategory() != null && Platform.getWS() != Platform.WS_GTK) {
				buff.append(" alt=\""); //$NON-NLS-1$
				buff.append(hit.getCategory().getLabel());
				buff.append("\""); //$NON-NLS-1$
			}
			buff.append(">"); //$NON-NLS-1$
			String elabel = null;
			if (isPotentialHit) {
				// add "(potential hit)"
				elabel = NLS.bind(Messages.SearchPart_potential_hit, hit.getLabel());
			}
			else {
				elabel = hit.getLabel();
			}

			elabel = EscapeUtils.escapeSpecialChars(elabel);
			buff.append(elabel);
			buff.append("</a>"); //$NON-NLS-1$
			if (part.getShowDescription()) {
				String edesc = hit.getDescription();
				if (edesc != null) {
					edesc = EscapeUtils.escapeSpecialChars(edesc);
					buff.append("<br/>"); //$NON-NLS-1$
					buff.append(edesc);
				}
			}
			buff.append("</li>"); //$NON-NLS-1$
		}
		if (errorStatus != null)
			updateErrorStatus(buff);
		updateNavigation(results.length);
		buff.append("</form>"); //$NON-NLS-1$
		searchResults.setText(buff.toString(), true, false);
		section.layout();
		if (reflow)
			part.reflow();
	}

	/**
	 * Registers the given icon URL for use with this section. Icons
	 * must be registered before use and referenced by the returned
	 * ID.
	 *
	 * @param iconURL the URL to the icon
	 * @return the ID to use for referencing the icon
	 */
	private String registerHitIcon(URL iconURL) {
		Image image = HelpUIResources.getImage(iconURL);
		if (image != null) {
			searchResults.setImage(iconURL.toString(), image);
			return iconURL.toString();
		}
		return null;
	}

	/**
	 * Same as registerHitIcon() but to register a grayed icon. You
	 * can provide the same URL for both the regular and grayed icons,
	 * but two different IDs will be returned.
	 *
	 * @param iconURL the URL to the icon
	 * @return the ID to use for referencing the icon
	 */
	private String registerGrayedHitIcon(URL iconURL) {
		Image image = HelpUIResources.getImage(iconURL);
		if (image != null) {
			searchResults.setImage(iconURL.toString(), image);
			return KEY_PREFIX_GRAYED + iconURL.toString();
		}
		return null;
	}

	private void updateErrorStatus(StringBuilder buff) {
		int indent = 21;
		buff.append("<li indent=\"" + indent + "\" style=\"image\" value=\""); //$NON-NLS-1$ //$NON-NLS-2$
		buff.append(ISharedImages.IMG_OBJS_ERROR_TSK);
		buff.append("\">"); //$NON-NLS-1$
		buff.append("<b>"); //$NON-NLS-1$
		buff.append(EscapeUtils.escapeSpecialChars(errorStatus.getMessage()));
		buff.append("</b>"); //$NON-NLS-1$
		buff.append("<br/>"); //$NON-NLS-1$
		Throwable t = errorStatus.getException();
		if (t != null && t.getMessage() != null)
			buff.append(EscapeUtils.escapeSpecialChars(t.getMessage()));
		buff.append("</li>"); //$NON-NLS-1$
	}

	private void updateNavigation(int size) {
		if (size > HITS_PER_PAGE) {
			if (prevLink == null) {
				FormToolkit toolkit = part.getToolkit();
				Composite navContainer = toolkit.createComposite(container);
				TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
				navContainer.setLayoutData(td);
				GridLayout glayout = new GridLayout();
				glayout.numColumns = 2;
				navContainer.setLayout(glayout);
				GridData gd;
				/*
				 * Label sep = toolkit.createLabel(navContainer, null, SWT.SEPARATOR |
				 * SWT.HORIZONTAL); GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				 * gd.horizontalSpan = 2; gd.widthHint = 2; sep.setLayoutData(gd);
				 */
				prevLink = toolkit.createImageHyperlink(navContainer, SWT.NULL);

				prevLink.setText(NLS.bind(Messages.EngineResultSection_previous, "" + HITS_PER_PAGE)); //$NON-NLS-1$
				prevLink.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
						ISharedImages.IMG_TOOL_BACK));
				prevLink.addHyperlinkListener(new HyperlinkAdapter() {

					@Override
					public void linkActivated(HyperlinkEvent e) {
						resultOffset -= HITS_PER_PAGE;
						asyncUpdateResults(false, true);
					}
				});
				nextLink = toolkit.createImageHyperlink(navContainer, SWT.RIGHT);

				nextLink.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
						ISharedImages.IMG_TOOL_FORWARD));
				gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
				gd.grabExcessHorizontalSpace = true;
				nextLink.setLayoutData(gd);
				nextLink.addHyperlinkListener(new HyperlinkAdapter() {

					@Override
					public void linkActivated(HyperlinkEvent e) {
						resultOffset += HITS_PER_PAGE;
						asyncUpdateResults(false, true);
					}
				});
			}
			prevLink.setVisible(resultOffset > 0);

			int nextOffset = resultOffset + HITS_PER_PAGE;
			int remainder = hits.size() - nextOffset;
			remainder = Math.min(remainder, HITS_PER_PAGE);

			nextLink.setText(NLS.bind(Messages.EngineResultSection_next, "" + remainder)); //$NON-NLS-1$
			nextLink.setVisible(hits.size() > resultOffset + HITS_PER_PAGE);
		} else {
			if (prevLink != null) {
				prevLink.getParent().setMenu(null);
				prevLink.getParent().dispose();
				prevLink = null;
				nextLink = null;
			}
		}
	}

	private void updateSectionTitle(int size) {
		if (errorStatus != null) {
			Label label = part.getToolkit().createLabel(section, null);
			label.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
					ISharedImages.IMG_OBJS_ERROR_TSK));
			section.setTextClient(label);
			section.setText(Messages.EngineResultSection_sectionTitle_error);
		} else {
			section.setTextClient(null);
		}
		if (size == 1)
			section.setText(NLS.bind(Messages.EngineResultSection_sectionTitle_hit, desc.getLabel(), "" //$NON-NLS-1$
					+ hits.size()));
		else if (size <= HITS_PER_PAGE)
			section.setText(NLS.bind(Messages.EngineResultSection_sectionTitle_hits, desc.getLabel(),
					"" + hits.size())); //$NON-NLS-1$
		else {
			int from = (resultOffset + 1);
			int to = (resultOffset + HITS_PER_PAGE);
			to = Math.min(to, size);
			section.setText(NLS.bind(Messages.EngineResultSection_sectionTitle_hitsRange, new String[] {
					desc.getLabel(), "" + from, "" + to, "" + size })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private void doBookmark(final String label, String href) {
		final String fhref = href.substring(4);
		BusyIndicator.showWhile(container.getDisplay(),
				() -> BaseHelpSystem.getBookmarkManager().addBookmark(fhref, label));
	}

	public void dispose() {
		part.parent.unhookFormText(searchResults);
		if (!section.isDisposed()) {
			recursiveSetMenu(section, null);
			section.dispose();
		}
	}

	private void recursiveSetMenu(Control control, Menu menu) {
		control.setMenu(menu);
		if (control instanceof Composite) {
			Composite parent = (Composite) control;
			Control[] children = parent.getChildren();
			for (int i = 0; i < children.length; i++) {
				recursiveSetMenu(children[i], menu);
			}
		}
	}
}
