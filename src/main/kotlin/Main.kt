import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main() = runBlocking<Unit> {
    val orders = listOf(Order.Cafe, Order.Cappuccino, Order.Milk)

    val channel = Channel<Order>()
    println("STARTED")

    coroutineScope {
        launch {
            orders.forEach { channel.send(it) }
            channel.close()
        }
        launch(CoroutineName("barista-1")) {
            makeOrder(channel)
        }
        launch(CoroutineName("barista-2")) {
            makeOrder(channel)
        }
    }

    println("END")
}

suspend fun makeOrder(channel: ReceiveChannel<Order>) {
    for (order in channel) {
        log(" -> START Making $order")
        when (order) {
            Order.Cafe -> delay(1000)
            Order.Cappuccino -> delay(2000)
            Order.Milk -> delay(500)
        }
        log(" -> END Making $order")
    }
}

sealed class Order {
    object Cafe : Order()
    object Cappuccino : Order()
    object Milk : Order()
}

class Client {
    fun hello() = "Hello!"
    fun test() = "Test!"
}