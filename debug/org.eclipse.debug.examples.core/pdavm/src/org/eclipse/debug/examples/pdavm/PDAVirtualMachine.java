/*******************************************************************************
 * Copyright (c) 2005, 2018 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *     IBM Coporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.examples.pdavm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Push Down Automata interpreter.
 *
 * @since 3.5
 */
public class PDAVirtualMachine {

	static class Stack extends LinkedList<Object> {
		private static final long serialVersionUID = 1L;

		@Override
		public Object pop() {
			return isEmpty() ? Integer.valueOf(0) : remove(size() - 1);
		}

		@Override
		public void push(Object value) {
			add(value);
		}
	}

	static class Register {
		Register(String name) {
			fName = name;
		}
		String fName;
		String fGroup = "<no_group>"; //$NON-NLS-1$
		boolean fIsWriteable = true;
		Map<String, BitField> fBitFields = new LinkedHashMap<>(0);
		int fValue;
	}

	static class BitField {
		BitField(String name) {
			fName = name;
		}
		String fName;
		int fBitOffset;
		int fBitCount;
		Map<String, Integer> fMnemonics = new LinkedHashMap<>(0);
	}

	Map<String, Register> fRegisters = new LinkedHashMap<>(0);

	class Args {
		final String[] fArgs;

		int next = 0;

		Args(String[] args) {
			fArgs = args;
		}

		boolean hasNextArg() {
			return fArgs.length > next;
		}

		String getNextStringArg() {
			if (fArgs.length > next) {
				return fArgs[next++];
			}
			return ""; //$NON-NLS-1$
		}

		int getNextIntArg() {
			String arg = getNextStringArg();
			try {
				return Integer.parseInt(arg);
			} catch (NumberFormatException e) {
			}
			return 0;
		}

		boolean getNextBooleanArg() {
			String arg = getNextStringArg();
			try {
				return Boolean.getBoolean(arg);
			} catch (NumberFormatException e) {
			}
			return false;
		}

		Object getNextIntOrStringArg() {
			String arg = getNextStringArg();
			try {
				return Integer.valueOf(arg);
			} catch (NumberFormatException e) {
			}
			return arg;
		}

		PDAThread getThreadArg() {
			int id = getNextIntArg();
			return fThreads.get( Integer.valueOf(id) );
		}
	}

	class PDAThread {
		final int fID;

		/** The push down automata data stack (the data stack). */
		final Stack fStack = new Stack();

		/**
		 * PDAThread copy of the code. It can differ from the program if
		 * performing an evaluation.
		 */
		String[] fThreadCode;

		/** PDAThread copy of the labels. */
		Map<String, Integer> fThreadLabels;

		/** The stack of stack frames (the control stack) */
		final List<Frame> fFrames = new LinkedList<>();

		/** Current stack frame (not includced in fFrames) */
		Frame fCurrentFrame;

		/**
		 * The run flag is true if the thread is running. If the run flag is
		 * false, the thread exits the next time the main instruction loop runs.
		 */
		boolean fRun = true;

		String fSuspend = null;

		boolean fStep = false;

		boolean fStepReturn = false;

		int fSavedPC;

		boolean fPerformingEval = false;

		PDAThread(int id, String function, int pc) {
			fID = id;
			fCurrentFrame = new Frame(function, pc);
			fThreadCode = fCode;
			fThreadLabels = fLabels;
		}
	}

	final Map<Integer, PDAThread> fThreads = new LinkedHashMap<>();

	int fNextThreadId = 1;

	boolean fStarted = true;
	/**
	 * The code is stored as an array of strings, each line of the source file
	 * being one entry in the array.
	 */
	final String[] fCode;

	/** A mapping of labels to indicies in the code array */
	final Map<String, Integer> fLabels;

	/** Each stack frame is a mapping of variable names to values. */
	class Frame {
		final Map<String, Object> fLocalVariables = new LinkedHashMap<>();

		/**
		 * The name of the function in this frame
		 */
		final String fFunction;

		/**
		 * The current program counter in the frame the pc points to the next
		 * instruction to be executed
		 */
		int fPC;

		Frame(String function, int pc) {
			fFunction = function;
			fPC = pc;
		}

		void set(String name, Object value) {
			if (name.startsWith("$")) { //$NON-NLS-1$
				setRegisterValue(name, value);
			} else {
				fLocalVariables.put(name, value);
			}
		}

		Object get(String name) {
			if (name.startsWith("$")) { //$NON-NLS-1$
				return getRegisterValue(name);
			} else {
				return fLocalVariables.get(name);
			}
		}
	}

	void setRegisterValue(String name, Object value) {
		Register reg = fRegisters.get(getRegisterPartOfName(name));
		if (reg == null) {
			return;
		}
		String bitFieldName = getBitFieldPartOfName(name);
		if (bitFieldName != null) {
			BitField bitField = reg.fBitFields.get(bitFieldName);
			if (bitField == null) {
				return;
			}
			Integer intValue = null;
			if (value instanceof Integer) {
				intValue = (Integer)value;
			} else if (value instanceof String) {
				intValue = bitField.fMnemonics.get(value);
			}
			if (intValue != null) {
				int bitFieldMask = 2^(bitField.fBitCount - 1);
				int registerMask = ~(bitFieldMask << bitField.fBitOffset);
				int bitFieldValue = intValue.intValue() & bitFieldMask;
				reg.fValue = (reg.fValue & registerMask) | (bitFieldValue << bitField.fBitOffset);
			}
		} else if (value instanceof Integer) {
			reg.fValue = ((Integer)value).intValue();
		}
	}

