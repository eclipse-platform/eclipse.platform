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

import org.eclipse.help.HelpSystem;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.util.OverlayIcon;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class AllTopicsPart extends HyperlinkTreePart {

	private Image containerWithTopicImage;

	//private Action showAllAction;

	class TopicsProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement == AllTopicsPart.this)
				return HelpSystem.getTocs();
			if (parentElement instanceof IToc)
				return ((IToc) parentElement).getTopics();
			if (parentElement instanceof ITopic)
				return ((ITopic) parentElement).getSubtopics();
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof IToc) {
				return AllTopicsPart.this;
			}
			else if (element instanceof UAElement) {
				return ((UAElement)element).getParentElement();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	class TopicsLabelProvider extends LabelProvider {

		@Override
		public String getText(Object obj) {
			if (obj instanceof IHelpResource)
				return ((IHelpResource) obj).getLabel();
			return super.getText(obj);
		}

		@Override
		public Image getImage(Object obj) {
			boolean expanded = treeViewer.getExpandedState(obj);
			boolean expandable = treeViewer.isExpandable(obj);
			if (obj instanceof Toc){
				Toc toc = (Toc) obj;
				Image icon   = HelpUIResources.getImageFromId(toc.getIcon(), expanded, !expandable);
				if (icon != null) {
					return icon;
				}
			}

			if (obj instanceof Topic) {
				Topic topic = (Topic) obj;
				Image icon   = HelpUIResources.getImageFromId(topic.getIcon(), expanded, !expandable);
				if (icon != null) {
					return icon;
				}
			}

			if (obj instanceof IToc) {
				String key = expanded ? IHelpUIConstants.IMAGE_TOC_OPEN
						: IHelpUIConstants.IMAGE_TOC_CLOSED;
				return HelpUIResources.getImage(key);
			}
			if (obj instanceof ITopic) {
				if (expandable) {
					ITopic topic = (ITopic) obj;
					if (topic.getHref() != null)
						return containerWithTopicImage;
				}
				String key = expandable ? IHelpUIConstants.IMAGE_CONTAINER
						: IHelpUIConstants.IMAGE_FILE_F1TOPIC;
				return HelpUIResources.getImage(key);
			}
			return super.getImage(obj);
		}
	}

	static class EmptyContainerFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (element instanceof IToc) {
				return isNotEmpty((IToc) element);
			} else if (element instanceof ITopic) {
				return isNotEmpty((ITopic) element);
			}
			return false;
		}

		private boolean isNotEmpty(IToc toc) {
			ITopic[] topics = toc.getTopics();
			return isNotEmpty(topics);
		}

		private boolean isNotEmpty(ITopic topic) {
			String href = topic.getHref();
			ITopic[] topics = topic.getSubtopics();
			return href != null || isNotEmpty(topics);
		}

		private boolean isNotEmpty(ITopic[] topics) {
			for (int i = 0; i < topics.length; i++) {
				if (isNotEmpty(topics[i]))
					return true;
			}
			return false;
		}
	}

	/**
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public AllTopicsPart(Composite parent, final FormToolkit toolkit,
			IToolBarManager tbm) {
		super(parent, toolkit, tbm);
	}

	@Override
	protected void configureTreeViewer() {
		initializeImages();
		treeViewer.setContentProvider(new TopicsProvider());
		treeViewer.setLabelProvider(new TopicsLabelProvider());
		treeViewer.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				postUpdate(event.getElement());
			}

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				postUpdate(event.getElement());
			}
		});
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		super.init(parent, id, memento);
		if (parent.isFilteredByRoles())
			treeViewer.addFilter(parent.getRoleFilter());
		treeViewer.addFilter(parent.getUAFilter());
		treeViewer.addFilter(new EmptyContainerFilter());
	}

	private void initializeImages() {
		ImageDescriptor base = HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_CONTAINER);
		ImageDescriptor ovr = HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_DOC_OVR);
		ImageDescriptor desc = new OverlayIcon(base,
				new ImageDescriptor[][] { { ovr } });
		containerWithTopicImage = desc.createImage();
	}

	@Override
	public void dispose() {
		containerWithTopicImage.dispose();
		super.dispose();
	}

	@Override
	protected void doOpen(Object obj) {
		if (!(obj instanceof IHelpResource))
			return;
		IHelpResource res = (IHelpResource) obj;
		if (res instanceof IToc
				|| (res instanceof ITopic
						&& ((ITopic) obj).getSubtopics().length > 0 && res
						.getHref() == null))
			treeViewer.setExpandedState(obj, !treeViewer.getExpandedState(res));
		if (res instanceof IToc)
			postUpdate(res);
		else if (res.getHref() != null)
			parent.showURL(res.getHref());
	}

	@Override
	protected String getHref(IHelpResource res) {
		return (res instanceof ITopic) ? res.getHref() : null;
	}

	public void selectReveal(String href) {
		IToc[] tocs = HelpSystem.getTocs();
		for (int i = 0; i < tocs.length; i++) {
			IToc toc = tocs[i];
			ITopic topic = toc.getTopic(href);
			if (topic != null) {
				selectReveal(topic);
				return;
			}
		}
	}

	public void selectReveal(IHelpResource res) {
		treeViewer.setSelection(new StructuredSelection(res), true);
		treeViewer.expandToLevel(res, 1);
		treeViewer.getControl().setFocus();
	}

	@Override
	protected boolean canAddBookmarks() {
		return true;
	}

	@Override
	public void toggleRoleFilter() {
		if (parent.isFilteredByRoles())
			treeViewer.addFilter(parent.getRoleFilter());
		else
			treeViewer.removeFilter(parent.getRoleFilter());
	}

	@Override
	public void saveState(IMemento memento) {
	}
}