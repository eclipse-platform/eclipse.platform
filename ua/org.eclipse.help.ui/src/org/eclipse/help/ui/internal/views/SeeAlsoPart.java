/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

public class SeeAlsoPart extends AbstractFormPart implements IHelpPart {
	private Composite container;
	private Composite linkContainer;
	private ReusableHelpPart helpPart;
	private String id;
	private Image bgImage;
	private HyperlinkGroup hyperlinkGroup;

	/**
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public SeeAlsoPart(Composite parent, FormToolkit toolkit) {
		container = toolkit.createComposite(parent);
		container.setBackgroundMode(SWT.INHERIT_DEFAULT);

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		//layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.marginTop = 2;
		container.setLayout(layout);

		linkContainer = new Composite(container, SWT.NULL);
		linkContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		RowLayout rlayout = new RowLayout();
		rlayout.marginBottom = 0;
		rlayout.marginTop = 0;
		rlayout.marginLeft = 0;
		rlayout.marginRight = 0;
		rlayout.justify = false;
		rlayout.wrap = true;
		linkContainer.setLayout(rlayout);

		hyperlinkGroup = new HyperlinkGroup(container.getDisplay());
		hyperlinkGroup.setHyperlinkUnderlineMode(toolkit.getHyperlinkGroup().getHyperlinkUnderlineMode());
	}

	private void updateLinks(String href) {
		Control [] children = linkContainer.getChildren();
		for (int i=0; i<children.length; i++) {
			ImageHyperlink link = (ImageHyperlink)children[i];
			RowData data = (RowData)link.getLayoutData();
			boolean isCurrentPage = link.getHref().equals(href);
			if (isCurrentPage) {
				link.setFont(JFaceResources.getBannerFont());
			} else {
				link.setFont(JFaceResources.getDefaultFont());
			}
			data.exclude = false;
			link.setVisible(!data.exclude);
		}
		linkContainer.layout();
		helpPart.reflow();
	}

	private void addLinks(final Composite container, FormToolkit toolkit) {
		IHyperlinkListener listener = new HyperlinkAdapter() {

			@Override
			public void linkActivated(final HyperlinkEvent e) {
				container.getDisplay()
						.asyncExec(() -> SeeAlsoPart.this.helpPart.showPage((String) e.getHref(), true));
			}
		};
		if ((helpPart.getStyle() & ReusableHelpPart.ALL_TOPICS) != 0)
			addPageLink(container, toolkit, Messages.SeeAlsoPart_allTopics, IHelpUIConstants.HV_ALL_TOPICS_PAGE,
				IHelpUIConstants.IMAGE_ALL_TOPICS, listener);
		if ((helpPart.getStyle() & ReusableHelpPart.SEARCH) != 0) {
			addPageLink(container, toolkit, Messages.SeeAlsoPart_search, IHelpUIConstants.HV_FSEARCH_PAGE,
				IHelpUIConstants.IMAGE_HELP_SEARCH, listener);
		}
		if ((helpPart.getStyle() & ReusableHelpPart.CONTEXT_HELP) != 0) {
			addPageLink(container, toolkit, Messages.SeeAlsoPart_contextHelp,
				IHelpUIConstants.HV_CONTEXT_HELP_PAGE,
				IHelpUIConstants.IMAGE_RELATED_TOPICS, listener);
		}
		if ((helpPart.getStyle() & ReusableHelpPart.BOOKMARKS) != 0) {
			addPageLink(container, toolkit, Messages.SeeAlsoPart_bookmarks,
				IHelpUIConstants.HV_BOOKMARKS_PAGE,
				IHelpUIConstants.IMAGE_BOOKMARKS, listener);
		}
		if ((helpPart.getStyle() & ReusableHelpPart.INDEX) != 0) {
			addPageLink(container, toolkit, Messages.SeeAlsoPart_index,
				IHelpUIConstants.HV_INDEX_PAGE,
				IHelpUIConstants.IMAGE_INDEX, listener);
		}
	}

	private void addPageLink(Composite container, FormToolkit toolkit, String text, String id,
			String imgRef, IHyperlinkListener listener) {
		String cid = helpPart.getCurrentPageId();
		if (cid!=null && cid.equals(id))
			return;
		ImageHyperlink link = new ImageHyperlink(container, SWT.WRAP|toolkit.getOrientation());
		toolkit.adapt(link, true, true);
		link.setImage(HelpUIResources.getImage(imgRef));
		link.setText(text);
		link.setHref(id);
		link.setBackground(null);
		link.addHyperlinkListener(listener);
		hyperlinkGroup.add(link);
		RowData data = new RowData();
		data.exclude = false;
		link.setLayoutData(data);
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.helpPart = parent;
		this.id = id;
		FormToolkit toolkit = helpPart.getForm().getToolkit();
		addLinks(linkContainer, toolkit);
		Composite padding = toolkit.createComposite(container);
		GridData paddingData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		paddingData.heightHint = 2;
		padding.setLayoutData(paddingData);
		Composite separator = new Composite(container, SWT.NULL);
		GridData seperatorData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		separator.setBackground(toolkit.getColors().getBorderColor());
		seperatorData.heightHint = 1;
		separator.setLayoutData(seperatorData);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		container.setVisible(visible);
		if (visible)
			markStale();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (bgImage != null && !bgImage.isDisposed()) {
			bgImage.dispose();
		}
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return false;
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return control != null && control.getParent() == linkContainer;
	}

	@Override
	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.COPY.getId()))
			return helpPart.getCopyAction();
		return null;
	}

	@Override
	public void stop() {
	}

	@Override
	public void refresh() {
		if (linkContainer!=null && helpPart.getCurrentPageId()!=null)
			updateLinks(helpPart.getCurrentPageId());
		super.refresh();
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

	@Override
	public void setFocus() {
		if (linkContainer!=null)
			linkContainer.setFocus();
	}
}
