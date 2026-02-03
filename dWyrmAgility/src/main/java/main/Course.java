package main;

public interface Course {

    int poll(dWyrmAgility dWyrmAgility);

    int[] regions();

    String name();

    default String displayName() {
        return name();
    }
}