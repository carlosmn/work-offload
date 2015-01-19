package workoffload;


/**
 * Cost model to optimize for
 */
public class CostModels {
	static public CostModel responseTime() {
		return new ResponseTime();
	}

	static public CostModel energyConsumption(float computing, float idling, float transmitting) {
		return new EnergyConsumption(computing, idling, transmitting);
	}

	static public CostModel weightedTimeAndEnergy(float computing, float idling, float transmitting, float omega) {
		return new WeightedTimeEnergy(computing, idling, transmitting, omega);
	}

	static class ResponseTime implements CostModel {
		public void setNodes(Offload.Node[] nodes) {
			// noop
		}

		public float localCost(float in) {
			return in;
		}

		public float remoteCost(float in) {
			return in;
		}

		public float transmissionCost(float in) {
			return in;
		}
	}

	static class EnergyConsumption implements CostModel {
		float computing, idling, transmitting;

		EnergyConsumption(float computing, float idling, float transmitting) {
			this.computing = computing;
			this.idling = idling;
			this.transmitting = transmitting;
		}

		public void setNodes(Offload.Node[] nodes) {
			// noop
		}

		public float localCost(float in) {
			return in * this.computing;
		}

		public float remoteCost(float in) {
			return in * this.idling;
		}

		public float transmissionCost(float in) {
			return in * this.transmitting;
		}
	}

	static class WeightedTimeEnergy implements CostModel {
		float computing, idling, transmitting, omega;
		float localSum;

		public WeightedTimeEnergy(float computing, float idling, float transmitting, float omega) {
			this.computing = computing;
			this.idling = idling;
			this.transmitting = transmitting;
			this.omega = omega;
		}

		public void setNodes(final Offload.Node[] nodes) {
			this.localSum = 0;
			for (Offload.Node n : nodes) {
				this.localSum += n.localCost;
			}
		}

		float ensureNumber(float val) {
			if (Float.isNaN(val))
				return Float.MAX_VALUE;

			return val;
		}

		public float localCost(float in) {
			float val = (omega * in / localSum) +
						((1 - omega) * in * this.computing / (localSum * this.computing));

			return ensureNumber(val);
		}

		public float remoteCost(float in) {
			float val = (omega * in / localSum) +
					((1 - omega) * in * this.idling / (localSum * this.computing));

			return ensureNumber(val);
		}

		public float transmissionCost(float in) {
			float val = (omega * in / localSum) +
					((1 - omega) * in * this.transmitting / (localSum * this.computing));

			return ensureNumber(val);
		}
	}

}
