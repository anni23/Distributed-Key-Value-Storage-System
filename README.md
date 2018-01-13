# Distributed-Key-Value-Storage-System
Implemented Amazon's Dynamo style Distributed Key-Value Storage System with functionalities such as Load Balancing, Replication and Failure Handling in Android.

There are three main parts in this project 
1)	Partitioning 
2)	Replication
3)	Failure Handling

Partitioning – Partitioning deals with distributing the key – value pairs among the nodes in the system. The idea of consistent hashing is used to implement partitioning.
A Hash function like SHA is used and the id space of the hash function is partitioned among several nodes by mapping the nodes and keys to same id space.
A key is stored at its successor node id to map the keys to the nodes, so that we know which nodes are responsible for resolving the lookups for a particular key.
Consistent hashing provides load balancing and flexibility when a node joins or leaves. 

Replication – Every key value pair to be inserted into a node is also replicated in next two successor nodes.

Failure Handling – After failure, a node recovers by getting the missed key value pairs from its replicas.
