# print-job-app
1. Install SBT 0.13
2. Clone project to local and cd to project directory
3. Run command "sbt run"
4. Choose 1 for JobNode
5. Choose 2 for WorkerNode
6. Run command "sbt test" for running test cases

## JobNode
There should be only one job node started. The job node holds only one print job. It listens to messages from worker nodes. Job nodes send print job to the first worker node from which it receives "Workerstarted" message

## WorkerNode
Multiple worker nodes can be started. If the job node is not alive when a worker nodes starts up, the worker node will wait for job node to start. Only one worker node gets the print job from the job node.

