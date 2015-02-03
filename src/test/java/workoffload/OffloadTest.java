package workoffload;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.util.Arrays;
import java.util.HashSet;

/**
 *
 */
public class OffloadTest {
    Offload.Node a, b, c, d, e, f;

    @Before
    public void setUp() {
        a = new Offload.Node(0, 0, false);
        b = new Offload.Node(4, 1);
        c = new Offload.Node(8, 2);
        d = new Offload.Node(8, 2, false);
        e = new Offload.Node(4, 1);
        f = new Offload.Node(8, 2);

        a.setEdge(b, 10);
        b.setEdge(c, 6);
        c.setEdge(d, 5);
        d.setEdge(e, 5);
        e.setEdge(f, 4);
    }

    @Test
    public void testLinear()
        throws Exception {
        Offload offload = new Offload(a, b, c, d, e, f);
        Offload.Result result = offload.optimize(CostModels.responseTime());

        Assert.assertEquals(new HashSet(Arrays.asList(a, b, c, d)), result.local);
        Assert.assertEquals(new HashSet(Arrays.asList(e, f)), result.remote);
    }

    @Test
    public void testResultComparison() {
        Offload.Result dummyResult = new Offload.Result();
        dummyResult.local = new HashSet<Offload.Node>(Arrays.asList(a, b));
        dummyResult.remote = new HashSet(Arrays.asList(c, d, e, f));
        Assert.assertEquals(dummyResult.local, new HashSet(Arrays.asList(a, b)));
        Assert.assertEquals(dummyResult.remote, new HashSet(Arrays.asList(c, d, e, f)));

        dummyResult.remote = new HashSet(Arrays.asList(c, d, f, e));
        Assert.assertEquals(dummyResult.remote, new HashSet(Arrays.asList(c, d, f, e)));
    }

    @Test
    public void testPaperGraph()
        throws Exception {
        a = new Offload.Node(0, 0, false);
        b = new Offload.Node(3, 1);
        c = new Offload.Node(3, 1);
        d = new Offload.Node(6, 2);
        e = new Offload.Node(6, 2);
        f = new Offload.Node(9, 3);

        a.setEdge(b, 10);
        b.setEdge(c, 1);
        b.setEdge(d, 2);
        c.setEdge(d, 1);
        c.setEdge(e, 1);
        d.setEdge(e, 2);
        d.setEdge(f, 1);
        e.setEdge(f, 1);

        Offload offload = new Offload(a, b, c, d, e, f);
        Offload.Result result = offload.optimize(CostModels.responseTime());
        Assert.assertEquals(new HashSet(Arrays.asList(a, b)), result.local);
        Assert.assertEquals(new HashSet(Arrays.asList(c, d, e, f)), result.remote);
        Assert.assertEquals(27, result.originalCost, 0);
        Assert.assertEquals(14, result.cost, 0);
        Assert.assertEquals(0.518, result.savings, 0.001);
    }

    @Test
    public void TestEdgeUpdate()
        throws Exception {

        a = new Offload.Node(0, 0, false);
        b = new Offload.Node(3, 1);
        c = new Offload.Node(3, 1);
        d = new Offload.Node(6, 2);
        e = new Offload.Node(6, 2);
        f = new Offload.Node(9, 3);

        a.setEdge(b, 10);
        b.setEdge(c, 1);
        b.setEdge(d, 2);
        c.setEdge(d, 1);
        c.setEdge(e, 1);
        d.setEdge(e, 2);
        d.setEdge(f, 1);
        e.setEdge(f, 1);

        Offload offload = new Offload(a, b, c, d, e, f);
        Offload.Result result = offload.optimize(CostModels.responseTime());
        Assert.assertEquals(new HashSet(Arrays.asList(a, b)), result.local);
        Assert.assertEquals(new HashSet(Arrays.asList(c, d, e, f)), result.remote);
        Assert.assertEquals(27, result.originalCost, 0);
        Assert.assertEquals(14, result.cost, 0);
        Assert.assertEquals(0.481, result.savings, 0.001);

        a.setEdge(b, 1);
        result = offload.optimize(CostModels.responseTime());
        Assert.assertEquals(new HashSet(Arrays.asList(a)), result.local);
        Assert.assertEquals(new HashSet(Arrays.asList(b, c, d, e, f)), result.remote);
        Assert.assertEquals(27, result.originalCost, 0);
        Assert.assertEquals(10, result.cost, 0);
        Assert.assertEquals(0.629, result.savings, 0.001);
    }
}
