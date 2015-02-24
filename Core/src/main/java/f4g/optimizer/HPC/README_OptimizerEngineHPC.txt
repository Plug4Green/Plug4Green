README for OptimizerEngineHPC
Authors: mikko.majanen@vtt.fi, olli.mammela@vtt.fi
16.4.2012

1. Description of the current implementation

Currently all the functionalities of the OptimizerEngineHPC are inside the runGlobalOptimization(model) function that takes 
the FIT4GreenType model as a parameter. Inside runGlobalOptimization(), all the needed information is read from the model,
i.e. all the node and job related information is gathered to three lists: one for servers (List<ServerType> srvList),
one for queued jobs (List<JobType> jobList), and one for running jobs (List<JobType> runningList).

Then the jobList is traversed and checked whether some jobs can be started on some nodes, or perhaps some servers can 
be set to sleep/power off mode. These are done in dequeue() function by using findResources() and checkIdleServers()
functions, respectively.

Finally, actions (start job, power off node) are created (createActionRequest()) and sent to Controller. 
Jobs are started only on idle servers, not sleeping/power off servers, so in order to start a job on 
sleeping/power off node(s), the node is first waken up by the COM, which sends a power on action. 

After that there should be a new runGlobalOptimization() call (now with enough idle nodes for starting the job). 
OptimizerEngineHPC needs runGlobalOptimization() call every time something changes in the data centre, e.g. a job 
stops/starts, or some nodes are set to sleep/power on/off states. 

- Used scheduling algorithm is read from a parameter stored in a file "OptimizerEngineHPC.properties" 
  (located in config directory of the optimizer)
- There are three algorithms available: priority FIFO, backfill first fit and backfill best fit
- The scheduling algorithm should be set by the data centre operator

There are also other parameters, such as:

# Threshold to be used for setting servers to standby / powering off
threshold=50

This information is needed for calculating when it is beneficial to power off a server. Currently the code uses 50 s.

# Boolean variable: whether servers are shut down / set to standby by the optimizer
poweroff=true




