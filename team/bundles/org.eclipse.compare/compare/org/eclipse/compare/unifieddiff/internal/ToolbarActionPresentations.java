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
package org.eclipse.compare.unifieddiff.internal;

import java.util.EnumMap;
import java.util.Map;

import org.eclipse.compare.unifieddiff.UnifiedDiff.ToolbarAction;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * The texts and images a caller supplied for the built-in toolbar actions.
 * Absent entries fall back to the default of the action; an entry mapped to
 * <code>null</code> is an override to "no image".
 */
public final class ToolbarActionPresentations {

	/** No overrides, so every action keeps its default text and image. */
	public static final ToolbarActionPresentations NONE = new ToolbarActionPresentations(Map.of(), Map.of());

	private final Map<ToolbarAction, String> texts = new EnumMap<>(ToolbarAction.class);
	private final Map<ToolbarAction, ImageDescriptor> images = new EnumMap<>(ToolbarAction.class);

	public ToolbarActionPresentations(Map<ToolbarAction, String> texts, Map<ToolbarAction, ImageDescriptor> images) {
		this.texts.putAll(texts);
		this.images.putAll(images);
	}

	String text(ToolbarAction action, String defaultText) {
		return texts.getOrDefault(action, defaultText);
	}

	ImageDescriptor image(ToolbarAction action, ImageDescriptor defaultImage) {
		return images.containsKey(action) ? images.get(action) : defaultImage;
	}
}
