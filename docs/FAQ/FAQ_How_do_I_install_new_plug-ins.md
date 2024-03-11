

FAQ How do I install new plug-ins?
==================================

Your best approach is to use the Eclipse Update Manager.

More experienced plug-in developers — and sometimes lazy plug-in publishers — have learned to find the eclipse/plugins directory and install their plug-ins there manually. This obviously is a more dangerous approach, as no certification takes place about the suitability of the plug-in; it may rely on other plug-ins not available in your installation. In the case of compatibility conflicts, you won't find out until you use the plug-in that it might break.

You may compare installing plug-ins to installing applications on Windows. You can, of course, install your dynamic link libraries (DLLs) in the System32 directory or play with the PATH environment variable. But, how are you going to remove the application later when you no longer need it? On Windows, specialized installation programs have been devised, and uninstallation is easy through the **Start** menu. The Eclipse Update Manager can be seen as the Eclipse equivalent of InstallShield and the Windows Registry combined.

For day-to-day development and prototyping of small plug-ins, you might still be tempted to use the manual installation process. You could, but we strongly advise against it. Creating a feature and a corresponding update site is child's play using the PDE wizards and will greatly improve the quality of your work. Eventually, you will want to share your fruits with others, and having an update site ready from the start will make it much easier to boast of your Eclipse knowledge to your friends and colleagues.

See Also:
---------

[FAQ\_How\_do\_I\_create\_a\_plug-in?](./FAQ_How_do_I_create_a_plug-in.md "FAQ How do I create a plug-in?")

[FAQ\_How\_do\_I\_create\_a\_feature?](./FAQ_How_do_I_create_a_feature.md "FAQ How do I create a feature?")

[FAQ\_What\_is\_the\_Update_Manager?](./FAQ_What_is_the_Update_Manager.md "FAQ What is the Update Manager?")

[FAQ\_What\_is\_the\_purpose\_of\_activities?](./FAQ_What_is_the_purpose_of_activities.md "FAQ What is the purpose of activities?")

