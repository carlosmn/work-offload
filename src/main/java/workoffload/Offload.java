package workoffload;

import java.util.*;

/**
 * Implementation of the Optimal Offloading Partitioning Algorithm (Authors: Huaming Wu, Katinka Wolter)
 * 	by Carlos Martín Nieto, Daniel Seidenstücker (both Freie Universität Berlin)
 */

public class Offload {
	static public class Node {
		public final float localCost;
		public final float remoteCost;
		public final boolean offloadable;

		/** contains target edges with corresponding costs */
		List<Edge> edges = new ArrayList<Edge>();

		/**
		 * Create a new node
		 *
		 * @param local local cost of processing (in time)
		 * @param remote remote cost of processing (in time)
		 */
		public Node(int local, int remote) {
			this(local, remote, true);
		}

		/**
		 * Create a new node
		 *
		 * @param local local cost of processing (in time)
		 * @param remote remote cost of processing (in time)
		 * @param offloadable whether this node can be offloaded
		 */
		public Node(int local, int remote, boolean offloadable) {
			this.localCost = local;
			this.remoteCost = remote;
			this.offloadable = offloadable;
		}

		/**
		 * Set the costs for an edge between this node and 'n'.
		 *
		 * If the edge does not exist, it will be created. If it does exist, the
		 * cost will be updated.
		 *
		 * @param n the node to connect to
		 * @param transmissionCost the cost of transmitting data (in time)
		 */
		public void setEdge(Node n, int transmissionCost) {
			for (Edge e : this.edges) {
				if (e.node == n) {
					e.cost = transmissionCost;
					return;
				}
			}

			this.edges.add(new Edge(n, transmissionCost));
		}
	}//class end Node
	
	static public class Edge {
		/** target node */
		public final Node node;
		public float cost;//TODO: eindeutig benennen

		public Edge(Node node, float cost) {
			this.node = node;
			this.cost = cost;
		}
	}//class end Edge

	// this would be a good candidate for optimisation if the processing time
	// gets too expensive
	static class InternalNode {
		/** internal identification number for nodes, beginning at 0 */
		int id;
		float localCost;
		float remoteCost;
		boolean offloadable;
		Node parent;		//corresponding node object

		public InternalNode(int id, Node parent) {
			this.id = id;
			this.localCost = parent.localCost;
			this.remoteCost = parent.remoteCost;
			this.offloadable = parent.offloadable;
			this.parent = parent;
		}

		public InternalNode(InternalNode n) {
			this.id = n.id;
			this.localCost = n.localCost;
			this.remoteCost = n.remoteCost;
			this.offloadable = n.parent.offloadable;//InternalNodes may be merged during algorithm
			this.parent = n.parent;

		}
	}//class end InternalNode
	
	/** a cut is a partitioning of a graph into 2 set of nodes: local calculated (A) and remote calculated (nodes-A) */
	static class Cut {
		/** second to last vertex added to A */
		public final int s;
		/** last vertex added to A */
		public final int t;
		/** growing set of nodes, see algorithm paper */
		public final Set<InternalNode> A;
		/** all nodes as InternalNodes */
		public final InternalNode[] nodes;
		/** edge matric with communication costs (similar to m) */
		public final float[][] graph;
		/** calculated weight of this cut () */
		public final float weight;

		public Cut(Set<InternalNode> A, float[][] graph, float[][] origGraph, InternalNode[] nodes, int s, int t) {
			this.A = A;
			this.graph = graph;
			this.nodes = nodes;
			this.s = s;
			this.t = t;
			this.weight = calculateWeight(origGraph);
		}
		
		/** sum=all localcosts-(t.localCost-t.remoteCost)+communication costs of edges t->graph\{t} */
		float calculateWeight(float[][] m) {
			float sum = 0;

			// The zeroth node contains the sum of all local and remote costs for this cut, so we can simply
			// read from it and then substract the difference between local and remote for 't', which is the
			// one we want to merge in the main graph.
			sum += this.nodes[0].localCost;
			sum -= this.nodes[t].localCost - this.nodes[t].remoteCost;

			// Then we add the cost for each connection which exists on the original graph
			for (int i = 0; i < m.length; i++) {
				float cost = m[t][i];
				if (cost >= 0)//-1 means no connection
					sum += cost;
			}
			return sum;
		}
	}//class end Cut	
	
