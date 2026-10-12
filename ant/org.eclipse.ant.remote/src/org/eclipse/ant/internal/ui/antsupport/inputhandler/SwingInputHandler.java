/*******************************************************************************
 * Copyright (c) 2026 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.antsupport.inputhandler;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.input.MultipleChoiceInputRequest;

/**
 * Ant input handler used for Ant builds running in a separate JVM: prompts the user with a Swing dialog.
 * <p>
 * Only depends on Java SE (<code>java.desktop</code> module), so it works on every JRE that can be used to run Ant in a separate JVM.
 * </p>
 */
public class SwingInputHandler extends DefaultInputHandler {

	@Override
	public void handleInput(InputRequest request) throws BuildException {
		if (System.getProperty("eclipse.ant.noInput") != null) { //$NON-NLS-1$
			throw new BuildException(RemoteAntMessages.getString("SwingInputHandler.noInput")); //$NON-NLS-1$
		}
		if (GraphicsEnvironment.isHeadless()) {
			throw new BuildException(RemoteAntMessages.getString("SwingInputHandler.headless")); //$NON-NLS-1$
		}
		String prompt;
		if (request instanceof MultipleChoiceInputRequest) {
			// choices are shown in the combo
			prompt = request.getPrompt();
		} else {
			prompt = getPrompt(request);
		}
		boolean[] accepted = new boolean[1];
		Runnable dialogRunnable = () -> accepted[0] = new InputDialog(request, prompt).open();
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				dialogRunnable.run();
			} else {
				SwingUtilities.invokeAndWait(dialogRunnable);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BuildException(RemoteAntMessages.getString("SwingInputHandler.cancelled"), e); //$NON-NLS-1$
		}
		catch (InvocationTargetException e) {
			throw new BuildException(RemoteAntMessages.getString("SwingInputHandler.cancelled"), e.getCause()); //$NON-NLS-1$
		}
		if (!accepted[0]) {
			throw new BuildException(RemoteAntMessages.getString("SwingInputHandler.cancelled")); //$NON-NLS-1$
		}
	}

	/**
	 * Modal dialog asking for the input; must be created and opened in the AWT event dispatch thread.
	 */
	private static final class InputDialog {

		private static final int MESSAGE_COLUMNS = 60;
		private static final int MARGIN = 12;
		private static final int SPACING = 6;

		private final InputRequest request;
		private final JDialog dialog;
		private final JTextField text;
		private final JComboBox<String> combo;
		private final JLabel errorLabel;
		private final JButton okButton;
		private boolean firstValidation = true;
		private boolean accepted;

		InputDialog(InputRequest request, String prompt) {
			this.request = request;
			useSystemLookAndFeel();

			dialog = new JDialog((Frame) null, RemoteAntMessages.getString("SwingInputHandler.title"), true); //$NON-NLS-1$
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			// the dialog is opened by a background process, make sure it is not hidden behind the IDE
			dialog.setAlwaysOnTop(true);

			JPanel content = new JPanel(new BorderLayout(0, SPACING));
			content.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
			content.add(createMessageArea(prompt), BorderLayout.NORTH);

			JComponent inputControl;
			if (request instanceof MultipleChoiceInputRequest choiceRequest) {
				combo = new JComboBox<>();
				combo.addItem(""); //$NON-NLS-1$
				for (String choice : choiceRequest.getChoices()) {
					combo.addItem(choice);
				}
				text = null;
				inputControl = combo;
			} else {
				text = new JTextField();
				combo = null;
				inputControl = text;
			}
			errorLabel = new JLabel(" "); //$NON-NLS-1$
			JPanel inputPanel = new JPanel(new BorderLayout(0, SPACING));
			inputPanel.add(inputControl, BorderLayout.NORTH);
			inputPanel.add(errorLabel, BorderLayout.SOUTH);
			content.add(inputPanel, BorderLayout.CENTER);

			okButton = new JButton(RemoteAntMessages.getString("SwingInputHandler.ok")); //$NON-NLS-1$
			JButton cancelButton = new JButton(RemoteAntMessages.getString("SwingInputHandler.cancel")); //$NON-NLS-1$
			okButton.addActionListener(e -> close(true));
			cancelButton.addActionListener(e -> close(false));
			JPanel buttons = new JPanel(new GridLayout(1, 2, SPACING, 0));
			buttons.add(okButton);
			buttons.add(cancelButton);
			JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
			buttonBar.add(buttons);
			content.add(buttonBar, BorderLayout.SOUTH);

			dialog.setContentPane(content);
			dialog.getRootPane().setDefaultButton(okButton);
			dialog.getRootPane().registerKeyboardAction(e -> close(false), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent e) {
					inputControl.requestFocusInWindow();
				}
			});

			setDefaultValue();
			if (combo != null) {
				combo.addActionListener(e -> validateInput());
			} else {
				text.getDocument().addDocumentListener(new DocumentListener() {
					@Override
					public void insertUpdate(DocumentEvent e) {
						validateInput();
					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						validateInput();
					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						validateInput();
					}
				});
			}
			validateInput();
		}

		/**
		 * Opens the dialog and blocks until it is closed.
		 *
		 * @return <code>true</code> if the user confirmed the input
		 */
		boolean open() {
			dialog.pack();
			dialog.setLocationRelativeTo(null);
			dialog.toFront();
			dialog.setVisible(true); // blocks until disposed, the dialog is modal
			return accepted;
		}

		private void close(boolean ok) {
			accepted = ok;
			dialog.dispose();
		}

		private void setDefaultValue() {
			String value = getDefaultValue();
			if (value == null) {
				return;
			}
			if (combo != null) {
				combo.setSelectedItem(value);
			} else {
				text.setText(value);
				text.selectAll();
			}
		}

		private String getDefaultValue() {
			try {
				return request.getDefaultValue();
			}
			catch (NoSuchMethodError e) {
				// pre Ant 1.7.0
				return null;
			}
		}

		private void validateInput() {
			String input;
			if (combo != null) {
				input = (String) combo.getSelectedItem();
			} else {
				input = text.getText();
			}
			request.setInput(input == null ? "" : input); //$NON-NLS-1$
			String errorMessage = null;
			if (!request.isInputValid()) {
				// don't complain before the user had a chance to enter something
				errorMessage = firstValidation ? "" : RemoteAntMessages.getString("SwingInputHandler.invalidInput"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			firstValidation = false;
			errorLabel.setText(errorMessage == null || errorMessage.isEmpty() ? " " : errorMessage); //$NON-NLS-1$
			okButton.setEnabled(errorMessage == null);
		}

		private static JTextArea createMessageArea(String prompt) {
			JTextArea message = new JTextArea(prompt == null ? "" : prompt); //$NON-NLS-1$
			message.setEditable(false);
			message.setFocusable(false);
			message.setLineWrap(true);
			message.setWrapStyleWord(true);
			message.setOpaque(false);
			message.setBorder(null);
			message.setFont(UIManager.getFont("Label.font")); //$NON-NLS-1$
			message.setForeground(UIManager.getColor("Label.foreground")); //$NON-NLS-1$
			message.setColumns(MESSAGE_COLUMNS);
			// let the text area compute the wrapped height for the given width before the dialog is packed
			message.setSize(message.getPreferredSize().width, Short.MAX_VALUE);
			return message;
		}

		private static void useSystemLookAndFeel() {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch (Exception e) {
				// keep the default look and feel
			}
		}
	}
}
