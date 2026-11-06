package ru.webcourse.backup;

import java.nio.file.Path;

/** A real child JVM keeps the timeout tests independent of shell and installed database tools. */
public class ProcessFixture {
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "child" -> Thread.sleep(60_000);
            case "tree" -> {
                var child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp", System.getProperty("java.class.path"), ProcessFixture.class.getName(), "child")
                        .inheritIO().start();
                System.out.println("child=" + child.pid());
                System.out.flush();
                Thread.sleep(60_000);
            }
            case "echo" -> System.out.println("stdin=" + new String(System.in.readAllBytes()).trim());
            case "flood" -> {
                for (int i = 0; i < 50_000; i++) System.out.println("line=" + i);
            }
            default -> throw new IllegalArgumentException(args[0]);
        }
    }
}
