package org.eclipse.update.internal.ui.win32.views;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.ole.win32.*;
import org.eclipse.swt.widgets.*;

/**
 * ActiveX based web browser control.
 */
public class WebBrowser implements OleListener {
	// Generated from typelib filename: shdocvw.dll

	// Constants for WebBrowser CommandStateChange

	public static final short CSC_UPDATECOMMANDS = -1;
	public static final short CSC_NAVIGATEFORWARD = 1;
	public static final short CSC_NAVIGATEBACK = 2;

	// Web Browser Control Events 
	public static final int BeforeNavigate = 100;
	// Fired when a new hyperlink is being navigated to.
	public static final int NavigateComplete = 101;
	// Fired when the document being navigated to becomes visible and enters the navigation stack.
	public static final int StatusTextChange = 102;
	// Statusbar text changed.
	public static final int ProgressChange = 108;
	// Fired when download progress is updated.
	public static final int DownloadComplete = 104;
	// Download of page complete.
	public static final int CommandStateChange = 105;
	// The enabled state of a command changed
	public static final int DownloadBegin = 106;
	// Download of a page started.
	public static final int NewWindow = 107;
	// Fired when a new window should be created.
	public static final int TitleChange = 113;
	// Document title changed.
	public static final int FrameBeforeNavigate = 200;
	// Fired when a new hyperlink is being navigated to in a frame.
	public static final int FrameNavigateComplete = 201;
	// Fired when a new hyperlink is being navigated to in a frame.
	public static final int FrameNewWindow = 204;
	// Fired when a new window should be created.
	public static final int Quit = 103;
	// Fired when application is quiting.
	public static final int WindowMove = 109;
	// Fired when window has been moved.
	public static final int WindowResize = 110;
	// Fired when window has been sized.
	public static final int WindowActivate = 111;
	// Fired when window has been activated.
	public static final int PropertyChange = 112;
	// Fired when the PutProperty method has been called.
	public static final int BeforeNavigate2 = 250;
	// Fired when a new hyperlink is being navigated to.
	public static final int NewWindow2 = 251;
	// Fired when a new window should be created.
	public static final int DocumentComplete = 259;
	// Fired when the document being navigated to reaches ReadyState_Complete.

	// Web Browser properties
	public static final int DISPID_READYSTATE = -525;

	// Web Browser state
	public static final int READYSTATE_UNINITIALIZED = 0;
	public static final int READYSTATE_LOADING = 1;
	public static final int READYSTATE_LOADED = 2;
	public static final int READYSTATE_INTERACTIVE = 3;
	public static final int READYSTATE_COMPLETE = 4;

	// Keep track of the whether it is possible to navigate in the forward and backward directions
	private boolean backwardEnabled;
	private boolean forwardEnabled;

	// The automation object and Control associated with the main OLE control
	private OleAutomation oleObject;
	private BrowserControlSite controlSite;

	// The OLE frame (there should only be one)
	private OleFrame controlFrame;

	//private NestedPrintDelegate aPrintDelegate = null;

