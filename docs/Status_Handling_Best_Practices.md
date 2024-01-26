

Status Handling Best Practices
==============================

Contents
--------

*   [1 Introduction](#Introduction)
*   [2 Messages](#Messages)
    *   [2.1 Message content](#Message-content)
    *   [2.2 Provide additional information in your message](#Provide-additional-information-in-your-message)
    *   [2.3 Attempt to use a unique identifier](#Attempt-to-use-a-unique-identifier)
    *   [2.4 Developing messages in eclipse: using the NLS.bind method](#Developing-messages-in-eclipse:-using-the-NLS.bind-method)
*   [3 About logging and Error Dialog](#About-logging-and-Error-Dialog)
    *   [3.1 The concept of a LogRecord](#The-concept-of-a-LogRecord)
    *   [3.2 Best practices for logging](#Best-practices-for-logging)
*   [4 The Eclipse IStatus](#The-Eclipse-IStatus)
    *   [4.1 Calling the Status API](#Calling-the-Status-API)
    *   [4.2 Implementing your own Status](#Implementing-your-own-Status)
*   [5 The Eclipse 3.3 Status handler framework](#The-Eclipse-3.3-Status-handler-framework)
*   [6 Using the new Eclipse status handler API](#Using-the-new-Eclipse-status-handler-API)
    *   [6.1 Calling the StatusManager handle method](#Calling-the-StatusManager-handle-method)
*   [7 Developing a StatusHandler](#Developing-a-StatusHandler)
    *   [7.1 Implementing the handle method](#Implementing-the-handle-method)
*   [8 Developing an ErrorSupportProvider](#Developing-an-ErrorSupportProvider)
    *   [8.1 Registering your ErrorSupportProvider](#Registering-your-ErrorSupportProvider)
    *   [8.2 Implementing the createSupportArea method](#Implementing-the-createSupportArea-method)
    *   [8.3 The flow](#The-flow)

Introduction
============

In software engineering and hardware engineering, serviceability is also known as supportability, and is one of the -ilities or aspects. It refers to the ability of technical support personnel to debug or perform root cause analysis in pursuit of solving a problem with a product. \[ [http://en.wikipedia.org/wiki/Serviceability](http://en.wikipedia.org/wiki/Serviceability)\]

One of the feature of Serviceability includes the logging of state and the notification of the user.

Eclipse already provides different framework to manage the Log file as well as Dialog to notify the user. In Eclipse 3.3 we introduce a framework that will make the logging and Dialog consistent across the Eclipse platform. This new framework also allows provider to plug-in their diagnosis tools, providing extra value to the user.

This paper will explain the best practices a developer should follow to exploit this new framework appropriately using the IStatus class. The second part will explain to plug-in provider how to exploit the new ‘StatusHandler’ model, allowing them to contribute to the user interface as well as managing the IStatus we are about to show or log.

Before we investigate IStatus, we need to agree on a couple basic principle about logging and error message rendering.

Eclipse UI best practices about Common Error Message \[ [http://wiki.eclipse.org/index.php/UI\_Best\_Practices\_v3.x#Common\_Error_Messages](http://wiki.eclipse.org/index.php/UI_Best_Practices_v3.x#Common_Error_Messages)\]

Messages
========

A message is the principal information the user will see in the log or in the User Interface. A message that is logged is supposed to be user consumable and thus follow the same readability and globalization rules as a String rendered in a menu.

  

Message content
---------------

There are tons of information on how to create a usable message. We rely on your Software Engineering experience to avoid messages like: ‘internal error, please see log’.

  

Provide additional information in your message
----------------------------------------------

We recommend you provide two other pieces of information when you create a message.

1.  An explanation of the message. This is a String that will provide more information to the user than a one line message. The explanation should not be too technical, yet provide more value than the message text itself. Once again, information is available on the web.
2.  A recommendation of the message. This is a String that will tell the user what he/she should do. When you write this part, put yourself in the shoes of the user and answer the following question: “ok, now what do I do ?”

  

Attempt to use a unique identifier
----------------------------------

Unique identifier could be an int or a String that uniquely tag the message. There are two main reasons to add a unique identifier to your message.

1.  It is easier to search a knowledge base with a unique int than the full text of the message.
2.  Because messages are translated, a user in a foreign country may send your support team a translated message. If you do not have a unique identifier, it will be difficult for your team to translate it back into the original language.

  

Developing messages in eclipse: using the NLS.bind method
---------------------------------------------------------

Eclipse provides a great mechanism to manage your messages from within your Java code. Look at the NLS class

Developing Message Bundles in Eclipse \[ [http://help.eclipse.org/help32/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/message_bundles.html](http://help.eclipse.org/help32/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/message_bundles.html)\]

Internationalization best practices \[ [http://www-128.ibm.com/developerworks/opensource/library/os-i18n/](http://www-128.ibm.com/developerworks/opensource/library/os-i18n/)\] \[ [http://www.icu-project.org/](http://www.icu-project.org/)\]

About logging and Error Dialog
==============================

The concept of a LogRecord
--------------------------

While a message is important, we need to realize it can occurs in many situation. I do not mean by that you should reuse a message in different places in your code..(it is a big no-no). I mean this message will occur on a certain machine at a certain time. This is what we call the ‘metadata’ around the message. This encompasses things like the timestamp, the thread identifier, the ip of the machine etc etc…

Example of such record are found in java.logging.logging.LogRecord from the JSR specification. Other logger like Apache Log4J currently use an Object as a record.

Eclipse does not provide the concept of a record in itself yet. So we usually end up writing our own method like debug(String) or warn(String) in our plug-ins. The equinox team is working on proving a full fledge logging framework, including the concept of a LogRecord in future releases.

_NotaBene: The previous sentence is pure speculation as the team is currently investigating…_

Best practices for logging
--------------------------

There is a lot of debate about what to log and what not to log.

1.  If you log too little, it will be difficult to troubleshoot a problem. In some cases we will have to reproduce the problem in debug mode (allowing trace to be persisted). In the worse case scenario, if you cannot reproduce the issue, you will have to ship an instrumented version of your code to the client.
2.  If you log too much, a couple things can happen
    1.  You could fill the hard drive with information and slow the process of your application
    2.  You will scare the product administrator who will see tons of 'possible' errors fill the log.

There are couple best practices I gathered during my years of Support Engineer.

**Rule #1:** if you show it, log it The rationale is that if you do not log it, we have no persistence of the issue. If the user closes the Dialog and closes support, we will have to reproduce the problem. Of course, in the case of a client-server environment, you do not have to log it in both places. If the message comes from the server, the client part can \*just\* present the message that will contain information about the real log record in the server log, so the administrator can retrieve it. Still I would recommend you save the message only (not the whole record) in the client environment, in case the clients forgets the correlator identifier to the server log. Another possibility is to log the message at a DEBUG or TRACE level. Remember to be cautious or you will end up in Scenario #1 : Logging too little.

**Rule #2:** Log even expected exception Some exceptions are expected in your environment. For instance, if you connect to a database using JDBC, you may encounter the java.sql.SQLException. It may be an exception you can recover from (i.e. StaleData) or one you cannot recover from (Server is down). You should not log the StaleData exception or the log will contain too much information. The user/administrator is not interested about your internal code and how you recover. Yet you should log the unexpected exception. We recommend you log the message of an expected exception only, not the whole stackTrace, but at a WARNING or DEBUG level.

Other technique exists that keep a statistic view of your expected errors. This allow an administrator to realize, for instance, that the application is getting a lot of StaleException between 8pm and 9pm on Friday.

**Rule #3: ZZZZZZZZZZZZZZZZZZZ I DO NOT HAVE A RULE #3 :( ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ**

There is tons of information and documentation on the web. On interesting paper about logging exception is the following: \[ [http://dev2dev.bea.com/pub/a/2006/11/effective-exceptions.html](http://dev2dev.bea.com/pub/a/2006/11/effective-exceptions.html)\]

The Eclipse IStatus
===================

As we saw, Eclipse does not have the concept of a LogRecord as per say, but has the concept of an ‘outcome of an operation’. The class org.eclipse.core.runtime.IStatus represents this outcome.

Most of the framework related to Logging and Error rendering uses an IStatus:

       * ILog.log(IStatus status)
       * IProgressMonitorWithBlocking.setBlocked(IStatus reason)
       * ErrorDialog.openError(Shell parent, String dialogTitle, String message, IStatus status)
    

 

Calling the Status API
----------------------

You should investigate the Status and MultiStatus classes. We recommend you use the following constructor:

public Status(int severity, String pluginId, int code, String message, Throwable exception)

    public Status(int severity, String pluginId, int code, String message, Throwable exception)

**ZZZZZZZZZZZZZ DO MORE HERE ZZZZZZZZZZZZZZ**

Implementing your own Status
----------------------------

You should consider the following when creating an IStatus instance.

1.  Try to create a subclass of IStatus that will carry your specific payload. An Example in Eclipse 3.3 is the class CVSStatus. The class carries information that diagnostic tool can use to validate CVS.
2.  Do not use IStatus.ERROR as a code. Try to use your own code. As an example, look at the Class CVSSTatus.

    public class CVSStatus extends TeamStatus {
     
    /*** Status codes ***/
    public static final int SERVER_ERROR = -10;
    public static final int NO_SUCH_TAG = -11;
    public static final int CONFLICT = -12;
    ...
    public static final int SERVER_IS_UNKNOWN = -22;
    ...
     
    public CVSStatus(int severity, int code, String message, Throwable t, ICVSRepositoryLocation cvsLocation) {
      super(severity, CVSProviderPlugin.ID, code, message, t,null);
      this.cvsLocation = cvsLocation;
    }

The Eclipse 3.3 Status handler framework
========================================

In Eclipse 3.3 we added a new framework to handle statuses. The goal was triple:

1.  Providing consistency about Logging and ErrorMessage from an Eclipse UI point of view. The work in the core is in progress in the Equinox subproject (as of March 07)
2.  Allow products based on eclipse a central way to handle statuses before they are rendered or logged
3.  Allow products based on eclipse to extend the ErrorDialog for specific errors

Check the following link to learn more about the StatusHandler project \[ [http://wiki.eclipse.org/index.php/Platform\_UI\_Error_Handling](http://wiki.eclipse.org/index.php/Platform_UI_Error_Handling)\]

In the first part of this paragraph, we will describe the best practices a developer should use to log or render an IStatus. In the second part of this paragraph, we will explain the best practices to create your own StatusHandler.

Using the new Eclipse status handler API
========================================

First we will present the new StatusManager class and its API. Then we will explain how you could modify your existing code to use the new features of the framework.

Calling the StatusManager handle method
---------------------------------------

Calling the new framework is straight forward. Once your IStatus is created, you pass it to the singleton instance of the class StatusManager. You can specify if you want the message to be logged (LOG) and.or shown (SHOW).

Remember the Handler has the last decision about showing and logging. Consider the previous information as a hint to the handler, but do not rely on them for your execution.

    public void run(IAction action) {
      // Throw an error to be handled
      File aConfigurationFile = obtainConfigurationFileFor(authenticatedUser);
      FileInputStream aStream = null;
      try {
        aStream = new FileInputStream(aConfigurationFile);
        //... if no error, then continue
      } catch (IOException exception){
        // Build a message
        String message = NLS.bind(Error_Accessing_Configuration, aConfigurationFile);
        // Build a new IStatus
        IStatus status = new CompanyStatus(IStatus.ERROR,Activator.PLUGIN_ID,CompanyStatus.CONFIGURATION_ERROR,
                                            message,exception, aConfigurationFile);
        // Let the StatusManager handle the Status and provide a hint
        StatusManager.getManager().handle(status, StatusManager.LOG|StatusManager.SHOW);
      } finally {
        if (aStream!=null){
          try {aStream.close();} catch (Exception e){};
        }
      } 
    }

Developing a StatusHandler
==========================

In this section, we will explain how to extend the Status handler framework. There will be a unique extension per application.

A StatusHandler is the counterpart of StatusManager. As a developer, you will call the StatusManager code, passing the IStatus. The StatusHandler will handle the IStatus based on its policy. For instance, a StatusHandler can determine if they need to be shown and/or logged. A StatusHandler can decide to send an email to an administrator.

Implementing the handle method
------------------------------

    public void handle(StatusAdapter statusAdapter, int style) {
     
      // Retrieve IStatus and message
      IStatus oldStatus = statusAdapter.getStatus();		
     
      // Verify we do not have a CompanyStatusWithID
      if (!(oldStatus instanceof CompanyStatusWithID)){
        String message = oldStatus.getMessage();
     
        //All our ID start with DYNA
        if (null!=message && message.startsWith("DYNA")){		
     
          //Remove any unique identifier to not show it to the user
          int lengthOfUniqueId = message.indexOf(' ');
     
          String uniqueID = message.substring(0,lengthOfUniqueId);			
          message = message.substring(lengthOfUniqueId);
          message = message + getExplanation(oldStatus);	// Retrieve the explanation for this status
     
          // Create a new CompanyStatusWithID
          IStatus newStatus = new CompanyStatusWithID(oldStatus.getPlugin(),oldStatus.getCode(), new IStatus[]{oldStatus}, uniqueID, message,null);
          statusAdapter.setStatus(newStatus);
        }
      }
     
      // check the Style. We still want to trace even if we do not log
      if ((style & StatusManager.LOG)==0){
        trace(statusAdapter);
      }
     
      super.handle(statusAdapter, style);
    }

Developing an ErrorSupportProvider
==================================

Registering your ErrorSupportProvider
-------------------------------------

To Register your SupportArea, you must call the following code

Policy.setErrorSupportProvider(<instance of org.eclipse.jface.dialogs.ErrorSupportProvider>);

    Policy.setErrorSupportProvider(<instance of org.eclipse.jface.dialogs.ErrorSupportProvider>);
    

 

We recommend you do it in the Activator class of your bundle, in the start method. Of course you can change it later, for instance in your handle method. In that case, there is no contract that the ErrorSupportProvider you set will be the one receiving the IStatus your are processing.

Implementing the createSupportArea method
-----------------------------------------

Implement the createSupportArea method, returning the control you want to show the user.

    public Control createSupportArea(Composite parent, IStatus status) {
      parent.addDisposeListener(this); // get notified so we can clean our SWT widgets
     
      // if the dialog is too short, make it taller
      ensureMinimumHeight(parent.getShell());
     
      toolkit = new FormToolkit(parent.getDisplay());
      toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(HyperlinkGroup.UNDERLINE_HOVER);
      toolkit.getColors().initializeSectionToolBarColors();
      ...
    }

Here is a simple example that will open a web page.

    public Control createSupportArea(Composite parent, IStatus status) {
      viewer = new Browser(parent, SWT.NONE);
      viewer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
      viewer.setUrl(<a String representing the URL>);
      return viewer;
    }

We highly recommend you implement an IDisposeListener to clean up after yourself, when the ErrorDialog is closed.

The flow
--------

When a IStatus needs to be handled, the StatusManager will retrieve the StatusHandler associated with the application. It will pass the StatusAdapter to the handle method. An Error dialog will open if you called the super.handle() method with a hint of StatusManager.SHOW. When an IStatus is selected, the ErrorSupportProvider registered in the Policy will be called. StatusManager will pass the IStatus to the ErrorSupportProvider.createSupportArea.

