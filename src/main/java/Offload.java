import java.util.*;

/**
 *
 */
public class Offload {
    static public class Edge {
        public final Node node;
        public final int cost;

        public Edge(Node node, int cost) {
            this.node = node;
            this.cost = cost;
        }
    }

    static public class Node {
        public final int localCost;
        public final int remoteCost;
        public final boolean offloadable;

        List<Edge> edges = new ArrayList<Edge>();

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
    }

    static public class Result {
        Set<Node> local;
        Set<Node> remote;

        public Result() {
            this.local = new HashSet<Node>();
            this.remote = new HashSet<Node>();
        }
    }

    // this would be a good candidate for optimisation if the processing time
    // gets too expensive
    static class InternalNode {
        int id;
        int localCost;
        int remoteCost;
        boolean offloadable;
        Node parent;
        List<InternalNode> merged = new ArrayList<InternalNode>();

        public InternalNode(int id, Node parent) {
            this.id = id;
            this.localCost = parent.localCost;
            this.remoteCost = parent.remoteCost;
            this.offloadable = parent.offloadable;
            this.parent = parent;
        }
    }

    static class Cut {
        // second to last vertex added to A
        public final int s;
        // last vertex added to A
        public final int t;
        public final Set<InternalNode> A;
        public final InternalNode[] nodes;
        public final int[][] graph;

        public Cut(Set<InternalNode> A, int[][] graph, InternalNode[] nodes, int s, int t) {
            this.A = A;
            this.graph = graph;
            this.nodes = nodes;
            this.s = s;
            this.t = t;
        }
    }

    InternalNode startNode;
    /* Edge matrix with transmission costs */
    int[][] m;
    InternalNode[] nodes;

    public Offload(Node... nodes) {
        this.m = new int[nodes.length][nodes.length];
        // fill the matrix with -1 to indicate no connection, we will fill this
        // with cost values later
        for (int[] arr : this.m) {
            Arrays.fill(arr, -1);
        }
        this.nodes = new InternalNode[nodes.length];
        // mapping between an object and our offset for it
        Map<Node, Integer> mapping = new HashMap<Node, Integer>();

        for (int i = 0; i < nodes.length; i++) {
            Node n = nodes[i];
            mapping.put(n, i);
            this.nodes[i] = new InternalNode(i, n);
        }

        for (Node n : nodes) {
            int i = mapping.get(n);
            for (Edge e : n.edges) {
                int j = mapping.get(e.node);
                this.m[i][j] = e.cost;
                this.m[j][i] = e.cost;
            }
        }
    }

    public Set<Node> getStartNodes() {
        Set<Node> lst = new HashSet<Node>();
        lst.add(startNode.parent);
        for (InternalNode n : startNode.merged) {
            lst.add(n.parent);
        }

        return lst;
    }

    /**
     * Optimise the graph according to the given rules (todo: allow specifying the rules).
     *
     * @return a Result. The lists are filled with the (unchanged) nodes specified
     * in the constructor.
     */
    public Result optimize()
        throws Exception {
        Result result = new Result();
        List<InternalNode> unoff = findUnoffloadable();
        if (unoff.isEmpty())
            throw new Exception("no unoffloadable nodes");

        startNode = unoff.get(0);
        for (int j = 1; j < unoff.size(); j++) {
            mergeVertices(m, startNode, unoff.get(j));
        }

        result.local.addAll(getStartNodes());
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

    static void mergeVertices(int[][] graph, InternalNode s, InternalNode t) {
        // the computation cost is added up
        s.localCost += t.localCost;
        s.remoteCost += t.remoteCost;

        // these two nodes are no longer connected
        graph[s.id][t.id] = -1;
        graph[t.id][s.id] = -1;

        int tRow[] = graph[t.id];
        for (int i = 0; i < graph.length; i++) {
            int tCost = tRow[i];
            if (tCost == -1)
                continue;

            // handle the -1 as well as a set cost from s
            int sCost = graph[s.id][i];
            int newCost;
            if (sCost == -1)
                newCost = tCost;
            else
                newCost = sCost + tCost;

            // set the new cost in the edge matrix
            graph[s.id][i] = newCost;
            graph[i][s.id] = newCost;
            // and remove the old edge
            graph[t.id][i] = -1;
            graph[i][t.id] = -1;
        }

        s.merged.add(t);
    }

    Cut minCutPhase() {
        // we need to make a copy of the graph, as we are going to merge nodes and
        // we do not want those changes to appear on the main node
        int[][] graph = new int[m.length][0];
        for (int i = 0; i < m.length; i++) {
            graph[i] = Arrays.copyOf(m[i], m[i].length);
        }
        // the same goes for the nodes, we want to merge the copies
        InternalNode[] scratchNodes = new InternalNode[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            scratchNodes[i] = new InternalNode(nodes[i].id, nodes[i].parent);
        }

        // keep track of which nodes we've already merged
        Set<InternalNode> A = new HashSet<InternalNode>();

        int aIdx = 0;
        int s = 0, t = 0;

        A.add(scratchNodes[aIdx]);
        // while A =/= V_i
        while (A.size() < scratchNodes.length) {
            int vMaxIdx = 0, vMaxGain = 0;
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

        //return cut(A-t, t), s, t
        return new Cut(A, graph, scratchNodes, s, t);
    }
}