	Object getRegisterValue(String name) {
		Register reg = fRegisters.get(getRegisterPartOfName(name));
		if (reg == null) {
			return null;
		}
		String bitFieldName = getBitFieldPartOfName(name);
		if (bitFieldName != null) {
			BitField bitField = reg.fBitFields.get(bitFieldName);
			if (bitField == null) {
				return null;
			}
			int bitFieldMask = 2^(bitField.fBitCount - 1);
			int registerMask = bitFieldMask << bitField.fBitOffset;
			return Integer.valueOf( (reg.fValue & registerMask) >> bitField.fBitOffset );
		} else {
			return Integer.valueOf(reg.fValue);
		}
	}

	/**
	 * Breakpoints are stored per each each line of code.  The boolean indicates
	 * whether the whole VM should suspend or just the triggering thread.
	 */
	final Map<Integer, Boolean> fBreakpoints = new HashMap<>();

	/**
	 * The suspend flag is true if the VM should suspend running the program and
	 * just listen for debug commands.
	 */
	String fSuspendVM;

	/** Flag indicating whether the debugger is performing a step. */
	boolean fStepVM = false;

	/** Flag indicating whether the debugger is performing a step return */
	boolean fStepReturnVM = false;

	int fSteppingThread = 0;

	/** Name of the pda program being debugged */
	final String fFilename;

	/** The command line argument to start a debug session. */
	final boolean fDebug;

	/** The port to listen for debug commands on */
	final int fCommandPort;

	/**
	 * Command socket for receiving debug commands and sending command responses
	 */
	Socket fCommandSocket;

	/** Command socket reader */
	BufferedReader fCommandReceiveStream;

	/** Command socket write stream. */
	OutputStream fCommandResponseStream;

	/** The port to send debug events to */
	final int fEventPort;

	/** Event socket */
	Socket fEventSocket;

	/** Event socket and write stream. */
	OutputStream fEventStream;

	/** The eventstops table holds which events cause suspends and which do not. */
	final Map<String, Boolean> fEventStops = new HashMap<>();
	{
		fEventStops.put("unimpinstr", Boolean.FALSE); //$NON-NLS-1$
		fEventStops.put("nosuchlabel", Boolean.FALSE); //$NON-NLS-1$
	}

	/**
	 * The watchpoints table holds watchpoint information.
	 * <p/>
	 * variablename_stackframedepth => N
	 * <ul>
	 * <li>N = 0 is no watch</li>
	 * <li>N = 1 is read watch</li>
	 * <li>N = 2 is write watch</li>
	 * <li>N = 3 is both, etc.</li>
	 */
	final Map<String, Integer> fWatchpoints = new HashMap<>();

	public static void main(String[] args) {
		String programFile = args.length >= 1 ? args[0] : null;
		if (programFile == null) {
			System.err.println("Error: No program specified"); //$NON-NLS-1$
			return;
		}

		String debugFlag = args.length >= 2 ? args[1] : ""; //$NON-NLS-1$
		boolean debug = "-debug".equals(debugFlag); //$NON-NLS-1$
		int commandPort = 0;
		int eventPort = 0;

		if (debug) {
			String commandPortStr = args.length >= 3 ? args[2] : ""; //$NON-NLS-1$
			try {
				commandPort = Integer.parseInt(commandPortStr);
			} catch (NumberFormatException e) {
				System.err.println("Error: Invalid command port"); //$NON-NLS-1$
				return;
			}

			String eventPortStr = args.length >= 4 ? args[3] : ""; //$NON-NLS-1$
			try {
				eventPort = Integer.parseInt(eventPortStr);
			} catch (NumberFormatException e) {
				System.err.println("Error: Invalid event port"); //$NON-NLS-1$
				return;
			}
		}

		PDAVirtualMachine pdaVM = null;
		try {
			pdaVM = new PDAVirtualMachine(programFile, debug, commandPort, eventPort);
			pdaVM.startDebugger();
		} catch (IOException e) {
			System.err.println("Error: " + e); //$NON-NLS-1$
			return;
		}
		pdaVM.run();
	}

	PDAVirtualMachine(String inputFile, boolean debug, int commandPort, int eventPort) throws IOException {
		fFilename = inputFile;

		// Load all the code into memory
		StringWriter stringWriter = new StringWriter();
		List<String> code = new LinkedList<>();
		try (FileReader fileReader = new FileReader(inputFile)) {
			int c = fileReader.read();
			while (c != -1) {
				if (c == '\n') {
					code.add(stringWriter.toString().trim());
					stringWriter = new StringWriter();
				} else {
					stringWriter.write(c);
				}
				c = fileReader.read();
			}
		}

		code.add(stringWriter.toString().trim());
		fCode = code.toArray(new String[code.size()]);

		fLabels = mapLabels(fCode);

		fDebug = debug;
		fCommandPort = commandPort;
		fEventPort = eventPort;
	}

	/**
	 * Initializes the labels map
	 */
	Map<String, Integer> mapLabels(String[] code) {
		Map<String, Integer> labels = new HashMap<>();
		for (int i = 0; i < code.length; i++) {
			if (code[i].length() != 0 && code[i].charAt(0) == ':') {
				labels.put(code[i].substring(1), Integer.valueOf(i));
			}
		}
		return labels;
	}

	void sendCommandResponse(String response) {
		try {
			fCommandResponseStream.write(response.getBytes());
			fCommandResponseStream.flush();
		} catch (IOException e) {
		}
	}

	void sendDebugEvent(String event, boolean error) {
		if (fDebug) {
			try {
				fEventStream.write(event.getBytes());
				fEventStream.write('\n');
				fEventStream.flush();
			} catch (IOException e) {
				System.err.println("Error: " + e); //$NON-NLS-1$
				System.exit(1);
			}
		} else if (error) {
			System.err.println("Error: " + event); //$NON-NLS-1$
		}
	}

