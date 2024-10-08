package org.benf.cfr.reader.state;

import it.unimi.dsi.fastutil.objects.*;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils.getPackageAndClassNames;

public class ClassFileSourceImpl implements ClassFileSource2 {
    private final Set<String> explicitJars = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, byte[]> zipCache = new ConcurrentHashMap<>();
    private volatile Map<String, JarSourceEntry> classToPathMap;
    private final Options options;
    private ClassRenamer classRenamer;

    private ClassFileRelocator classRelocator;
    /*
     * Initialisation info
     */
    private static final boolean JrtPresent = CheckJrt();
    private static final Map<String, String> packMap = JrtPresent ? getPackageToModuleMap() : new HashMap<>();

    private static boolean CheckJrt() {
        try {
            return Object.class.getResource("Object.class").getProtocol().equals("jrt");
        } catch (Exception e) {
            return false;
        }
    }

    public ClassFileSourceImpl(Options options) {
        this.options = options;
    }

    private byte[] getBytesFromFile(InputStream is, long length) throws IOException {
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead;
        while (offset < bytes.length
            && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file");
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    @Override
    public String getPossiblyRenamedPath(String path) {
        if (classRenamer == null) return path;
        String res = classRenamer.getRenamedClass(path + ".class");
        if (res == null) return path;
        return res.substring(0, res.length() - 6);
    }

    @Override
    public Pair<byte[], String> getClassFileContent(String path) throws IOException {
        return getClassFileContent(path, classRelocator);
    }

    @Override
    public Pair<byte[], String> getClassFileContent(final String inputPath, ClassFileRelocator classRelocator) throws IOException {
        Map<String, JarSourceEntry> classPathFiles = getClassPathClasses();

        JarSourceEntry jarEntry = classPathFiles.get(inputPath);

        // If path is an alias due to case insensitivity, restore to the correct name here, before
        // accessing zipfile.
        String path = classRenamer == null ? inputPath : classRenamer.getOriginalClass(inputPath);

        if (classRelocator == null) {
            classRelocator = this.classRelocator;
        }
        String usePath = classRelocator.correctPath(path);
        boolean forceJar = jarEntry != null && explicitJars.contains(jarEntry.path());
        File file = forceJar ? null : new File(usePath);
        byte[] content;
        if (file != null && file.exists()) {
            InputStream is = new FileInputStream(file);
            long length = file.length();
            content = getBytesFromFile(is, length);
        } else if (jarEntry != null) {
            String zipPath = inputPath;
            if (jarEntry.analysisType == AnalysisType.WAR) {
                zipPath = MiscConstants.WAR_PREFIX + zipPath;
            }
            content = zipCache.get(zipPath);
            if (content == null) {
                synchronized (jarEntry) {
                    content = zipCache.get(inputPath);
                    if (content == null) {
                        try (ZipFile zipFile = new ZipFile(new File(jarEntry.path()), ZipFile.OPEN_READ)) {
                            zipFile.stream()
                                .filter(zipEntry -> !zipEntry.isDirectory())
                                .forEach(zipEntry -> {
                                    try {
                                        InputStream is = zipFile.getInputStream(zipEntry);
                                        byte[] content2 = getBytesFromFile(is, zipEntry.getSize());
                                        zipCache.put(zipEntry.getName(), content2);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                        }
                        content = zipCache.get(zipPath);
                        assert content != null;
                    }
                }
            }
        } else {
            // Fallback - can we get the bytes using a java9 extractor?
            content = getInternalContent(inputPath);
        }

        return Pair.make(content, inputPath);
    }

    /*
     * There are costs associated in the Class.forName method of finding the URL for a class -
     * notably the running of the static initialiser.
     *
     * We can avoid that by knowing what packages are in what modules, and skipping for those.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> getPackageToModuleMap() {
        Map<String, String> mapRes = MapFactory.newMap();
        try {
            Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer");
            Method bootMethod = moduleLayerClass.getMethod("boot");
            Object boot = bootMethod.invoke(null);
            Method modulesMeth = boot.getClass().getMethod("modules");
            Object modules = modulesMeth.invoke(boot);
            Class<?> moduleClass = Class.forName("java.lang.Module");
            Method getPackagesMethod = moduleClass.getMethod("getPackages");
            Method getNameMethod = moduleClass.getMethod("getName");
            for (Object module : (Set) modules) {
                Set<String> packageNames = (Set<String>) getPackagesMethod.invoke(module);
                String moduleName = (String) getNameMethod.invoke(module);
                for (String packageName : packageNames) {
                    if (mapRes.containsKey(packageName)) {
                        mapRes.put(packageName, null);
                    } else {
                        mapRes.put(packageName, moduleName);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return anything we have.
        }
        return mapRes;
    }

    private byte[] getContentByFromReflectedClass(final String inputPath) {
        try {
            String classPath = inputPath.replace("/", ".").substring(0, inputPath.length() - 6);

            Pair<String, String> packageAndClassNames = getPackageAndClassNames(classPath);

            String packageName = packageAndClassNames.getFirst();
            String moduleName = packMap.get(packageName);

            if (moduleName != null) {
                byte[] res = getUrlContent(new URL("jrt:/" + moduleName + "/" + inputPath));
                if (res != null) return res;
            }

            int idx = inputPath.lastIndexOf("/");
            String name = idx < 0 ? inputPath : inputPath.substring(idx + 1);

            // Going down this branch will trigger a class load, which will cause
            // static initialisers to be run.
            // This is expensive, but normally tolerable, except that there is a javafx
            // static initialiser which crashes the VM if called unexpectedly!
            URL resource = Class.forName(classPath).getResource(name);

            if (resource == null) {
                return null;
            }
            return getUrlContent(resource);
        } catch (Throwable t) {
            // Class.forName can throw a linkage error in some circumstances, so it's necessary to trap throwable.
            return null;
        }
    }

    private byte[] getUrlContent(URL url) {
        String protocol = url.getProtocol();
        // Strictly speaking, we could use this mechanism for pre-9 classes, but it's.... so wrong!
        if (!protocol.equals("jrt")) return null;

        InputStream is;
        int len;
        try {
            URLConnection uc;
            uc = url.openConnection();
            uc.connect();
            is = uc.getInputStream();
            len = uc.getContentLength();
        } catch (IOException ioe) {
            return null;
        }

        try {
            if (len >= 0) {
                byte[] b = new byte[len];
                int i = len;
                while (i > 0) {
                    if (i < (i -= is.read(b, len - i, i))) i = -1;
                }
                if (i == 0) return b;
            }
        } catch (IOException e) {
            //
        }
        return null;
    }

    private byte[] getInternalContent(final String inputPath) throws IOException {
        if (JrtPresent) {
            byte[] res = getContentByFromReflectedClass(inputPath);
            if (res != null) return res;
        }
        throw new IOException("No such file " + inputPath);
    }

    @Deprecated
    public Collection<String> addJar(String jarPath) {
        return addJarContent(jarPath, AnalysisType.JAR).getClassFiles();
    }

    public JarContent addJarContent(String jarPath, AnalysisType analysisType) {
        // Make sure classpath is scraped first, so we'll overwrite it.
        getClassPathClasses();

        File file = new File(jarPath);
        if (!file.exists()) {
            throw new ConfusedCFRException("No such jar file " + jarPath);
        }
        jarPath = file.getAbsolutePath();
        JarContent jarContent = processClassPathFile(file, false, analysisType);
        if (jarContent == null) {
            throw new ConfusedCFRException("Failed to load jar " + jarPath);
        }

        JarSourceEntry sourceEntry = new JarSourceEntry(analysisType, jarPath);

        if (classRenamer != null) {
            classRenamer.notifyClassFiles(jarContent.getClassFiles());
        }

        String jarClassPath = jarContent.getManifestEntries().get(MiscConstants.MANIFEST_CLASS_PATH);
        if (jarClassPath != null) {
            addToRelativeClassPath(file, jarClassPath);
        }

        ObjectList<String> output = new ObjectArrayList<>();
        for (String classPath : jarContent.getClassFiles()) {
            if (classPath.toLowerCase().endsWith(".class")) {
                // nb : entry.value will always be the jar here, but ....
                if (classRenamer != null) {
                    classPath = classRenamer.getRenamedClass(classPath);
                }
                classToPathMap.put(classPath, sourceEntry);
                output.add(classPath);
            }
        }
        explicitJars.add(jarPath);
        return jarContent;
    }

    private record JarSourceEntry(AnalysisType analysisType, String path) {

        @Override
        @SuppressWarnings("unused")
        public AnalysisType analysisType() {
            return analysisType;
        }
    }

    private Map<String, JarSourceEntry> getClassPathClasses() {
        if (classToPathMap == null) {
            boolean dump = options.getOption(OptionsImpl.DUMP_CLASS_PATH);

            classToPathMap = new ConcurrentHashMap<>();
            String classPath = System.getProperty("java.class.path");
            String sunBootClassPath = System.getProperty("sun.boot.class.path");
            if (sunBootClassPath != null) {
                classPath += File.pathSeparatorChar + sunBootClassPath;
            }

            if (dump) {
                System.out.println("/* ClassPath Diagnostic - searching :" + classPath);
            }
            String extraClassPath = options.getOption(OptionsImpl.EXTRA_CLASS_PATH);
            if (null != extraClassPath) {
                classPath = classPath + File.pathSeparatorChar + extraClassPath;
            }

            classRenamer = ClassRenamer.create(options);

            String[] classPaths = classPath.split("" + File.pathSeparatorChar);
            for (String path : classPaths) {
                processToClassPath(dump, path);
            }
            if (dump) {
                System.out.println(" */");
            }
        }
        return classToPathMap;
    }

    private void processToClassPath(boolean dump, String path) {
        if (dump) {
            System.out.println(" " + path);
        }
        File f = new File(path);
        if (f.exists()) {
            if (f.isDirectory()) {
                if (dump) {
                    System.out.println(" (Directory)");
                }
                // Load all the jars in that directory.
                File[] files = f.listFiles();
                if (files != null) {
                    for (File file : files) {
                        processClassPathFile(file, file.getAbsolutePath(), classToPathMap, AnalysisType.JAR, dump);
                    }
                }
            } else {
                processClassPathFile(f, path, classToPathMap, AnalysisType.JAR, dump);
            }
        } else {
            if (dump) {
                System.out.println(" (Can't access)");
            }
        }
    }

    private void addToRelativeClassPath(File file, String jarClassPath) {
        String[] classPaths = jarClassPath.split(" ");
        boolean dump = options.getOption(OptionsImpl.DUMP_CLASS_PATH);
        String relative = null;
        try {
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent == null) {
                return;
            }
            relative = parent.getCanonicalPath();
        } catch (IOException e) {
            return;
        }
        for (String path : classPaths) {
            processToClassPath(dump, relative + File.separatorChar + path);
        }
    }

    private void processClassPathFile(
        File file,
        String absolutePath,
        Map<String, JarSourceEntry> classToPathMap,
        AnalysisType analysisType,
        boolean dump
    ) {
        JarContent content = processClassPathFile(file, dump, analysisType);
        if (content == null) {
            return;
        }
        JarSourceEntry sourceEntry = new JarSourceEntry(analysisType, absolutePath);
        for (String name : content.getClassFiles()) {
            classToPathMap.put(name, sourceEntry);
        }
    }

    private JarContent processClassPathFile(final File file, boolean dump, AnalysisType analysisType) {
        ObjectList<String> content = new ObjectArrayList<>();
        Map<String, String> manifest;
        try {
            try (ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ)) {
                manifest = getManifestContent(zipFile);
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry entry = enumeration.nextElement();
                    if (!entry.isDirectory()) {
                        String name = entry.getName();
                        if (name.endsWith(".class")) {
                            if (dump) {
                                System.out.println("  " + name);
                            }
                            content.add(name);
                        } else {
                            if (dump) {
                                System.out.println("  [ignoring] " + name);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        if (analysisType == AnalysisType.WAR) {
            // Strip WEB-INF/classes from the front of class files.
            final int prefixLen = MiscConstants.WAR_PREFIX.length();
            content = Functional.map(
                Functional.filter(content, in -> in.startsWith(MiscConstants.WAR_PREFIX)),
                arg -> arg.substring(prefixLen)
            );
        }

        return new JarContentImpl(content, manifest, analysisType);
    }

    private Map<String, String> getManifestContent(ZipFile zipFile) {
        try {
            ZipEntry manifestEntry = zipFile.getEntry(MiscConstants.MANIFEST_PATH);
            Map<String, String> manifest;
            if (manifestEntry == null) {
                // Odd, but feh.
                manifest = MapFactory.newMap();
            } else {
                InputStream is = zipFile.getInputStream(manifestEntry);
                BufferedReader bis = new BufferedReader(new InputStreamReader(is));
                manifest = MapFactory.newMap();
                String line;
                while (null != (line = bis.readLine())) {
                    int idx = line.indexOf(':');
                    if (idx <= 0) continue;
                    manifest.put(line.substring(0, idx), line.substring(idx + 1).trim());
                }
                bis.close();
            }
            return manifest;
        } catch (Exception e) {
            return MapFactory.newMap();
        }
    }

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String specPath) {
        classRelocator = new ClassFileRelocator.Configurator().configureWith(usePath, specPath);
    }

    @Override
    public ClassFileRelocator getClassRelocator() {
        return classRelocator;
    }
}
