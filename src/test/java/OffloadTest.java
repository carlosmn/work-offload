import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class OffloadTest {
    @Test
    public void testLinear() {
        int localCost = 4;
        int remoteCost = 1;

        Offload.Node a = new Offload.Node(0, 0, false);
        Offload.Node b = new Offload.Node(4, 1);
        Offload.Node c = new Offload.Node(8, 2);
        Offload.Node d = new Offload.Node(8, 2);
        Offload.Node e = new Offload.Node(4, 1);
        Offload.Node f = new Offload.Node(8, 2);

        a.addEdge(b, 10);
        b.addEdge(c, 6);
        c.addEdge(d, 5);
        d.addEdge(e, 5);
        e.addEdge(f, 4);

        Offload offload = new Offload(Arrays.asList(new Offload.Node[] {a,b,c,d,e,f}), a);
        List<Offload.Node> result = offload.optimize();
    }
}
