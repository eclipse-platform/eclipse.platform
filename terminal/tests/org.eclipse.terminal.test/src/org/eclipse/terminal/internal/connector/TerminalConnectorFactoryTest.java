/*******************************************************************************
 * Copyright (c) 2007, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - [225853][api] Provide more default functionality in TerminalConnectorImpl
 * Martin Oberhuber (Wind River) - [204796] Terminal should allow setting the encoding to use
 * Uwe Stieber (Wind River) - [282996] [terminal][api] Add "hidden" attribute to terminal connector extension point
 * Anton Leherbauer (Wind River) - [433751] Add option to enable VT100 line wrapping mode
 *******************************************************************************/
package org.eclipse.terminal.internal.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.charset.Charset;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.connector.ISettingsStore;
import org.eclipse.terminal.connector.ITerminalControl;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.connector.provider.AbstractTerminalConnector;
import org.eclipse.terminal.control.TerminalTitleRequestor;
import org.junit.jupiter.api.Test;

public class TerminalConnectorFactoryTest {
	public class SettingsMock implements ISettingsStore {

		@Override
		public String get(String key) {
			return null;
		}

		@Override
		public String get(String key, String defaultValue) {
			return null;
		}

		@Override
		public void put(String key, String value) {
		}

	}

	public static class TerminalControlMock implements ITerminalControl {

		@Override
		public void setCharset(Charset charset) {
		}

		@Override
		public Charset getCharset() {
			return Charset.defaultCharset();
		}

		@Override
		public void displayTextInTerminal(String text) {
		}

		@Override
		public OutputStream getRemoteToTerminalOutputStream() {
			return null;
		}

		@Override
		public Shell getShell() {
			return null;
		}

		@Override
		public TerminalState getState() {
			return null;
		}

		@Override
		public void setMsg(String msg) {
		}

		@Override
		public void setState(TerminalState state) {
		}

		@Override
		public void setTerminalTitle(String title, TerminalTitleRequestor requestor) {
		}

		@Override
		public void setupTerminal(Composite parent) {
		}

		@Override
		public boolean isConnectOnEnterIfClosed() {
			return false;
		}

		@Override
		public void setConnectOnEnterIfClosed(boolean on) {
		}

		@Override
		public void setVT100LineWrapping(boolean enable) {
		}

		@Override
		public boolean isVT100LineWrapping() {
			return false;
		}

		@Override
		public void updateTerminalDimensions() {
		}
	}

	static class ConnectorMock extends AbstractTerminalConnector {

		public boolean fEcho;
		public int fWidth;
		public int fHeight;
		public ITerminalControl fTerminalControl;
		public ISettingsStore fSaveStore;
		public ISettingsStore fLoadStore;
		public boolean fDisconnect;

		@Override
		public boolean isLocalEcho() {
			return fEcho;
		}

		@Override
		public void setTerminalSize(int newWidth, int newHeight) {
			fWidth = newWidth;
			fHeight = newHeight;
		}

		@Override
		public void connect(ITerminalControl control) {
			super.connect(control);
			fTerminalControl = control;
		}

		@Override
		public void doDisconnect() {
			fDisconnect = true;
		}

		@Override
		public OutputStream getTerminalToRemoteStream() {
			return null;
		}

		@Override
		public String getSettingsSummary() {
			return "Summary";
		}

		@Override
		public void load(ISettingsStore store) {
			fLoadStore = store;
		}

		@Override
		public void save(ISettingsStore store) {
			fSaveStore = store;
		}
	}

	protected TerminalConnector makeTerminalConnector() {
		return makeTerminalConnector(new ConnectorMock());
	}

	protected TerminalConnector makeTerminalConnector(final AbstractTerminalConnector mock) {
		TerminalConnector c = new TerminalConnector(() -> mock, "xID", "xName", false);
		return c;
	}

	@Test
	public void testGetInitializationErrorMessage() {
		TerminalConnector c = makeTerminalConnector();
		c.connect(new TerminalControlMock());
		assertNull(c.getInitializationErrorMessage());

		c = makeTerminalConnector(new ConnectorMock() {
			@Override
			public void initialize() throws Exception {
				throw new Exception("FAILED");
			}
		});
		c.connect(new TerminalControlMock());
		assertEquals("FAILED", c.getInitializationErrorMessage());

	}

