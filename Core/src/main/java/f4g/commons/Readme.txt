### This file contains directions on how to set up the "local.build.properties" file, related to configuration elements that are local to the pc. ###

INTRODUCTION:
Every "build.xml" file refers to two properties files located in the root of this project (Commons):

- "build.properties": is already in the repository, and contains items that are common to all the projects and that are not dependent on local settings. This file is versioned under SVN and shared among all the developers 

- "local.build.properties": must be created locally, and contains items that are specific to local settings and that must not be shared. For this purpose this file must *not* be put under version control

HOW TO CREATE AND CONFIGURE THE "local.build.properties" file:
A template file, called "local.build.properties.template", is provided into the svn repository as an example.

1) Get the "local.build.properties.template" file from the repository (you will find it in the same folder of this Readme.txt file)
2) Create a new file in the same folder, called "local.build.properties"
3) Paste the contents of the template file into the new file
4) Update the entries according to your local configuration
5) Once created, right click on it from inside Eclipse and select "Team | Add to svn:ignore". From now on, this file will be resident on your pc only and will not be added to the SVN repo. It also will not be overwritten or deleted when you update the project.

