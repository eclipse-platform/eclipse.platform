

FAQ How do activities get enabled?
==================================

There are two kinds of activities

1.  [Conventional activities](#1.-Conventional-activities)
2.  [Expression-enabled activities](#2.-Expression-enabled-activities)

1\. Conventional activities
---------------------------

When a user starts a new workspace, all activities will typically be disabled. As the user explores the application and starts to use new features, more activities can become enabled as they are needed. This enablement typically happens automatically when the workbench determines that a user is starting to use features defined in a disabled activity. The workbench defines a number of _trigger points_, UI interactions that signal the introduction of a new activity. For example, the Eclipse IDE workbench considers creation of a project to be a trigger point. When a project is created, the workbench looks at the natures on that project and enables any activities that have pattern bindings matching any of the nature IDs.

  
You can define your own trigger points for any logical place where new plug-in functionality will be introduced. The trigger point itself doesn't need to know about any particular activities; it simply needs to define an identifier that represents the new functionality. The identifier should be in the form either `<plugin-id>`/`<local-id>` or simply `<local-id>`. Using that ID, you can enable any matching activities as follows:

        String id = ...
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchActivitySupport as = wb.getActivitySupport();
        IActivityManager am = as.getActivityManager();
        IIdentifier identifier = am.getIdentifier(id);
        Set activities = new HashSet(am.getEnabledActivityIds());
        if (activities.addAll(identifier.getActivityIds())) {
        as.setEnabledActivityIds(activities);
        }

The exact format of the ID used for your trigger point should be well documented so that activity writers will be able to write appropriate pattern bindings for activating their activities using your trigger points.

  
Activities can also be enabled or disabled explicitly by an end user. Again, this is something only the advanced user will do. The success of activities as a mechanism for scalability and progressive disclosure relies on carefully chosen trigger points in the user interface for seamlessly enabling activities.

2\. Expression-enabled activities
---------------------------------

For a description of expression-enabled activities look at [FAQ What is the purpose of activities?](./FAQ_What_is_the_purpose_of_activities.md "FAQ What is the purpose of activities?").  
Expression-enabled activities can, of course, only be enabled through an enablement of their expressions.

  