	void startDebugger() throws IOException {
		if (fDebug) {
			System.out.println("-debug " + fCommandPort + " " + fEventPort); //$NON-NLS-1$ //$NON-NLS-2$
		}

		try (ServerSocket commandServerSocket = new ServerSocket(fCommandPort)) {
			fCommandSocket = commandServerSocket.accept();
			fCommandReceiveStream = new BufferedReader(new InputStreamReader(fCommandSocket.getInputStream()));
			fCommandResponseStream = new PrintStream(fCommandSocket.getOutputStream());
		}

		try (ServerSocket eventServerSocket = new ServerSocket(fEventPort)) {
			fEventSocket = eventServerSocket.accept();
			fEventStream = new PrintStream(fEventSocket.getOutputStream());
		}

		System.out.println("debug connection accepted"); //$NON-NLS-1$

		fSuspendVM = "client"; //$NON-NLS-1$
	}

	void run() {
		int id = fNextThreadId++;
		sendDebugEvent("vmstarted", false); //$NON-NLS-1$
		fThreads.put(Integer.valueOf(id), new PDAThread(id, "main", 0)); //$NON-NLS-1$
		if (fDebug) {
			sendDebugEvent("started " + id, false); //$NON-NLS-1$
		}

		boolean allThreadsSuspended = false;
		while (!fThreads.isEmpty()) {
			checkForBreakpoint();

			if (fSuspendVM != null) {
				debugUI();
			} else {
				yieldToDebug(allThreadsSuspended);
				if (fSuspendVM != null) {
					// Received a command to suspend VM, skip executing threads.
					continue;
				}
			}

			PDAThread[] threadsCopy = fThreads.values().toArray(new PDAThread[fThreads.size()]);
			allThreadsSuspended = true;
			for (int i = 0; i < threadsCopy.length; i++) {
				PDAThread thread = threadsCopy[i];
				if (thread.fSuspend == null) {
					allThreadsSuspended = false;

					String instruction = thread.fThreadCode[thread.fCurrentFrame.fPC];
					thread.fCurrentFrame.fPC++;
					doOneInstruction(thread, instruction);
					if (thread.fCurrentFrame.fPC >= thread.fThreadCode.length) {
						// Thread reached end of code, exit from the thread.
						thread.fRun = false;
					} else if (thread.fStepReturn) {
						// If this thread is in a step-return operation, check
						// if we've returned from a call.
						instruction = thread.fThreadCode[thread.fCurrentFrame.fPC];
						if ("return".equals(instruction)) { //$NON-NLS-1$
							// Note: this will only be triggered if the current
							// thread also has the fStepReturn flag set.
							if (fStepReturnVM) {
								fSuspendVM = thread.fID + " step"; //$NON-NLS-1$
							} else {
								thread.fSuspend = "step"; //$NON-NLS-1$
							}
						}
					}
					if (!thread.fRun) {
						sendDebugEvent("exited " + thread.fID, false); //$NON-NLS-1$
						fThreads.remove(Integer.valueOf(thread.fID));
					} else if (thread.fSuspend != null) {
						sendDebugEvent("suspended " + thread.fID + " " + thread.fSuspend, false); //$NON-NLS-1$ //$NON-NLS-2$
						thread.fStep = thread.fStepReturn = thread.fPerformingEval = false;
					}
				}
			}

			// Force thread context switch to avoid starving out other
			// processes in the system.
			Thread.yield();
		}

		sendDebugEvent("vmterminated", false); //$NON-NLS-1$
		if (fDebug) {
			try {
				fCommandReceiveStream.close();
				fCommandResponseStream.close();
				fCommandSocket.close();
				fEventStream.close();
				fEventSocket.close();
			} catch (IOException e) {
				System.out.println("Error: " + e); //$NON-NLS-1$
			}
		}

	}

	void doOneInstruction(PDAThread thread, String instr) {
		StringTokenizer tokenizer = new StringTokenizer(instr);
		String op = tokenizer.nextToken();
		List<String> tokens = new LinkedList<>();
		while (tokenizer.hasMoreTokens()) {
			tokens.add(tokenizer.nextToken());
		}
		Args args = new Args( tokens.toArray(new String[tokens.size()]) );

		boolean opValid = true;
		if (op.equals("add")) { //$NON-NLS-1$
			iAdd(thread, args);
		} else if (op.equals("branch_not_zero")) { //$NON-NLS-1$
			iBranchNotZero(thread, args);
		} else if (op.equals("call")) { //$NON-NLS-1$
			iCall(thread, args);
		} else if (op.equals("dec")) { //$NON-NLS-1$
			iDec(thread, args);
		} else if (op.equals("def")) { //$NON-NLS-1$
			iDef(thread, args);
		} else if (op.equals("dup")) { //$NON-NLS-1$
			iDup(thread, args);
		} else if (op.equals("exec")) { //$NON-NLS-1$
			iExec(thread, args);
		} else if (op.equals("halt")) { //$NON-NLS-1$
			iHalt(thread, args);
		} else if (op.equals("output")) { //$NON-NLS-1$
			iOutput(thread, args);
		} else if (op.equals("pop")) { //$NON-NLS-1$
			iPop(thread, args);
		} else if (op.equals("push")) { //$NON-NLS-1$
			iPush(thread, args);
		} else if (op.equals("return")) { //$NON-NLS-1$
			iReturn(thread, args);
		} else if (op.equals("var")) { //$NON-NLS-1$
			iVar(thread, args);
		} else if (op.equals("xyzzy")) { //$NON-NLS-1$
			iInternalEndEval(thread, args);
		} else if (op.startsWith(":")) {} // label //$NON-NLS-1$
		else if (op.startsWith("#")) {} // comment //$NON-NLS-1$
		else {
			opValid = false;
		}

		if (!opValid) {
			sendDebugEvent("unimplemented instruction " + op, true); //$NON-NLS-1$
			if ( fEventStops.get("unimpinstr").booleanValue() ) { //$NON-NLS-1$
				fSuspendVM = thread.fID + " event unimpinstr"; //$NON-NLS-1$
				thread.fCurrentFrame.fPC--;
			}
		} else if (thread.fStep) {
			if (fStepVM) {
				fSuspendVM = thread.fID + " step"; //$NON-NLS-1$
				fStepVM = false;
			} else {
				thread.fSuspend = "step"; //$NON-NLS-1$
			}
			thread.fStep = false;
		}
	}

