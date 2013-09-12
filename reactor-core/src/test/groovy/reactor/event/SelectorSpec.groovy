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

package reactor.event

import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static reactor.GroovyTestUtils.$
import static reactor.GroovyTestUtils.consumer
import static reactor.event.selector.Selectors.R
import static reactor.event.selector.Selectors.T
import static reactor.event.selector.Selectors.U
import reactor.core.spec.Reactors
import reactor.function.Functions;
import spock.lang.Specification

/**
 * @author Jon Brisbin
 * @author Andy Wilkinson
 * @author Stephane Maldini
 */
class SelectorSpec extends Specification {

	def "Selectors work with Strings"() {

		when: "a selector backed by a String is defined"
		def selector = $("test")

		then: "it matches the string"
		selector.matches "test"
	}

	def "Selectors work with primitives"() {

		when: "a selector backed by a primitve is defined"
		def selector = $(1L)

		then: "it matches the primitive"
		selector.matches 1L
	}

	def "Class selectors match on isAssignableFrom"() {

		when: "a selector based on Class is defined"
		def clz1 = T(Throwable)
		def clz2 = T(IllegalArgumentException)

		then: "it matches the class and its sub types"
		clz1.matches Throwable
		clz1.matches IllegalArgumentException
	}

	def "Regex selectors match on expressions"() {

		when: "A selector based on a regular expression and a matching key are defined"
		def sel1 = R("test([0-9]+)")
		def key = "test1"

		then: "they match"
		sel1.matches "test1"

		when: "a key the does not fit the pattern is provided"
		def key2 = "test-should-not-match"

		then: "it does not match"
		!sel1.matches(key2)

	}

	def "Selectors can be matched on URI"() {

		given: "A UriTemplateSelector"
		def sel1 = U("/path/**/{resource}")
		def key = "/path/to/some/resourceId"
		def r = Reactors.reactor().synchronousDispatcher().get()
		def resourceId = ""
		r.on(sel1, consumer { Event<String> ev ->
			resourceId = ev.headers["resource"]
		})

		when: "The selector is matched"
		r.notify key, Event.wrap("")

		then: "The resourceId has been set when the headers"
		resourceId == 'resourceId'

	}

	def "Consumers can be called using round-robin routing"() {

		given: "A Reactor using round-robin routing and a set of consumers assigned to the same selector"
		def r = Reactors.reactor().synchronousDispatcher().roundRobinEventRouting().get()
		def called = []
		def a1 = {
			called << 1
		}
		def a2 = {
			called << 2
		}
		def a3 = {
			called << 3
		}
		def a4 = {
			called << 4
		}
		r.on($('key'), Functions.consumer(a1))
		r.on($('key'), Functions.consumer(a2))
		r.on($('key'), Functions.consumer(a3))
		r.on($('key'), Functions.consumer(a4))

		when: "events are triggered"
		(1..4).each {
			r.notify('key', Event.wrap("Hello World!"))
		}

		then: "all consumers should have been called once"
		assertThat(called, hasItems(1, 2, 3, 4))
	}

	def "Consumers can be routed to randomly"() {

		given: "A Reactor using random routing and a set of consumers assigned to the same selector"

		def r = Reactors.reactor().synchronousDispatcher().randomEventRouting().get()
		def called = []
		def a1 = {
			called << 1
		}
		def a2 = {
			called << 2
		}
		def a3 = {
			called << 3
		}
		def a4 = {
			called << 4
		}
		r.on(Functions.consumer(a1))
		r.on(Functions.consumer(a2))
		r.on(Functions.consumer(a3))
		r.on(Functions.consumer(a4))

		when: "events are triggered"

		(1..4).each {
			r.notify(Event.wrap("Hello World!"))
		}

		then: "random selection of consumers have been called"
		assertThat(called, anyOf(hasItem(1), hasItem(2), hasItem(3), hasItem(4)))
	}

}
