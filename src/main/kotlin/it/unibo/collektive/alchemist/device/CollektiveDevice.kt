package it.unibo.collektive.alchemist.device

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Node.Companion.asPropertyOrNull
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Time
import it.unibo.collektive.ID
import it.unibo.collektive.IntId
import it.unibo.collektive.field.Field
import it.unibo.collektive.messages.AnisotropicMessage
import it.unibo.collektive.messages.InboundMessage
import it.unibo.collektive.messages.IsotropicMessage
import it.unibo.collektive.messages.OutboundMessage
import it.unibo.collektive.networking.Network
import it.unibo.collektive.stack.Path

/**
 * Collektive device in Alchemist.
 * @param P the position type.
 * @property environment the environment.
 * @property node the node.
 * @property retainMessagesFor the time for which messages are retained.
 */
class CollektiveDevice<P>(
    private val environment: Environment<Any, P>,
    override val node: Node<Any>,
    private val retainMessagesFor: Time,
) : NodeProperty<Any>, Network, DistanceSensor where P : Position<P> {

    /**
     * The current time.
     */
    var currentTime: Time = Time.ZERO

    private var validMessages = mapOf<ID, Pair<Time, Map<Path, *>>>()

    private fun receiveMessage(time: Time, from: ID, message: Map<Path, *>) {
        validMessages += from to (time to message)
    }

    override fun cloneOnNewNode(node: Node<Any>) = TODO()

    override fun read(): Set<InboundMessage> {
        return validMessages
            .filter { (_, value) -> value.first + retainMessagesFor >= currentTime }
            .also { validMessages = it }
            .mapValues { (id, value) -> InboundMessage(id, value.second) }
            .values
            .toSet()
    }

    override fun write(messages: Set<OutboundMessage>) {
        messages.forEach { message ->
            when (message) {
                is IsotropicMessage ->
                    environment.getNeighborhood(node)
                        .mapNotNull { it.asPropertyOrNull<Any, CollektiveDevice<P>>() }
                        .forEach { it.receiveMessage(currentTime, message.senderId, message.message) }
                is AnisotropicMessage ->
                    environment.getNeighborhood(node)
                        .mapNotNull { it.asPropertyOrNull<Any, CollektiveDevice<P>>() }
                        .firstOrNull { it.node.id == (message.receiverId as IntId).id }
                        ?.also { it.receiveMessage(currentTime, message.senderId, message.message) }
            }
        }
    }

    override fun distances(): Field<Double> {
        val res: Map<ID, Double> = mapOf(IntId(node.id) to 0.0) +
            environment
                .getNeighborhood(node)
                .associate { IntId(it.id) to environment.getDistanceBetweenNodes(node, it) }
        return Field(IntId(node.id), res)
    }
}
