/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IMemento;

public interface IHelpPartPage {
	void addPart(String id, boolean flexible);
	void addPart(String id, boolean flexible, boolean grabVertical);
	boolean canOpen();
	void dispose();
	IHelpPart findPart(String id);
	int getHorizontalMargin();
	String getIconId();
	String getId();
	int getNumberOfFlexibleParts();
	String getText();
	IToolBarManager getToolBarManager();
	int getVerticalSpacing();
	void refilter();
	void saveState(IMemento memento);
	void setFocus();
	void setHorizontalMargin(int value);
	void setVerticalSpacing(int value);
	void setVisible(boolean visible);
	void stop();
	void toggleRoleFilter();
}
