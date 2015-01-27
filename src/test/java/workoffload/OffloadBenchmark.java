package workoffload;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class OffloadBenchmark extends AbstractBenchmark {
	Offload.Node a, b, c, d, e, f, g, h, i, j, k, l, m, n, o;

	@Test
	@BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 1)
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
	}

	@Test
	@BenchmarkOptions(benchmarkRounds = 50, warmupRounds = 1)
	public void testLargishGraph()
		throws Exception {
		a = new Offload.Node(5, 5, false);
		b = new Offload.Node(5, 5, false);
		c = new Offload.Node(5, 5);
		d = new Offload.Node(5, 5);
		e = new Offload.Node(5, 5);
		f = new Offload.Node(5, 5, false);
		g = new Offload.Node(5, 5);
		h = new Offload.Node(5, 5);
		i = new Offload.Node(5, 5);
		j = new Offload.Node(5, 5);
		k = new Offload.Node(5, 5);
		l = new Offload.Node(5, 5);
		m = new Offload.Node(5, 5);
		n = new Offload.Node(5, 5);
		o = new Offload.Node(5, 5);

		a.setEdge(b, 2);
		a.setEdge(e, 2);
		a.setEdge(f, 2);
		b.setEdge(c, 2);
		c.setEdge(d, 2);
		d.setEdge(j, 2);
		e.setEdge(g, 2);
		f.setEdge(g, 2);
		f.setEdge(h, 2);
		g.setEdge(i, 2);
		h.setEdge(i, 2);
		h.setEdge(j, 2);
		i.setEdge(l, 2);
		j.setEdge(k, 2);
		k.setEdge(m, 2);
		k.setEdge(n, 2);
		l.setEdge(m, 2);
		m.setEdge(o, 2);
		n.setEdge(o, 2);

		Offload offload = new Offload(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
		Offload.Result result = offload.optimize(CostModels.responseTime());
	}
}
