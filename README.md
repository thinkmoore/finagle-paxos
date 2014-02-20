Finagle Benchmarks
===============

This repository contains two benchmarks for Finagle RPC on top of Thrift.

Invoking the benchmark commands listed below will install any necessary dependencies and compile the benchmarks using the Scala simple-build-tool (sbt).

Null RPC Goodput
-------------------------

```./sbt 'run-main Benchmark <servers> <requests> <window size> [delay] [-s]'```

This benchmark tests Null RPC throughput. The RPC call invokes a remote function with the Thrift IDL specification ```void foo()```.

It has the following options:

1. ```<servers>``` The number of  servers on which to invoke ```foo()``` per request. A request is successfully processed when all servers have responded.

2. ```<requests>``` The number of requests to process in the run.

3. ```<window size>``` The number of concurrent *requests* to process at a time.

4. ```[delay]``` Optional. The delay in seconds to wait after initializing servers and the client before processing requests.

5. ```[-s]``` Optional. Must specify ```[delay]``` if used. If ```-s``` is present, windows are processed synchronously. That is, all requests in the window must be processed before sending the next window.

Paxos
--------

```./sbt 'run-main Paxos <failures> <rounds> [delay]'```

This benchmark tests the throughput of iterated Paxos. Prepare and accept messages are sent to all Acceptors and a round is finished after all messages are acknowledged.

It has the following options:

1. ```<failures>``` Number of failures that can be tolerated. Benchmark runs with ```2 * failures + 1``` Acceptors and a single Proposer.

2. ```<rounds>``` Number of iterations of Paxos to attempt.

3. ```[delay]``` Optional. The delay in seconds to wait after initializing the Proposer and Acceptors before proposing the first value.