	/**
	 */
	public WebBrowser(Composite parent) {

		// Create the OLE frame. 
		controlFrame = createOleFrame(parent);

		// Creates the IE5 OLE Control
		// The constructor also registers all the necessary OLE listeners.
		// for now, only catch the execption if creating and activating the 
		// control fails. No checking if the correct version of the OLE control 
		// is installed.
		try {
			controlSite =
				new BrowserControlSite(
					controlFrame,
					SWT.NONE,
					"Shell.Explorer");
			controlSite.setBrowser(this);
			oleObject = new OleAutomation(controlSite);

			backwardEnabled = false;
			forwardEnabled = false;

			// Listen for changes to the Command States
			controlSite.addEventListener(CommandStateChange, this);

			// initialize control
			controlSite.doVerb(OLE.OLEIVERB_SHOW);

			// create print Delegate (has to be done early, here!).
			//aPrintDelegate = new NestedPrintDelegate(this, oleObject, controlSite);

		} catch (Exception e) {
			String id = "org.eclipse.update.ui.win32";
			IStatus status =
				new Status(IStatus.ERROR, id, IStatus.OK, e.getMessage(), e);
			Platform.getPlugin("org.eclipse.core.runtime").getLog().log(status);
		}
	}
	/**
	 */
	public int back() {

		if (!backwardEnabled)
			return OLE.S_FALSE;
		forwardEnabled = true;

		// dispid=100, type=METHOD, name="GoBack"

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = 100;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid = oleObject.getIDsOfNames(new String[] { "GoBack" });
		int dispIdMember = rgdispid[0];

		Variant pVarResult = oleObject.invoke(dispIdMember);

		if (pVarResult == null)
			return 0;
		return pVarResult.getInt();
	}
	/**
	 */
	public int copy() {
		int result = controlSite.queryStatus(OLE.OLECMDID_COPY);
		if ((result & OLE.OLECMDF_ENABLED) == OLE.OLECMDF_ENABLED) {
			result =
				controlSite.exec(
					OLE.OLECMDID_COPY,
					OLE.OLECMDEXECOPT_DODEFAULT,
					null,
					null);
		}
		return result;
	}
	protected OleFrame createOleFrame(Composite parent) {
		if (controlFrame == null) {
			controlFrame = new OleFrame(parent, SWT.NONE);
			controlFrame.setLayoutData(
				new GridData(
					GridData.GRAB_HORIZONTAL
						| GridData.GRAB_VERTICAL
						| GridData.HORIZONTAL_ALIGN_FILL
						| GridData.VERTICAL_ALIGN_FILL));
		}
		return controlFrame;

	}
	/**
	 * clean up
	 */
	public void dispose() {
		//** clean up
		if (oleObject != null)
			oleObject.dispose();
		oleObject = null;

		if (controlSite != null)
			controlSite.dispose();
		controlSite = null;

		if (controlFrame != null)
			controlFrame.dispose();
		controlFrame = null;

	}
	/**
	 */
	public int forward() {
		if (!forwardEnabled)
			return OLE.S_FALSE;
		backwardEnabled = true;
		// dispid=101, type=METHOD, name="GoForward"

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = 101;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid = oleObject.getIDsOfNames(new String[] { "GoForward" });
		int dispIdMember = rgdispid[0];

		Variant pVarResult = oleObject.invoke(dispIdMember);
		if (pVarResult == null)
			return 0;
		return pVarResult.getInt();
	}
	public Control getControl() {
		return controlFrame;
	}
	/**
	 */
	public String getLocationName() {
		// dispid=210, type=PROPGET, name="LocationName"

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = 210;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid =
			oleObject.getIDsOfNames(new String[] { "LocationName" });
		int dispIdMember = rgdispid[0];
		Variant pVarResult = oleObject.getProperty(dispIdMember);
		if (pVarResult == null)
			return null;
		return pVarResult.getString();
	}
	/**
	 */
	public String getLocationURL() {
		// dispid=211, type=PROPGET, name="LocationURL"
		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = 211;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid =
			oleObject.getIDsOfNames(new String[] { "LocationURL" });
		int dispIdMember = rgdispid[0];

		Variant pVarResult = oleObject.getProperty(dispIdMember);
		if (pVarResult == null)
			return null;
		return pVarResult.getString();
	}
	protected OleFrame getOleFrame() {
		return controlFrame;

	}

