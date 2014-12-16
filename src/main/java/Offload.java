import java.util.*;

/**
 * Implementation of the Optimal Offloading Partitioning Algorithm (Authors: Huaming Wu, Katinka Wolter) 
 * 	by Carlos Martín Nieto, Daniel Seidenstücker (both Freie Universität Berlin)
 */

public class Offload {
	////////////////////////////////////
	////nested classes//////////////////
	////////////////////////////////////
	static public class Node {
		public final int localCost;
		public final int remoteCost;
		public final boolean offloadable;

		List<Edge> edges = new ArrayList<Edge>();//contains target edges with corresponding costs

		public Node(int local, int remote) {
			this(local, remote, true);
		}

		public Node(int local, int remote, boolean offloadable) {
			this.localCost = local;
			this.remoteCost = remote;
			this.offloadable = offloadable;
		}

		public void addEdge(Node n, int transmissionCost) {
			this.edges.add(new Edge(n, transmissionCost));
		}
	}//class end Node
	
	static public class Edge {
		public final Node node;//target node
		public final int cost;//TODO: eindeutig benennen

		public Edge(Node node, int cost) {
			this.node = node;
			this.cost = cost;
		}
	}//class end Edge
	
	// this would be a good candidate for optimisation if the processing time
	// gets too expensive
	static class InternalNode {
		int id;				//internal identification number for nodes, beginning at 0
		int localCost;
		int remoteCost;
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
	
	//a cut is a partitioning of a graph into 2 set of nodes: local calculated (A) and remote calculated (nodes-A)
	static class Cut {		
		public final int s;					//second to last vertex added to A		
		public final int t;					//last vertex added to A
		public final Set<InternalNode> A; 	//growing set of nodes, see algorithm paper 
		public final InternalNode[] nodes;	//all nodes as InternalNodes
		public final int[][] graph;			//edge matric with communication costs (similar to m)
		public final float weight;			//calculated weight of this cut ()

		public Cut(Set<InternalNode> A, int[][] graph, int[][] origGraph,InternalNode[] nodes, int s, int t) {
			this.A = A;
			this.graph = graph;
			this.nodes = nodes;
			this.s = s;
			this.t = t;
			this.weight = calculateWeight(origGraph);
		}
		
		//sum=all localcosts-(t.localCost-t.remoteCost)+communication costs of edges t->graph\{t} 
		float calculateWeight(int[][] m) {
			float sum = 0;
			for (int i = 0; i < this.nodes.length; i++) {
				sum += this.nodes[i].parent.localCost;
			}
			sum -= this.nodes[t].localCost - this.nodes[t].remoteCost;
			for (int i = 0; i < m.length; i++) {
				int cost = m[t][i];
				if (cost >= 0)//-1 means no connection
					sum += cost;
			}
			return sum;
		}
	}//class end Cut	
	
	static public class Result {
		Set<Node> local;	//set of nodes which should be calculated locally
		Set<Node> remote;	//set of nodes which should be calculated remotely

		public Result() {
			this.local = new HashSet<Node>();
			this.remote = new HashSet<Node>();
		}
	}//class end Result	
	////////////////////////////////////
	////end nested classes//////////////
	////////////////////////////////////
	
	InternalNode startNode;	//algorithm needs an arbitrary startNode, we always take the first which is unoffloadable
	int[][] m;				//edge matrix with communication costs; symmetric (only undirected edges)
	InternalNode[] nodes;	//set of all nodes
	int activeNodes;		//counter of active nodes, TODO: geht bestimmt besser

	public Offload(Node... nodes) {
		this.m = new int[nodes.length][nodes.length];
		//filling m initally with -1 (no connection)
		for (int[] arr : this.m) {
			Arrays.fill(arr, -1);
		}
		this.nodes = new InternalNode[nodes.length];
		this.activeNodes = nodes.length;		
		Map<Node, Integer> mapping = new HashMap<Node, Integer>();// mapping between an object and our offset for it
		
		//filling nodes
		for (int i = 0; i < nodes.length; i++) {
			Node n = nodes[i];
			mapping.put(n, i);
			this.nodes[i] = new InternalNode(i, n);
		}
		//filling m
		for (Node n : nodes) {
			int i = mapping.get(n);
			for (Edge e : n.edges) {
				int j = mapping.get(e.node);
				this.m[i][j] = e.cost;
				this.m[j][i] = e.cost;
			}
		}
	}

	/**
	 * Optimise the graph according to the given rules (TODO: allow specifying the rules).
	 * is called MinCut in the paper
	 * 
	 * @return a Result object with sets of nodes which should be computed locally and remotely. These sets contain
	 * the unmodified Nodes given as input.
	 */	
	public Result optimize() throws Exception {
		Result result = new Result();

		List<InternalNode> unoff = findUnoffloadable();
		if (unoff.isEmpty())
			throw new Exception("no unoffloadable nodes");//TODO algo sollte auch ohne unoffloadable funktionieren

		startNode = unoff.get(0);
		//merge all unoffloadable nodes into one
		for (int j = 1; j < unoff.size(); j++) {
			mergeVertices(m, startNode, unoff.get(j));
			this.activeNodes--;
		}

		Cut minCut = null, lastCut = null;

		//determine optimal cut
		do {
			lastCut = minCutPhase();
			if (minCut == null || lastCut.weight < minCut.weight) {
				minCut = lastCut;
			}
			mergeVertices(this.m, this.nodes[lastCut.s], this.nodes[lastCut.t]);
			this.activeNodes--;
		} while (lastCut.A.size() > 1);
		
		//fill local set in result object
		for (InternalNode n : unoff) {
			result.local.add(n.parent);
		}
		for (InternalNode n : minCut.A) {
			result.local.add(n.parent);
		}
		//fill remote set in result object
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

	//merge 2 vertices (s,t) into one (s) and add local, remote, and communication costs
	static void mergeVertices(int[][] graph, InternalNode s, InternalNode t) {
		//add up the computation costs
		s.localCost += t.localCost;
		s.remoteCost += t.remoteCost;

		//disconnect the vertices to be merged
		graph[s.id][t.id] = -1;
		graph[t.id][s.id] = -1;

		//add communication costs of t to s
		int tRow[] = graph[t.id];
		for (int i = 0; i < graph.length; i++) {
			int tCost = tRow[i];
			if (tCost == -1)
				continue;
			
			int sCost = graph[s.id][i];
			int newCost;
			if (sCost == -1)
				newCost = tCost;
			else
				newCost = sCost + tCost;

			//set the new cost in the edge matrix
			graph[s.id][i] = newCost;
			graph[i][s.id] = newCost;
			//remove the old edge
			graph[t.id][i] = -1;
			graph[i][t.id] = -1;
		}
	}

	Cut minCutPhase() {
		//we need to make a copy of the graph, as we are going to merge nodes and
		//we do not want those changes to appear on the main nodes
		int[][] graph = new int[m.length][0];
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

		int aIdx = 0;
		int s = 0, t = 0;

		A.add(scratchNodes[aIdx]);
		
		// while A =/= V_i
		while (A.size() < this.activeNodes) {
			int vMaxIdx = 0, vMaxGain = Integer.MIN_VALUE;
			// while v \in V_i and v \not\in A
			for (int i = 0; i < scratchNodes.length; i++) {
				if (A.contains(scratchNodes[i]) || graph[aIdx][i] == -1) {
					continue;
				}
				InternalNode node = scratchNodes[i];
				int gain = graph[aIdx][i] - (node.localCost - node.remoteCost);
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
