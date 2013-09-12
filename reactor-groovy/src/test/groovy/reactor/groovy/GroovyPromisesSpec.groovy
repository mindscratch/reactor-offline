/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package reactor.groovy

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import reactor.core.Environment
import reactor.core.composable.Deferred
import reactor.core.composable.Promise
import reactor.core.composable.spec.Promises
import reactor.event.dispatch.BlockingQueueDispatcher
import spock.lang.Shared
import spock.lang.Specification
/**
 * @author Stephane Maldini
 */
class GroovyPromisesSpec extends Specification {

	@Shared def testEnv

	void setupSpec(){
		testEnv = new Environment()
		testEnv.addDispatcher('eventLoop',new BlockingQueueDispatcher('eventLoop', 256))
	}

	def "Promise returns value"() {
		when: "a deferred Promise"
		def p = Promises.success("Hello World!").get()

		then: 'Promise contains value'
		p.get() == "Hello World!"
	}


	def "Promise from Closure"() {
		when: "a deferred Promise"
		Promise<String> p = Promises.task{"Hello World!"}.get()

		then: 'Promise contains value'
		p.await() == "Hello World!"

		when: "a deferred Promise"
		p = Promise.<String>from{"Hello World!"}.get()

		then: 'Promise contains value'
		p.await() == "Hello World!"
	}

	def "Compose from Closure"() {
		when:
			'Defer a composition'
			def c = Promises.task { sleep 500; 1 } get()

		and:
			'apply a transformation'
			def d = c | { it + 1 }

		then:
			'Composition contains value'
			d.await() == 2
	}

	def "Promise notifies of Failure"() {
		when: "a deferred failed Promise"
		Promise p = Promises.error(new Exception("Bad code! Bad!")).get()

		and: "invoke result"
		p.get()

		then:
		p.error
		thrown(RuntimeException)

		when: "a deferred failed Promise with runtime exception"
			 p = Promises.error(new IllegalArgumentException("Bad code! Bad!")).get()

		and: "invoke result"
			p.get()

		then:
			p.error
			thrown(IllegalArgumentException)
	}

	def "Promises can be mapped"() {
		given: "a synchronous promise"
		Deferred p = Promises.defer().get()

		when: "add a mapping closure"
		Promise s = p | { Integer.parseInt it }

		and: "setting a value"
		p << '10'

		then:
		s.get() == 10

		when: "add a mapping closure"
		p = Promises.defer().get()
		s = p.compose().then { Integer.parseInt it }

		and: "setting a value"
		p << '10'

		then:
		s.get() == 10
	}

	def "A promise can be be consumed by another promise"() {
		given: "two synchronous promises"
		Deferred p1 = Promises.defer().get()
		Deferred p2 = Promises.defer().get()

		when: "p1 is consumed by p2"
		p1 << p2 //p1.consume p2

		and: "setting a value"
		p1 << 'Hello World!'

		then: 'P2 consumes the value when P1'
		p2.compose().get() == 'Hello World!'
	}



	def "Errors stop compositions"() {
		given: "a promise"
		Deferred p = Promises.defer().env(testEnv).dispatcher('eventLoop').get()
		final latch = new CountDownLatch(1)

		when: "p1 is consumed by p2"
		Promise s = p.compose().then{ Integer.parseInt it }.
				when (NumberFormatException, { latch.countDown() }).
				then{ println('not in log'); true }

		and: "setting a value"
		p << 'not a number'
		s.await(2000, TimeUnit.MILLISECONDS)

		then: 'No value'
		thrown(NumberFormatException)
		latch.count == 0
	}

	def "Promise compose after set"() {
		given: "a synchronous promise"
		def p = Promises.success('10').get()

		when: "composing 2 functions"
		def s = p | { Integer.parseInt it } | { it*10 }

		then:
		s.get() == 100
	}

}
