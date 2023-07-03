/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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

import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.util.EscapeUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class ScopeSelectPart extends AbstractFormPart implements IHelpPart  {

	public class ScopeObserver implements Observer {

		@Override
		public void update(Observable o, Object arg) {
			String name = ScopeState.getInstance().getScopeSetManager().getActiveSet().getName();
			setScopeLink(name);
		}

	}

	private FormText scopeSetLink;
	private Composite container;
	private String id;
	private ScopeObserver scopeObserver;

	public ScopeSelectPart(Composite parent, FormToolkit toolkit) {
		container = toolkit.createComposite(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 1;
		container.setLayout(layout);
		ScopeSetManager scopeSetManager = ScopeState.getInstance().getScopeSetManager();
		String name = scopeSetManager.getActiveSet().getName();
		scopeSetLink = toolkit.createFormText(container, false);
		setScopeLink(name);
		scopeSetLink.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				doChangeScopeSet();
			}
		});
		toolkit.adapt(scopeSetLink, true, true);
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.valign = TableWrapData.MIDDLE;
		scopeSetLink.setLayoutData(td);
		scopeObserver = new ScopeObserver();
		scopeSetManager.addObserver(scopeObserver);
	}

	private void doChangeScopeSet() {
		ScopeSetManager scopeSetManager = ScopeState.getInstance().getScopeSetManager();
		ScopeSetDialog dialog = new ScopeSetDialog(container.getShell(),
				scopeSetManager,
				ScopeState.getInstance().getEngineManager(), true);
		dialog.setInput(scopeSetManager);
		dialog.create();
		dialog.getShell().setText(Messages.ScopeSetDialog_wtitle);
		if (dialog.open() == ScopeSetDialog.OK) {
			ScopeSet set = dialog.getActiveSet();
			if (set != null) {
				setActiveScopeSet(set);
			}
			scopeSetManager.save();
			scopeSetManager.notifyObservers();
		}
	}

	private void setActiveScopeSet(ScopeSet set) {
		setScopeLink(set.getName());
		ScopeState.getInstance().getScopeSetManager().setActiveSet(set);
		ScopeState.getInstance().getScopeSetManager().notifyObservers();
	}

	private void setScopeLink(String name) {
		StringBuilder buff = new StringBuilder();
		StringBuilder nameBuff = new StringBuilder();
		nameBuff.append("</b> <a href=\"rescope\" "); //$NON-NLS-1$
		if (!Platform.getWS().equals(Platform.WS_GTK)) {
			nameBuff.append(" alt=\""); //$NON-NLS-1$
			nameBuff.append(Messages.FederatedSearchPart_changeScopeSet);
			nameBuff.append("\""); //$NON-NLS-1$
		}

		nameBuff.append(">");  //$NON-NLS-1$
		nameBuff.append(EscapeUtils.escapeSpecialChars(name ));
		nameBuff.append(" </a><b>"); //$NON-NLS-1$
		String scopeMessage = NLS.bind(Messages.ScopeSelect_scope, nameBuff.toString());
		buff.append("<form><p><b>"); //$NON-NLS-1$
		buff.append(scopeMessage);
		buff.append("</b></p></form>"); //$NON-NLS-1$
		scopeSetLink.setText(buff.toString(), true, false);
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.id = id;
		ScopeState.getInstance().setEngineManager(parent.getEngineManager());
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		container.setVisible(visible);
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return false;
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return false;
	}

	@Override
	public IAction getGlobalAction(String id) {
		return null;
	}

	@Override
	public void stop() {

	}

	@Override
	public void toggleRoleFilter() {

	}

	@Override
	public void refilter() {

	}

	@Override
	public void dispose() {
		if (scopeObserver != null) {
			ScopeState.getInstance().getScopeSetManager().deleteObserver(scopeObserver);
		}
		super.dispose();
	}

}
