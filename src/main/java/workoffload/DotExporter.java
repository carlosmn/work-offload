package workoffload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Created by carlos on 03/02/15.
 */
public class DotExporter {
	static Dictionary<Offload.Node, String> nodeLabels(Offload.Node[] nodes) {
		Dictionary<Offload.Node, String> nodeLabels = new Hashtable<Offload.Node, String>();
		char label = 'a';
		for (Offload.Node n : nodes) {
			nodeLabels.put(n, String.format("%c", label));
			label++;
		}

		return nodeLabels;
	}

	static String formatCost(Offload.Node node) {
		return String.format("%.2f/%.2f", node.localCost, node.remoteCost);
	}

	public static String fromNodes(Offload.Node... nodes) {

		Dictionary<Offload.Node, String> nodeLabels = nodeLabels(nodes);
		StringBuilder bld = new StringBuilder();

		bld.append("graph workoffload {\n");

		for (Offload.Node n : nodes) {
			bld.append(String.format("%s [label=\"%s\"];\n", nodeLabels.get(n),
					formatCost(n)));
		}

		for (Offload.Node n : nodes) {
			for (Offload.Edge e : n.edges) {
				bld.append(nodeLabels.get(n) + " -- " + nodeLabels.get(e.node) + ";\n");
			}
		}

		// tail for "graph {"
		bld.append("}");

		return bld.toString();
	}

	public static String fromResult(Offload.Result result) {
		Offload.Node[] nodes = new Offload.Node[result.local.size() + result.remote.size()];

		int offset = 0;
		for (Offload.Node n : result.local) {
			nodes[offset] = n;
			offset++;
		}
		for (Offload.Node n : result.remote) {
			nodes[offset] = n;
			offset++;
		}

		Dictionary<Offload.Node, String> nodeLabels = nodeLabels(nodes);
		StringBuilder bld = new StringBuilder();

		bld.append("graph workoffload {\n");

		for (Offload.Node n : nodes) {
			String color = result.remote.contains(n) ? "\"#FF3300\"" : "\"#66CCFF\"";
			bld.append(String.format("%s [label=\"%s\" fillcolor=%s style=filled];\n",
					nodeLabels.get(n), formatCost(n), color));
		}

		for (Offload.Node n : nodes) {
			for (Offload.Edge e : n.edges) {
				bld.append(nodeLabels.get(n) + " -- " + nodeLabels.get(e.node) + ";\n");
			}
		}

		// tail for "graph {"
		bld.append("}");

		return bld.toString();
	}

	public static void main(String[] args)
		throws Exception {
		Offload.Node a = new Offload.Node(0, 0, false);
		Offload.Node b = new Offload.Node(3, 1);
		Offload.Node c = new Offload.Node(3, 1);
		Offload.Node d = new Offload.Node(6, 2);
		Offload.Node e = new Offload.Node(6, 2);
		Offload.Node f = new Offload.Node(9, 3);

		a.setEdge(b, 10);
		b.setEdge(c, 1);
		b.setEdge(d, 2);
		c.setEdge(d, 1);
		c.setEdge(e, 1);
		d.setEdge(e, 2);
		d.setEdge(f, 1);
		e.setEdge(f, 1);

		//System.out.println(fromNodes(a, b, c, d, e, f));

		Offload offload = new Offload(a, b, c, d, e, f);
		Offload.Result result = offload.optimize(CostModels.responseTime());

		System.out.println(fromResult(result));
	}
}
