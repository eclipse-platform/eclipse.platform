/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model.viewer;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroIdElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroAnchor;
import org.eclipse.ui.internal.intro.impl.model.IntroContentProvider;
import org.eclipse.ui.internal.intro.impl.model.IntroExtensionContent;
import org.eclipse.ui.internal.intro.impl.model.IntroGroup;
import org.eclipse.ui.internal.intro.impl.model.IntroHTML;
import org.eclipse.ui.internal.intro.impl.model.IntroImage;
import org.eclipse.ui.internal.intro.impl.model.IntroInclude;
import org.eclipse.ui.internal.intro.impl.model.IntroLink;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.IntroPageTitle;
import org.eclipse.ui.internal.intro.impl.model.IntroPartPresentation;
import org.eclipse.ui.internal.intro.impl.model.IntroSeparator;
import org.eclipse.ui.internal.intro.impl.model.IntroText;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;

public class IntroModelLabelProvider extends LabelProvider {

	// Images created in this ILabelProvider

	static {
		ImageUtil.registerImage(ImageUtil.INTRO_MODEL_CONTAINER,
			"container_obj.png"); //$NON-NLS-1$
		ImageUtil.registerImage(ImageUtil.INTRO_MODEL_LEAF, "topic.png"); //$NON-NLS-1$
	}

	public IntroModelLabelProvider() {
		super();
	}

	@Override
	public Image getImage(Object element) {
		Image image = null;
		AbstractIntroElement introElement = null;
		if (element instanceof AbstractIntroElement)
			// synch the resource first.
			introElement = (AbstractIntroElement) element;
		if (introElement == null)
			return null;

		int elementType = introElement.getType();
		switch (elementType) {
		case AbstractIntroElement.GROUP:
		case AbstractIntroElement.PAGE:
		case AbstractIntroElement.HOME_PAGE:
			image = ImageUtil.getImage(ImageUtil.INTRO_MODEL_CONTAINER);
			break;
		default:
			image = ImageUtil.getImage(ImageUtil.INTRO_MODEL_LEAF);
			break;
		}
		return image;
	}

	@Override
	public String getText(Object element) {

		String label = null;
		AbstractIntroElement introElement = null;
		if (element instanceof AbstractIntroElement)
			// synch the resource first.
			introElement = (AbstractIntroElement) element;
		if (introElement == null)
			return null;

		int elementType = introElement.getType();
		switch (elementType) {
		case AbstractIntroElement.GROUP:
			label = "GROUP: " + ((IntroGroup) introElement).getId(); //$NON-NLS-1$
			break;
		case AbstractIntroElement.LINK:
			label = "LINK: " + ((IntroLink) introElement).getLabel(); //$NON-NLS-1$
			break;
		case AbstractIntroElement.TEXT:
			label = "TEXT: " + ((IntroText) introElement).getText(); //$NON-NLS-1$
			break;
		case AbstractIntroElement.IMAGE:
			label = "IMAGE: " + ((IntroImage) introElement).getId(); //$NON-NLS-1$
			break;
		case AbstractIntroElement.HR:
			label = "HR: " + ((IntroSeparator) introElement).getId(); //$NON-NLS-1$
			break;
		case AbstractIntroElement.HTML:
			label = "HTML: " + ((IntroHTML) introElement).getId(); //$NON-NLS-1$
			break;
		case AbstractIntroElement.INCLUDE:
			label = "Unresolved INCLUDE: " //$NON-NLS-1$
					+ ((IntroInclude) introElement).getPath();
			break;
		case AbstractIntroElement.PAGE:
			label = "PAGE: " + ((AbstractIntroPage) introElement).getId(); //$NON-NLS-1$
			break;
		case AbstractIntroElement.HOME_PAGE:
			label = "HOME PAGE: " //$NON-NLS-1$
					+ ((AbstractIntroPage) introElement).getId();
			break;
		case AbstractIntroElement.PRESENTATION:
			label = "PRESENTATION: " //$NON-NLS-1$
					+ ((IntroPartPresentation) introElement)
						.getImplementationKind();
			break;
		case AbstractIntroElement.CONTENT_PROVIDER:
			label = "CONTENT PROVIDER: " //$NON-NLS-1$
					+ ((IntroContentProvider) introElement).getClassName();
			break;
		case AbstractIntroElement.CONTAINER_EXTENSION:
			label = "Unresolved ConfigExtension: " //$NON-NLS-1$
					+ ((IntroExtensionContent) introElement).getPath();
			break;
		case AbstractIntroElement.ANCHOR:
			label = "ANCHOR: " //$NON-NLS-1$
					+ getPath(introElement.getParent(), ((IntroAnchor) introElement).getId());
			break;
		case AbstractIntroElement.PAGE_TITLE:
			label = "Title: " //$NON-NLS-1$
					+ ((IntroPageTitle) introElement).getTitle();
			break;
		default:
			label = super.getText(element);
			break;
		}
		return label;
	}

	private String getPath(AbstractIntroElement introElement, String suffix) {
		if (introElement == null || introElement instanceof IntroModelRoot) {
			return suffix;
		}
		String newSuffix = suffix;
		if (introElement instanceof AbstractIntroIdElement) {
			newSuffix = ((AbstractIntroIdElement)introElement).getId() + '/' + suffix;
		}
		return getPath(introElement.getParent(), newSuffix);

	}

}
