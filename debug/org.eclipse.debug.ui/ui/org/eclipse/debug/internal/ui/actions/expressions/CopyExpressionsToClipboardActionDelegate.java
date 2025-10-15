/*******************************************************************************
 *  Copyright (c) 2017, 2025 Bachmann electronic GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Bachmann electronic GmbH - initial API and implementation
 *     IBM Corporation - Exclude Expression UI values while copying
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;

import org.eclipse.debug.internal.core.WatchExpression;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.viewers.model.VirtualCopyToClipboardActionDelegate;

public class CopyExpressionsToClipboardActionDelegate extends VirtualCopyToClipboardActionDelegate {

	private static final String QUOTE = "\""; //$NON-NLS-1$
	@Override
	protected String trimLabel(String rawLabel) {
		String label = super.trimLabel(rawLabel);
		if (label == null) {
			return null;
		}
		if (label.startsWith(QUOTE)) {
			label = label.substring(1);
		}
		if (label.endsWith(QUOTE)) {
			label = label.substring(0, label.length() - 1);
		}
		return label;
	}

	@Override
	protected String exludeLabels(Object itemData, String label) {
		if (itemData instanceof WatchExpression watchExp) {
			if (watchExp.isPending() || watchExp.hasErrors()) {
				if (label.equals(DebugUIMessages.DefaultLabelProvider_12)
						|| label.contains(DebugUIMessages.DefaultLabelProvider_13)) {
					return null;
				}
			}
			if (!watchExp.isEnabled()) {
				if (label.equals(DebugUIMessages.DefaultLabelProvider_15)) {
					return null;
				}
				if (label.contains(DebugUIMessages.DefaultLabelProvider_15)) {
					int index = label.lastIndexOf(DebugUIMessages.DefaultLabelProvider_15);
					if (index != -1) {
						label = label.substring(0, index)
								+ label.substring(index + DebugUIMessages.DefaultLabelProvider_15.length());
					}
				}
			}
			return label;
		}
		return label;
	}


}
