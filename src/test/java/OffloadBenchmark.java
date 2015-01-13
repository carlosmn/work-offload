import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class OffloadBenchmark extends AbstractBenchmark {
	Offload.Node a, b, c, d, e, f;

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
}
