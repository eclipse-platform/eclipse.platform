

FAQ What is the difference between perspectives and activities?
===============================================================

  

  

At first glance, perspectives and activities are very similar. Both modify the appearance of the workbench, adding and removing menus and toolbar buttons, depending on a course-grained user task. When trying to figure out how to carve up a large product into usable pieces, it is sometimes difficult to figure out when to use activities, perspectives, or both. Apart from the prosaic difference that perspectives were an Eclipse 1.0 invention and activities are new in Eclipse 3.0, a number of other subtle differences exist.

  
The most obvious difference between activities and perspectives is that activities are not intended to alter the layout of views and editors that are currently open. Activities change the menus and toolbars but don't make real estate changes that can cause the user to lose context. Perspectives have a dual role of dictating screen layout and altering the menus and toolbars. This connection is often not obvious to users, and they are often confused about why a certain menu or keyboard shortcut appears in one place but not another.

  
Second, activities are intended to be largely implicit. Whereas perspectives are explicitly visible and controlled entirely by the user, activities are not directly user controlled. Essentially, activities try to have some smarts about when they are needed and activate and deactivate accordingly, depending on what the user is doing. Although perspectives do this to a certain extent, such as switching to the Debug perspective automatically when an application is launched in debug mode, automatically changing the user interface layout is a general user interface no-no. Because activities are not tied to a particular screen layout, they can change automatically without causing the user to lose context.

  
Finally, perspectives are partially defined programmatically, whereas activities are defined entirely declaratively. The importance of this difference is that activities are not intended to be defined by programmers who are working on individual plug-ins. Because the designer of a single plug-in cannot envision the emergent behavior of a system built of hundreds of plug-ins-or at least not without violating the modular principles of the plug-in architecture-he or she has no way of knowing what set of activities might be appropriate. Activities are a form of meta-glue that regulate how plug-ins interact and so must be defined at a higher level. Typically, activities will be created by a system configurer or human-computer interaction (HCI) expert who has a better picture of the total functionality of the product.

