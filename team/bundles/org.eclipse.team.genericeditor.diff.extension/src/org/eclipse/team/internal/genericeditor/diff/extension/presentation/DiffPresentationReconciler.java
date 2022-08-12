/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.team.internal.genericeditor.diff.extension.presentation;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.genericeditor.diff.extension.partitioner.IDiffPartitioning;
import org.eclipse.team.internal.genericeditor.diff.extension.rules.StartOfLineRule;

/**
 * Presentation reconciler collecting different rules for syntax coloring.
 */
public class DiffPresentationReconciler extends PresentationReconciler {

	private final TextAttribute bodyAttributeMinus = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_RED));
	private final TextAttribute bodyAttributePlus = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
	private final TextAttribute bodyAttributeMinusBold = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_RED), null, SWT.BOLD);
	private final TextAttribute bodyAttributePlusBold = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN), null, SWT.BOLD);
	private final TextAttribute hunkAttribute = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.BOLD);
	private final TextAttribute headerAttribute = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_DARK_MAGENTA));
	private final TextAttribute indexDiffAttribute = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_DARK_CYAN), null, SWT.BOLD);

	public DiffPresentationReconciler() {

		RuleBasedScanner hscanner = new RuleBasedScanner();
		IRule[] headerRules=new IRule[3];
		headerRules[0]=new StartOfLineRule("commit", null, new Token(headerAttribute));//$NON-NLS-1$
		headerRules[1]=new StartOfLineRule("Author:", null, new Token(headerAttribute));//$NON-NLS-1$
		headerRules[2]=new StartOfLineRule("Date:", null, new Token(headerAttribute));//$NON-NLS-1$
		hscanner.setRules(headerRules);
		DefaultDamagerRepairer hdr = new DefaultDamagerRepairer(hscanner);
		this.setDamager(hdr, IDiffPartitioning.PARTITION_HEADER);
		this.setRepairer(hdr, IDiffPartitioning.PARTITION_HEADER);

		IRule[] bodyRules=new IRule[7];
		bodyRules[0]=new StartOfLineRule("---", null, new Token(bodyAttributeMinusBold));//$NON-NLS-1$
		bodyRules[1]=new StartOfLineRule("+++", null, new Token(bodyAttributePlusBold));//$NON-NLS-1$
		bodyRules[2] = new StartOfLineRule("@@", null, new Token(hunkAttribute));//$NON-NLS-1$
		bodyRules[3]=new StartOfLineRule("diff --git", null, new Token(indexDiffAttribute));//$NON-NLS-1$
		bodyRules[4]=new StartOfLineRule("index", null, new Token(indexDiffAttribute));//$NON-NLS-1$
		bodyRules[5] = new StartOfLineRule("+", null, new Token(bodyAttributePlus));//$NON-NLS-1$
		bodyRules[6] = new StartOfLineRule("-", null, new Token(bodyAttributeMinus));//$NON-NLS-1$
		RuleBasedScanner scanner = new RuleBasedScanner();
		scanner.setRules(bodyRules);
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
		this.setDamager(dr, IDiffPartitioning.PARTITION_BODY);
		this.setRepairer(dr, IDiffPartitioning.PARTITION_BODY);
		this.setDocumentPartitioning(IDiffPartitioning.DIFF_PARTITIONINING);

	}
}
