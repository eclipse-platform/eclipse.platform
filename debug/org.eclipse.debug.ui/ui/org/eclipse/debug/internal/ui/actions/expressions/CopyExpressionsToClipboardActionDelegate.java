/*******************************************************************************
 *  Copyright (c) 2017 Bachmann electronic GmbH and others.
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
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;

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


}
