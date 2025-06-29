/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;

/**
 * Considers multilines as damage regions even if the document partitioning has not changed.
 *
 * @see org.eclipse.jface.text.rules.DefaultDamagerRepairer
 */
public class MultilineDamagerRepairer extends DefaultDamagerRepairer {

	@SuppressWarnings("deprecation")
	public MultilineDamagerRepairer(ITokenScanner scanner, TextAttribute defaultTextAttribute) {
		super(scanner, defaultTextAttribute);
	}

	public MultilineDamagerRepairer(ITokenScanner scanner) {
		super(scanner);
	}

	@Override
	public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent e, boolean documentPartitioningChanged) {
		return partition;
	}

	/**
	 * Configures the scanner's default return token. This is the text attribute which is returned when none is returned by the current token.
	 */
	public void setDefaultTextAttribute(TextAttribute defaultTextAttribute) {
		fDefaultTextAttribute = defaultTextAttribute;
	}
}
