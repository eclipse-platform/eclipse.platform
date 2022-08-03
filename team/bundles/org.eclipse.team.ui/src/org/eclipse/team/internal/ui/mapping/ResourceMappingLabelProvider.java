/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ResourceMappingLabelProvider extends LabelProvider {
	WorkbenchLabelProvider provider = new WorkbenchLabelProvider();
	@Override
	public String getText(Object element) {
		if (element instanceof ResourceMapping) {
			ResourceMapping mapping = (ResourceMapping) element;
			String text = provider.getText(mapping.getModelObject());
			if (text != null && text.length() > 0)
				return text;
			return super.getText(mapping.getModelObject());
		}
		if (element instanceof ModelProvider) {
			ModelProvider provider = (ModelProvider) element;
			return provider.getDescriptor().getLabel();
		}
		String text = provider.getText(element);
		if (text != null && text.length() > 0)
			return text;
		return super.getText(element);
	}
	@Override
	public Image getImage(Object element) {
		Image image = provider.getImage(element);
		if (image != null)
			return image;
		if (element instanceof ResourceMapping) {
			ResourceMapping mapping = (ResourceMapping) element;
			image = provider.getImage(mapping.getModelObject());
			if (image != null)
				return image;
		}
		return super.getImage(element);
	}
	@Override
	public void dispose() {
		provider.dispose();
		super.dispose();
	}
}