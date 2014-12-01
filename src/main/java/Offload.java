import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
    }

    // this would be a good candidate for optimisation if the processing time
    // gets too expensive
    static class InternalNode {
        int localCost;
        int remoteCost;

        public InternalNode(int local, int remote) {
            this.localCost = local;
            this.remoteCost = remote;
        }
    }

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
            this.nodes[i] = new InternalNode(n.localCost, n.remoteCost);
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

    /**
     * Optimise the graph according to the given rules (todo: allow specifying the rules).
     *
     * @return a Result. The lists are filled with the (unchanged) nodes specified
     * in the constructor.
     */
    public Result optimize() {
        throw new NotImplementedException();
    }
}
