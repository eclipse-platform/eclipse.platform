

FAQ Pages, parts, sites, windows: What is all this stuff?
=========================================================

Denizens of the Eclipse newsgroups, clearly expressing frustration with the overwhelming sea of terms, have been known to ask, "What is the Eclipsian word for X?" or "What is that thingy called in Eclipse Speak?" In an effort to assuage the suffering of new users, the following is a mile-high view of the pieces of the Eclipse UI.

The term used to represent the entire UI is _workbench_. The workbench itself has no physical manifestation, but the workbench object is used to access most of the general APIs and services available in the generic UI. The workbench is displayed in one or more _workbench windows_. These basic top-level windows make up an Eclipse application. Note that dialogs, wizards, and other transient pop-ups are not called workbench windows.

At the top of each window is the _title bar_, typically a native widget with a title and controls for resizing and closing. Next comes the _menu bar_, and after that is the _cool bar_. The cool bar is a fancy term for a bar of buttons that can be dragged around and reorganized across multiple lines. On the left, right, or bottom, depending on user preference, is the _fast view bar_, where _fast views_, (iconified views) are stored. At the bottom is the _status line_, where various bits of information are shown; the far-right corner of the status line is called the _progress indicator_.

The main body of a workbench window is represented by the _workbench page_, which in turn is made up of _workbench parts_, which come in two varieties: _views_ and _editors_. The initial size and orientation of the parts in the page are determined by a _perspective_.

Parts interact with the rest of the window via their _site_. The site is not a visible entity but simply an API mechanism to separate the methods that operate on the view from the methods that operate on controls and services outside the view. This allows the workbench implementers to add new features to the sites without breaking all the plug-ins that implement the parts. Figure 9.1 Spider graph shows how a view (ContentOutline) and an editor (WelcomeEditor) each has its own site, which is hosted by a page, inside a workbench window, owned by the workbench.

![FAQ views.png](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/FAQ/images/FAQ_views.png)

**Figure 9.1**  Spider diagram of site parts

In addition, sites bring together that functionality that different parts of the workbench had in common but could not be expressed well in a single inheritance hierarchy.

See Also:
---------

*   [FAQ\_What\_is\_the\_difference\_between\_a\_perspective\_and\_a\_workbench_page?](./FAQ_What_is_the_difference_between_a_perspective_and_a_workbench_page.md "FAQ What is the difference between a perspective and a workbench page?")
*   [FAQ\_What\_is\_a\_view?](./FAQ_What_is_a_view.md "FAQ What is a view?")
*   [FAQ\_What\_is\_the\_difference\_between\_a\_view\_and\_an\_editor?](./FAQ_What_is_the_difference_between_a_view_and_an_editor.md "FAQ What is the difference between a view and an editor?")