	static public class Result {
		/** Set of nodes which should be calculated locally */
		Set<Node> local;
		/** set of nodes which should be calculated remotely */
		Set<Node> remote;

		/**
		 * Cost of performing all computation locally
		 */
		float originalCost;
		/**
		 * Cost when using the local/remote partitioning in this object.
		 */
		float cost;

		/**
		 * Saved costs relative to performing computation locally, between 0 and 1.
		 */
		float savings;

		public Result() {
			this.local = new HashSet<Node>();
			this.remote = new HashSet<Node>();
		}
	}//class end Result	

	/** algorithm needs an arbitrary startNode, we always take the first which is unoffloadable */
	InternalNode startNode;
	/** edge matrix with communication costs; symmetric (only undirected edges) */
	float[][] m;
	/** set of all nodes */
	InternalNode[] nodes;
	// User nodes
	final Node userNodes[];
	/**
	 * We keep track of the number of active nodes as we remove merged nodes from the edge matrix
	 * but keep the corresponding rows and columns. This extra book-keeping lets minCutPhase() know
	 * how many nodes there are left which it needs in order to know when it has consumed all the
	 * nodes.
	 */
	int activeNodes;		//counter of active nodes, TODO: geht bestimmt besser

	public Offload(Node... nodes) {
		this.userNodes = nodes;
	}

	float sumLocalCost() {
		float sum = 0;

		for (InternalNode n : this.nodes) {
			sum += n.localCost;
		}

		return sum;
	}

	void internalizeNodes(CostModel model) {
		model.setNodes(this.userNodes);
		this.m = new float[userNodes.length][userNodes.length];
		//filling m initially with -1 (no connection)
		for (float[] arr : this.m) {
			Arrays.fill(arr, -1f);
		}
		this.nodes = new InternalNode[userNodes.length];
		this.activeNodes = userNodes.length;
		Map<Node, Integer> mapping = new HashMap<Node, Integer>();// mapping between an object and our offset for it

		// Create the internal representation of the nodes, which we can modify as needed
		// while keeping a reference to the unmodified input node.
		for (int i = 0; i < userNodes.length; i++) {
			Node n = userNodes[i];
			mapping.put(n, i);
			this.nodes[i] = new InternalNode(i, n);
			this.nodes[i].localCost = model.localCost(this.nodes[i].localCost);
			this.nodes[i].remoteCost = model.remoteCost(this.nodes[i].remoteCost);
		}

		// Go through each outgoing edge and store it as a bidirectional edge in our
		// edge matrix for simpler access.
		for (Node n : userNodes) {
			int i = mapping.get(n);
			for (Edge e : n.edges) {
				int j = mapping.get(e.node);
				this.m[i][j] = model.transmissionCost(e.cost);
				this.m[j][i] = model.transmissionCost(e.cost);
			}
		}
	}

	/**
	 * Optimise the graph according to the given rules (TODO: allow specifying the rules).
	 *
	 * This function is called MinCut in the paper.
	 * 
	 * @return a Result object with sets of nodes which should be computed locally and remotely. These sets contain
	 * the unmodified Nodes given as input.
	 */	
	public Result optimize(CostModel model) throws Exception {
		internalizeNodes(model);

		Result result = new Result();

		List<InternalNode> unoff = findUnoffloadable();
		if (unoff.isEmpty())
			throw new Exception("no unoffloadable nodes");//TODO algo sollte auch ohne unoffloadable funktionieren

		startNode = unoff.get(0);
		// All unoffloadable nodes are merged into a single one, as those can never be
		// remote. We can save some processing by pretending they're a single one.
		for (int j = 1; j < unoff.size(); j++) {
			mergeVertices(m, startNode, unoff.get(j));
			this.activeNodes--;
		}

		result.originalCost = sumLocalCost();

		Cut minCut = null, lastCut = null;

		// Find the minimal cut by storing the one with the lowest cost. We stop iterating when
		// the new cut's set has a single entry, which means that we've processed the whole graph
		do {
			lastCut = minCutPhase(model);
			if (minCut == null || lastCut.weight < minCut.weight) {
				minCut = lastCut;
			}
			mergeVertices(this.m, this.nodes[lastCut.s], this.nodes[lastCut.t]);
			this.activeNodes--;
		} while (lastCut.A.size() > 1);
		
		// Seed the local node set from the unoffloadable list (which are
		// by definition local)
		for (InternalNode n : unoff) {
			result.local.add(n.parent);
		}
		// and then add those from the cut we decided was the optimal one.
		for (InternalNode n : minCut.A) {
			result.local.add(n.parent);
		}

		result.cost = minCut.weight;
		result.savings = 1 - (result.cost / result.originalCost);

		// Every node which is not in the local set is automatically
		// in the remote set.
		for (InternalNode n : this.nodes) {
			if (!result.local.contains(n.parent))
				result.remote.add(n.parent);
		}
		return result;
	}

