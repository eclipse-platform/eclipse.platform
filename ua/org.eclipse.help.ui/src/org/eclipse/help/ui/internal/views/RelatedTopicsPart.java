/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ILayoutExtension;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class RelatedTopicsPart extends AbstractFormPart implements IHelpPart {
	private ManagedForm mform;

	private ContextHelpPart contextHelpPart;

	private DynamicHelpPart dynamicHelpPart;

	private ReusableHelpPart parent;

	private String id;

	private int VSPACE = 10;

	private static boolean dynamicHelpPreferenceRead = false;
	private static boolean useDynamicHelp = false;

	class RelatedLayout extends Layout implements ILayoutExtension {

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint,
				boolean flushCache) {
			Point topSize = contextHelpPart.getControl().computeSize(wHint,
					hHint, flushCache);

			Point botSize = dynamicHelpPart == null ? new Point(0, 0) :
				dynamicHelpPart.getControl().computeSize(wHint,
					hHint, flushCache);
			Point size = new Point(0, 0);
			size.x = Math.max(topSize.x, botSize.x);
			size.y = VSPACE + topSize.y + VSPACE + botSize.y;
			return size;
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle carea = composite.getClientArea();
			Point topSize = contextHelpPart.getControl().computeSize(
					carea.width, SWT.DEFAULT, flushCache);
			Point botSize = dynamicHelpPart == null ? new Point(0, 0) :
				dynamicHelpPart.getControl().computeSize(
					carea.width, SWT.DEFAULT, flushCache);
			int y = VSPACE;
			contextHelpPart.getControl().setBounds(0, y, carea.width, topSize.y);
			y += topSize.y + VSPACE;
			if (dynamicHelpPart != null) {
				dynamicHelpPart.getControl().setBounds(0, y, carea.width, botSize.y);
			}
		}

		@Override
		public int computeMinimumWidth(Composite parent, boolean changed) {
			int top = computeMinimumWidth(contextHelpPart, parent, changed);
			int bot = dynamicHelpPart == null ? 0 : computeMinimumWidth(dynamicHelpPart, parent, changed);
			return Math.max(top, bot);
		}

		private int computeMinimumWidth(IHelpPart part, Composite parent, boolean changed) {
			ILayoutExtension le = (ILayoutExtension)((Composite)part.getControl()).getLayout();
			return le.computeMinimumWidth(parent, changed);
		}

		@Override
		public int computeMaximumWidth(Composite parent, boolean changed) {
			return computeSize(parent, SWT.DEFAULT, SWT.DEFAULT, changed).x;
		}
	}

	public RelatedTopicsPart(Composite parent, FormToolkit toolkit) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		mform = new ManagedForm(toolkit, form) {

			@Override
			public void reflow(boolean changed) {
				super.reflow(changed);
				RelatedTopicsPart.this.parent.reflow();
			}
		};
		form.getBody().setLayout(new RelatedLayout());
		contextHelpPart = new ContextHelpPart(form.getBody(), toolkit);
		if (isUseDynamicHelp()) {
		     dynamicHelpPart = new DynamicHelpPart(form.getBody(), toolkit);
		}
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
		contextHelpPart.init(parent, IHelpUIConstants.HV_CONTEXT_HELP, memento);
		mform.addPart(contextHelpPart);
		if (isUseDynamicHelp()) {
			dynamicHelpPart.init(parent, IHelpUIConstants.HV_SEARCH_RESULT, memento);
			mform.addPart(dynamicHelpPart);
		}
		mform.initialize();
	}

	@Override
	public void dispose() {
		mform.dispose();
	}

	public void handleActivation(Control c, IWorkbenchPart wpart) {
		IContextProvider provider = (wpart == null) ? null : (IContextProvider) wpart.getAdapter(IContextProvider.class);
		contextHelpPart.handleActivation(provider, null, c, wpart, false);
	}

	public void setDefaultText(String defaultText) {
		contextHelpPart.setDefaultText(defaultText);
	}

	@Override
	public Control getControl() {
		return mform.getForm();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		mform.getForm().setVisible(visible);
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return contextHelpPart.hasFocusControl(control)
				|| (dynamicHelpPart != null && dynamicHelpPart.hasFocusControl(control));
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		Control focusControl = mform.getForm().getDisplay().getFocusControl();
		if (contextHelpPart.hasFocusControl(focusControl) || dynamicHelpPart == null)
			return contextHelpPart.fillContextMenu(manager);
		return dynamicHelpPart.fillContextMenu(manager);
	}

	@Override
	public IAction getGlobalAction(String id) {
		return contextHelpPart.getGlobalAction(id);
	}

	@Override
	public void stop() {
		contextHelpPart.stop();
		if (dynamicHelpPart != null) {
			dynamicHelpPart.stop();
		}
	}

	@Override
	public void toggleRoleFilter() {
		contextHelpPart.toggleRoleFilter();
		if (dynamicHelpPart != null) {
			dynamicHelpPart.toggleRoleFilter();
		}
	}

	@Override
	public void refilter() {
		contextHelpPart.refilter();
		if (dynamicHelpPart != null) {
			dynamicHelpPart.refilter();
		}
	}

	@Override
	public boolean setFormInput(Object input) {
		return mform.setInput(input);
	}
	public void startSearch(String newPhrase, IContext excludeContext) {
		if (dynamicHelpPart != null) {
			dynamicHelpPart.startSearch(newPhrase, excludeContext);
		}
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public void setFocus() {
		if (contextHelpPart!=null)
			contextHelpPart.setFocus();
	}

	public static boolean isUseDynamicHelp() {
		if (!dynamicHelpPreferenceRead) {
			dynamicHelpPreferenceRead = true;
			// Preference created in case anyone complains about the removal of
			// the dynamic help section. This is not API, not documented and may
			// be removed in a future release.
			useDynamicHelp =
				Platform.getPreferencesService().getBoolean(HelpBasePlugin.PLUGIN_ID, "show_dynamic_help", false, null); //$NON-NLS-1$
		}
		return useDynamicHelp;
	}
}
