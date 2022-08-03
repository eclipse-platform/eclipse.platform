/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.net;

import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.internal.net.ProxySelector;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TableColumn;

public class ProxyEntriesLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {

	public ProxyEntriesLabelProvider() {
		super();
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element == null) {
			return null;
		}
		ProxyData data = (ProxyData) element;
		switch (columnIndex) {
		case 0:
			return null;
		case 1:
			return data.getType();
		case 2:
			if (data.isDynamic()) {
				return NetUIMessages.ProxyPreferencePage_18;
			}
			return data.getHost();
		case 3:
			if (data.isDynamic()) {
				return NetUIMessages.ProxyPreferencePage_18;
			}
			if (data.getPort() == -1) {
				return ""; //$NON-NLS-1$
			}
			return Integer.toString(data.getPort());
		case 4:
			return ProxySelector.localizeProvider(data.getSource());
		case 5:
			return data.isRequiresAuthentication() ? NetUIMessages.ProxyPreferencePage_19 : NetUIMessages.ProxyPreferencePage_20;
		case 6:
			return data.getUserId();
		case 7:
			if (data.getPassword() == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < data.getPassword().length(); i++) {
				sb.append('*');
			}
			return sb.toString();
		default:
			return null;
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void createColumns(TableViewer viewer) {
		String[] titles = {
				"", //$NON-NLS-1$
				NetUIMessages.ProxyPreferencePage_2,
				NetUIMessages.ProxyPreferencePage_3,
				NetUIMessages.ProxyPreferencePage_4,
				NetUIMessages.ProxyPreferencePage_5,
				NetUIMessages.ProxyPreferencePage_6,
				NetUIMessages.ProxyPreferencePage_7,
				NetUIMessages.ProxyPreferencePage_8 };
		for (String title : titles) {
			TableColumn column = new TableViewerColumn(viewer, SWT.NONE)
					.getColumn();
			column.setText(title);
			column.setResizable(true);
		}
	}
}
