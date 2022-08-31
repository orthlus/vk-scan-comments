import com.google.common.collect.Iterables;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class TestSupplier {
	public static void main(String[] args) {
		List<Integer> integers = List.of(
				new Random().nextInt(),
				new Random().nextInt()
		);
		Iterator<Integer> iterator = Iterables.cycle(integers).iterator();
		Supplier<Integer> integer = iterator::next;
		System.out.println(integer.get());
		System.out.println(integer.get());
		System.out.println(integer.get());
		System.out.println(integer.get());
		System.out.println(integer.get());
		System.out.println(integer.get());
		System.out.println(integer.get());
	}
}
