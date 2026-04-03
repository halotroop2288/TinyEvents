/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Falkreon (Isaac Ellingson)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blue.endless.tinyevents;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.Supplier;

import blue.endless.tinyevents.function.BooleanBinaryOperator;
import blue.endless.tinyevents.function.IntBiConsumer;

public class EventFactories {
	public static EventFactory<BooleanSupplier> BOOLEAN_SUPPLIER_FAVOR_FALSE = () -> EventFactories.booleanSupplier(BooleanBinaryOperator.AND);
	public static EventFactory<BooleanSupplier> BOOLEAN_SUPPLIER_FAVOR_TRUE  = () -> EventFactories.booleanSupplier(BooleanBinaryOperator.OR);
	public static EventFactory<DoubleConsumer>  DOUBLE_CONSUMER              = EventFactories::doubleConsumer;
	public static EventFactory<DoublePredicate> DOUBLE_PREDICATE_FAVOR_FALSE = () -> EventFactories.doublePredicate(BooleanBinaryOperator.AND);
	public static EventFactory<DoublePredicate> DOUBLE_PREDICATE_FAVOR_TRUE  = () -> EventFactories.doublePredicate(BooleanBinaryOperator.OR);
	public static EventFactory<IntConsumer>     INT_CONSUMER                 = EventFactories::intConsumer;
	public static EventFactory<Runnable>        RUNNABLE                     = EventFactories::runnable;
	
	/**
	 * Creates an event that BiConsumer event-handlers may be registered to.
	 *
	 * @param <X> The type of the first parameter that event-handlers will receive
	 * @param <Y> The type of the second parameter that event-handlers will receive
	 * @return the new Event
	 */
	public static <X, Y> Event<BiConsumer<X, Y>> biConsumer() {
		return new Event<>(handlers -> (x, y) ->
				handlers.forEach(entry -> entry.handler().accept(x, y))
		);
	}
	
