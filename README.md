# print-job-app
1. Install SBT 0.13
2. Clone project to local and cd to project directory
3. From one terminal session run command `sbt "run-main nodes.worker.WorkerNode worker1.conf" ` to start node 1
4. From one terminal session run command `sbt "run-main nodes.worker.WorkerNode worker2.conf" ` to start node 2
5. From one terminal session run command `sbt "run-main nodes.worker.WorkerNode worker3.conf" ` to start node 3
6. Create an file under user.home directory with name job.txt with some content
7. "=======We are started============" will be printed by one of the node


## WorkerNode
1. All nodes started will form an akka actor cluster.
2. Each actor has internal scheduler to trigger file checking action at 10 sec interval
3. If the file named job.txt exists in ${user.home} directory and is none-empty, one of the cluster node will print out "We are started". 
4. The cluster maintains an token, the node acquires the token before checking file and executing print job.

