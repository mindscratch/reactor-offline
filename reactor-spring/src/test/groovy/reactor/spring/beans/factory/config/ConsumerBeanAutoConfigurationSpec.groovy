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




package reactor.spring.beans.factory.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.Environment
import reactor.core.Reactor
import reactor.event.Event
import reactor.spring.annotation.Consumer
import reactor.spring.annotation.Selector
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
class ConsumerBeanAutoConfigurationSpec extends Specification {

	def "Annotated Consumer is wired to a Reactor"() {

		given:
		"an ApplicationContext with an annotated bean handler"
		def appCtx = new AnnotationConfigApplicationContext(AnnotatedHandlerConfig)
		def handlerBean = appCtx.getBean(HandlerBean)
		def reactor = appCtx.getBean(Reactor)

		when:
		"an Event is emitted onto the Reactor in context"
		reactor.notify('test', Event.wrap("Hello World!"))

		then:
		"the method has been invoked"
		handlerBean.latch.await(1, TimeUnit.SECONDS)
	}

}

@Consumer
class HandlerBean {
	@Autowired
	Reactor reactor
	def latch = new CountDownLatch(1)

	@Selector('test')
	void handleTest(String s) {
		latch.countDown()
	}
}

@Configuration
class AnnotatedHandlerConfig {

	@Bean
	Environment reactorSpringEnvironment() {
		return new Environment()
	}

	@Bean
	Reactor rootReactor(Environment env) {
		return env.rootReactor
	}

	@Bean
	ConsumerBeanAutoConfiguration consumerBeanAutoConfiguration(Environment env) {
		return new ConsumerBeanAutoConfiguration()
	}

	@Bean
	HandlerBean handlerBean() {
		return new HandlerBean()
	}

}