	/**
	 * Creates an event that Functions may respond to. All the responders are turned into a single value by recursively
	 * applying the reducer function for every two values returned.
	 *
	 * @param <T>     The type of the first argument that event-handlers will receive
	 * @param <U>     The type of the second argument that event-handlers will receive
	 * @param <V>     The type of data that event-handlers will provide
	 * @param reducer a function which will turn responses into a single value
	 * @return the new Event
	 */
	public static <T, U, V> Event<BiFunction<T, U, V>> biFunction(BinaryOperator<V> reducer) {
		return new Event<>(handlers -> (t, u) -> {
			V result = null;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().apply(t, u);
				result = i == 0 ? cur : reducer.apply(result, cur);
			}
			
			return result;
		});
	}
	
	/**
	 * Creates an Event that handlers can use to vote on a single boolean-valued result.
	 *
	 * @param reducer A function that reconciles multiple votes to produce a single value
	 * @return the new Event
	 */
	public static Event<BooleanSupplier> booleanSupplier(BooleanBinaryOperator reducer) {
		return new Event<>(handlers -> () -> {
			boolean result = false;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().getAsBoolean();
				result = i == 0 ? cur : reducer.apply(result, cur);
			}
			return result;
		});
	}
	
	/**
	 * Creates an event that Consumer event-handlers may be registered to.
	 *
	 * @param <X> The type of data which event-handlers will receive (consume)
	 * @return the new Event
	 */
	public static <X> Event<Consumer<X>> consumer() {
		return new Event<>(handlers -> eventPayload ->
				handlers.forEach(entry -> entry.handler().accept(eventPayload))
		);
	}
	
	/**
	 * Creates an event that DoubleConsumer event-handlers may be registered to.
	 *
	 * @return the new Event
	 */
	public static Event<DoubleConsumer> doubleConsumer() {
		return new Event<>(handlers -> value ->
				handlers.forEach(entry -> entry.handler().accept(value))
		);
	}
	
	/**
	 * Creates an event that receives a double value and produces a reference-typed result.
	 *
	 * @param <X>     The "result type" for the Event to create
	 * @param reducer A function that turns multiple handler-results into a single event-result
	 * @return the new Event
	 */
	public static <X> Event<DoubleFunction<X>> doubleFunction(BinaryOperator<X> reducer) {
		return new Event<>(handlers -> value -> {
			X result = null;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().apply(value);
				result = i == 0 ? cur : reducer.apply(result, cur);
			}
			
			return result;
		});
	}
	
	/**
	 * Creates an event that receives a double value and produces a boolean result.
	 *
	 * @param reducer A function that turns multiple handler-results into a single event-result
	 * @return the new Event
	 */
	public static Event<DoublePredicate> doublePredicate(BooleanBinaryOperator reducer) {
		return new Event<>(handlers -> value -> {
			boolean result = false;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final boolean cur = entry.handler().test(value);
				result = i == 0 ? cur : reducer.apply(result, cur);
			}
			
			return result;
		});
	}
	
	public static Event<DoubleSupplier> doubleSupplier(DoubleBinaryOperator reducer) {
		return new Event<>(handlers -> () -> {
			double result = 0;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().getAsDouble();
				result = i == 0 ? cur : reducer.applyAsDouble(cur, result);
			}
			
			return result;
		});
	}
	
	public static Event<DoubleToIntFunction> doubleToIntFunction(IntBinaryOperator reducer) {
		return new Event<>(handlers -> value -> {
			int result = 0;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().applyAsInt(value);
				result = i == 0 ? cur : reducer.applyAsInt(cur, result);
			}
			
			return result;
		});
	}
	
	public static Event<DoubleToLongFunction> doubleToLongFunction(LongBinaryOperator reducer) {
		return new Event<>(handlers -> value -> {
			long result = 0;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().applyAsLong(value);
				result = i == 0 ? cur : reducer.applyAsLong(cur, result);
			}
			
			return result;
		});
	}
	
	/**
	 * Creates an event that Functions may respond to. All the responders are turned into a single value by recursively
	 * applying the reducer function for every two values returned.
	 *
	 * @param <T>     The type of data that event-handlers will receive
	 * @param <U>     The type of data that event-handlers will provide
	 * @param reducer a function which will turn responses into a single value
	 * @return the new Event
	 */
	public static <T, U> Event<Function<T, U>> function(BinaryOperator<U> reducer) {
		return new Event<>(handlers -> t -> {
			U result = null;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().apply(t);
				result = i == 0 ? cur : reducer.apply(result, cur);
			}
			
			return result;
		});
	}
	
	public static Event<IntConsumer> intConsumer() {
		return new Event<>(handlers -> value ->
				handlers.forEach(entry -> entry.handler().accept(value))
		);
	}
	
	/**
	 * Creates an event that receives an integer value and produces a reference-typed result.
	 *
	 * @param <X>     The "result type" for the Event to create
	 * @param reducer A function that turns multiple handler-results into a single event-result
	 * @return the new Event
	 */
	public static <X> Event<IntFunction<X>> intFunction(BinaryOperator<X> reducer) {
		return new Event<>(handlers -> value -> {
			X result = null;
			
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().apply(value);
				result = i == 0 ? cur : reducer.apply(result, cur);
			}
			
			return result;
		});
	}
	
	/**
	 * Creates an event that receives an integer value and produces a boolean result.
	 *
	 * @param reducer A function that turns multiple handler-results into a single event-result
	 * @return the new Event
	 */
	public static Event<IntPredicate> intPredicate(BooleanBinaryOperator reducer) {
		return new Event<>(handlers -> value -> {
			boolean result = false;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().test(value);
				result = i == 0 ? cur : reducer.apply(result, cur);
			}
			
			return result;
		});
	}
	
	
	/**
	 * Creates an event that Runnable event-handlers may be registered to.
	 *
	 * @return the new Event
	 */
	public static Event<Runnable> runnable() {
		return new Event<>(handlers -> () ->
				handlers.forEach(entry -> entry.handler().run())
		);
	}
	
	/**
	 * Creates an event that Suppliers may respond to. Responders which are registered later will override
	 * earlier ones.
	 *
	 * @param <X> The type of data that event-handlers will provide
	 * @return the new Event
	 */
	public static <X> Event<Supplier<X>> supplier() {
		return supplier((a, b) -> b);
	}
	
	/**
	 * Creates an event that Suppliers may respond to. All the responders are coalesced with the reducer function. For
	 * instance, if responders reply with `[ 1, 2, 3, 4 ]` with reducer function `(a, b) -&gt; a + b`, the operations
	 * performed will be `((1 + 2) + 3) + 4`.
	 *
	 * @param <X>     The type of data that event-handlers will provide
	 * @param reducer a function which will be used to coalesce responses into a single value
	 * @return the new Event
	 */
	public static <X> Event<Supplier<X>> supplier(BinaryOperator<X> reducer) {
		return new Event<>(handlers -> () -> {
			X result = null;
			for (int i = 0, size = handlers.size(); i < size; i++) {
				final var entry = handlers.get(i);
				final var cur = entry.handler().get();
				result = i == 0 ? cur : reducer.apply(result, cur);
			}
			return result;
		});
	}
	
	public static Event<IntBiConsumer> intBiConsumer() {
		return new Event<>(handlers -> (x, y) ->
				handlers.forEach(entry -> entry.handler().accept(x, y))
		);
	}
}
