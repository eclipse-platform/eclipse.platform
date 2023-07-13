package org.eclipse.debug.tests;

import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.IIOConsolePage;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.IPageBookViewPage;

public class TestsConsolePageParticipant implements IConsolePageParticipant {

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		if (console instanceof TextConsole textConsole) {
			Object history = textConsole.getAttribute("history");
			if (history != null && history.equals("true")) {
				if (page instanceof IIOConsolePage iocPage) {
					iocPage.setEnableCommandLineHistory(true);
				}
			}
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public void activated() {
	}

	@Override
	public void deactivated() {
	}
}
