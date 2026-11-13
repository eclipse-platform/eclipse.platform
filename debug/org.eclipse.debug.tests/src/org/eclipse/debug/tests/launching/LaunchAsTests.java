/*******************************************************************************
 * Copyright (c) 2026 Sergii Zolotarov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sergii Zolotarov - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.launching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.actions.LaunchAsChoices;
import org.eclipse.debug.internal.ui.actions.LaunchAsChoices.Choice;
import org.eclipse.debug.internal.ui.actions.LaunchAsDialog;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the choices and the dialog of the "Run As...", "Debug As..." and
 * "Profile As..." commands.
 */
public class LaunchAsTests {

	private static final String SHORTCUT_LABEL = "Launch As Test Shortcut"; //$NON-NLS-1$

	private ILaunchConfiguration configuration;

	@BeforeEach
	public void setUp() {
		TestLaunchShortcut.LAUNCHES.clear();
	}

	@AfterEach
	public void tearDown() throws Exception {
		TestLaunchShortcut.given = new ILaunchConfiguration[0];
		if (configuration != null) {
			configuration.delete();
		}
	}

	@Test
	public void testShortcutForMatchingSelection() {
		assertThat(labels(choicesForTarget(ILaunchManager.RUN_MODE))).contains(SHORTCUT_LABEL);
	}

	@Test
	public void testNoShortcutForOtherSelection() {
		assertThat(labels(LaunchAsChoices.of(new StructuredSelection("other"), ILaunchManager.RUN_MODE))) //$NON-NLS-1$
				.doesNotContain(SHORTCUT_LABEL);
	}

	@Test
	public void testNoDisabledShortcut() {
		// The "Run As" menu shows this shortcut as disabled, so it cannot launch
		assertThat(labels(choicesForTarget(ILaunchManager.RUN_MODE))).doesNotContain("Launch As Disabled Test Shortcut"); //$NON-NLS-1$
	}

	@Test
	public void testOnlyShortcutsOfTheMode() {
		// The test shortcut has only the run mode
		assertThat(labels(choicesForTarget(ILaunchManager.DEBUG_MODE))).doesNotContain(SHORTCUT_LABEL);
	}

	@Test
	public void testChoiceLaunchesTheShownSelection() {
		IStructuredSelection selection = new StructuredSelection(new TestLaunchShortcut.Target("a")); //$NON-NLS-1$
		choice(LaunchAsChoices.of(selection, ILaunchManager.RUN_MODE), SHORTCUT_LABEL).launch().run();
		assertEquals(List.of("run " + selection), TestLaunchShortcut.LAUNCHES); //$NON-NLS-1$
	}

	@Test
	public void testConfigurationsComeAfterTheShortcuts() throws Exception {
		configuration = TestUtil.getLaunchConfiguration("LaunchAsTests configuration"); //$NON-NLS-1$
		TestLaunchShortcut.given = new ILaunchConfiguration[] { configuration };

		List<Choice> choices = choicesForTarget(ILaunchManager.RUN_MODE);
		String label = "LaunchAsTests configuration (" + configuration.getType().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		assertThat(labels(choices)).containsSubsequence(SHORTCUT_LABEL, label);
		assertEquals("configuration:LaunchAsTests configuration", choice(choices, label).key()); //$NON-NLS-1$
	}

	@Test
	public void testDialogSelectsTheLastChoiceAndFilters() {
		Choice first = new Choice("Alpha Application", null, "a", () -> { //$NON-NLS-1$ //$NON-NLS-2$
		});
		Choice second = new Choice("Beta Test", null, "b", () -> { //$NON-NLS-1$ //$NON-NLS-2$
		});
		Choice third = new Choice("Gamma Test", null, "c", () -> { //$NON-NLS-1$ //$NON-NLS-2$
		});
		LaunchAsDialog dialog = new LaunchAsDialog(PlatformUI.getWorkbench().getWorkbenchWindows()[0].getShell(),
				"Run As", "Run", List.of(first, second, third), second); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			assertSame(second, selected(dialog));
			assertEquals(3, dialog.getViewer().getTable().getItemCount());

			// The filter hides the selected choice: the first shown one is selected
			dialog.getFilterText().setText("gamma"); //$NON-NLS-1$
			assertEquals(1, dialog.getViewer().getTable().getItemCount());
			assertSame(third, selected(dialog));

			dialog.getFilterText().setText("test"); //$NON-NLS-1$
			assertEquals(2, dialog.getViewer().getTable().getItemCount());
		} finally {
			dialog.close();
		}
		assertNull(dialog.getResult());
	}

	private static List<Choice> choicesForTarget(String mode) {
		return LaunchAsChoices.of(new StructuredSelection(new TestLaunchShortcut.Target("a")), mode); //$NON-NLS-1$
	}

	private static Object selected(LaunchAsDialog dialog) {
		return ((IStructuredSelection) dialog.getViewer().getSelection()).getFirstElement();
	}

	private static List<String> labels(List<Choice> choices) {
		return choices.stream().map(Choice::label).toList();
	}

	private static Choice choice(List<Choice> choices, String label) {
		return choices.stream().filter(choice -> label.equals(choice.label())).findFirst().orElseThrow();
	}
}
