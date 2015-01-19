package workoffload;

public interface ICostModel {
	void setNodes(final Offload.Node [] nodes);
	float localCost(float cost);
	float remoteCost(float cost);
	float transmissionCost(float cost);
}
