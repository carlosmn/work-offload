import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.ExpectedException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class OffloadTest {
    @Test
    public void testLinear() {
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

        Offload offload = new Offload(a, b, c, d, e, f);
        Offload.Result result = offload.optimize();

        Assert.assertArrayEquals(result.local.toArray(), new Offload.Node[]{a, b});
        Assert.assertArrayEquals(result.local.toArray(), new Offload.Node[]{c, d, e, f});
    }

    @Test
    public void testResultComparison() {
        Offload.Node a = new Offload.Node(0, 0, false);
        Offload.Node b = new Offload.Node(4, 1);
        Offload.Node c = new Offload.Node(8, 2);
        Offload.Node d = new Offload.Node(8, 2);
        Offload.Node e = new Offload.Node(4, 1);
        Offload.Node f = new Offload.Node(8, 2);

        Offload.Result dummyResult = new Offload.Result();
        dummyResult.local = new HashSet<Offload.Node>(Arrays.asList(a, b));
        dummyResult.remote = new HashSet(Arrays.asList(c, d, e, f));
        Assert.assertEquals(dummyResult.local, new HashSet(Arrays.asList(a, b)));
        Assert.assertEquals(dummyResult.remote, new HashSet(Arrays.asList(c, d, e, f)));

        dummyResult.remote = new HashSet(Arrays.asList(c, d, f, e));
        Assert.assertEquals(dummyResult.remote, new HashSet(Arrays.asList(c, d, f, e)));
    }
}