	public BrowserControlSite getControlSite() {
		return controlSite;
	}
	/**
	 */
	public int getReadyState() {
		// dispid=4294966771, type=PROPGET, name="ReadyState"
		// READYSTATE_UNINITIALIZED = 0;
		// READYSTATE_LOADING = 1;
		// READYSTATE_LOADED = 2;
		// READYSTATE_INTERACTIVE = 3;
		// READYSTATE_COMPLETE = 4;

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = -525;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid = oleObject.getIDsOfNames(new String[] { "ReadyState" });
		int dispIdMember = rgdispid[0];

		Variant pVarResult = oleObject.getProperty(dispIdMember);
		if (pVarResult == null)
			return -1;
		return pVarResult.getInt();
	}
	/**
	 */
	public void handleEvent(OleEvent event) {
		switch (event.type) {
			case (CommandStateChange) :
				int command = 0;
				boolean enabled = false;

				Variant varResult = event.arguments[0];
				if (varResult != null) {
					command = varResult.getInt();
				}

				varResult = event.arguments[1];
				if (varResult != null) {
					enabled = varResult.getBoolean();
				}

				if (command == CSC_NAVIGATEBACK)
					backwardEnabled = enabled;
				if (command == CSC_NAVIGATEFORWARD)
					forwardEnabled = enabled;

				return;

			case (DocumentComplete) :
				varResult = event.arguments[0];
				return;
		}
		//throw new OleError(OLE.ERROR_NOT_IMPLEMENTED);
	}
	/**
	 */
	public int home() {
		// dispid=102, type=METHOD, name="GoHome"

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = 102;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid = oleObject.getIDsOfNames(new String[] { "GoHome" });
		int dispIdMember = rgdispid[0];

		Variant pVarResult = oleObject.invoke(dispIdMember);
		if (pVarResult == null)
			return 0;
		return pVarResult.getInt();
	}
	/**
	 */
	public int navigate(String url) {
		return navigate(oleObject, url);
	}
	/**
	 */
	protected int navigate(OleAutomation aOleAutomation, String url) {
		//if (Logger.DEBUG)
		//Logger.logDebugMessage("WebBrowser", "navigate to: " + url);
		// dispid=104, type=METHOD, name="Navigate"

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = 104;

		// Alternatively, you can look up the DISPID dynamically
		// Here we are looking up the id of the argument as well
		int[] rgdispid =
			aOleAutomation.getIDsOfNames(new String[] { "Navigate", "URL" });
		int dispIdMember = rgdispid[0];

		Variant[] rgvarg = new Variant[1];
		rgvarg[0] = new Variant(url);
		int[] rgdispidNamedArgs = new int[1];
		rgdispidNamedArgs[0] = rgdispid[1]; // identifier of argument
		Variant pVarResult =
			aOleAutomation.invoke(dispIdMember, rgvarg, rgdispidNamedArgs);
		if (pVarResult == null || pVarResult.getType() == OLE.VT_EMPTY)
			return 0;
		return pVarResult.getInt();

	}
	/**
	 */
	public int print() {
		return print(controlSite, true);
	}
	/**
	 */
	protected int print(BrowserControlSite aControlSite, boolean promptuser) {

		int result = aControlSite.queryStatus(OLE.OLECMDID_PRINT);

		if ((result & OLE.OLECMDF_ENABLED) == OLE.OLECMDF_ENABLED) {
			if (promptuser)
				result =
					aControlSite.exec(
						OLE.OLECMDID_PRINT,
						OLE.OLECMDEXECOPT_PROMPTUSER,
						null,
						null);
			else
				result =
					aControlSite.exec(
						OLE.OLECMDID_PRINT,
						OLE.OLECMDEXECOPT_DONTPROMPTUSER,
						null,
						null);
		}

		//if (Logger.DEBUG)
		//Logger.logDebugMessage("WebBrowser", "exec returns: " + Integer.toString(result));
		return result;
	}

	/**
	 * Refresh the currently viewed page.
	 */
	public void refresh() {

		// dispid= 4294966746, type=METHOD, name="Refresh"

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember =  4294966746;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid = oleObject.getIDsOfNames(new String[] { "Refresh" });
		int dispIdMember = rgdispid[0];

		oleObject.invokeNoReply(dispIdMember);
	}
	/**
	 */
	public int search() {
		// dispid=103, type=METHOD, name="GoSearch"

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = 103;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid = oleObject.getIDsOfNames(new String[] { "GoSearch" });
		int dispIdMember = rgdispid[0];

		Variant pVarResult = oleObject.invoke(dispIdMember);
		if (pVarResult == null)
			return 0;
		return pVarResult.getInt();
	}
	/**
	 */
	public void stop() {
		// dispid=106, type=METHOD, name="Stop"

		// You can hard code the DISPID if you know it before hand - this is of course the fastest way
		//int dispIdMember = 106;

		// Alternatively, you can look up the DISPID dynamically
		int[] rgdispid = oleObject.getIDsOfNames(new String[] { "Stop" });
		int dispIdMember = rgdispid[0];

		Variant pVarResult = oleObject.invoke(dispIdMember);
	}
	/**
	 * Gets the forwardEnabled
	 * @return Returns a boolean
	 */
	public boolean isForwardEnabled() {
		return forwardEnabled;
	}

	/**
	 * Gets the backwardEnabled
	 * @return Returns a boolean
	 */
	public boolean isBackwardEnabled() {
		return backwardEnabled;
	}
}