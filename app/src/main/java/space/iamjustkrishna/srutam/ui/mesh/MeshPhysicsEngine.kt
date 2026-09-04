package space.iamjustkrishna.srutam.ui.mesh

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.math.min

/**
 * Spring-mass physics simulation for interactive 2D node graph.
 * Handles node dragging, Hooke spring connectivity, collision repulsion,
 * and velocity damping.
 */
class MeshPhysicsEngine(
    val graphData: MeshGraphData
) {
    // Dynamic positions in world coordinates
    val positions = mutableStateMapOf<String, Offset>()
    // Velocity vectors in world coordinates
    private val velocities = mutableMapOf<String, Offset>()
    // Equilibrium spring rest lengths for each edge
    private val restLengths = mutableMapOf<String, Float>()

    init {
        reset()
    }

    /**
     * Resets positions to initial builder coordinates and clears velocities.
     */
    fun reset() {
        positions.clear()
        velocities.clear()
        restLengths.clear()

        graphData.nodes.forEach { node ->
            positions[node.id] = Offset(node.x, node.y)
            velocities[node.id] = Offset.Zero
        }

        graphData.edges.forEach { edge ->
            val from = graphData.nodeMap[edge.fromId]
            val to = graphData.nodeMap[edge.toId]
            if (from != null && to != null) {
                val naturalDist = hypot(from.x - to.x, from.y - to.y)
                restLengths[edge.id] = naturalDist.coerceIn(80f, 420f)
            }
        }
    }

    /**
     * Direct update for the node currently being dragged by user touch.
     */
    fun updateDraggedNode(nodeId: String, newWorldPos: Offset) {
        if (newWorldPos.x.isNaN() || newWorldPos.y.isNaN() ||
            newWorldPos.x.isInfinite() || newWorldPos.y.isInfinite()
        ) return

        positions[nodeId] = newWorldPos
        velocities[nodeId] = Offset.Zero
    }

    /**
     * Executes one tick of the physical simulation.
     * @param draggedNodeId Node currently held by user finger (pinned, skips velocity integration).
     * @param dt Elapsed frame time in seconds (clamped for numerical stability).
     * @return true if the system still possesses kinetic energy, false when settled to rest.
     */
    fun step(draggedNodeId: String?, dt: Float): Boolean {
        val safeDt = dt.coerceIn(0.005f, 0.033f)
        var totalKineticEnergy = 0f

        val springK = 7.5f // Hooke elasticity coefficient
        val damping = 0.86f // Velocity decay / viscous drag
        val repulsionK = 1400f // Soft collision repulsion
        val homeTetherK = 0.30f // Gentle centering pull towards base layout

        val forces = mutableMapOf<String, Offset>()
        graphData.nodes.forEach { node ->
            forces[node.id] = Offset.Zero
        }

        // 1. Hooke Spring Forces along Connected Edges
        graphData.edges.forEach { edge ->
            val pA = positions[edge.fromId] ?: return@forEach
            val pB = positions[edge.toId] ?: return@forEach
            val rest = restLengths[edge.id] ?: 130f

            val dx = pB.x - pA.x
            val dy = pB.y - pA.y
            val dist = hypot(dx, dy).coerceAtLeast(0.5f)
            val displacement = dist - rest

            // Clamp spring force to prevent extreme impulses
            val fMag = (springK * displacement).coerceIn(-600f, 600f)
            val fx = (dx / dist) * fMag
            val fy = (dy / dist) * fMag

            val fA = forces[edge.fromId] ?: Offset.Zero
            val fB = forces[edge.toId] ?: Offset.Zero
            forces[edge.fromId] = Offset(fA.x + fx, fA.y + fy)
            forces[edge.toId] = Offset(fB.x - fx, fB.y - fy)
        }

        // 2. Collision / Soft Repulsion between Neighboring Nodes
        val nodes = graphData.nodes
        val count = nodes.size
        for (i in 0 until count) {
            val nodeA = nodes[i]
            val pA = positions[nodeA.id] ?: continue
            for (j in i + 1 until count) {
                val nodeB = nodes[j]
                val pB = positions[nodeB.id] ?: continue

                val dx = pB.x - pA.x
                val dy = pB.y - pA.y
                val dist = hypot(dx, dy).coerceAtLeast(0.5f)
                val minDist = (nodeA.radius + nodeB.radius) + 14f

                if (dist < minDist) {
                    val overlap = minDist - dist
                    val repForce = min(250f, (overlap / dist) * 16f)
                    val rfx = (dx / dist) * repForce
                    val rfy = (dy / dist) * repForce

                    val fA = forces[nodeA.id] ?: Offset.Zero
                    val fB = forces[nodeB.id] ?: Offset.Zero
                    forces[nodeA.id] = Offset(fA.x - rfx, fA.y - rfy)
                    forces[nodeB.id] = Offset(fB.x + rfx, fB.y + rfy)
                }
            }
        }

        // 3. Anchor Tether to Original Builder Position
        graphData.nodes.forEach { node ->
            val p = positions[node.id] ?: return@forEach
            val f = forces[node.id] ?: Offset.Zero
            val hx = (node.x - p.x) * homeTetherK
            val hy = (node.y - p.y) * homeTetherK
            forces[node.id] = Offset(f.x + hx, f.y + hy)
        }

        // 4. Numerical Integration (Euler with Viscous Damping)
        graphData.nodes.forEach { node ->
            if (node.id == draggedNodeId) {
                // Pin dragged node to touch
                velocities[node.id] = Offset.Zero
                return@forEach
            }

            val f = forces[node.id] ?: Offset.Zero
            val v = velocities[node.id] ?: Offset.Zero
            val mass = if (node.kind == MeshNodeKind.RECORDING) 2.4f else 1.0f

            val newVx = (v.x + (f.x / mass) * safeDt) * damping
            val newVy = (v.y + (f.y / mass) * safeDt) * damping

            val speedSq = newVx * newVx + newVy * newVy
            totalKineticEnergy += speedSq

            if (speedSq < 0.03f) {
                velocities[node.id] = Offset.Zero
            } else {
                velocities[node.id] = Offset(newVx, newVy)
                val p = positions[node.id] ?: Offset(node.x, node.y)
                positions[node.id] = Offset(p.x + newVx * safeDt, p.y + newVy * safeDt)
            }
        }

        return totalKineticEnergy > 0.4f
    }
}
