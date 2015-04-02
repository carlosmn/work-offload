Optimal Partitioning Algorithm
==============================

This is an implementation in Java of an Optimal Partitioning
Algorithm.

The algorithm takes a weighted graph as input which represents your
application's operations/calculations as the nodes and the
communication between them as the edges. Each node has two costs: the
first is the cost of performing the operation locally (e.g. on a
phone) and the second is the cost of performing it elsewhere (e.g. in
the cloud). The weight of the edges is the communication cost to the
offloaded computation. It is assumed that the communication cost
between operations in each location are negligible.

The result contains information about the costs and reports which
operations should be performed locally and which should be offloaded.

There are different cost models in order to choose what you want to
optimise for (time, energy cost or a balance). You can also create
your custom cost model if these do not satisfy your needs.

In order to avoid adding more work to an operation which is already
already costly, we suggest that you create the graph once at
application startup and update the edge costs if the network situation
changes. The graph can be re-used for multiple calculations.

Create Input
--------------

In order to create the graph, start by creating `Node` objects and
connect them via edges. An /unoffloadable/ node represents an
operation which must be performed on the mobile device. There must be
at least one such node; this is where the algorithm will start
calculating.

These nodes will be the same ones returned by the algorithm, so we
recommend that you give these variables a memorable name related to
their goal, e.g. `takePicture`, `applySepia`, etc.

The units are abstract. The algorithm will work as long as you keep
them consistent.

```java
Offload.Node a, b, c, d, e, f;

//parameters:
//local cost, remote cost, isOffloadable (default: true)
a = new Offload.Node(0, 0, false);
b = new Offload.Node(4, 1);
c = new Offload.Node(8, 2);
d = new Offload.Node(8, 2, false);
e = new Offload.Node(4, 1);
f = new Offload.Node(8, 2);

//parameters:
//target node, transmission cost
a.setEdge(b, 10);
b.setEdge(c, 6);
c.setEdge(d, 5);
d.setEdge(e, 5);
e.setEdge(f, 4);
```

Calculate What to Offload
-------------------------

Once you have created the nodes and edges, create an `Offload` object,
initializing it with the nodes. Choose a cost model and call
`Offload.optimize()` with it. The `CostModels` class contains
constructors for the models implemented by this package.

```java
Offload Offload = new Offload(a, b, c, d, e, f);
Offload.Result result = Offload.optimize(CostModels.responseTime());
```
Some cost models need more information:

```java
//parameters:
//energy consumption while computing, idling, transmitting, omega
result = Offload.optimize(CostModels.energyConsumption(0.9f, 0.3f, 1.3f));
result = Offload.optimize(CostModels.weightedTimeAndEnergy(0.9f, 0.3f, 1.3f, 1f));
```

Getting Results
----------------

The result object contains the results of what to offload and what to
keep local. The `local` and `remote` sets contain the nodes you gave
the `Offload` object in the constructor, so if you gave the nodes
significant names, you can perform a check such as
`result.local.contains(applySepia)`.

```java
//set of nodes which should be calculated locally
Set<Node> local = result.local;

//set of nodes which should be calculated remotely
Set<Node> remote = result.remote;

//cost of performing all computation locally
float originalCost = result.originalCost;

//cost when using the local/remote partitioning of this object
float cost = result.cost;

//saved costs relative to performing computation locally, between 0 and 1.
//savings = 1 - (result.cost / result.originalCost)
float savings = result.savings;
```

Modify an Old Input and Update Edge Costs
------------------------------------------

It is possible to update the edge costs of an input, simply by using
the same setEdge function between different optimize calls. This way
you can reuse the same objects but only update the transmission costs
if your app detects changes in the network situation.

```java
Offload offload = new Offload(a, b, c, d, e, f);
Offload.Result result1 = Offload.optimize(CostModels.responseTime());
a.setEdge(b, 1);
Offload.Result result2 = Offload.optimize(CostModels.responseTime());
```

Implement a Custom Cost Model
---------------------------

If none of the provided cost models satisfy your needs, you can create
your own by implementing the `CostModel` interface and passing it to
`Offload.optimize()`.

```java
static class MyCostModel implements CostModel {
    public void setNodes(final Offload.Node[] nodes){
        ...//if access to nodes is necessary
    }

    public float localCost(float in){
        return in*...//for modifying the local costs
    }

    public float remoteCost(float in){
        return in*...//for modifying the remote costs
    }

    public float transmissionCost(float in){
        return in*...//for modifying the transmission costs
    }
}
```

Output in dot Format
--------------------

We also provide a built-in way to create a string in dot-format which
shows graph before and/or after the optimization. The optimized graph
will be colored – blue for locally computed and red for remotely
computed. You can use tools which understand the 'dot' format to
generate a graph; for example GraphViz which has the `dot` command and
it likely available for your platform.

```java
Offload offload = new Offload(a, b, c, d, e, f);
Offload.Result result = offload.optimize(CostModels.responseTime());
String dotformat = DotExporter.fromResult(result);
```

Or to graph the input

```java
String dotformat = DotExporter.fromNodes(a, b, c, d, e, f)
```