	void checkForBreakpoint() {
		if (fDebug) {
			for (Iterator<PDAThread> itr = fThreads.values().iterator(); itr.hasNext();) {
				PDAThread thread = itr.next();
				Integer pc = Integer.valueOf(thread.fCurrentFrame.fPC);
				// Suspend for breakpoint if:
				// - the VM is not yet set to suspend, for e.g. as a result of step end,
				// - the thread is not yet suspended and is not performing an evaluation
				// - the breakpoints table contains a breakpoint for the given line.
				if (fSuspendVM == null &&
					thread.fSuspend == null && !thread.fPerformingEval &&
					fBreakpoints.containsKey(pc))
				{
					if ( fBreakpoints.get(pc).booleanValue() ) {
						fSuspendVM = thread.fID + " breakpoint " + pc; //$NON-NLS-1$
					} else {
						thread.fSuspend = "breakpoint " + pc; //$NON-NLS-1$
						thread.fStep = thread.fStepReturn = false;
						sendDebugEvent("suspended " + thread.fID + " " + thread.fSuspend, false); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
	}

	/**
	 * After each instruction, we check the debug command channel for control input. If
	 * there are commands, process them.
	 */
	void yieldToDebug(boolean allThreadsSuspended) {
		if (fDebug) {
			String line = ""; //$NON-NLS-1$
			try {
				if (allThreadsSuspended || fCommandReceiveStream.ready()) {
					line = fCommandReceiveStream.readLine();
					processDebugCommand(line);
				}
			} catch (IOException e) {
				System.err.println("Error: " + e); //$NON-NLS-1$
				System.exit(1);
			}
		}
	}

	/**
	 *  Service the debugger commands while the VM is suspended
	 */
	void debugUI() {
		if (!fStarted) {
			sendDebugEvent("vmsuspended " + fSuspendVM, false); //$NON-NLS-1$
		} else {
			fStarted = false;
		}

		// Clear all stepping flags.  In case the VM suspended while
		// a step operation was being performed for the VM or some thread.
		fStepVM = fStepReturnVM = false;
		for (Iterator<PDAThread> itr = fThreads.values().iterator(); itr.hasNext();) {
			PDAThread thread = itr.next();
			thread.fSuspend = null;
			thread.fStep = thread.fStepReturn = thread.fPerformingEval = false;
		}

		while (fSuspendVM != null) {
			String line = ""; //$NON-NLS-1$
			try {
				line = fCommandReceiveStream.readLine();
			} catch (IOException e) {
				System.err.println("Error: " + e); //$NON-NLS-1$
				System.exit(1);
				return;
			}
			processDebugCommand(line);
		}

		if (fStepVM || fStepReturnVM) {
			sendDebugEvent("vmresumed step", false); //$NON-NLS-1$
		} else {
			sendDebugEvent("vmresumed client", false); //$NON-NLS-1$
		}
	}

	void processDebugCommand(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line.trim());
		if (line.length() == 0) {
			return;
		}

		String command = tokenizer.nextToken();
		List<String> tokens = new LinkedList<>();
		while (tokenizer.hasMoreTokens()) {
			tokens.add(tokenizer.nextToken());
		}
		Args args = new Args( tokens.toArray(new String[tokens.size()]));

		if ("children".equals(command)) { //$NON-NLS-1$
			debugChildren(args);
		} else if ("clear".equals(command)) { //$NON-NLS-1$
			debugClearBreakpoint(args);
		} else if ("data".equals(command)) { //$NON-NLS-1$
			debugData(args);
		} else if ("drop".equals(command)) { //$NON-NLS-1$
			debugDropFrame(args);
		} else if ("eval".equals(command)) { //$NON-NLS-1$
			debugEval(args);
		} else if ("eventstop".equals(command)) { //$NON-NLS-1$
			debugEventStop(args);
		} else if ("frame".equals(command)) { //$NON-NLS-1$
			debugFrame(args);
		} else if ("groups".equals(command)) { //$NON-NLS-1$
			debugGroups(args);
		} else if ("popdata".equals(command)) { //$NON-NLS-1$
			debugPopData(args);
		} else if ("pushdata".equals(command)) { //$NON-NLS-1$
			debugPushData(args);
		} else if ("registers".equals(command)) { //$NON-NLS-1$
			debugRegisters(args);
		} else if ("restart".equals(command)) { //$NON-NLS-1$
			debugRestart(args);
		} else if ("resume".equals(command)) { //$NON-NLS-1$
			debugResume(args);
		} else if ("set".equals(command)) { //$NON-NLS-1$
			debugSetBreakpoint(args);
		} else if ("setdata".equals(command)) { //$NON-NLS-1$
			debugSetData(args);
		} else if ("setvar".equals(command)) { //$NON-NLS-1$
			debugSetVariable(args);
		} else if ("stack".equals(command)) { //$NON-NLS-1$
			debugStack(args);
		} else if ("stackdepth".equals(command)) { //$NON-NLS-1$
			debugStackDepth(args);
		} else if ("state".equals(command)) { //$NON-NLS-1$
			debugState(args);
		} else if ("step".equals(command)) { //$NON-NLS-1$
			debugStep(args);
		} else if ("stepreturn".equals(command)) { //$NON-NLS-1$
			debugStepReturn(args);
		} else if ("suspend".equals(command)) { //$NON-NLS-1$
			debugSuspend(args);
		} else if ("terminate".equals(command)) { //$NON-NLS-1$
			debugTerminate();
		} else if ("threads".equals(command)) { //$NON-NLS-1$
			debugThreads();
		} else if ("var".equals(command)) { //$NON-NLS-1$
			debugVar(args);
		} else if ("vmresume".equals(command)) { //$NON-NLS-1$
			debugVMResume();
		} else if ("vmsuspend".equals(command)) { //$NON-NLS-1$
			debugVMSuspend();
		} else if ("watch".equals(command)) { //$NON-NLS-1$
			debugWatch(args);
		} else {
			sendCommandResponse("error: invalid command\n"); //$NON-NLS-1$
		}
	}

	void debugChildren(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		int sfnumber = args.getNextIntArg();
		String var = args.getNextStringArg();

		Frame frame = sfnumber >= thread.fFrames.size()
			? thread.fCurrentFrame : (Frame)thread.fFrames.get(sfnumber);

		String varDot = var + "."; //$NON-NLS-1$
		List<String> children = new ArrayList<>();
		for (Iterator<String> itr = frame.fLocalVariables.keySet().iterator(); itr.hasNext();) {
			String localVar = itr.next();
			if (localVar.startsWith(varDot) && localVar.indexOf('.', varDot.length() + 1) == -1) {
				children.add(localVar);
			}
		}

		StringBuilder result = new StringBuilder();
		for (Iterator<String> itr = children.iterator(); itr.hasNext();) {
			result.append(itr.next());
			result.append('|');
		}
		result.append('\n');

		sendCommandResponse(result.toString());
	}

	void debugClearBreakpoint(Args args) {
		int line = args.getNextIntArg();

		fBreakpoints.remove( Integer.valueOf(line) );
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	private static Pattern fPackPattern = Pattern.compile("%([a-fA-F0-9][a-fA-F0-9])"); //$NON-NLS-1$

	void debugData(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		StringBuilder result = new StringBuilder();
		for (Iterator<?> itr = thread.fStack.iterator(); itr.hasNext();) {
			result.append(itr.next());
			result.append('|');
		}
		result.append('\n');
		sendCommandResponse(result.toString());
	}

	void debugDropFrame(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		if (!thread.fFrames.isEmpty()) {
			thread.fCurrentFrame = thread.fFrames.remove(thread.fFrames.size() - 1);
		}
		thread.fCurrentFrame.fPC--;
		sendCommandResponse("ok\n"); //$NON-NLS-1$
		if (fSuspendVM != null) {
			sendDebugEvent("vmresumed drop", false); //$NON-NLS-1$
			sendDebugEvent("vmsuspended " + thread.fID + " drop", false); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			sendDebugEvent("resumed " + thread.fID + " drop", false); //$NON-NLS-1$ //$NON-NLS-2$
			sendDebugEvent("suspended " + thread.fID + " drop", false); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	void debugEval(Args args) {
		if (fSuspendVM != null) {
			sendCommandResponse("error: cannot evaluate while vm is suspended\n");         //$NON-NLS-1$
			return;
		}

		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		if (thread.fSuspend == null) {
			sendCommandResponse("error: thread running\n"); //$NON-NLS-1$
			return;
		}

		StringTokenizer tokenizer = new StringTokenizer(args.getNextStringArg(), "|"); //$NON-NLS-1$
		tokenizer.countTokens();

		int numEvalLines = tokenizer.countTokens();
		thread.fThreadCode = new String[fCode.length + numEvalLines + 1];
		System.arraycopy(fCode, 0, thread.fThreadCode, 0, fCode.length);
		for (int i = 0; i < numEvalLines; i++) {
			String line = tokenizer.nextToken();
			StringBuilder lineBuf = new StringBuilder(line.length());
			Matcher matcher = fPackPattern.matcher(line);
			int lastMatchEnd = 0;
			while (matcher.find()) {
				lineBuf.append(line.substring(lastMatchEnd, matcher.start()));
				String charCode = line.substring(matcher.start() + 1, matcher.start() + 3);
				try {
					lineBuf.append((char) Integer.parseInt(charCode, 16));
				} catch (NumberFormatException e) {
				}
				lastMatchEnd = matcher.end();
			}
			if (lastMatchEnd < line.length()) {
				lineBuf.append(line.substring(lastMatchEnd));
			}
			thread.fThreadCode[fCode.length + i] = lineBuf.toString();
		}
		thread.fThreadCode[fCode.length + numEvalLines] = "xyzzy"; //$NON-NLS-1$
		thread.fThreadLabels = mapLabels(fCode);

		thread.fSavedPC = thread.fCurrentFrame.fPC;
		thread.fCurrentFrame.fPC = fCode.length;
		thread.fPerformingEval = true;

		thread.fSuspend = null;

		sendCommandResponse("ok\n"); //$NON-NLS-1$

		sendDebugEvent("resumed " + thread.fID + " eval", false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	void debugEventStop(Args args) {
		String event = args.getNextStringArg();
		int stop = args.getNextIntArg();
		fEventStops.put(event, Boolean.valueOf(stop > 0));
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugTerminate() {
		sendCommandResponse("ok\n"); //$NON-NLS-1$
		sendDebugEvent("vmterminated", false); //$NON-NLS-1$
		System.exit(0);
	}

	void debugFrame(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		int sfnumber = args.getNextIntArg();
		Frame frame = null;
		if (sfnumber >= thread.fFrames.size()) {
			frame = thread.fCurrentFrame;
		} else {
			frame = thread.fFrames.get(sfnumber);
		}
		sendCommandResponse(printFrame(frame) + "\n"); //$NON-NLS-1$
	}

	/**
	 * @param args
	 */
	void debugGroups(Args args) {
		TreeSet<String> groups = new TreeSet<>();
		for (Iterator<Register> itr = fRegisters.values().iterator(); itr.hasNext();) {
			Register reg = itr.next();
			groups.add(reg.fGroup);
		}
		StringBuilder response = new StringBuilder();
		for (Iterator<String> itr = groups.iterator(); itr.hasNext();) {
			response.append(itr.next());
			response.append('|');
		}
		response.append('\n');
		sendCommandResponse(response.toString());
	}

	void debugPopData(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		thread.fStack.pop();
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugPushData(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		Object val = args.getNextIntOrStringArg();
		thread.fStack.push(val);
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugRegisters(Args args) {
		String group = args.getNextStringArg();

		StringBuilder response = new StringBuilder();
		for (Iterator<Register> itr = fRegisters.values().iterator(); itr.hasNext();) {
			Register reg = itr.next();
			if (group.equals(reg.fGroup)) {
				response.append(reg.fName);
				response.append(' ');
				response.append(reg.fIsWriteable);
				for (Iterator<BitField> itr2 = reg.fBitFields.values().iterator(); itr2.hasNext();) {
					BitField bitField = itr2.next();
					response.append('|');
					response.append(bitField.fName);
					response.append(' ');
					response.append(bitField.fBitOffset);
					response.append(' ');
					response.append(bitField.fBitCount);
					response.append(' ');
					for (Iterator<Entry<String, Integer>> itr3 = bitField.fMnemonics.entrySet().iterator(); itr3.hasNext();) {
						Entry<String, Integer> mnemonicEntry = itr3.next();
						response.append(mnemonicEntry.getKey());
						response.append(' ');
						response.append(mnemonicEntry.getValue());
						response.append(' ');
					}
				}

				response.append('#');
			}
		}
		response.append('\n');
		sendCommandResponse(response.toString());
	}

	/**
	 * @param args
	 */
	void debugRestart(Args args) {
		fSuspendVM = "restart"; //$NON-NLS-1$

		for (Iterator<Integer> itr = fThreads.keySet().iterator(); itr.hasNext();) {
			Integer id = itr.next();
			sendDebugEvent("exited " + id, false);             //$NON-NLS-1$
		}
		fThreads.clear();

		int id = fNextThreadId++;
		fThreads.put(Integer.valueOf(id), new PDAThread(id, "main", 0)); //$NON-NLS-1$
		sendDebugEvent("started " + id, false);             //$NON-NLS-1$

		fRegisters.clear();

		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugResume(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}
		if (fSuspendVM != null) {
			sendCommandResponse("error: cannot resume thread when vm is suspended\n"); //$NON-NLS-1$
			return;
		}
		if (thread.fSuspend == null) {
			sendCommandResponse("error: thread already running\n"); //$NON-NLS-1$
			return;
		}

		thread.fSuspend = null;
		sendDebugEvent("resumed " + thread.fID + " client", false); //$NON-NLS-1$ //$NON-NLS-2$

		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugSetBreakpoint(Args args) {
		int line = args.getNextIntArg();
		int stopVM = args.getNextIntArg();

		fBreakpoints.put(Integer.valueOf(line), Boolean.valueOf(stopVM != 0));
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugSetData(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		int offset = args.getNextIntArg();
		Object val = args.getNextIntOrStringArg();

		if (offset < thread.fStack.size()) {
			thread.fStack.set(offset, val);
		} else {
			thread.fStack.add(0, val);
		}
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugSetVariable(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		int sfnumber = args.getNextIntArg();
		String var = args.getNextStringArg();
		Object val = args.getNextIntOrStringArg();
		while (args.hasNextArg()) {
			val = val + " " + args.getNextStringArg(); //$NON-NLS-1$
		}

		if (sfnumber >= thread.fFrames.size()) {
			thread.fCurrentFrame.set(var, val);
		} else {
			thread.fFrames.get(sfnumber).set(var, val);
		}
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugStack(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		StringBuilder result = new StringBuilder();

		for (Iterator<Frame> itr = thread.fFrames.iterator(); itr.hasNext();) {
			Frame frame = itr.next();
			result.append(printFrame(frame));
			result.append('#');
		}
		result.append(printFrame(thread.fCurrentFrame));
		result.append('\n');
		sendCommandResponse(result.toString());
	}

	void debugStackDepth(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}
		sendCommandResponse( (thread.fFrames.size() + 1) + "\n" ); //$NON-NLS-1$
	}


	/**
	 * The stack frame output is: frame # frame # frame ... where each frame is:
	 * filename | line number | function name | var | var | var | var ...
	 */
	private String printFrame(Frame frame) {
		StringBuilder buf = new StringBuilder();
		buf.append(fFilename);
		buf.append('|');
		buf.append(frame.fPC);
		buf.append('|');
		buf.append(frame.fFunction);
		for (Iterator<String> itr = frame.fLocalVariables.keySet().iterator(); itr.hasNext();) {
			String var = itr.next();
			if (var.indexOf('.') == -1) {
				buf.append('|');
				buf.append(var);
			}
		}
		return buf.toString();
	}

	void debugState(Args args) {
		PDAThread thread = args.getThreadArg();
		String response = null;
		if (thread == null) {
			response = fSuspendVM == null ? "running" : fSuspendVM; //$NON-NLS-1$
		} else if (fSuspendVM != null) {
			response = "vm"; //$NON-NLS-1$
		} else {
			response = thread.fSuspend == null ? "running" : thread.fSuspend; //$NON-NLS-1$
		}
		sendCommandResponse(response + "\n"); //$NON-NLS-1$
	}

	void debugStep(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		// Set suspend to null to allow the debug loop to exit back to the
		// instruction loop and thus run an instruction. However, we want to
		// come back to the debug loop right away, so the step flag is set to
		// true which will cause the suspend flag to get set to true when we
		// get to the next instruction.
		if (fSuspendVM != null) {
			// All threads are suspended, so suspend all threads again when
			// step completes.
			fSuspendVM = null;
			fStepVM = true;
			// Also mark the thread that initiated the step to mark it as
			// the triggering thread when suspending.
			thread.fStep = true;
		} else {
			if (thread.fSuspend == null) {
				sendCommandResponse("error: thread already running\n"); //$NON-NLS-1$
				return;
			}
			thread.fSuspend = null;
			thread.fStep = true;
			sendDebugEvent("resumed " + thread.fID + " step", false); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugStepReturn(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		if (fSuspendVM != null) {
			fSuspendVM = null;
			fStepReturnVM = true;
			thread.fStepReturn = true;
		} else {
			if (thread.fSuspend == null) {
				sendCommandResponse("error: thread running\n"); //$NON-NLS-1$
				return;
			}
			thread.fSuspend = null;
			thread.fStepReturn = true;
			sendDebugEvent("resumed " + thread.fID + " step", false); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugSuspend(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}
		if (fSuspendVM != null) {
			sendCommandResponse("error: vm already suspended\n"); //$NON-NLS-1$
			return;
		}
		if (thread.fSuspend != null) {
			sendCommandResponse("error: thread already suspended\n"); //$NON-NLS-1$
			return;
		}

		thread.fSuspend = "client"; //$NON-NLS-1$
		sendDebugEvent("suspended " + thread.fID + " client", false); //$NON-NLS-1$ //$NON-NLS-2$
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugThreads() {
		StringBuilder response = new StringBuilder();
		for (Iterator<Integer> itr = fThreads.keySet().iterator(); itr.hasNext();) {
			response.append(itr.next());
			response.append(' ');
		}
		sendCommandResponse(response.toString().trim() + "\n"); //$NON-NLS-1$
	}

	void debugVar(Args args) {
		PDAThread thread = args.getThreadArg();
		if (thread == null) {
			sendCommandResponse("error: invalid thread\n"); //$NON-NLS-1$
			return;
		}

		int sfnumber = args.getNextIntArg();
		String var = args.getNextStringArg();

		Frame frame = sfnumber >= thread.fFrames.size()
			? thread.fCurrentFrame : (Frame)thread.fFrames.get(sfnumber);

		Object val = frame.get(var);
		if (val == null) {
			sendCommandResponse("error: variable undefined\n"); //$NON-NLS-1$
		} else {
			sendCommandResponse(val + "\n"); //$NON-NLS-1$
		}
	}

	void debugVMResume() {
		if (fSuspendVM == null) {
			sendCommandResponse("error: vm already running\n"); //$NON-NLS-1$
			return;
		}

		fSuspendVM = null;
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugVMSuspend() {
		if (fSuspendVM != null) {
			sendCommandResponse("error: vm already suspended\n"); //$NON-NLS-1$
			return;
		}

		fSuspendVM = "client"; //$NON-NLS-1$
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	void debugWatch(Args args) {
		String funcAndVar = args.getNextStringArg();
		int flags = args.getNextIntArg();
		fWatchpoints.put(funcAndVar, Integer.valueOf(flags));
		sendCommandResponse("ok\n"); //$NON-NLS-1$
	}

	/**
	 * @param thread
	 * @param args
	 */
	void iAdd(PDAThread thread, Args args) {
		Object val1 = thread.fStack.pop();
		Object val2 = thread.fStack.pop();
		if (val1 instanceof Integer && val2 instanceof Integer) {
			int intVal1 = ((Integer) val1).intValue();
			int intVal2 = ((Integer) val2).intValue();
			thread.fStack.push( Integer.valueOf(intVal1 + intVal2) );
		} else {
			thread.fStack.push( Integer.valueOf(-1) );
		}
	}

	void iBranchNotZero(PDAThread thread, Args args) {
		Object val = thread.fStack.pop();
		if (val instanceof Integer && ((Integer) val).intValue() != 0) {
			String label = args.getNextStringArg();
			if (thread.fThreadLabels.containsKey(label)) {
				thread.fCurrentFrame.fPC = thread.fThreadLabels.get(label).intValue();
			} else {
				sendDebugEvent("no such label " + label, true); //$NON-NLS-1$
				if ( fEventStops.get("nosuchlabel").booleanValue() ) { //$NON-NLS-1$
					fSuspendVM = thread.fID + " event nosuchlabel"; //$NON-NLS-1$
					thread.fStack.push(val);
					thread.fCurrentFrame.fPC--;
				}
			}
		}
	}

	void iCall(PDAThread thread, Args args) {
		String label = args.getNextStringArg();
		if (thread.fThreadLabels.containsKey(label)) {
			thread.fFrames.add(thread.fCurrentFrame);
			thread.fCurrentFrame = new Frame(label, thread.fThreadLabels.get(label).intValue());
		} else {
			sendDebugEvent("no such label " + label, true); //$NON-NLS-1$
			if ( fEventStops.get("nosuchlabel").booleanValue() ) { //$NON-NLS-1$
				fSuspendVM = thread.fID + " event nosuchlabel"; //$NON-NLS-1$
				thread.fCurrentFrame.fPC--;
			}
		}
	}

	/**
	 * @param thread
	 * @param args
	 */
	void iDec(PDAThread thread, Args args) {
		Object val = thread.fStack.pop();
		if (val instanceof Integer) {
			val = Integer.valueOf(((Integer) val).intValue() - 1);
		}
		thread.fStack.push(val);
	}

	/**
	 * @param thread
	 * @param args
	 */
	void iDef(PDAThread thread, Args args) {
		String type = args.getNextStringArg();

		String name = args.getNextStringArg();
		String regName = getRegisterPartOfName(name);
		String bitFieldName = getBitFieldPartOfName(name);

		if ("register".equals(type)) { //$NON-NLS-1$
			Register reg = new Register(regName);
			reg.fGroup = args.getNextStringArg();
			fRegisters.put(regName, reg);
			reg.fIsWriteable = args.getNextBooleanArg();
		} else if ("bitfield".equals(type)) { //$NON-NLS-1$
			Register reg = fRegisters.get(regName);
			if (reg == null) {
				return;
			}
			BitField bitField = new BitField(bitFieldName);
			bitField.fBitOffset = args.getNextIntArg();
			bitField.fBitCount = args.getNextIntArg();
			reg.fBitFields.put(bitFieldName, bitField);
		} else if ("mnemonic".equals(type)) { //$NON-NLS-1$
			Register reg = fRegisters.get(regName);
			if (reg == null) {
				return;
			}
			BitField bitField = reg.fBitFields.get(bitFieldName);
			if (bitField == null) {
				return;
			}
			bitField.fMnemonics.put(args.getNextStringArg(), Integer.valueOf(args.getNextIntArg()));
		}
		sendDebugEvent("registers", false); //$NON-NLS-1$
	}

	private String getRegisterPartOfName(String name) {
		if (name.startsWith("$")) { //$NON-NLS-1$
			int end = name.indexOf('.');
			end = end != -1 ? end : name.length();
			return name.substring(1, end);
		}
		return null;
	}

	private String getBitFieldPartOfName(String name) {
		int start = name.indexOf('.');
		if (name.startsWith("$") && start != -1) { //$NON-NLS-1$
			return name.substring(start + 1, name.length());
		}
		return null;
	}

	/**
	 * @param thread
	 * @param args
	 */
	void iDup(PDAThread thread, Args args) {
		Object val = thread.fStack.pop();
		thread.fStack.push(val);
		thread.fStack.push(val);
	}

	void iExec(PDAThread thread, Args args) {
		String label = args.getNextStringArg();
		if (fLabels.containsKey(label)) {
			int id = fNextThreadId++;
			fThreads.put( Integer.valueOf(id), new PDAThread(id, label, fLabels.get(label).intValue()) );
			sendDebugEvent("started " + id, false); //$NON-NLS-1$
		} else {
			sendDebugEvent("no such label " + label, true); //$NON-NLS-1$
			if ( fEventStops.get("nosuchlabel").booleanValue() ) { //$NON-NLS-1$
				thread.fSuspend = "event nosuchlabel"; //$NON-NLS-1$
				thread.fCurrentFrame.fPC--;
			}
		}
	}

	/**
	 * @param thread
	 * @param args
	 */
	void iHalt(PDAThread thread, Args args) {
		thread.fRun = false;
	}

	/**
	 * @param thread
	 * @param args
	 */
	void iOutput(PDAThread thread, Args args) {
		System.out.println(thread.fStack.pop());
	}

	void iPop(PDAThread thread, Args args) {
		String arg = args.getNextStringArg();
		if (arg.startsWith("$")) { //$NON-NLS-1$
			String var = arg.substring(1);
			thread.fCurrentFrame.set(var, thread.fStack.pop());
			String key = thread.fCurrentFrame.fFunction + "::" + var; //$NON-NLS-1$
			if ( fWatchpoints.containsKey(key) && (fWatchpoints.get(key).intValue() & 2) != 0 ) {
				fSuspendVM = thread.fID + " watch write " + key; //$NON-NLS-1$
			}
		} else {
			thread.fStack.pop();
		}
	}

	void iPush(PDAThread thread, Args args) {
		String arg = args.getNextStringArg();
		while (arg.length() != 0) {
			if (arg.startsWith("$")) { //$NON-NLS-1$
				String var = arg.substring(1);
				Object val = thread.fCurrentFrame.get(var);
				if (val == null)
				 {
					val = "<undefined>"; //$NON-NLS-1$
				}
				thread.fStack.push(val);
				String key = thread.fCurrentFrame.fFunction + "::" + var; //$NON-NLS-1$
				if (fWatchpoints.containsKey(key) && (fWatchpoints.get(key).intValue() & 1) != 0) {
					fSuspendVM = thread.fID + " watch read " + key; //$NON-NLS-1$
				}
			} else {
				Object val = arg;
				if (args.hasNextArg()) {
					while (args.hasNextArg()) {
						val = val + " " + args.getNextStringArg(); //$NON-NLS-1$
					}
				} else {
					try {
						val = Integer.valueOf(arg);
					} catch (NumberFormatException e) {
					}
				}
				thread.fStack.push(val);
			}

			arg = args.getNextStringArg();
		}
	}

	/**
	 * @param thread
	 * @param args
	 */
	void iReturn(PDAThread thread, Args args) {
		if (!thread.fFrames.isEmpty()) {
			thread.fCurrentFrame = thread.fFrames.remove(thread.fFrames.size() - 1);
		} else {
			// Execution returned from the top frame, which means this thread
			// should exit.
			thread.fRun = false;
		}
	}

	void iVar(PDAThread thread, Args args) {
		String var = args.getNextStringArg();
		thread.fCurrentFrame.set(var, Integer.valueOf(0));
	}

	/**
	 * @param thread
	 * @param args
	 */
	void iInternalEndEval(PDAThread thread, Args args) {
		Object result = thread.fStack.pop();
		thread.fThreadCode = fCode;
		thread.fThreadLabels = fLabels;
		thread.fCurrentFrame.fPC = thread.fSavedPC;
		sendDebugEvent("evalresult " + result, false); //$NON-NLS-1$
		thread.fSuspend = "eval"; //$NON-NLS-1$
		thread.fPerformingEval = false;
	}

}
