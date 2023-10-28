package org.eclipse.tips.ide.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tips.ui.internal.TipComposite;

import jakarta.annotation.PostConstruct;

public class TipPart {

	@PostConstruct
	public void createPartControl(Composite pParent) {
		Composite composite = new Composite(pParent, SWT.NONE);
		composite.setLayout(new FillLayout());
		IDETipManager manager = (IDETipManager) IDETipManager.getInstance();
		TipsStartupService.loadProviders();
		new TipComposite(composite, SWT.NONE).setTipManager(manager);
	}
}