	List<InternalNode> findUnoffloadable() {
		List<InternalNode> lst = new ArrayList<InternalNode>();
		for (InternalNode n : this.nodes) {
			if (n.offloadable)
				continue;

			lst.add(n);
		}
		return lst;
	}

	/**
	 * Merge vertex t into vertex s, adding together their local, remote, and communication costs.
	 *
	 * @param graph the edge matrix for these nodes.
	 * @param s the node to merge into.
	 * @param t the node to merge into s.
	 */
	static void mergeVertices(float[][] graph, InternalNode s, InternalNode t) {
		//add up the computation costs
		s.localCost += t.localCost;
		s.remoteCost += t.remoteCost;

		//disconnect the vertices to be merged
		graph[s.id][t.id] = -1f;
		graph[t.id][s.id] = -1f;

		// Add t's edges to s and remove them from t. For edges with a common target
		// we add up the costs.
		float tRow[] = graph[t.id];
		for (int i = 0; i < graph.length; i++) {
			float tCost = tRow[i];
			if (tCost == -1f)
				continue;
			
			float sCost = graph[s.id][i];
			float newCost;
			if (sCost == -1f)
				newCost = tCost;
			else
				newCost = sCost + tCost;

			//set the new cost in the edge matrix
			graph[s.id][i] = newCost;
			graph[i][s.id] = newCost;
			//remove the old edge
			graph[t.id][i] = -1f;
			graph[i][t.id] = -1f;
		}
	}

	Cut minCutPhase(CostModel model) {
		//we need to make a copy of the graph, as we are going to merge nodes and
		//we do not want those changes to appear on the main nodes
		float[][] graph = new float[m.length][0];
		for (int i = 0; i < m.length; i++) {
			graph[i] = Arrays.copyOf(m[i], m[i].length);
		}
		//the same goes for the nodes, we want to merge the copies
		InternalNode[] scratchNodes = new InternalNode[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			scratchNodes[i] = new InternalNode(nodes[i]);
		}

		//keep track of which nodes we've already merged
		Set<InternalNode> A = new HashSet<InternalNode>();

		// It's important that this stays at zero. This is the node in which we merge the node
		// we determine to be the most tightly connected node in each iteration. This node then
		// contains the sum of all the local and remote costs.
		int aIdx = 0;
		int s = 0, t = 0;

		A.add(scratchNodes[aIdx]);
		
		// while A =/= V_i
		while (A.size() < this.activeNodes) {
			int vMaxIdx = 0;
			float vMaxGain = Float.NEGATIVE_INFINITY;
			// while v \in V_i and v \not\in A
			for (int i = 0; i < scratchNodes.length; i++) {
				if (A.contains(scratchNodes[i]) || graph[aIdx][i] == -1f) {
					continue;
				}
				InternalNode node = scratchNodes[i];
				float gain = graph[aIdx][i] - (node.localCost - node.remoteCost);
				if (gain > vMaxGain) {
					vMaxGain = gain;
					vMaxIdx = i;
				}
			}

			// vMaxIdx is the most tightly connected vertex to A
			s = t;
			t = vMaxIdx;
			A.add(scratchNodes[vMaxIdx]);
			mergeVertices(graph, scratchNodes[aIdx], scratchNodes[vMaxIdx]);
		}

		A.remove(scratchNodes[t]);

		// return cut(A-t, t), s, t
		return new Cut(A, graph, this.m, scratchNodes, s, t);
	}
}//class end Offload
