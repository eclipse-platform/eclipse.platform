

The Official Eclipse FAQs
=========================

Note that this FAQ is updated less frequently than [this FAQ](/IRC_FAQ "IRC FAQ"). You may find your answer there before it makes its way here.

Contents
--------

*   [1 Part I -- The Eclipse Ecosystem](#Part-I----The-Eclipse-Ecosystem)
    *   [1.1 The Eclipse Community](#The-Eclipse-Community)
    *   [1.2 Getting Started](#Getting-Started)
    *   [1.3 Java Development in Eclipse](#Java-Development-in-Eclipse)
    *   [1.4 Plug-In Development Environment](#Plug-In-Development-Environment)
*   [2 Part II -- The Rich Client Platform](#Part-II----The-Rich-Client-Platform)
    *   [2.1 All about Plug-ins](#All-about-Plug-ins)
    *   [2.2 Runtime Facilities](#Runtime-Facilities)
    *   [2.3 Standard Widget Toolkit (SWT)](#Standard-Widget-Toolkit-.28SWT.29)
    *   [2.4 JFace](#JFace)
    *   [2.5 Generic Workbench](#Generic-Workbench)
    *   [2.6 Perspectives and Views](#Perspectives-and-Views)
    *   [2.7 Generic Editors](#Generic-Editors)
    *   [2.8 Actions, Commands, and Activities](#Actions.2C-Commands.2C-and-Activities)
    *   [2.9 Building Your Own Application](#Building-Your-Own-Application)
    *   [2.10 Productizing an Eclipse Offering](#Productizing-an-Eclipse-Offering)
*   [3 Part III -- The Eclipse IDE Platform](#Part-III----The-Eclipse-IDE-Platform)
    *   [3.1 Text Editors](#Text-Editors)
    *   [3.2 Help, Search, and Compare](#Help.2C-Search.2C-and-Compare)
    *   [3.3 Workspace and Resources API](#Workspace-and-Resources-API)
    *   [3.4 Workbench IDE](#Workbench-IDE)
    *   [3.5 Implementing Support for Your Own Language](#Implementing-Support-for-Your-Own-Language)
        *   [3.5.1 The language already has parsers, compilers, and other services](#The-language-already-has-parsers.2C-compilers.2C-and-other-services)
        *   [3.5.2 This is a DSL of your own](#This-is-a-DSL-of-your-own)
        *   [3.5.3 Legacy](#Legacy)
    *   [3.6 Java Development Tool API](#Java-Development-Tool-API)
*   [4 Where to buy the original book](#Where-to-buy-the-original-book)

Part I -- The Eclipse Ecosystem
-------------------------------

### The Eclipse Community

Eclipse has taken the computing industry by storm. The download data for the Eclipse Software Development Kit (SDK) is astounding and a true ecosystem is forming around this new phenomenon. In this chapter we discuss what Eclipse is and who is involved in it and give you a glimpse of how large a community has put its weight behind this innovative technology.

An open source project would be nothing without a supporting community. The Eclipse ecosystem is a thriving one, with many research projects based on Eclipse, commercial products that ship on top of Eclipse, lively discussions in newsgroups and mailing lists, and a long list of articles and books that address the platform. The following pages will give you a roadmap of the community, so that you will feel more at home as you come to wander its winding streets.

*   [FAQ What is Eclipse?](./FAQ_What_is_Eclipse.md "FAQ What is Eclipse?")
*   [FAQ What is the Eclipse Platform?](./FAQ_What_is_the_Eclipse_Platform.md "FAQ What is the Eclipse Platform?")
*   [FAQ Where did Eclipse come from?](./FAQ_Where_did_Eclipse_come_from.md "FAQ Where did Eclipse come from?")
*   [FAQ What is the Eclipse Foundation?](./FAQ_What_is_the_Eclipse_Foundation.md "FAQ What is the Eclipse Foundation?")
*   [FAQ How can my users tell where Eclipse ends and a product starts?](./FAQ_How_can_my_users_tell_where_Eclipse_ends_and_a_product_starts.md "FAQ How can my users tell where Eclipse ends and a product starts?")
*   [FAQ What are Eclipse projects and technologies?](./FAQ_What_are_Eclipse_projects_and_technologies.md "FAQ What are Eclipse projects and technologies?")
*   [FAQ How do I propose my own project?](./FAQ_How_do_I_propose_my_own_project.md "FAQ How do I propose my own project?")
*   [FAQ Who is building commercial products based on Eclipse?](./FAQ_Who_is_building_commercial_products_based_on_Eclipse.md "FAQ Who is building commercial products based on Eclipse?")
*   [FAQ What open source projects are based on Eclipse?](./FAQ_What_open_source_projects_are_based_on_Eclipse.md "FAQ What open source projects are based on Eclipse?")
*   [FAQ What academic research projects are based on Eclipse?](./FAQ_What_academic_research_projects_are_based_on_Eclipse.md "FAQ What academic research projects are based on Eclipse?")
*   [FAQ Who uses Eclipse in the classroom?](./FAQ_Who_uses_Eclipse_in_the_classroom.md "FAQ Who uses Eclipse in the classroom?")
*   [FAQ What is an Eclipse Innovation Grant?](./FAQ_What_is_an_Eclipse_Innovation_Grant.md "FAQ What is an Eclipse Innovation Grant?")
*   [FAQ What Eclipse newsgroups are available?](./FAQ_What_Eclipse_newsgroups_are_available.md "FAQ What Eclipse newsgroups are available?")
*   [FAQ How do I get access to Eclipse newsgroups?](./FAQ_How_do_I_get_access_to_Eclipse_newsgroups.md "FAQ How do I get access to Eclipse newsgroups?")
*   [FAQ What Eclipse mailing lists are available?](./FAQ_What_Eclipse_mailing_lists_are_available.md "FAQ What Eclipse mailing lists are available?")
*   [FAQ What articles on Eclipse have been written?](./FAQ_What_articles_on_Eclipse_have_been_written.md "FAQ What articles on Eclipse have been written?")
*   [FAQ What books have been written on Eclipse?](./FAQ_What_books_have_been_written_on_Eclipse.md "FAQ What books have been written on Eclipse?")
*   [FAQ How do I report a bug in Eclipse?](./FAQ_How_do_I_report_a_bug_in_Eclipse.md "FAQ How do I report a bug in Eclipse?")
*   [FAQ How can I search the existing list of bugs in Eclipse?](./FAQ_How_can_I_search_the_existing_list_of_bugs_in_Eclipse.md "FAQ How can I search the existing list of bugs in Eclipse?")
*   [FAQ What do I do if my feature request is ignored?](./FAQ_What_do_I_do_if_my_feature_request_is_ignored.md "FAQ What do I do if my feature request is ignored?")
*   [FAQ Can I get my documentation in PDF form, please?](./FAQ_Can_I_get_my_documentation_in_PDF_form_please.md "FAQ Can I get my documentation in PDF form, please?")
*   [FAQ Where do I find documentation for a given extension point?](./FAQ_Where_do_I_find_documentation_for_a_given_extension_point.md "FAQ Where do I find documentation for a given extension point?")
*   [FAQ How is Eclipse licensed?](./FAQ_How_is_Eclipse_licensed.md "FAQ How is Eclipse licensed?")

### Getting Started

Eclipse can be seen as a very advanced Java program. Running Eclipse may sound simple—simply run the included eclipse.exe or eclipse executable—yet in practice, you may want to tweak the inner workings of the platform. First, Eclipse does not come with a Java virtual machine (JVM), so you have to get one yourself. Note that any version later than Eclipse 3.0 needs a 1.4-compatible Java runtime environment (JRE), and the Java 5 JRE is recommended for Eclipse version 3.3.

To use Eclipse effectively, you will need to learn how to make Eclipse use a specific JRE. In addition, you may want to influence how much heap Eclipse may allocate, where it loads and saves its workspace from, and how you can add more plug-ins to your Eclipse installation.

This chapter should get you going. We also included some FAQs for plug-in developers who have already written plug-ins and want to get started with plug-in development for Eclipse 3.0.

*   [FAQ Where do I get and install Eclipse?](./FAQ_Where_do_I_get_and_install_Eclipse.md "FAQ Where do I get and install Eclipse?")
*   [FAQ How do I run Eclipse?](./FAQ_How_do_I_run_Eclipse.md "FAQ How do I run Eclipse?")
*   [FAQ How do I increase the heap size available to Eclipse?](./FAQ_How_do_I_increase_the_heap_size_available_to_Eclipse.md "FAQ How do I increase the heap size available to Eclipse?")
*   [FAQ Where can I find that elusive .log file?](./FAQ_Where_can_I_find_that_elusive_log_file.md "FAQ Where can I find that elusive .log file?")
*   [FAQ Does Eclipse run on any Linux distribution?](./FAQ_Does_Eclipse_run_on_any_Linux_distribution.md "FAQ Does Eclipse run on any Linux distribution?")
*   [FAQ I unzipped Eclipse, but it won't start](./FAQ_I_unzipped_Eclipse_but_it_wont_start.md "FAQ I unzipped Eclipse, but it won't start")
*   [FAQ How do I upgrade Eclipse?](./FAQ_How_do_I_upgrade_Eclipse.md "FAQ How do I upgrade Eclipse?")
*   [FAQ How do I install new plug-ins?](./FAQ_How_do_I_install_new_plug-ins.md "FAQ How do I install new plug-ins?")
*   [FAQ Can I install plug-ins outside the main install directory?](./FAQ_Can_I_install_plug-ins_outside_the_main_install_directory.md "FAQ Can I install plug-ins outside the main install directory?")
*   [FAQ How do I remove a plug-in?](./FAQ_How_do_I_remove_a_plug-in.md "FAQ How do I remove a plug-in?")
*   [FAQ How do I find out what plug-ins have been installed?](./FAQ_How_do_I_find_out_what_plug-ins_have_been_installed.md "FAQ How do I find out what plug-ins have been installed?")
*   [FAQ Where do I get help?](./FAQ_Where_do_I_get_help.md "FAQ Where do I get help?")
*   [FAQ How do I report a bug?](./FAQ_How_do_I_report_a_bug.md "FAQ How do I report a bug?")
*   [FAQ How do I accommodate project layouts that don't fit the Eclipse model?](./FAQ_How_do_I_accommodate_project_layouts_that_dont_fit_the_Eclipse_model.md "FAQ How do I accommodate project layouts that don't fit the Eclipse model?")
*   [FAQ What is new in Eclipse 3.0?](./FAQ_What_is_new_in_Eclipse_3_0.md "FAQ What is new in Eclipse 3.0?")
*   [FAQ Is Eclipse 3.0 going to break all of my old plug-ins?](./FAQ_Is_Eclipse_3.0_going_to_break_all_of_my_old_plug-ins.md "FAQ Is Eclipse 3.0 going to break all of my old plug-ins?")
*   [FAQ How do I prevent my plug-in from being broken when I update Eclipse?](./FAQ_How_do_I_prevent_my_plug-in_from_being_broken_when_I_update_Eclipse.md "FAQ How do I prevent my plug-in from being broken when I update Eclipse?")

### Java Development in Eclipse

The topic of how to use Eclipse for typical Java development is beyond the scope of this FAQ list. We focus more on the issues Eclipse users may run into when developing new plug-ins for the platform. Also, as a plug-in developer, you need to be familiar with the ways in which Eclipse is used. To achieve seamless integration with the platform, your plug-in must respect common usage patterns and offer the same level of functionality that users of your plug-in have come to expect from the platform. This chapter focuses on user-level issues of interest to plug-in developers as users or as enablers for other users of the platform.

For a comprehensive guide to using Eclipse, refer to other books such as The Java Developer’s Guide to Eclipse (Addison-Wesley, 2003).

*   [FAQ How do I get started if I am new to Java and Eclipse?](./FAQ_How_do_I_get_started_if_I_am_new_to_Java_and_Eclipse.md "FAQ How do I get started if I am new to Java and Eclipse?")
*   [FAQ How do I show or hide files like classpath in the Navigator?](./FAQ_How_do_I_show_or_hide_files_like_classpath_in_the_Navigator.md "FAQ How do I show or hide files like classpath in the Navigator?")
*   [FAQ How do I link the Navigator with the currently active editor?](./FAQ_How_do_I_link_the_Navigator_with_the_currently_active_editor.md "FAQ How do I link the Navigator with the currently active editor?")
*   [FAQ How do I use the keyboard to traverse between editors?](./FAQ_How_do_I_use_the_keyboard_to_traverse_between_editors.md "FAQ How do I use the keyboard to traverse between editors?")
*   [FAQ How can I rearrange Eclipse views and editors?](./FAQ_How_can_I_rearrange_Eclipse_views_and_editors.md "FAQ How can I rearrange Eclipse views and editors?")
*   [FAQ Why doesn't my program start when I click the Run button?](./FAQ_Why_doesnt_my_program_start_when_I_click_the_Run_button.md "FAQ Why doesn't my program start when I click the Run button?")
*   [FAQ How do I turn off autobuilding of Java code?](./FAQ_How_do_I_turn_off_autobuilding_of_Java_code.md "FAQ How do I turn off autobuilding of Java code?")
*   [FAQ How do I hide referenced libraries in the Package Explorer?](./FAQ_How_do_I_hide_referenced_libraries_in_the_Package_Explorer.md "FAQ How do I hide referenced libraries in the Package Explorer?")
*   [FAQ Where do my class files disappear to?](./FAQ_Where_do_my_class_files_disappear_to.md "FAQ Where do my class files disappear to?")
*   [FAQ What editor keyboard shortcuts are available?](./FAQ_What_editor_keyboard_shortcuts_are_available.md "FAQ What editor keyboard shortcuts are available?")
*   [FAQ How do I stop the Java editor from showing a single method at once?](./FAQ_How_do_I_stop_the_Java_editor_from_showing_a_single_method_at_once.md "FAQ How do I stop the Java editor from showing a single method at once?")
*   [FAQ How do I open a type in a Java editor?](./FAQ_How_do_I_open_a_type_in_a_Java_editor.md "FAQ How do I open a type in a Java editor?")
*   [FAQ How do I control the Java formatter?](./FAQ_How_do_I_control_the_Java_formatter.md "FAQ How do I control the Java formatter?")
*   [FAQ How do I choose my own compiler?](./FAQ_How_do_I_choose_my_own_compiler.md "FAQ How do I choose my own compiler?")
*   [FAQ What Java refactoring support is available?](./FAQ_What_Java_refactoring_support_is_available.md "FAQ What Java refactoring support is available?")
*   [FAQ How can Content Assist make me the fastest coder ever?](./FAQ_How_can_Content_Assist_make_me_the_fastest_coder_ever.md "FAQ How can Content Assist make me the fastest coder ever?")
*   [FAQ How can templates make me the fastest coder ever?](./FAQ_How_can_templates_make_me_the_fastest_coder_ever.md "FAQ How can templates make me the fastest coder ever?")
*   [FAQ What is a Quick Fix?](./FAQ_What_is_a_Quick_Fix.md "FAQ What is a Quick Fix?")
*   [FAQ How do I profile my Java program?](./FAQ_How_do_I_profile_my_Java_program.md "FAQ How do I profile my Java program?")
*   [FAQ How do I debug my Java program?](./FAQ_How_do_I_debug_my_Java_program.md "FAQ How do I debug my Java program?")
*   [FAQ How do I find out the command-line arguments of a launched program?](./FAQ_How_do_I_find_out_the_command-line_arguments_of_a_launched_program.md "FAQ How do I find out the command-line arguments of a launched program?")
*   [FAQ What is hot code replace?](./FAQ_What_is_hot_code_replace.md "FAQ What is hot code replace?")
*   [FAQ How do I set a conditional breakpoint?](./FAQ_How_do_I_set_a_conditional_breakpoint.md "FAQ How do I set a conditional breakpoint?")
*   [FAQ How do I find all Java methods that return a String?](./FAQ_How_do_I_find_all_Java_methods_that_return_a_String.md "FAQ How do I find all Java methods that return a String?")
*   [FAQ What can I view in the Hierarchy view?](./FAQ_What_can_I_view_in_the_Hierarchy_view.md "FAQ What can I view in the Hierarchy view?")
*   [FAQ How do I add an extra library to my project's classpath?](./FAQ_How_do_I_add_an_extra_library_to_my_projects_classpath.md "FAQ How do I add an extra library to my project's classpath?")
*   [FAQ What is the advantage of sharing the project file in a repository?](./FAQ_What_is_the_advantage_of_sharing_the_project_file_in_a_repository.md "FAQ What is the advantage of sharing the project file in a repository?")
*   [FAQ What is the function of the .cvsignore file?](./FAQ_What_is_the_function_of_the_cvsignore_file.md "FAQ What is the function of the .cvsignore file?")
*   [FAQ How do I set up a Java project to share in a repository?](./FAQ_How_do_I_set_up_a_Java_project_to_share_in_a_repository.md "FAQ How do I set up a Java project to share in a repository?")
*   [FAQ Why does the Eclipse compiler create a different serialVersionUID from javac?](./FAQ_Why_does_the_Eclipse_compiler_create_a_different_serialVersionUID_from_javac.md "FAQ Why does the Eclipse compiler create a different serialVersionUID from javac?")

### Plug-In Development Environment

This book is all about extending the Eclipse Platform. The main instrument for extending the platform is a plug-in. Plug-ins solidify certain crucial design criteria underlying Eclipse. Special tooling has been developed as part of Eclipse to support the development of plug-ins. This set of plug-ins is called the Plug-in Development Environment; or PDE. The PDE tools cover the entire lifecycle of plug-in development, from creating them using special wizards to editing them to building them to launching them to exporting and sharing them.

This chapter describes the mechanics of plug-in development, such as creating plug-ins, features, and update sites, and introduces the PDE tooling. We go into much more depth about what plug-ins are in later FAQs. If you want to jump ahead, we suggest that you first visit [FAQ What is a plug-in?](./FAQ_What_is_a_plug-in.md "FAQ What is a plug-in?").

*   [FAQ How do I create a plug-in?](./FAQ_How_do_I_create_a_plug-in.md "FAQ How do I create a plug-in?")
*   [FAQ How do I use the plug-in Manifest Editor?](./FAQ_How_do_I_use_the_plug-in_Manifest_Editor.md "FAQ How do I use the plug-in Manifest Editor?")
*   [FAQ Why doesn't my plug-in build correctly?](./FAQ_Why_doesnt_my_plug-in_build_correctly.md "FAQ Why doesn't my plug-in build correctly?")
*   [FAQ How do I run my plug-in in another instance of Eclipse?](./FAQ_How_do_I_run_my_plug-in_in_another_instance_of_Eclipse.md "FAQ How do I run my plug-in in another instance of Eclipse?")
*   [FAQ What causes my plug-in to build but not to load in a runtime workbench?](./FAQ_What_causes_my_plug-in_to_build_but_not_to_load_in_a_runtime_workbench.md "FAQ What causes my plug-in to build but not to load in a runtime workbench?")
*   [FAQ My runtime workbench runs, but my plug-in does not show. Why?](./FAQ_My_runtime_workbench_runs_but_my_plug-in_does_not_show.md "FAQ My runtime workbench runs, but my plug-in does not show. Why?")
*   [FAQ How do I add images and other resources to a runtime JAR file?](./FAQ_How_do_I_add_images_and_other_resources_to_a_runtime_JAR_file.md "FAQ How do I add images and other resources to a runtime JAR file?")
*   [FAQ Can I add icons declared by my plugin.xml in the runtime JAR?](./FAQ_Can_I_add_icons_declared_by_my_plugin_xml_in_the_runtime_JAR.md "FAQ Can I add icons declared by my plugin.xml in the runtime JAR?")
*   [FAQ When does PDE change a plug-in's Java build path?](./FAQ_When_does_PDE_change_a_plug-ins_Java_build_path.md "FAQ When does PDE change a plug-in's Java build path?")
*   [FAQ What is a PDE JUnit test?](./FAQ_What_is_a_PDE_JUnit_test.md "FAQ What is a PDE JUnit test?")
*   [FAQ Where can I find the Eclipse plug-ins?](./FAQ_Where_can_I_find_the_Eclipse_plug-ins.md "FAQ Where can I find the Eclipse plug-ins?")
*   [FAQ How do I find a particular class from an Eclipse plug-in?](./FAQ_How_do_I_find_a_particular_class_from_an_Eclipse_plug-in.md "FAQ How do I find a particular class from an Eclipse plug-in?")
*   [FAQ Why do I get a 'plug-in was unable to load class' error when I activate a menu or toolbar action?](./FAQ_Why_do_I_get_a_plug-in_was_unable_to_load_class_error_when_I_activate_a_menu_or_toolbar_action.md "FAQ Why do I get a 'plug-in was unable to load class' error when I activate a menu or toolbar action?")
*   [FAQ What is the use of the build.xml file?](./FAQ_What_is_the_use_of_the_build_xml_file.md "FAQ What is the use of the build.xml file?")
*   [FAQ How do I prevent my build.xml file from being overwritten?](./FAQ_How_do_I_prevent_my_build_xml_file_from_being_overwritten.md "FAQ How do I prevent my build.xml file from being overwritten?")
*   [FAQ When is the build.xml script executed?](./FAQ_When_is_the_build_xml_script_executed.md "FAQ When is the build.xml script executed?")
*   [FAQ How do I declare my own extension point?](./FAQ_How_do_I_declare_my_own_extension_point.md "FAQ How do I declare my own extension point?")
*   [FAQ How do I find all the plug-ins that contribute to my extension point?](./FAQ_How_do_I_find_all_the_plug-ins_that_contribute_to_my_extension_point.md "FAQ How do I find all the plug-ins that contribute to my extension point?")
*   [FAQ Why is the interface for my new extension point not visible?](./FAQ_Why_is_the_interface_for_my_new_extension_point_not_visible.md "FAQ Why is the interface for my new extension point not visible?")
*   [FAQ Can my extension point schema contain nested elements?](./FAQ_Can_my_extension_point_schema_contain_nested_elements.md "FAQ Can my extension point schema contain nested elements?")
*   [FAQ How do I create a feature?](./FAQ_How_do_I_create_a_feature.md "FAQ How do I create a feature?")
*   [FAQ How do I synchronize versions between a feature and its plug-in(s)?](./FAQ_How_do_I_synchronize_versions_between_a_feature_and_its_plug-ins.md "FAQ How do I synchronize versions between a feature and its plug-in(s)?")
*   [FAQ What is the Update Manager?](./FAQ_What_is_the_Update_Manager.md "FAQ What is the Update Manager?")
*   [FAQ How do I create an update site?](./FAQ_How_do_I_create_an_update_site.md "FAQ How do I create an update site?")
*   [FAQ Why does my update site need a license?](./FAQ_Why_does_my_update_site_need_a_license.md "FAQ Why does my update site need a license?")

Part II -- The Rich Client Platform
-----------------------------------

### All about Plug-ins

Part I discussed the Eclipse ecosystem: how to run it, how to use it, and how to extend it. In this chapter, we revisit the topic of plug-ins and lay the groundwork for all plug-in development topics to be discussed in later chapters. This chapter answers questions about the core concepts of the Eclipse kernel, including plug-ins, extension points, fragments, and more. All APIs mentioned in this chapter are found in the org.eclipse.core.runtime plug-in.

*   [FAQ What is a plug-in?](./FAQ_What_is_a_plug-in.md "FAQ What is a plug-in?")
*   [FAQ Do I use plugin or plug-in?](./FAQ_Do_I_use_plugin_or_plug-in.md "FAQ Do I use plugin or plug-in?")
*   [FAQ What is the plug-in manifest file (plugin.xml)?](./FAQ_What_is_the_plug-in_manifest_file_plugin_xml.md "FAQ What is the plug-in manifest file (plugin.xml)?")
*   [FAQ How do I make my plug-in connect to other plug-ins?](./FAQ_How_do_I_make_my_plug-in_connect_to_other_plug-ins.md "FAQ How do I make my plug-in connect to other plug-ins?")
*   [FAQ What are extensions and extension points?](./FAQ_What_are_extensions_and_extension_points.md "FAQ What are extensions and extension points?")
*   [FAQ What is an extension point schema?](./FAQ_What_is_an_extension_point_schema.md "FAQ What is an extension point schema?")
*   [FAQ How do I find out more about a certain extension point?](./FAQ_How_do_I_find_out_more_about_a_certain_extension_point.md "FAQ How do I find out more about a certain extension point?")
*   [FAQ When does a plug-in get started?](./FAQ_When_does_a_plug-in_get_started.md "FAQ When does a plug-in get started?")
*   [FAQ Where do plug-ins store their state?](./FAQ_Where_do_plug-ins_store_their_state.md "FAQ Where do plug-ins store their state?")
*   [FAQ How do I find out the install location of a plug-in?](./FAQ_How_do_I_find_out_the_install_location_of_a_plug-in.md "FAQ How do I find out the install location of a plug-in?")
*   [FAQ What is the classpath of a plug-in?](./FAQ_What_is_the_classpath_of_a_plug-in.md "FAQ What is the classpath of a plug-in?")
*   [FAQ How do I add a library to the classpath of a plug-in?](./FAQ_How_do_I_add_a_library_to_the_classpath_of_a_plug-in.md "FAQ How do I add a library to the classpath of a plug-in?")
*   [FAQ How can I share a JAR among various plug-ins?](./FAQ_How_can_I_share_a_JAR_among_various_plug-ins.md "FAQ How can I share a JAR among various plug-ins?")
*   [FAQ How do I use the context class loader in Eclipse?](./FAQ_How_do_I_use_the_context_class_loader_in_Eclipse.md "FAQ How do I use the context class loader in Eclipse?")
*   [FAQ Why doesn't Eclipse play well with Xerces?](./FAQ_Why_doesnt_Eclipse_play_well_with_Xerces.md "FAQ Why doesn't Eclipse play well with Xerces?")
*   [FAQ What is a plug-in fragment?](./FAQ_What_is_a_plug-in_fragment.md "FAQ What is a plug-in fragment?")
*   [FAQ Can fragments be used to patch a plug-in?](./FAQ_Can_fragments_be_used_to_patch_a_plug-in.md "FAQ Can fragments be used to patch a plug-in?")
*   [FAQ What is a configuration?](./FAQ_What_is_a_configuration.md "FAQ What is a configuration?")
*   [FAQ How do I find out whether the Eclipse Platform is running?](./FAQ_How_do_I_find_out_whether_the_Eclipse_Platform_is_running.md "FAQ How do I find out whether the Eclipse Platform is running?")
*   [FAQ Where does System.out and System.err output go?](./FAQ_Where_does_System_out_and_System_err_output_go.md "FAQ Where does System.out and System.err output go?")
*   [FAQ How do I locate the owner plug-in from a given class?](./FAQ_How_do_I_locate_the_owner_plug-in_from_a_given_class.md "FAQ How do I locate the owner plug-in from a given class?")
*   [FAQ How does OSGi and the new runtime affect me?](./FAQ_How_does_OSGi_and_the_new_runtime_affect_me.md "FAQ How does OSGi and the new runtime affect me?")
*   [FAQ What is a dynamic plug-in?](./FAQ_What_is_a_dynamic_plug-in.md "FAQ What is a dynamic plug-in?")
*   [FAQ How do I make my plug-in dynamic enabled?](./FAQ_How_do_I_make_my_plug-in_dynamic_enabled.md "FAQ How do I make my plug-in dynamic enabled?")
*   [FAQ How do I make my plug-in dynamic aware?](./FAQ_How_do_I_make_my_plug-in_dynamic_aware.md "FAQ How do I make my plug-in dynamic aware?")

  

### Runtime Facilities

Above, we already discussed most of the basic functionality of the org.eclipse.core.runtime plug-in. This chapter covers the remaining facilities of Eclipse Platform runtime: APIs for logging, tracing, storing preferences, and other such core functionality. These various services, although not strictly needed by all plug-ins, are common enough that they merit being located directly alongside the Eclipse kernel. In Eclipse 3.0, this plug-in was expanded to add infrastructure for running and managing background operations. This chapter answers some of the questions that may arise when you start to use this new concurrency infrastructure.

*   [FAQ How do I use progress monitors?](./FAQ_How_do_I_use_progress_monitors.md "FAQ How do I use progress monitors?")
*   [FAQ How do I use a SubProgressMonitor?](./FAQ_How_do_I_use_a_SubProgressMonitor.md "FAQ How do I use a SubProgressMonitor?")
*   [FAQ How do I use the platform logging facility?](./FAQ_How_do_I_use_the_platform_logging_facility.md "FAQ How do I use the platform logging facility?")
*   [FAQ How do I use the platform debug tracing facility](./FAQ_How_do_I_use_the_platform_debug_tracing_facility.md "FAQ How do I use the platform debug tracing facility")?
*   [FAQ How do I load and save plug-in preferences?](./FAQ_How_do_I_load_and_save_plug-in_preferences.md "FAQ How do I load and save plug-in preferences?")
*   [FAQ How do I use the preference service?](./FAQ_How_do_I_use_the_preference_service.md "FAQ How do I use the preference service?")
*   [FAQ What is a preference scope?](./FAQ_What_is_a_preference_scope.md "FAQ What is a preference scope?")
*   [FAQ How do I use IAdaptable and IAdapterFactory?](./FAQ_How_do_I_use_IAdaptable_and_IAdapterFactory.md "FAQ How do I use IAdaptable and IAdapterFactory?")
*   [FAQ Does the platform have support for concurrency?](./FAQ_Does_the_platform_have_support_for_concurrency.md "FAQ Does the platform have support for concurrency?")
*   [FAQ How do I prevent two jobs from running at the same time?](./FAQ_How_do_I_prevent_two_jobs_from_running_at_the_same_time.md "FAQ How do I prevent two jobs from running at the same time?")
*   [FAQ What is the purpose of job families?](./FAQ_What_is_the_purpose_of_job_families.md "FAQ What is the purpose of job families?")
*   [FAQ How do I find out whether a particular job is running?](./FAQ_How_do_I_find_out_whether_a_particular_job_is_running.md "FAQ How do I find out whether a particular job is running?")
*   [FAQ How can I track the lifecycle of jobs?](./FAQ_How_can_I_track_the_lifecycle_of_jobs.md "FAQ How can I track the lifecycle of jobs?")
*   [FAQ How do I create a repeating background task?](./FAQ_How_do_I_create_a_repeating_background_task.md "FAQ How do I create a repeating background task?")

### Standard Widget Toolkit (SWT)

One of the great success stories of the Eclipse Platform has been the overwhelming groundswell of support for its windowing toolkit, SWT. This toolkit offers a fast, thin, mostly native alternative to the most common Java UI toolkits, Swing and Abstract Windowing Toolkit (AWT). Religious debates abound over the relative merits of Swing versus SWT, and we take great pains to avoid these debates here. Suffice it to say that SWT generates massive interest and manages to garner as much, if not more, interest as the Eclipse Platform built on top of it.

The popularity of SWT has forced us to take a slightly different approach with this chapter. The SWT newsgroup was created in July 2003 and since then has generated an average of 136 messages every day. In this book, we could not even scratch the surface of the information available there. Although we could present the illusion of completeness by answering a couple dozen popular technical questions, we would not be doing the topic justice. Instead, we focus on answering a few of the higher-level questions and providing as many forward pointers as we can to further information on SWT available elsewhere. A benefit of SWT’s popularity is the wealth of Web sites, discussion forums, books, and other forms of documentation out there. Thus, although we won’t be able to answer all SWT questions, we hope at least to steer you to the resources that can. However, a handful of questions are asked so often that we can’t resist answering them here.

*   [FAQ What is SWT?](./FAQ_What_is_SWT.md "FAQ What is SWT?")
*   [FAQ Why does Eclipse use SWT?](./FAQ_Why_does_Eclipse_use_SWT.md "FAQ Why does Eclipse use SWT?")
*   [FAQ Can I use SWT outside Eclipse for my own project?](./FAQ_Can_I_use_SWT_outside_Eclipse_for_my_own_project.md "FAQ Can I use SWT outside Eclipse for my own project?")
*   [FAQ How do I configure an Eclipse Java project to use SWT?](./FAQ_How_do_I_configure_an_Eclipse_Java_project_to_use_SWT.md "FAQ How do I configure an Eclipse Java project to use SWT?")
*   [FAQ How do I create an executable JAR file for a stand-alone SWT program?](./FAQ_How_do_I_create_an_executable_JAR_file_for_a_stand-alone_SWT_program.md "FAQ How do I create an executable JAR file for a stand-alone SWT program?")
*   [FAQ Are there any visual composition editors available for SWT?](./FAQ_Are_there_any_visual_composition_editors_available_for_SWT.md "FAQ Are there any visual composition editors available for SWT?")
*   [FAQ Why do I have to dispose of colors, fonts, and images?](./FAQ_Why_do_I_have_to_dispose_of_colors_fonts_and_images.md "FAQ Why do I have to dispose of colors, fonts, and images?")
*   [FAQ Why do I get an invalid thread access exception?](./FAQ_Why_do_I_get_an_invalid_thread_access_exception.md "FAQ Why do I get an invalid thread access exception?")
*   [FAQ How do I get a Display instance?](./FAQ_How_do_I_get_a_Display_instance.md "FAQ How do I get a Display instance?")
*   [FAQ How do I prompt the user to select a file or a directory?](./FAQ_How_do_I_prompt_the_user_to_select_a_file_or_a_directory.md "FAQ How do I prompt the user to select a file or a directory?")
*   [FAQ How do I display a Web page in SWT?](./FAQ_How_do_I_display_a_Web_page_in_SWT.md "FAQ How do I display a Web page in SWT?")
*   [FAQ How do I embed AWT and Swing inside SWT?](./FAQ_How_do_I_embed_AWT_and_Swing_inside_SWT.md "FAQ How do I embed AWT and Swing inside SWT?")
*   [FAQ Where can I find more information on SWT?](./FAQ_Where_can_I_find_more_information_on_SWT.md "FAQ Where can I find more information on SWT?")

### JFace

JFace is a Java application framework based on SWT. The goal of JFace is to provide a set of reusable components that make it easier to write a Java-based GUI application. Among the components JFace provides are such familiar GUI concepts as wizards, preference pages, actions, and dialogs. These components tend to be the bits and pieces that are integral to the basic widget set but are common enough that there is significant benefit to drawing them together into a reusable framework. Although its heritage is based on a long line of frameworks for writing IDEs, most of JFace is generally useful in a broad range of graphical desktop applications. JFace has a few connections to classes in the Eclipse runtime kernel, but it is fairly straightforward to extract JFace and SWT for use in stand-alone Java applications that are not based on the Eclipse runtime. JFace does not make use of such Eclipse-specific concepts as extensions and extension points.

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ What are content and label providers?](./FAQ_What_are_content_and_label_providers.md "FAQ What are content and label providers?")
*   [FAQ What kinds of viewers does JFace provide?](./FAQ_What_kinds_of_viewers_does_JFace_provide.md "FAQ What kinds of viewers does JFace provide?")
*   [FAQ Why should I use a viewer?](./FAQ_Why_should_I_use_a_viewer.md "FAQ Why should I use a viewer?")
*   [FAQ How do I sort the contents of a viewer?](./FAQ_How_do_I_sort_the_contents_of_a_viewer.md "FAQ How do I sort the contents of a viewer?")
*   [FAQ How do I filter the contents of a viewer?](./FAQ_How_do_I_filter_the_contents_of_a_viewer.md "FAQ How do I filter the contents of a viewer?")
*   [FAQ How do I use properties to optimize a viewer?](./FAQ_How_do_I_use_properties_to_optimize_a_viewer.md "FAQ How do I use properties to optimize a viewer?")
*   [FAQ What is a label decorator?](./FAQ_What_is_a_label_decorator.md "FAQ What is a label decorator?")
*   [FAQ How do I use image and font registries?](./FAQ_How_do_I_use_image_and_font_registries.md "FAQ How do I use image and font registries?")
*   [FAQ What is a wizard?](./FAQ_What_is_a_wizard.md "FAQ What is a wizard?")
*   [FAQ How do I specify the order of pages in a wizard?](./FAQ_How_do_I_specify_the_order_of_pages_in_a_wizard.md "FAQ How do I specify the order of pages in a wizard?")
*   [FAQ How can I reuse wizard pages in more than one wizard?](./FAQ_How_can_I_reuse_wizard_pages_in_more_than_one_wizard.md "FAQ How can I reuse wizard pages in more than one wizard?")
*   [FAQ Can I reuse wizards from other plug-ins?](./FAQ_Can_I_reuse_wizards_from_other_plug-ins.md "FAQ Can I reuse wizards from other plug-ins?")
*   [FAQ How do I make my wizard appear in the UI?](./FAQ_How_do_I_make_my_wizard_appear_in_the_UI.md "FAQ How do I make my wizard appear in the UI?")
*   [FAQ How do I run a lengthy process in a wizard?](./FAQ_How_do_I_run_a_lengthy_process_in_a_wizard.md "FAQ How do I run a lengthy process in a wizard?")
*   [FAQ How do I launch the preference page that belongs to my plug-in?](./FAQ_How_do_I_launch_the_preference_page_that_belongs_to_my_plug-in.md "FAQ How do I launch the preference page that belongs to my plug-in?")
*   [FAQ How do I ask a simple yes or no question?](./FAQ_How_do_I_ask_a_simple_yes_or_no_question.md "FAQ How do I ask a simple yes or no question?")
*   [FAQ How do I inform the user of a problem?](./FAQ_How_do_I_inform_the_user_of_a_problem.md "FAQ How do I inform the user of a problem?")
*   [FAQ How do I create a dialog with a details area?](./FAQ_How_do_I_create_a_dialog_with_a_details_area.md "FAQ How do I create a dialog with a details area?")
*   [FAQ How do I set the title of a custom dialog?](./FAQ_How_do_I_set_the_title_of_a_custom_dialog.md "FAQ How do I set the title of a custom dialog?")
*   [FAQ How do I save settings for a dialog or wizard?](./FAQ_How_do_I_save_settings_for_a_dialog_or_a_wizard.md "FAQ How do I save settings for a dialog or wizard?")
*   [FAQ How to decorate a TableViewer or TreeViewer with Columns?](./FAQ_How_to_decorate_a_TableViewer_or_TreeViewer_with_Columns.md "FAQ How to decorate a TableViewer or TreeViewer with Columns?")
*   [FAQ How do I configure my Eclipse project to use stand-alone JFace?](./FAQ_How_do_I_configure_my_Eclipse_project_to_use_stand-alone_JFace.md "FAQ How do I configure my Eclipse project to use stand-alone JFace?")
*   [FAQ How do I deploy a stand-alone JFace application?](./FAQ_How_do_I_deploy_a_stand-alone_JFace_application.md "FAQ How do I deploy a stand-alone JFace application?")

### Generic Workbench

This chapter covers FAQs relating to the generic workbench and its APIs. Workbench is the term used for the generic Eclipse UI. Originally the UI was called the desktop, but because Eclipse was a platform primarily for tools rather than for stationery, workbench was deemed more suitable. In Eclipse 3.0, tools are no longer the sole focus, so the term Rich Client Platform, is starting to creep in as the term for the generic, non-tool-specific UI. After all, people don’t want to play mine sweeper or send e-mails to Mom from such a prosaically named application as a workbench. A rich client, on the other hand, is always welcome at the dinner table.

Many of the important workbench concepts, such as editors, views, and actions, generate enough questions that they deserve their own chapters. This chapter focuses on general questions about integrating your plug-in with the various extension hooks the workbench provides.

*   [FAQ Pages, parts, sites, windows: What is all this stuff?](./FAQ_Pages_parts_sites_windows_What_is_all_this_stuff.md "FAQ Pages, parts, sites, windows: What is all this stuff?")
*   [FAQ How do I find out what object is selected?](./FAQ_How_do_I_find_out_what_object_is_selected.md "FAQ How do I find out what object is selected?")
*   [FAQ How do I find out what view or editor is selected?](./FAQ_How_do_I_find_out_what_view_or_editor_is_selected.md "FAQ How do I find out what view or editor is selected?")
*   [FAQ How do I find the active workbench page?](./FAQ_How_do_I_find_the_active_workbench_page.md "FAQ How do I find the active workbench page?")
*   [FAQ How do I show progress on the workbench status line?](./FAQ_How_do_I_show_progress_on_the_workbench_status_line.md "FAQ How do I show progress on the workbench status line?")
*   [FAQ Why should I use the new progress service?](./FAQ_Why_should_I_use_the_new_progress_service.md "FAQ Why should I use the new progress service?")
*   [FAQ How do I write a message to the workbench status line?](./FAQ_How_do_I_write_a_message_to_the_workbench_status_line.md "FAQ How do I write a message to the workbench status line?")
*   [FAQ How do I create a label decorator declaratively?](./FAQ_How_do_I_create_a_label_decorator_declaratively.md "FAQ How do I create a label decorator declaratively?")
*   [FAQ How do I add label decorations to my viewer?](./FAQ_How_do_I_add_label_decorations_to_my_viewer.md "FAQ How do I add label decorations to my viewer?")
*   [FAQ How do I make the workbench shutdown?](./FAQ_How_do_I_make_the_workbench_shutdown.md "FAQ How do I make the workbench shutdown?")
*   [FAQ How can I use IWorkbenchAdapter to display my model elements?](./FAQ_How_can_I_use_IWorkbenchAdapter_to_display_my_model_elements.md "FAQ How can I use IWorkbenchAdapter to display my model elements?")
*   [FAQ How do I create my own preference page?](./FAQ_How_do_I_create_my_own_preference_page.md "FAQ How do I create my own preference page?")
*   [FAQ How do I use property pages?](./FAQ_How_do_I_use_property_pages.md "FAQ How do I use property pages?")
*   [FAQ How do I open a Property dialog?](./FAQ_How_do_I_open_a_Property_dialog.md "FAQ How do I open a Property dialog?")
*   [FAQ How do I add my wizard to the New, Import, or Export menu categories?](./FAQ_How_do_I_add_my_wizard_to_the_New_Import_or_Export_menu_categories.md "FAQ How do I add my wizard to the New, Import, or Export menu categories?")
*   [FAQ Can I activate my plug-in when the workbench starts?](./FAQ_Can_I_activate_my_plug-in_when_the_workbench_starts.md "FAQ Can I activate my plug-in when the workbench starts?")
*   [FAQ How do I create an image registry for my plug-in?](./FAQ_How_do_I_create_an_image_registry_for_my_plug-in.md "FAQ How do I create an image registry for my plug-in?")
*   [FAQ How do I use images defined by other plug-ins?](./FAQ_How_do_I_use_images_defined_by_other_plug-ins.md "FAQ How do I use images defined by other plug-ins?")
*   [FAQ How do I show progress for things happening in the background?](./FAQ_How_do_I_show_progress_for_things_happening_in_the_background.md "FAQ How do I show progress for things happening in the background?")
*   [FAQ How do I switch from using a Progress dialog to the Progress view?](./FAQ_How_do_I_switch_from_using_a_Progress_dialog_to_the_Progress_view.md "FAQ How do I switch from using a Progress dialog to the Progress view?")
*   [FAQ Can I make a job run in the UI thread?](./FAQ_Can_I_make_a_job_run_in_the_UI_thread.md "FAQ Can I make a job run in the UI thread?")
*   [FAQ Are there any special Eclipse UI guidelines?](./FAQ_Are_there_any_special_Eclipse_UI_guidelines.md "FAQ Are there any special Eclipse UI guidelines?")
*   [FAQ Why do the names of some interfaces end with the digit 2?](./FAQ_Why_do_the_names_of_some_interfaces_end_with_the_digit_2.md "FAQ Why do the names of some interfaces end with the digit 2?")

### Perspectives and Views

This chapter answers questions about two central concepts in the Eclipse Platform UI. Perspectives define the set of actions and parts that appear in a workbench window and specify the initial size and position of views within that window. Views are the draggable parts that make up the bulk of a workbench window’s contents. This chapter does not deal with any specific perspectives or views, but with questions that arise when you implement your own perspectives and views.

*   [FAQ How do I create a new perspective?](./FAQ_How_do_I_create_a_new_perspective.md "FAQ How do I create a new perspective?")
*   [FAQ How can I add my views and actions to an existing perspective?](./FAQ_How_can_I_add_my_views_and_actions_to_an_existing_perspective.md "FAQ How can I add my views and actions to an existing perspective?")
*   [FAQ How do I show a given perspective?](./FAQ_How_do_I_show_a_given_perspective.md "FAQ How do I show a given perspective?")
*   [FAQ What is the difference between a perspective and a workbench page?](./FAQ_What_is_the_difference_between_a_perspective_and_a_workbench_page.md "FAQ What is the difference between a perspective and a workbench page?")
*   [FAQ How do I create fixed views and perspectives?](./FAQ_How_do_I_create_fixed_views_and_perspectives.md "FAQ How do I create fixed views and perspectives?")
*   [FAQ What is a view?](./FAQ_What_is_a_view.md "FAQ What is a view?")
*   [FAQ What is the difference between a view and a viewer?](./FAQ_What_is_the_difference_between_a_view_and_a_viewer.md "FAQ What is the difference between a view and a viewer?")
*   [FAQ How do I create my own view?](./FAQ_How_do_I_create_my_own_view.md "FAQ How do I create my own view?")
*   [FAQ How do I set the size or position of my view?](./FAQ_How_do_I_set_the_size_or_position_of_my_view.md "FAQ How do I set the size or position of my view?")
*   [FAQ Why can't I control when, where, and how my view is presented?](./FAQ_Why_cant_I_control_when_where_and_how_my_view_is_presented.md "FAQ Why can't I control when, where, and how my view is presented?")
*   [FAQ How will my view show up in the Show View menu?](./FAQ_How_will_my_view_show_up_in_the_Show_View_menu.md "FAQ How will my view show up in the Show View menu?")
*   [FAQ How do I make my view appear in the Show In menu?](./FAQ_How_do_I_make_my_view_appear_in_the_Show_In_menu.md "FAQ How do I make my view appear in the Show In menu?")
*   [FAQ How do I add actions to a view's menu and toolbar?](./FAQ_How_do_I_add_actions_to_a_views_menu_and_toolbar.md "FAQ How do I add actions to a view's menu and toolbar?")
*   [FAQ How do I make a view respond to selection changes in another view?](./FAQ_How_do_I_make_a_view_respond_to_selection_changes_in_another_view.md "FAQ How do I make a view respond to selection changes in another view?")
*   [FAQ How does a view persist its state between sessions?](./FAQ_How_does_a_view_persist_its_state_between_sessions.md "FAQ How does a view persist its state between sessions?")
*   [FAQ How do I open multiple instances of the same view?](./FAQ_How_do_I_open_multiple_instances_of_the_same_view.md "FAQ How do I open multiple instances of the same view?")

  

### Generic Editors

In Eclipse, editors are parts that have an associated input inside a workbench window and additional lifecycle methods, such as save and revert. This chapter answers questions about interacting with editors and about writing your own editors, whether they are text based or graphical. See Chapter 15 for a complete treatment of questions about writing your own text-based editors.

*   [FAQ What is the difference between a view and an editor?](./FAQ_What_is_the_difference_between_a_view_and_an_editor.md "FAQ What is the difference between a view and an editor?")
*   [FAQ How do I open an editor programmatically?](./FAQ_How_do_I_open_an_editor_programmatically.md "FAQ How do I open an editor programmatically?")
*   [FAQ How do I open an external editor?](./FAQ_How_do_I_open_an_external_editor.md "FAQ How do I open an external editor?")
*   [FAQ How do I dynamically register an editor to handle a given extension?](./FAQ_How_do_I_dynamically_register_an_editor_to_handle_a_given_extension.md "FAQ How do I dynamically register an editor to handle a given extension?")
*   [FAQ How do I switch to vi or emacs-style key bindings?](./FAQ_How_do_I_switch_to_vi_or_emacs-style_key_bindings.md "FAQ How do I switch to vi or emacs-style key bindings?")
*   [FAQ How do I create my own editor?](./FAQ_How_do_I_create_my_own_editor.md "FAQ How do I create my own editor?")
*   [FAQ How do I enable the Save and Revert actions?](./FAQ_How_do_I_enable_the_Save_and_Revert_actions.md "FAQ How do I enable the Save and Revert actions?")
*   [FAQ How do I enable global actions such as Cut, Paste, and Print in my editor?](./FAQ_How_do_I_enable_global_actions_such_as_Cut_Paste_and_Print_in_my_editor.md "FAQ How do I enable global actions such as Cut, Paste, and Print in my editor?")
*   [FAQ How do I hook my editor to the Back and Forward buttons?](./FAQ_How_do_I_hook_my_editor_to_the_Back_and_Forward_buttons.md "FAQ How do I hook my editor to the Back and Forward buttons?")
*   [FAQ How do I create a form-based editor, such as the plug-in Manifest Editor?](./FAQ_How_do_I_create_a_form-based_editor_such_as_the_plug-in_Manifest_Editor.md "FAQ How do I create a form-based editor, such as the plug-in Manifest Editor?")
*   [FAQ How do I create a graphical editor?](./FAQ_How_do_I_create_a_graphical_editor.md "FAQ How do I create a graphical editor?")
*   [FAQ How do I make an editor that contains another editor?](./FAQ_How_do_I_make_an_editor_that_contains_another_editor.md "FAQ How do I make an editor that contains another editor?")

  

### Actions, Commands, and Activities

This chapter answers questions about creating menu bars, context menus, and tool bars and the actions that fill them. A variety of both declarative and programmatic methods are available for contributing actions to the Eclipse UI and for managing and filtering those actions once they have been defined. This chapter also discusses the various ways to execute the long-running tasks that can be triggered by menu and toolbar actions.

**Actions are currently considered to be inferior to commands. The following information about actions is still left here to help people which are still using actions. If you can you should use commands.**

  

*   [FAQ Actions, commands, operations, jobs: What does it all mean?](./FAQ_Actions_commands_operations_jobs_What_does_it_all_mean.md "FAQ Actions, commands, operations, jobs: What does it all mean?")
*   [FAQ What is an action set?](./FAQ_What_is_an_action_set.md "FAQ What is an action set?")
*   [FAQ How do I make my action set visible?](./FAQ_How_do_I_make_my_action_set_visible.md "FAQ How do I make my action set visible?")
*   [FAQ How do I add actions to the global toolbar?](./FAQ_How_do_I_add_actions_to_the_global_toolbar.md "FAQ How do I add actions to the global toolbar?")
*   [FAQ How do I add menus to the main menu?](./FAQ_How_do_I_add_menus_to_the_main_menu.md "FAQ How do I add menus to the main menu?")
*   [FAQ How do I add actions to the main menu?](./FAQ_How_do_I_add_actions_to_the_main_menu.md "FAQ How do I add actions to the main menu?")
*   [FAQ Why are some actions activated without a target?](./FAQ_Why_are_some_actions_activated_without_a_target.md "FAQ Why are some actions activated without a target?")
*   [FAQ Where can I find a list of existing action group names?](./FAQ_Where_can_I_find_a_list_of_existing_action_group_names.md "FAQ Where can I find a list of existing action group names?")
*   [FAQ What is the difference between a command and an action?](./FAQ_What_is_the_difference_between_a_command_and_an_action.md "FAQ What is the difference between a command and an action?")
*   [FAQ How do I associate an action with a command?](./FAQ_How_do_I_associate_an_action_with_a_command.md "FAQ How do I associate an action with a command?")
*   [FAQ How do I create my own key-binding configuration?](./FAQ_How_do_I_create_my_own_key-binding_configuration.md "FAQ How do I create my own key-binding configuration?")
*   [FAQ How do I provide a keyboard shortcut for my action?](./FAQ_How_do_I_provide_a_keyboard_shortcut_for_my_action.md "FAQ How do I provide a keyboard shortcut for my action?")
*   [FAQ How can I change the name or tooltip of my action?](./FAQ_How_can_I_change_the_name_or_tooltip_of_my_action.md "FAQ How can I change the name or tooltip of my action?")
*   [FAQ How do I hook into global actions, such as Copy and Delete?](./FAQ_How_do_I_hook_into_global_actions_such_as_Copy_and_Delete.md "FAQ How do I hook into global actions, such as Copy and Delete?")
*   [FAQ How do I build menus and toolbars programmatically?](./FAQ_How_do_I_build_menus_and_toolbars_programmatically.md "FAQ How do I build menus and toolbars programmatically?")
*   [FAQ How do I make menus with dynamic contents?](./FAQ_How_do_I_make_menus_with_dynamic_contents.md "FAQ How do I make menus with dynamic contents?")
*   [FAQ What is the difference between a toolbar and a cool bar?](./FAQ_What_is_the_difference_between_a_toolbar_and_a_cool_bar.md "FAQ What is the difference between a toolbar and a cool bar?")
*   [FAQ How to create a context menu & add actions to the same?](./FAQ_How_to_create_a_context_menu_and_add_actions_to_the_same.md "FAQ How to create a context menu & add actions to the same?")
*   [FAQ Can other plug-ins add actions to my part's context menu?](./FAQ_Can_other_plug-ins_add_actions_to_my_parts_context_menu.md "FAQ Can other plug-ins add actions to my part's context menu?")
*   [FAQ How do I add other plug-ins' actions to my menus?](./FAQ_How_do_I_add_other_plug-ins_actions_to_my_menus.md "FAQ How do I add other plug-ins' actions to my menus?")
*   [FAQ What is the purpose of activities?](./FAQ_What_is_the_purpose_of_activities.md "FAQ What is the purpose of activities?")
*   [FAQ How do I add activities to my plug-in?](./FAQ_How_do_I_add_activities_to_my_plug-in.md "FAQ How do I add activities to my plug-in?")
*   [FAQ How do activities get enabled?](./FAQ_How_do_activities_get_enabled.md "FAQ How do activities get enabled?")
*   [FAQ What is the difference between perspectives and activities?](./FAQ_What_is_the_difference_between_perspectives_and_activities.md "FAQ What is the difference between perspectives and activities?")

### Building Your Own Application

Prior to the introduction of RCP, most of the Eclipse community was focused on developing plug-ins for a particular Eclipse application called the workbench. Eclipse, however, has always supported the ability to create your own stand alone applications based on the Eclipse plug-in architecture. Eclipse applications can range from simple headless programs with no user interface to full-blown IDEs. In Eclipse 3.0, the platform began a shift toward giving greater power and flexibility to applications built on the Eclipse infrastructure. This chapter guides you through the process of building your own Eclipse application and explores some of the new Eclipse 3.0 APIs available only to applications.

*   [FAQ What is an Eclipse application?](./FAQ_What_is_an_Eclipse_application.md "FAQ What is an Eclipse application?")
*   [FAQ How do I create an application?](./FAQ_How_do_I_create_an_application.md "FAQ How do I create an application?")
*   [FAQ What is the minimal Eclipse configuration?](./FAQ_What_is_the_minimal_Eclipse_configuration.md "FAQ What is the minimal Eclipse configuration?")
*   [FAQ How do I create a Rich Client application?](./FAQ_How_do_I_create_a_Rich_Client_application.md "FAQ How do I create a Rich Client application?")
*   [FAQ How do I customize the menus in an RCP application?](./FAQ_How_do_I_customize_the_menus_in_an_RCP_application.md "FAQ How do I customize the menus in an RCP application?")
*   [FAQ How do I make key bindings work in an RCP application?](./FAQ_How_do_I_make_key_bindings_work_in_an_RCP_application.md "FAQ How do I make key bindings work in an RCP application?")
*   [FAQ Can I create an application that doesn't have views or editors?](./FAQ_Can_I_create_an_application_that_doesnt_have_views_or_editors.md "FAQ Can I create an application that doesn't have views or editors?")
*   [FAQ How do I specify where application data is stored?](./FAQ_How_do_I_specify_where_application_data_is_stored.md "FAQ How do I specify where application data is stored?")
*   [FAQ Can I create an application that doesn't have a data location?](./FAQ_Can_I_create_an_application_that_doesnt_have_a_data_location.md "FAQ Can I create an application that doesn't have a data location?")
*   [FAQ What is an Eclipse product?](./FAQ_What_is_an_Eclipse_product.md "FAQ What is an Eclipse product?")
*   [FAQ How do I create an Eclipse product?](./FAQ_How_do_I_create_an_Eclipse_product.md "FAQ How do I create an Eclipse product?")
*   [FAQ What is the difference between a product and an application?](./FAQ_What_is_the_difference_between_a_product_and_an_application.md "FAQ What is the difference between a product and an application?")
*   [FAQ How do I distribute my Eclipse offering?](./FAQ_How_do_I_distribute_my_Eclipse_offering.md "FAQ How do I distribute my Eclipse offering?")
*   [FAQ Can I use an installation program to distribute my Eclipse product?](./FAQ_Can_I_use_an_installation_program_to_distribute_my_Eclipse_product.md "FAQ Can I use an installation program to distribute my Eclipse product?")
*   [FAQ Can I install my product as an add-on to another product?](./FAQ_Can_I_install_my_product_as_an_add-on_to_another_product.md "FAQ Can I install my product as an add-on to another product?")

### Productizing an Eclipse Offering

In this chapter, we look at turning an Eclipse configuration into a product. When an Eclipse product is created, the anonymous collection of plug-ins takes on application-specific branding, complete with custom images, splash screen, and launcher. In creating your own product, you typically also need to write an installer and uninstaller and consider how your users will obtain and upgrade your product.

*   [FAQ Where do I find suitable Eclipse logos and wordmarks?](./FAQ_Where_do_I_find_suitable_Eclipse_logos_and_wordmarks.md "FAQ Where do I find suitable Eclipse logos and wordmarks?")
*   [FAQ When do I need to write a plug-in install handler?](./FAQ_When_do_I_need_to_write_a_plug-in_install_handler.md "FAQ When do I need to write a plug-in install handler?")
*   [FAQ How do I support multiple natural languages in my plug-in messages?](./FAQ_How_do_I_support_multiple_natural_languages_in_my_plug-in_messages.md "FAQ How do I support multiple natural languages in my plug-in messages?")
*   [FAQ How do I replace the Eclipse workbench window icon with my own?](./FAQ_How_do_I_replace_the_Eclipse_workbench_window_icon_with_my_own.md "FAQ How do I replace the Eclipse workbench window icon with my own?")
*   [FAQ How do I write my own eclipseexe platform launcher?](./FAQ_How_do_I_write_my_own_eclipseexe_platform_launcher.md "FAQ How do I write my own eclipseexe platform launcher?")
*   [FAQ Who shows the Eclipse splash screen?](./FAQ_Who_shows_the_Eclipse_splash_screen.md "FAQ Who shows the Eclipse splash screen?")
*   [FAQ How can I publish partial upgrades (patches) to my product?](./FAQ_How_can_I_publish_partial_upgrades_(patches)_to_my_product.md "FAQ How can I publish partial upgrades (patches) to my product?")

  

Part III -- The Eclipse IDE Platform
------------------------------------

### Text Editors

The most important purpose of an IDE is to browse and edit code. Therefore, perhaps even more than any other IDE platform, the Eclipse editor framework has grown into a highly evolved, flexible, easy-to-use, and easy-to-extend environment for editing program source files. In this chapter, we look at what support exists for writing editors and how easy it is to plug them into the Eclipse IDE platform.

*   [FAQ What support is there for creating custom text editors?](./FAQ_What_support_is_there_for_creating_custom_text_editors.md "FAQ What support is there for creating custom text editors?")
*   [Where can I find RCP text editor examples?](https://www.eclipse.org/eclipse/platform-text/development/rcp/examples/index.html)
*   [FAQ I'm still confused! How do all the editor pieces fit together?](./FAQ_Im_still_confused_How_do_all_the_editor_pieces_fit_together.md "FAQ I'm still confused! How do all the editor pieces fit together?")
*   [FAQ How do I get started with creating a custom text editor?](./FAQ_How_do_I_get_started_with_creating_a_custom_text_editor.md "FAQ How do I get started with creating a custom text editor?")
*   [FAQ How do I use the text document model?](./FAQ_How_do_I_use_the_text_document_model.md "FAQ How do I use the text document model?")
*   [FAQ What is a document partition?](./FAQ_What_is_a_document_partition.md "FAQ What is a document partition?")
*   [FAQ How do I add Content Assist to my editor?](./FAQ_How_do_I_add_Content_Assist_to_my_editor.md "FAQ How do I add Content Assist to my editor?")
*   [FAQ How do I provide syntax coloring in an editor?](./FAQ_How_do_I_provide_syntax_coloring_in_an_editor.md "FAQ How do I provide syntax coloring in an editor?")
*   [FAQ How do I support formatting in my editor?](./FAQ_How_do_I_support_formatting_in_my_editor.md "FAQ How do I support formatting in my editor?")
*   [FAQ How do I insert text in the active text editor?](./FAQ_How_do_I_insert_text_in_the_active_text_editor.md "FAQ How do I insert text in the active text editor?")
*   [FAQ What is the difference between highlight range and selection?](./FAQ_What_is_the_difference_between_highlight_range_and_selection.md "FAQ What is the difference between highlight range and selection?")
*   [FAQ How do I change the selection on a double-click in my editor?](./FAQ_How_do_I_change_the_selection_on_a_double-click_in_my_editor.md "FAQ How do I change the selection on a double-click in my editor?")
*   [FAQ How do I use a model reconciler?](./FAQ_How_do_I_use_a_model_reconciler.md "FAQ How do I use a model reconciler?")

### Help, Search, and Compare

Admittedly, this chapter covers a number of unrelated components in the Eclipse Platform. They have in common the fact that each is designed as an independent plug-in that can be added to any Eclipse-based application. Although they are at home mostly in IDE applications, these plug-ins can also be inserted into RCP applications when help, search, or compare facilities are needed.

*   [FAQ How do I add help content to my plug-in?](./FAQ_How_do_I_add_help_content_to_my_plug-in.md "FAQ How do I add help content to my plug-in?")
*   [FAQ How do I provide F1 help?](./FAQ_How_do_I_provide_F1_help.md "FAQ How do I provide F1 help?")
*   [FAQ How do I contribute help contexts?](./FAQ_How_do_I_contribute_help_contexts.md "FAQ How do I contribute help contexts?")
*   [FAQ How can I generate HTML and toc.xml files?](./FAQ_How_can_I_generate_HTML_and_toc_xml_files.md "FAQ How can I generate HTML and toc.xml files?")
*   [FAQ How do I write a Search dialog?](./FAQ_How_do_I_write_a_Search_dialog.md "FAQ How do I write a Search dialog?")
*   [FAQ How do I implement a search operation?](./FAQ_How_do_I_implement_a_search_operation.md "FAQ How do I implement a search operation?")
*   [FAQ How do I display search results?](./FAQ_How_do_I_display_search_results.md "FAQ How do I display search results?")
*   [FAQ How can I use and extend the compare infrastructure?](./FAQ_How_can_I_use_and_extend_the_compare_infrastructure.md "FAQ How can I use and extend the compare infrastructure?")
*   [FAQ How do I create a Compare dialog?](./FAQ_How_do_I_create_a_Compare_dialog.md "FAQ How do I create a Compare dialog?")
*   [FAQ How do I create a compare editor?](./FAQ_How_do_I_create_a_compare_editor.md "FAQ How do I create a compare editor?")
*   [FAQ How can I run an infocenter on different servers?](./FAQ_How_can_I_run_an_infocenter_on_different_servers.md "FAQ How can I run an infocenter on different servers?")

### Workspace and Resources API

A program is never written in isolation but instead depends on other code, icons, data, and configuration files. An extendable IDE should provide access to wherever these artifacts are stored. In Eclipse, the artifacts are referred to as resources and are stored in a workspace. The FAQs in this chapter show how resources are managed in a workspace and what API is available to control and track their lifecycle.

*   [FAQ How are resources created?](./FAQ_How_are_resources_created.md "FAQ How are resources created?")
*   [FAQ Can I create resources that don't reside in the file system?](./FAQ_Can_I_create_resources_that_dont_reside_in_the_file_system.md "FAQ Can I create resources that don't reside in the file system?")
*   [FAQ What is the difference between a path and a location?](./FAQ_What_is_the_difference_between_a_path_and_a_location.md "FAQ What is the difference between a path and a location?")
*   [FAQ When should I use refreshLocal?](./FAQ_When_should_I_use_refreshLocal.md "FAQ When should I use refreshLocal?")
*   [FAQ How do I create my own tasks, problems, bookmarks, and so on?](./FAQ_How_do_I_create_my_own_tasks_problems_bookmarks_and_so_on.md "FAQ How do I create my own tasks, problems, bookmarks, and so on?")
*   [FAQ How can I be notified of changes to the workspace?](./FAQ_How_can_I_be_notified_of_changes_to_the_workspace.md "FAQ How can I be notified of changes to the workspace?")
*   [FAQ How do I prevent builds between multiple changes to the workspace?](./FAQ_How_do_I_prevent_builds_between_multiple_changes_to_the_workspace.md "FAQ How do I prevent builds between multiple changes to the workspace?")
*   [FAQ Why should I add my own project nature?](./FAQ_Why_should_I_add_my_own_project_nature.md "FAQ Why should I add my own project nature?")
*   [FAQ Where can I find information about writing builders?](./FAQ_Where_can_I_find_information_about_writing_builders.md "FAQ Where can I find information about writing builders?")
*   [FAQ How do I store extra properties on a resource?](./FAQ_How_do_I_store_extra_properties_on_a_resource.md "FAQ How do I store extra properties on a resource?")
*   [FAQ How can I be notified on property changes on a resource?](./FAQ_How_can_I_be_notified_on_property_changes_on_a_resource.md "FAQ How can I be notified on property changes on a resource?")
*   [FAQ How and when do I save the workspace?](./FAQ_How_and_when_do_I_save_the_workspace.md "FAQ How and when do I save the workspace?")
*   [FAQ How can I be notified when the workspace is being saved?](./FAQ_How_can_I_be_notified_when_the_workspace_is_being_saved.md "FAQ How can I be notified when the workspace is being saved?")
*   [FAQ Where is the workspace local history stored?](./FAQ_Where_is_the_workspace_local_history_stored.md "FAQ Where is the workspace local history stored?")
*   [FAQ How can I repair a workspace that is broken?](./FAQ_How_can_I_repair_a_workspace_that_is_broken.md "FAQ How can I repair a workspace that is broken?")
*   [FAQ What support does the workspace have for team tools?](./FAQ_What_support_does_the_workspace_have_for_team_tools.md "FAQ What support does the workspace have for team tools?")

  

### Workbench IDE

The remaining plug-ins in the Eclipse Platform are truly oriented toward writing development tools. This chapter covers elements of the Eclipse IDE workbench, found in the org.eclipse.ui.ide plug-in. This plug-in includes most of the standard platform views, such as Navigator, Tasks, Problems, Properties, and Bookmark. We also take a quick look at advanced topics, such as writing repository clients and debuggers.

*   [FAQ How do I open an editor on a file in the workspace?](./FAQ_How_do_I_open_an_editor_on_a_file_in_the_workspace.md "FAQ How do I open an editor on a file in the workspace?")
*   [FAQ How do I open an editor on a file outside the workspace?](./FAQ_How_do_I_open_an_editor_on_a_file_outside_the_workspace.md "FAQ How do I open an editor on a file outside the workspace?")
*   [FAQ How do I open an editor on something that is not a file?](./FAQ_How_do_I_open_an_editor_on_something_that_is_not_a_file.md "FAQ How do I open an editor on something that is not a file?")
*   [FAQ Why don't my markers show up in the Tasks view?](./FAQ_Why_dont_my_markers_show_up_in_the_Tasks_view.md "FAQ Why don't my markers show up in the Tasks view?")
*   [FAQ Why don't my markers appear in the editor's vertical ruler?](./FAQ_Why_dont_my_markers_appear_in_the_editors_vertical_ruler.md "FAQ Why don't my markers appear in the editor's vertical ruler?")
*   [FAQ How do I access the active project?](./FAQ_How_do_I_access_the_active_project.md "FAQ How do I access the active project?")
*   [FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?](./FAQ_What_are_IWorkspaceRunnable_IRunnableWithProgress_and_WorkspaceModifyOperation.md "FAQ What are IWorkspaceRunnable, IRunnableWithProgress, and WorkspaceModifyOperation?")
*   [FAQ How do I write to the console from a plug-in?](./FAQ_How_do_I_write_to_the_console_from_a_plug-in.md "FAQ How do I write to the console from a plug-in?")
*   [FAQ How do I prompt the user to select a resource?](./FAQ_How_do_I_prompt_the_user_to_select_a_resource.md "FAQ How do I prompt the user to select a resource?")
*   [FAQ Can I use the actions from the Navigator in my own plug-in?](./FAQ_Can_I_use_the_actions_from_the_Navigator_in_my_own_plug-in.md "FAQ Can I use the actions from the Navigator in my own plug-in?")
*   [FAQ What APIs exist for integrating repository clients into Eclipse?](./FAQ_What_APIs_exist_for_integrating_repository_clients_into_Eclipse.md "FAQ What APIs exist for integrating repository clients into Eclipse?")
*   [FAQ How do I deploy projects to a server and keep the two synchronized?](./FAQ_How_do_I_deploy_projects_to_a_server_and_keep_the_two_synchronized.md "FAQ How do I deploy projects to a server and keep the two synchronized?")
*   [FAQ What is the difference between a repository provider and a team subscriber?](./FAQ_What_is_the_difference_between_a_repository_provider_and_a_team_subscriber.md "FAQ What is the difference between a repository provider and a team subscriber?")
*   [FAQ What is a launch configuration?](./FAQ_What_is_a_launch_configuration.md "FAQ What is a launch configuration?")
*   [FAQ When do I use a launch delegate?](./FAQ_When_do_I_use_a_launch_delegate.md "FAQ When do I use a launch delegate?")
*   [FAQ What is Ant?](./FAQ_What_is_Ant.md "FAQ What is Ant?")
*   [FAQ Why can't my Ant build find javac?](./FAQ_Why_cant_my_Ant_build_find_javac.md "FAQ Why can't my Ant build find javac?")
*   [FAQ How do I add my own external tools?](./FAQ_How_do_I_add_my_own_external_tools.md "FAQ How do I add my own external tools?")
*   [FAQ How do I create an external tool builder?](./FAQ_How_do_I_create_an_external_tool_builder.md "FAQ How do I create an external tool builder?")

  

### Implementing Support for Your Own Language

#### The language already has parsers, compilers, and other services

In this case, you can simply reuse them and skip the parts about writing a compiler, a DOM and so on; and focus in integration of the language into the workbench:

The following FAQ may be relevant:

*   [FAQ How do I run an external builder on my source files?](./FAQ_How_do_I_run_an_external_builder_on_my_source_files.md "FAQ How do I run an external builder on my source files?")
*   [FAQ How do I react to changes in source files?](./FAQ_How_do_I_react_to_changes_in_source_files.md "FAQ How do I react to changes in source files?")
*   [FAQ How do I implement an Eclipse builder?](./FAQ_How_do_I_implement_an_Eclipse_builder.md "FAQ How do I implement an Eclipse builder?")
*   [FAQ Where are project build specifications stored?](./FAQ_Where_are_project_build_specifications_stored.md "FAQ Where are project build specifications stored?")
*   [FAQ How do I add a builder to a given project?](./FAQ_How_do_I_add_a_builder_to_a_given_project.md "FAQ How do I add a builder to a given project?")
*   [FAQ How do I implement an incremental project builder?](./FAQ_How_do_I_implement_an_incremental_project_builder.md "FAQ How do I implement an incremental project builder?")
*   [FAQ How do I handle setup problems for a given builder?](./FAQ_How_do_I_handle_setup_problems_for_a_given_builder.md "FAQ How do I handle setup problems for a given builder?")
*   [FAQ How do I make my compiler incremental?](./FAQ_How_do_I_make_my_compiler_incremental.md "FAQ How do I make my compiler incremental?")
*   [FAQ Language integration phase 3: How do I edit programs?](./FAQ_Language_integration_phase_3_How_do_I_edit_programs.md "FAQ Language integration phase 3: How do I edit programs?")
*   [FAQ How do I write an editor for my own language?](./FAQ_How_do_I_write_an_editor_for_my_own_language.md "FAQ How do I write an editor for my own language?")
*   [FAQ How do I add Content Assist to my language editor?](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?")
*   [FAQ How do I add hover support to my text editor?](./FAQ_How_do_I_add_hover_support_to_my_text_editor.md "FAQ How do I add hover support to my text editor?")
*   [FAQ How do I create problem markers for my compiler?](./FAQ_How_do_I_create_problem_markers_for_my_compiler.md "FAQ How do I create problem markers for my compiler?")
*   [FAQ How do I implement Quick Fixes for my own language?](./FAQ_How_do_I_implement_Quick_Fixes_for_my_own_language.md "FAQ How do I implement Quick Fixes for my own language?")
*   [FAQ How do I support refactoring for my own language?](./FAQ_How_do_I_support_refactoring_for_my_own_language.md "FAQ How do I support refactoring for my own language?")
*   [FAQ How do I create an Outline view for my own language editor?](./FAQ_How_do_I_create_an_Outline_view_for_my_own_language_editor.md "FAQ How do I create an Outline view for my own language editor?")
*   [FAQ Language integration phase 4: What are the finishing touches?](./FAQ_Language_integration_phase_4_What_are_the_finishing_touches.md "FAQ Language integration phase 4: What are the finishing touches?")
*   [FAQ What wizards do I define for my own language?](./FAQ_What_wizards_do_I_define_for_my_own_language.md "FAQ What wizards do I define for my own language?")
*   [FAQ When does my language need its own nature?](./FAQ_When_does_my_language_need_its_own_nature.md "FAQ When does my language need its own nature?")
*   [FAQ When does my language need its own perspective?](./FAQ_When_does_my_language_need_its_own_perspective.md "FAQ When does my language need its own perspective?")
*   [FAQ How do I add documentation and help for my own language?](./FAQ_How_do_I_add_documentation_and_help_for_my_own_language.md "FAQ How do I add documentation and help for my own language?")
*   [FAQ How do I support source-level debugging for my own language?](./FAQ_How_do_I_support_source-level_debugging_for_my_own_language.md "FAQ How do I support source-level debugging for my own language?")

#### This is a DSL of your own

In this case, you should consider using [Xtext](https://www.eclipse.org/Xtext) to define the language. From the language definition, XText will generate the parser, DOM API, Editors, and most integration in the Eclipse IDE (and also in JetBrains tools). Extensive documentation is available on the XText site linked earlier.

#### Legacy

Through its JDT project, Eclipse has strong support for Java development, such as editing, refactoring, building, launching, and debugging. Likewise, the C development tools (CDT) project aims for similar support for writing C/C++ code, PDT for PHP, Phortran for Fortran, JSDT for JavaScript...

This chapter discusses the nuts and bolds with which JDT and CDT are implemented. It is helpful to understand the low level details of how Eclipse works internally. However, as Eclipse Platform has some Common Navigator and Generic Editor, and Eclipse now has the [Xtext](https://www.eclipse.org/Xtext) framework implementing support for your own language has become much easier. So this FAQ can somehow be considered as an **out of date** recommendation, although the technical parts are still valid.

In the following we look at the various ways of integrating with Eclipse: from no integration to a fully integrated language development environment. To structure our discussion, we take a closer look at eScript, an experimental script language developed especially for this book.

Many questions have been addressed in other FAQs in this book and may be somewhat repetitive. However, if you are planning to implement support for your own programming language, this chapter might serve well as a comprehensive overview of how to approach this big task.

Any classification of integration of a new programming language with Eclipse is somewhat arbitrary. We have identified the following degrees of integration of a new programming language, such as eScript, with Eclipse:

*   Phase 1—Compiling code and building projects. To obtain full integration with Eclipse in the area of compilation of programs and build processes for your own language, follow the various steps outlined in the FAQs below.

*   Phase 2—Implementing a DOM. The DOM is an in-memory structural representation of the source code of a program written in your language. Using the structural information contained in the DOM, all kinds of analysis and refactoring tools can be built. **IMPORTANT**: As your language is likely to be used also outside of Eclipse environment, you also need to consider alternative ways to implement the DOM, which aren't tied to Eclipse.

*   Phase 3—editing programs. After writing a compiler, a builder and a DOM; or by reusing existing libraries or services providing necessary features to integrate in the IDE, you are ready to consider all the individual steps to build the ultimate Eclipse editor for your language.

*   Phase 4—Adding the finishing touches. To give your language IDE a professional look, follow the steps outlined in the FAQs below.

If you carefully observe these four phases, you will find that the visual aspects of your language IDE happen late in the process. You will have to do some legwork before you are able to get to the pretty parts. We recommend patience and restraint. Time spent in phases 1 and 2 will be well spent, and once you get to phase 3 and 4, you will be grateful that you followed all the steps we outlined.

*   [FAQ What is eScript?](./FAQ_What_is_eScript.md "FAQ What is eScript?")
*   [FAQ Language integration phase 1: How do I compile and build programs?](./FAQ_Language_integration_phase_1_How_do_I_compile_and_build_programs.md "FAQ Language integration phase 1: How do I compile and build programs?")
*   [FAQ How do I load source files edited outside Eclipse?](./FAQ_How_do_I_load_source_files_edited_outside_Eclipse.md "FAQ How do I load source files edited outside Eclipse?")
*   [FAQ How do I run an external builder on my source files?](./FAQ_How_do_I_run_an_external_builder_on_my_source_files.md "FAQ How do I run an external builder on my source files?")
*   [FAQ How do I implement a compiler that runs inside Eclipse?](./FAQ_How_do_I_implement_a_compiler_that_runs_inside_Eclipse.md "FAQ How do I implement a compiler that runs inside Eclipse?")
*   [FAQ How do I react to changes in source files?](./FAQ_How_do_I_react_to_changes_in_source_files.md "FAQ How do I react to changes in source files?")
*   [FAQ How do I implement an Eclipse builder?](./FAQ_How_do_I_implement_an_Eclipse_builder.md "FAQ How do I implement an Eclipse builder?")
*   [FAQ Where are project build specifications stored?](./FAQ_Where_are_project_build_specifications_stored.md "FAQ Where are project build specifications stored?")
*   [FAQ How do I add a builder to a given project?](./FAQ_How_do_I_add_a_builder_to_a_given_project.md "FAQ How do I add a builder to a given project?")
*   [FAQ How do I implement an incremental project builder?](./FAQ_How_do_I_implement_an_incremental_project_builder.md "FAQ How do I implement an incremental project builder?")
*   [FAQ How do I handle setup problems for a given builder?](./FAQ_How_do_I_handle_setup_problems_for_a_given_builder.md "FAQ How do I handle setup problems for a given builder?")
*   [FAQ How do I make my compiler incremental?](./FAQ_How_do_I_make_my_compiler_incremental.md "FAQ How do I make my compiler incremental?")
*   [FAQ Language integration phase 2: How do I implement a DOM?](./FAQ_Language_integration_phase_2_How_do_I_implement_a_DOM.md "FAQ Language integration phase 2: How do I implement a DOM?")
*   [FAQ How do I implement a DOM for my language?](./FAQ_How_do_I_implement_a_DOM_for_my_language.md "FAQ How do I implement a DOM for my language?")
*   [FAQ How can I ensure that my model is scalable?](./FAQ_How_can_I_ensure_that_my_model_is_scalable.md "FAQ How can I ensure that my model is scalable?")
*   [FAQ Language integration phase 3: How do I edit programs?](./FAQ_Language_integration_phase_3_How_do_I_edit_programs.md "FAQ Language integration phase 3: How do I edit programs?")
*   [FAQ How do I write an editor for my own language?](./FAQ_How_do_I_write_an_editor_for_my_own_language.md "FAQ How do I write an editor for my own language?")
*   [FAQ How do I add Content Assist to my language editor?](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?")
*   [FAQ How do I add hover support to my text editor?](./FAQ_How_do_I_add_hover_support_to_my_text_editor.md "FAQ How do I add hover support to my text editor?")
*   [FAQ How do I create problem markers for my compiler?](./FAQ_How_do_I_create_problem_markers_for_my_compiler.md "FAQ How do I create problem markers for my compiler?")
*   [FAQ How do I implement Quick Fixes for my own language?](./FAQ_How_do_I_implement_Quick_Fixes_for_my_own_language.md "FAQ How do I implement Quick Fixes for my own language?")
*   [FAQ How do I support refactoring for my own language?](./FAQ_How_do_I_support_refactoring_for_my_own_language.md "FAQ How do I support refactoring for my own language?")
*   [FAQ How do I create an Outline view for my own language editor?](./FAQ_How_do_I_create_an_Outline_view_for_my_own_language_editor.md "FAQ How do I create an Outline view for my own language editor?")
*   [FAQ Language integration phase 4: What are the finishing touches?](./FAQ_Language_integration_phase_4_What_are_the_finishing_touches.md "FAQ Language integration phase 4: What are the finishing touches?")
*   [FAQ What wizards do I define for my own language?](./FAQ_What_wizards_do_I_define_for_my_own_language.md "FAQ What wizards do I define for my own language?")
*   [FAQ When does my language need its own nature?](./FAQ_When_does_my_language_need_its_own_nature.md "FAQ When does my language need its own nature?")
*   [FAQ When does my language need its own perspective?](./FAQ_When_does_my_language_need_its_own_perspective.md "FAQ When does my language need its own perspective?")
*   [FAQ How do I add documentation and help for my own language?](./FAQ_How_do_I_add_documentation_and_help_for_my_own_language.md "FAQ How do I add documentation and help for my own language?")
*   [FAQ How do I support source-level debugging for my own language?](./FAQ_How_do_I_support_source-level_debugging_for_my_own_language.md "FAQ How do I support source-level debugging for my own language?")

### Java Development Tool API

From the outset, Eclipse has been used to develop Eclipse itself. The plug-ins that make up Eclipse are written in Java, and the concept of self-hosting has propelled the JDT to their current maturity level. When you are writing your plug-ins, you will also spend considerable time inside the JDT. A full coverage of JDT’s functionality is way beyond the scope of the list of FAQs in this chapter; however, we do focus on topics that are directly related to writing plug-ins and discuss aspects of JDT that warrant a discussion about how they are implemented rather than used.

It is important to realize that JDT itself has been written as a set of plug-ins and receives no special support from the platform. JDT represents a wealth of knowledge and is by far the most elaborate and advanced set of plug-ins in Eclipse. It is definitely worth spending some time to observe how JDT extends the platform and how its own extension points and API have been designed. It is likely that your plug-ins will deploy very similar patterns of extensions, extendibility, and reuse.

Finally, the JDT is a useful set of plug-ins in its own right, but it has also been carefully designed for extension by other plug-ins. By having a published API, it is easy to create new Java projects, generate Java source code, manage Java builds, inspect and analyze Java projects, and implement special refactorings. Refer to Help > Help Contents > JDT Plug-in Developer Guide for extensive documentation and tutorials describing the extension points and API published by JDT. For a comprehensive guide to Java development using Eclipse, see the Java Developers Guide to Eclipse (Addison-Wesley, 2003).

*   [FAQ How do I extend the JDT?](./FAQ_How_do_I_extend_the_JDT.md "FAQ How do I extend the JDT?")
*   [FAQ What is the Java model?](./FAQ_What_is_the_Java_model.md "FAQ What is the Java model?")
*   [FAQ How do I create Java elements?](./FAQ_How_do_I_create_Java_elements.md "FAQ How do I create Java elements?")
*   [FAQ How do I create a Java project?](./FAQ_How_do_I_create_a_Java_project.md "FAQ How do I create a Java project?")
*   [FAQ How do I manipulate Java code?](./FAQ_How_do_I_manipulate_Java_code.md "FAQ How do I manipulate Java code?")
*   [FAQ What is a working copy?](./FAQ_What_is_a_working_copy.md "FAQ What is a working copy?")
*   [FAQ What is a JDOM?](./FAQ_What_is_a_JDOM.md "FAQ What is a JDOM?")
*   [FAQ What is an AST?](./FAQ_What_is_an_AST.md "FAQ What is an AST?")
*   [FAQ How do I create and examine an AST?](./FAQ_How_do_I_create_and_examine_an_AST.md "FAQ How do I create and examine an AST?")
*   [FAQ How do I distinguish between internal and external JARs on the build path?](./FAQ_How_do_I_distinguish_between_internal_and_external_JARs_on_the_build_path.md "FAQ How do I distinguish between internal and external JARs on the build path?")
*   [FAQ How do I launch a Java program?](./FAQ_How_do_I_launch_a_Java_program.md "FAQ How do I launch a Java program?")
*   [FAQ What is JUnit?](./FAQ_What_is_JUnit.md "FAQ What is JUnit?")
*   [FAQ How do I participate in a refactoring?](./FAQ_How_do_I_participate_in_a_refactoring.md "FAQ How do I participate in a refactoring?")
*   [FAQ What is LTK?](./FAQ_What_is_LTK.md "FAQ What is LTK?")

Where to buy the original book
------------------------------

The initial contents for these FAQ pages has come from The Offical Eclipse 3.0 FAQs written by John Arthorne and Chris Laffra.

Permission to publish the FAQ book contents here has been graciously offered by Addison-Wesley, publishers of the official Eclipse Series which wouldn't be possible without the great help from Greg Doench.

The book can be purchased from [Amazon.com](http://www.amazon.com/exec/obidos/ASIN/0321268385)

