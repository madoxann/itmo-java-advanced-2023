package info.kgeorgiy.ja.fedorenko.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Generates implementation of class or interface.
 */
public class Implementor implements JarImpler {
    /**
     * Generates java class, which implements given {@link Class} or Interface.
     * @param token class of which implementation is created
     * @param root {@link Path} root of directory where implementation should be
     * @throws ImplerException if a token cannot be implemented or if implementation cannot be written
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (Modifier.isPrivate(token.getModifiers()))
            throw new ImplerException("You may not implement private classes & interfaces");
        if (Modifier.isFinal(token.getModifiers()))
            throw new ImplerException("The class is final, no inheritance allowed");
        if (token == Enum.class)
            throw new ImplerException("Direct inheritance of enum is forbidden");

        try {
            Path dest = root.resolve(token.getPackageName().replace('.', File.separatorChar))
                    .resolve(getImplName(token) + ".java");
            Files.createDirectories(dest.getParent());

            try (BufferedWriter out = new BufferedWriter(new FileWriter(dest.toFile(), StandardCharsets.UTF_8))) {
                out.write(generateSourceCode(token));
            }
        } catch (IOException e) {
            throw new ImplerException("IOException occurred: " + e.getMessage());
        }
    }

    /**
     * Generates a JAR file, containing the code implementing {@link Class} or the Interface
     * @param token class of which implementation is created
     * @param jarFile {@link Path} a path to JAR file to be generated
     * @throws ImplerException if a token cannot be implemented or if JAR file cannot be written
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Path tmp = Path.of("temp");

            try (JarOutputStream out = new JarOutputStream(new PrintStream(jarFile.toFile(), StandardCharsets.UTF_8))) {
                implement(token, tmp);
                JarUtils.compileClass(tmp, token);
                Path compiledClass = JarUtils.getFile(tmp, token, JarUtils.GetFileOption.CLASS);
                out.putNextEntry(
                        new ZipEntry((token.getPackageName() + "." + getImplName(token))
                                .replace('.', '/') + ".class")
                );
                Files.copy(compiledClass, out);
            } catch (IOException | SecurityException e) {
                throw new ImplerException("Failed to implement class", e);
            } finally {
                JarUtils.clean(tmp);
            }
        } catch (InvalidPathException | SecurityException e) {
            throw new ImplerException("Failed to create temporary directory: ", e);
        }
    }

    /**
     * Main function, providing a command-line interface. In case of an error displays a message in {@code System.err}
     * @param args {@link String} array of arguments. Only one valid option is available for now
        <ul>
            <li> {@code -jar className path} - runs a {@link #implementJar} with mentioned arguments </li>
        </ul>
     */
    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                throw new IllegalArgumentException("Expected 3 arguments, got " + args.length);
            }

            boolean check = true;
            for (Iterator<String> str = Arrays.stream(args).iterator(); str.hasNext(); check &= str.next() != null);
            if (!check) {
                throw new IllegalArgumentException("None of arguments can be null!");
            }
            if (!args[0].equals("-jar")) throw new IllegalArgumentException("Unknown option, -jar expected");

            Implementor impl = new Implementor();
            impl.implementJar(Class.forName(args[2]), Paths.get(args[1]));
        } catch (ClassNotFoundException | LinkageError e) {
            System.err.println("Failed to load specified class: " + e.getMessage());
        } catch (AssertionError e) {
            System.err.println("Compilation failed: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Error, while implementing class: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Returns a name of class being an implementation of token
     * @param token {@link Class} of which implementation is created
     * @return {@link String} with "Impl" suffix
     * @see info.kgeorgiy.java.advanced.implementor.Impler
     */
    private static String getImplName(final Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Generates an implementation of given {@link Class} token
     * @param token class of which implementation is created
     * @return {@link String} of a correct java class being an inheritor of given token
     * @throws ImplerException if an implementation cannot be produced
     */
    private String generateSourceCode(Class<?> token) throws ImplerException {
        return String.format("%s\n\n%s {\n%s\n\n%s\n}", ImplUtils.generatePackageString(token),
                ImplUtils.generateHeader(token), (token.isInterface() ? "" : generateConstructors(token) + "\n\n"),
                generateMethods(token));
    }

    /**
     * Generates constructors of given {@link Class} token
     * @param token class of which constructor is being generated
     * @return formatted {@link String} of constructors overridden by @{@code super()} call
     * @throws ImplerException if class cannot have any constructors
     */
    private String generateConstructors(Class<?> token) throws ImplerException {
        String impl = Arrays.stream(token.getDeclaredConstructors())
                .filter(m -> !Modifier.isPrivate(m.getModifiers()))
                .map(c -> String.format("\n\t%s{\n\t\t%s%s;\n\t}",
                        ImplUtils.generateSignature(c, token), "super",
                        ImplUtils.generateParameters(c.getParameters(), false)
                ))
                .collect(Collectors.joining("\n"));

        if (impl.isEmpty()) throw new ImplerException("No non-private constructors available");
        return impl;
    }

    /**
     * Returns a {@link Set} of non-overridden methods of given {@link Class}
     * @param token class of which methods are retrieved
     * @return set of {@link UnifiedMethod} that weren't overridden by ancestors
     * @throws ImplerException if an inheritance from private interface is found
     */
    private Set<UnifiedMethod> getNonOverriddenMethods(Class<?> token) throws ImplerException {
        return accumulateMethods(token, new HashSet<>());
    }

    /**
     * A recursion collecting {@link Set} of non-overridden {@link UnifiedMethod} of given {@link Class}
     * @param token class to perform iteration on
     * @param accumulate current set of methods accumulated in recursion
     * @return set of non-overridden methods
     * @throws ImplerException if an inheritance from private interface is found
     */
    private Set<UnifiedMethod> accumulateMethods(Class<?> token, Set<UnifiedMethod> accumulate) throws ImplerException {
        if (token == null) return accumulate;

        Class<?> parent = token.getSuperclass();
        if (parent != null && token.isInterface() && Modifier.isPrivate(parent.getModifiers()))
            throw new ImplerException("Cannot inherit form private interface");

        Arrays.stream(token.getDeclaredMethods())
                .map(UnifiedMethod::new)
                .collect(Collectors.toCollection(() -> accumulate));

        return accumulateMethods(parent, accumulate);
    }

    /**
     * Generates methods of implementation of given {@link Class} token
     * @param token class of which methods are generated
     * @return formatted {@link String} of implemented methods
     * @throws ImplerException if a private interface inheritance is found
     */
    private String generateMethods(Class<?> token) throws ImplerException {
        Set<UnifiedMethod> m = Stream.concat(getNonOverriddenMethods(token).stream(),
                        Arrays.stream(token.getMethods()).map(UnifiedMethod::new))
                .filter(method -> !Modifier.isFinal(method.m.getModifiers()))
                .filter(method -> Modifier.isAbstract(method.m.getModifiers()))
                .collect(Collectors.toCollection(HashSet<UnifiedMethod>::new));

        return implementMethods(m);
    }

    /**
     * A helper function for {@link #generateMethods(Class)} that builds a string of implemented {@link UnifiedMethod}
     * @param coll {@link Collection} of methods to be implemented
     * @return formatted {@link String} containing implementation of all methods
     */
    private String implementMethods(Collection<UnifiedMethod> coll) {
        return coll.stream()
                .map(
                        method -> String.format("\t%s\n\t%s {\n\t\t%s\n\t}", "@Override",
                                ImplUtils.generateSignature(method.m, method.m.getReturnType()),
                                ImplUtils.generateReturn(method.m.getReturnType()))
                ).collect(Collectors.joining("\n\n"));
    }

    /**
     * A {@link Record} for comparing {@link Method}, making methods with same return types, parameters
     * and method names equal
     * @param m a {@link Method} being compared
     */
    private record UnifiedMethod(Method m) {
        /**
         * An overridden equals of {@link Method} to provide a better comparison between methods
         * @param o {@link Object} being compared
         * @return always return {@code false} if not compared against {@link UnifiedMethod}, otherwise objects are
         * compared by {@link #hashCode()}
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof UnifiedMethod)
                return o.hashCode() == hashCode();

            return false;
        }

        /**
         * Calculates hash of {@link UnifiedMethod}: methods with same names, return types and parameters are
         * considered identical
         * @return {@code int} hashcode
         */
        @Override
        public int hashCode() {
            return (m.getName().hashCode() * 31)
                    + Objects.hash(m.getReturnType(), Arrays.hashCode(m.getParameterTypes()));
        }
    }

    /**
     * Utility class for implementing methods
     */
    private static final class ImplUtils {
        /**
         * Generates a package of given {@link Class} token
         * @param token class of which package is being generated
         * @return package {@link String} or empty String if package is missing
         */
        static String generatePackageString(Class<?> token) {
            String p = token.getPackageName();
            return p.isEmpty() ? "" : "package " + p + ";";
        }

        /**
         * Generates a header of given {@link Class} token
         * @param token class token of which header is being generated
         * @return {@link String} representing a correct inheritance of a given token
         */
        static String generateHeader(Class<?> token) {
            return "public class " + getImplName(token)
                    + (token.isInterface() ? " implements " : " extends ") + token.getCanonicalName();
        }

        /**
         * Helper method generating a string of modifiers
         * @param mod {@code int} modifier flag
         * @return {@link String} of modifiers which flag is set
         */
        static String generateModifiers(int mod) {
            return Modifier.toString(mod & (~Modifier.ABSTRACT) & (~Modifier.VOLATILE) & (~Modifier.TRANSIENT));
        }

        /**
         * Generates a string of given {@link Parameter} array
         * @param params parameters of which string is being generated
         * @param isSignature {@code boolean} whether parameter string is a part of method signature
         * @return {@link String} representing a parameters for either method or constructor call
         */
        static String generateParameters(Parameter[] params, boolean isSignature) {
            return Arrays.stream(params)
                    .map(p -> (isSignature ? p.getType().getCanonicalName() + " " : "") + p.getName())
                    .collect(Collectors.joining(", ", "(", ")"));
        }

        /**
         * Generates a string of exceptions for given {@link Class} array
         * @param params classes of exception tokens
         * @return a {@link String} indicating a method throwing all off the exceptions in array
         */
        static String generateExceptions(Class<?>[] params) {
            return params.length == 0 ? "" : "throws " + Arrays.stream(params)
                    .map(Class::getCanonicalName).collect(Collectors.joining(", "));
        }

        /**
         * Generates a signature of given {@link Executable}
         * @param m executable of which signature is being generated
         * @param retType a {@link Class} token of return type. If a signature of constructor is being generated
         *                a token of implemented class is required
         * @return a {@link String} representing a signature of an Executable
         */
        static String generateSignature(Executable m, Class<?> retType) {
            return String.format("%s %s%s %s", generateModifiers(m.getModifiers()),
                    ((m instanceof Method) ? (retType.getCanonicalName() + " " + m.getName()) : getImplName(retType)),
                    generateParameters(m.getParameters(), true), generateExceptions(m.getExceptionTypes()));
        }

        /**
         * Generates return string of given {@link Class} token
         * @param type token of method's return type
         * @return {@link String} containing code of return with default value of mentioned type
         */
        static String generateReturn(Class<?> type) {
            String retStr = "null";

            if (type == void.class) retStr = "";
            else if (type == boolean.class) retStr = "false";
            else if (type.isPrimitive()) retStr = "0";

            return String.format("return %s;", retStr);
        }
    }

    /**
     * Utility class for creating JAR's and working with files
     */
    private static final class JarUtils {
        /**
         * {@link Enum} of possible file retrieval options for {@link #getFile}
         */
        enum GetFileOption {
            /** A file with .class extension */
            CLASS(".class"),
            /** A file with .java extension */
            JAVA(".java");
            /** A file extension */
            private final String ending;

            /**
             * Constructor of {@link GetFileOption}
             * @param s {@link String} of a given file extension
             */
            GetFileOption(String s) { ending = s; }
        }

        /**
         * Compiles the {@link Class} code by {@link Path}
         * @param root root of a source code directory
         * @param token token of class to be compiled
         */
        static void compileClass(final Path root, final Class<?> token) {
            final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            assert compiler != null;
            final String classpath = root + File.pathSeparator + getClassPath(token);
            final String[] args = Stream.of("-encoding", "UTF-8", getFile(
                    root, token, GetFileOption.JAVA).toString(), "-cp", classpath
            ).toArray(String[]::new);
            final int exitCode = compiler.run(null, null, null, args);
            assert exitCode == 0;
        }

        /**
         * Retrieves classpath of a given {@link Class} token
         * @param token token of class of witch classpath is to be found
         * @return {@link String} representing a classpath of a token
         */
        static String getClassPath(Class<?> token) {
            try {
                return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            } catch (final URISyntaxException e) {
                throw new AssertionError(e);
            }
        }

        /**
         * Retrieves a path of a given {@link Class} token in a root subdirectory
         * @param root {@link Path} where a file needs to ve found
         * @param token class of witch file is to be found
         * @param opt {@link GetFileOption} a file lookup option, return a file with matching extension
         * @return path of a requested file
         */
        static Path getFile(final Path root, final Class<?> token, GetFileOption opt) {
            return root.resolve((token.getPackageName() + "." + getImplName(token))
                            .replace(".", File.separator) + opt.ending);
        }

        /**
         * Recursively deletes files in a directory with a given {@link Path}
         * @param root path to directory to be cleaned. Note that {@code root} will be deleted as well
         * @throws ImplerException if {@link IOException} occurred during deletion
         */
        static void clean(final Path root) throws ImplerException {
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new ImplerException("Failed to close directory: ", e);
            }
        }
    }
}