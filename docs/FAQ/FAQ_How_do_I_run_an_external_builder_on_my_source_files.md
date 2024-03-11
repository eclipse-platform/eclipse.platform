

FAQ How do I run an external builder on my source files?
========================================================

Use the default Eclipse text editor to edit the eScript source files, and use an Ant script to compile the eScript source into Java bytecodes. Ant scripts are XML-based scripts that can be used to automate certain build processes. You could view them as a much more flexible incarnation of _Make_.

A simple Ant script (build.xml) may look like this:

		<?xml version="1.0" encoding="UTF-8"?>
		<project name="eScript" default="compile" basedir=".">
		<target name="compile">
			<exec executable="eScriptc.exe" dir="src">
				<arg value="-cp"/>
				<arg value="... long classpath specifier ..."/>
				<arg value="EscriptPlugin/SampleAction.eScript"/>
			</exec>
			<copy file="src/EscriptPlugin/SampleAction.class" 
				todir="bin/EscriptPlugin/actions/"/>
			<eclipse.convertPath 
						fileSystemPath="c:\\faq\\Escript Plugin\\" 
				property="resourcePath"/>
			<eclipse.refreshLocal resource="${resourcePath}" 
				depth="infinite"/>
			</target>
		</project>

Of course, this script can be made more elegant, but it serves to highlight the main problems with the approach. First, we have to compute the project's classpath, which can be quite complex for a plug-in, and pass it to the eScript compiler. Second, we have to explicitly pass in the name of the source file. Third, we need to replicate the JDT's behavior by copying the resulting class file to the project's bin directory. Finally, we have to refresh the workspace so that Eclipse gets notified of the changes in the class files and can rebuild dependant components in the workspace.

It is not easy to discover structure about Eclipse's installed plug-ins from outside Eclipse, so compilation of eScript source files becomes a real challenge when done outside Eclipse. Perhaps most troublesome is that each time the source changes, the user has to manually run Ant on the build.xml file. This spins off a new compiler, which has to load the entire classpath to do name and symbol resolution, and the compilation process becomes quite noticeable and annoying after a while.

But even with these limitations, this approach is viable for developers who do not want to write a new plug-in to support their language. Using the Eclipse support for launching external tools and Ant scripts, eScript files can be edited and compiled without having to leave the IDE.

See Also:
---------

*   [FAQ\_What\_is_Ant?](./FAQ_What_is_Ant.md "FAQ What is Ant?")
*   [FAQ\_How\_do\_I\_add\_my\_own\_external\_tools?](./FAQ_How_do_I_add_my_own_external_tools.md "FAQ How do I add my own external tools?")
*   [FAQ\_How\_do\_I\_implement\_a\_compiler\_that\_runs\_inside\_Eclipse?](./FAQ_How_do_I_implement_a_compiler_that_runs_inside_Eclipse.md "FAQ How do I implement a compiler that runs inside Eclipse?")

