package org.eclipse.ui.console.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.console.ConsoleView;

/**
 * @since 3.13
 */
public class ShowNextCommandHandler extends AbstractHandler {
	@Override
	public final Object execute(ExecutionEvent event) throws ExecutionException {
		// First find the console window.
		final IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part instanceof ConsoleView) {
			final IConsole console = ((ConsoleView) part).getConsole();
			if (console instanceof IOConsole) {
				IOConsole ioConsole = (IOConsole) console;
				ioConsole.showNextCommand();
			}
		}
		return null;
	}
}