	@Test
	public void testGetIdAndName() {
		TerminalConnector c = makeTerminalConnector();
		assertEquals("xID", c.getId());
		assertEquals("xName", c.getName());
	}

	@Test
	public void testIsInitialized() {
		TerminalConnector c = makeTerminalConnector();
		assertFalse(c.isInitialized());
		c.getId();
		assertFalse(c.isInitialized());
		c.getName();
		assertFalse(c.isInitialized());
		c.getSettingsSummary();
		assertFalse(c.isInitialized());
		c.setTerminalSize(10, 10);
		assertFalse(c.isInitialized());
		c.load(null);
		assertFalse(c.isInitialized());
		c.save(null);
		assertFalse(c.isInitialized());
		if (!Platform.isRunning())
			return;
		c.getAdapter(ConnectorMock.class);
		assertFalse(c.isInitialized());
	}

	@Test
	public void testConnect() {
		TerminalConnector c = makeTerminalConnector();
		assertFalse(c.isInitialized());
		c.connect(new TerminalControlMock());
		assertTrue(c.isInitialized());

	}

	@Test
	public void testDisconnect() {
		ConnectorMock mock = new ConnectorMock();
		TerminalConnector c = makeTerminalConnector(mock);
		TerminalControlMock control = new TerminalControlMock();
		c.connect(control);
		c.disconnect();
		assertTrue(mock.fDisconnect);
	}

	@Test
	public void testGetTerminalToRemoteStream() {
		ConnectorMock mock = new ConnectorMock();
		TerminalConnector c = makeTerminalConnector(mock);
		TerminalControlMock control = new TerminalControlMock();
		c.connect(control);
		assertSame(mock.fTerminalControl, control);
	}

	@Test
	public void testGetSettingsSummary() {
		TerminalConnector c = makeTerminalConnector();
		assertEquals("Not Initialized", c.getSettingsSummary());
		c.connect(new TerminalControlMock());
		assertEquals("Summary", c.getSettingsSummary());
	}

	@Test
	public void testIsLocalEcho() {
		ConnectorMock mock = new ConnectorMock();
		TerminalConnector c = makeTerminalConnector(mock);
		assertFalse(c.isLocalEcho());
		mock.fEcho = true;
		assertTrue(c.isLocalEcho());
	}

	@Test
	public void testLoad() {
		ConnectorMock mock = new ConnectorMock();
		TerminalConnector c = makeTerminalConnector(mock);
		ISettingsStore s = new SettingsMock();
		c.load(s);
		// the load is called after the connect...
		assertNull(mock.fLoadStore);
		c.connect(new TerminalControlMock());
		assertSame(s, mock.fLoadStore);
	}

	@Test
	public void testSave() {
		ConnectorMock mock = new ConnectorMock();
		TerminalConnector c = makeTerminalConnector(mock);
		ISettingsStore s = new SettingsMock();
		c.save(s);
		assertNull(mock.fSaveStore);
		c.connect(new TerminalControlMock());
		c.save(s);
		assertSame(s, mock.fSaveStore);
	}

	@Test
	public void testSetDefaultSettings() {
		ConnectorMock mock = new ConnectorMock();
		TerminalConnector c = makeTerminalConnector(mock);
		c.setDefaultSettings();
	}

	@Test
	public void testSetTerminalSize() {
		ConnectorMock mock = new ConnectorMock();
		TerminalConnector c = makeTerminalConnector(mock);
		c.setTerminalSize(100, 200);

	}

	@Test
	public void testGetAdapter() {
		if (!Platform.isRunning())
			return;
		ConnectorMock mock = new ConnectorMock();
		TerminalConnector c = makeTerminalConnector(mock);
		assertNull(c.getAdapter(ConnectorMock.class));
		// the load is called after the connect...
		c.connect(new TerminalControlMock());

		assertSame(mock, c.getAdapter(ConnectorMock.class));
	}

}
