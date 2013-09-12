package reactor.core

import reactor.function.Consumer
import reactor.function.Supplier
import reactor.function.Suppliers
import reactor.function.support.Pipe
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author Jon Brisbin
 */
class PipeSpec extends Specification {

	def 'Pipes connect a Supplier and a Consumer'() {

		given: 'a Supplier and a Consumer'
		def latch = new CountDownLatch(1)
		def hw = ""
		def supplier = { return "Hello World!" } as Supplier<String>
		def consumer = { String s -> hw = s; latch.countDown() } as Consumer<String>

		when: 'a Pipe is created between the two'
		def pipe = new Pipe(supplier, consumer)

		then: 'the Supplier has notified the Consumer'
		latch.await(1, TimeUnit.SECONDS)
		hw == "Hello World!"

		cleanup:
		pipe.shutdown()

	}

	def 'Pipes aggregate multiple Suppliers into a single Consumer'() {

		given: 'multiple Suppliers'
		def latch = new CountDownLatch(5)
		def suppliers = [
				Suppliers.supplyOnce(1),
				Suppliers.supplyOnce(2),
				Suppliers.supplyOnce(3),
				Suppliers.supplyOnce(4),
				Suppliers.supplyOnce(5)
		]
		def aggregate = Suppliers.collect(suppliers)
		def sum = 0
		def consumer = { Integer i ->
			println "i=$i"
			sum += i
			latch.countDown()
		} as Consumer<Integer>

		when: 'a Pipe is created between the two'
		def pipe = new Pipe(aggregate, consumer)

		then: 'the Supplier has notified the Consumer'
		latch.await(1, TimeUnit.SECONDS)
		sum == 15

	}

}
