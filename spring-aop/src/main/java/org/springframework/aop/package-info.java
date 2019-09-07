/**
 * Core Spring AOP interfaces, built on AOP Alliance AOP interoperability interfaces.
 * <p>
 * <br>Any AOP Alliance MethodInterceptor is usable in Spring.
 * <p>
 * <br>Spring AOP also offers:
 * <ul>
 * <li>Introduction support
 * <li>A Pointcut abstraction, supporting "static" pointcuts
 * (class and method-based) and "dynamic" pointcuts (also considering method arguments).
 * There are currently no AOP Alliance interfaces for pointcuts.
 * <li>A full range of advice types, including around, before, after returning and throws advice.
 * <li>Extensibility allowing arbitrary custom advice types to
 * be plugged in without modifying the core framework.
 * </ul>
 * <p>
 * <br>
 * Spring AOP can be used programmatically or (preferably)
 * integrated with the Spring IoC container.
 */
package org.springframework.aop;

