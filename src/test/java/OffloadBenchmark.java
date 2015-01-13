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

		a.addEdge(b, 10);
		b.addEdge(c, 1);
		b.addEdge(d, 2);
		c.addEdge(d, 1);
		c.addEdge(e, 1);
		d.addEdge(e, 2);
		d.addEdge(f, 1);
		e.addEdge(f, 1);

		Offload offload = new Offload(a, b, c, d, e, f);
		Offload.Result result = offload.optimize();
		Assert.assertEquals(new HashSet(Arrays.asList(a, b)), result.local);
		Assert.assertEquals(new HashSet(Arrays.asList(c, d, e, f)), result.remote);
	}

	@Test
	@BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 1)
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

		a.addEdge(b, 2);
		a.addEdge(e, 2);
		a.addEdge(f, 2);
		b.addEdge(c, 2);
		c.addEdge(d, 2);
		d.addEdge(j, 2);
		e.addEdge(g, 2);
		f.addEdge(g, 2);
		f.addEdge(h, 2);
		g.addEdge(i, 2);
		h.addEdge(i, 2);
		h.addEdge(j, 2);
		i.addEdge(l, 2);
		j.addEdge(k, 2);
		k.addEdge(m, 2);
		k.addEdge(n, 2);
		l.addEdge(m, 2);
		m.addEdge(o, 2);
		n.addEdge(o, 2);

		Offload offload = new Offload(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
		Offload.Result result = offload.optimize();
	}
}
