

FAQ What is the purpose of activities?
======================================

Activities serve two purposes:

1.  [Clearing of cluttered workspaces](#1.-Clearing-of-cluttered-workspaces)
2.  [Disabling and removal of forbidden UI elements](#2.-Disabling-and-removal-of-forbidden-UI-elements)

1\. Clearing of cluttered workspaces
------------------------------------

The potential number of installed plug-ins in a given Eclipse instance is open ended. A user can buy or download a large Eclipse-based product and then start to add various extra plug-ins from the open source community or from plug-in vendors. As each of these plug-ins adds its views, actions, and other UI contributions, the user interface can become very cluttered. Some products have been known to have dozens of perspectives and wizards and hundreds of preference pages. Because the base platform doesn't know anything about these plug-ins or about the user's intentions, it is difficult to filter out unneeded contributions intelligently.

The platform uses a number of mechanisms to deal with this problem of user interface scalability. For example, the active perspective can, to a certain extent, customize the menus and toolbars available in that perspective. Furthermore, the user can customize each perspective, adding new capabilities from other plug-ins or removing menu and toolbar actions to reduce clutter. The problem with these approaches is that customization is suited mainly for advanced users who are intimately familiar with all the menus and know what the available commands are and which ones they need. The clutter problem, however, is most pressing for the novice user. People opening a product for the first time and inundated with rows of toolbar buttons and massive menus will have a difficult time finding what they are looking for. The fact that buried in one of those menus is an action that allows users to control what they see is of no use, as they won't know what they are showing or hiding.

The notion of an _activity_ was introduced in Eclipse 3.0 to help address this cluttering problem. An activity roughly describes what the user is currently doing with an Eclipse-based product. This in turn maps to a set of features that need to be visible in the context of that activity. For example, a product might have Java development and Web development activities. When the user is doing Java development, only the menu actions, toolbar buttons, and views applicable to that activity are shown. As the user works, various activities can become active or inactive, depending on what the user is doing, and the user interface is filtered accordingly.

The key attribute of activities is that they enable progressive disclosure in the user interface. A user opening a product for the first time might begin with an introductory activity that has a very simple set of available features. As the user explores and begins working with different tools, new activities get enabled implicitly, ensuring that tools are available when needed, not before. Eventually, a power user may end up with all activities enabled and the user interface heavily cluttered again, which is okay because the power user won't be daunted by it.

You may notice that the term _activity_ is not used in the Eclipse UI. Usability studies showed that the term was too vague and confusing for end-users, so the term _capability_ is used instead. You will find that documentation and source code use the two terms interchangeably.

2\. Disabling and removal of forbidden UI elements
--------------------------------------------------

Due to the problem that Eclipse itself lacks an own authentication and authorization concept and implementation the wheel had to be reinvented for every big multi-user rich client implementation using Eclipse. Every company implemented its own custom system. To solve the authorization part of this problem, the activities were extended so that they can be controlled by [expressions](/Command_Core_Expressions "Command Core Expressions"). Expression controlled activities which are disabled by an disabled expression can only be enabled through an enablement of the expression. UI elements which are disabled through activties which are disabled by expressions, are also completely unreachable. They vanish from every list and are completely invisible to the Eclipse API. Not only for end-users, but also for programmers there's no chance to access them.

  

