---
layout: default
title: How to install and configure RJS
permalink: /howto/
nav_order: 2
---


# How to install and configure RJS?

## For RJS users
Download the [RJS package](https://github.com/amdia/rjs/blob/master/rjs.jar) to add it to the library of their project using ROS with Jason.

## For RJS developers
### Download and configuration

	git clone git@github.com:amdia/rjs.git
	export RJS_HOME=<YOUR_PATH>/rjs/
	cd rjs
	./gradlew eclipse <1>

<1> this task sets the right configuration to use Jason with Eclipse

Then, to add the project to Eclipse: **File > Import > Select Existing Projects into Workspace > Next > Browse the RJS directory in Select root directory > Finish**.

An other useful gradle task to clean the generated files:

	./gradlew clean

### To generate a .jar

	cd rjs/bin
	ant jar
	
The .jar is compiled based on the configuration given in the file build.xml.

### To bring modifications to Jason or to use Eclipse debug mode (to be able to have breakpoints in Jason classes)

	git clone -b perso-dev2.6 git@github.com:amdia/jason.git
	cd jason
	./gradlew config // <1> <2>
	./gradlew eclipse // <3>

<1> the config task builds the Jason jar, configure Jason properties file, and place all jars in `build/libs`.
<2> this task also prints out the commands to set up the `JASON_HOME` and `PATH` variables.
<3> this task sets the right configuration to use Jason with Eclipse


- To install the Jason Eclipse plugin, follow the tutorial written by the Jason developers [here](http://jason.sourceforge.net/mini-tutorial/eclipse-plugin/) (it is possible to use Jason projects without it).

- Add the project to Eclipse: see [above here](#download-and-configuration)

- Remove the Jason jar from the RJS build path and add the Jason project to the RJS build path.

- To be sure to commit the right version of the Jason jar while committing RJS, it is possible to use a pre-commit hook. The file `/.githooks/pre-commit` should be copy-paste in `/.git/hooks/`.





