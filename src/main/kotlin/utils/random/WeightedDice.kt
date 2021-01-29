package utils.random

import java.util.concurrent.ThreadLocalRandom

class WeightedDice<T>(probabilities: List<Pair<T, Double>>) {

    private val sum = probabilities.sumByDouble { it.second }

    private val rangedDistribution = probabilities.let { probs ->
        var binStart = 0.0
        probs.asSequence().sortedBy { it.second }
            .map { it.first to OpenDoubleRange(binStart, it.second + binStart) }
            .onEach { binStart = it.second.endExclusive }
            .toMap()
    }

    fun roll() = ThreadLocalRandom.current().nextDouble(0.0, sum).let {
        rangedDistribution.asIterable().first { rng -> it in rng.value }.key
    }
}