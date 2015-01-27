package workoffload;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OffloadLargeBenchmark extends AbstractBenchmark {

    List<Offload.Node> nodes;

    /**
     * Create a five-node pattern which we can repeat in order to create a large graph
     *
     * @param isFirst whether this is the first call of the function. This is needed
     *                in order to mark which are the initial nodes
     * @return
     */
    Offload.Node[] createNodePattern(boolean isFirst) {
        Offload.Node[] ns = new Offload.Node[5];

        ns[0] = new Offload.Node(5, 4, isFirst);
        ns[1] = new Offload.Node(3, 2, isFirst);
        ns[2] = new Offload.Node(6, 3);
        ns[3] = new Offload.Node(7, 4);
        ns[4] = new Offload.Node(7, 3);

        ns[0].setEdge(ns[1], 2);
        ns[0].setEdge(ns[2], 1);
        ns[1].setEdge(ns[3], 5);
        ns[2].setEdge(ns[3], 8);
        ns[2].setEdge(ns[4], 7);

        return ns;
    }

    @Before
    public void setUp() {
        nodes = new ArrayList<Offload.Node>(150);

        Offload.Node[] lastCreated, currentSet = null;
        for (int i = 0; i < 30; i++) {
            lastCreated = currentSet;
            currentSet = createNodePattern(i == 0);

            // if we do have a previous set (the usual case), connect the nodes at the edges
            if (lastCreated != null) {
                lastCreated[3].setEdge(currentSet[1], 5);
                lastCreated[4].setEdge(currentSet[0], 5);
            }

            nodes.addAll(Arrays.asList(currentSet));
        }
    }

    @Test
    @BenchmarkOptions(benchmarkRounds = 50, warmupRounds = 5)
    public void TestLargeGraph()
    throws Exception {
        Offload offload = new Offload(nodes.toArray(new Offload.Node[nodes.size()]));
        Offload.Result result = offload.optimize(CostModels.responseTime());
    }
}
