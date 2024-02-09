

FAQ What is a PDE JUnit test?
=============================

PDE JUnit tests are used to write unit tests for your plug-ins. The tests themselves look exactly like any other JUnit test, organized into TestCase subclasses, one test per method, with setUp and tearDown methods for creating and discarding any state required by the test.

  

  
The difference with PDE JUnit tests is in how they are executed. Instead of using the standard JUnit class TestRunner, PDE JUnit tests are executed by a special test runner that launches another Eclipse instance in a separate VM-just like a runtime workbench-and executes the test methods within that workbench. This means your tests can call Eclipse Platform API, along with methods from your own plug-in, to exercise the functionality you want to test.

  

  
PDE JUnit tests are launched by selecting your test class and pressing **Run > Run As > JUnit Plug-in Test**. On the **Arguments** tab of the launch configuration dialog, you can choose what Eclipse application will be used to run your tests. By default, the Eclipse IDE workbench is used. If you are not testing user-interface components, you can choose **\[No Application\] - Headless Mode**. If you have written your own Eclipse application-such as a rich client application-you will need to write your own test runner application as well.

  

  

  

See Also:
---------

[FAQ\_How\_do\_I\_run\_my\_plug-in\_in\_another\_instance\_of_Eclipse?](./FAQ_How_do_I_run_my_plug-in_in_another_instance_of_Eclipse.md "FAQ How do I run my plug-in in another instance of Eclipse?")

  
[FAQ\_What\_is_JUnit?](./FAQ_What_is_JUnit.md "FAQ What is JUnit?")

