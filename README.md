[![Build Status](https://travis-ci.org/Plug4Green/Plug4Green.svg?branch=master)](https://travis-ci.org/Plug4Green/Plug4Green)


Plug4Green
==========

Plug4Green is a software focusing on saving energy in data centres optimizing the ICT resources.
P4G is an energy-aware VM placement algorithm able to compute the placement of the VMs and state of the servers. In particular P4G achieves the VMs consolidation and server state management based on configured SLAs and description of the current configuration of the data center. 
It has been developped within the EU FP7 projects [FIT4Green](http://www.fit4green.eu) and [DC4Cities](http://www.dc4cities.eu).

Install
-------

In a console, type:

    mvn install

To skip the tests, you can type:

    mvn install -DskipTests

Run
---

[This module](https://github.com/Plug4Green/Plug4Green/tree/master/P4GDemo) contains a full procedure to install an OpenStack environment and demontrate the features of Plug4Green.


Copyright
---------

Copyright 2012-2015.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
