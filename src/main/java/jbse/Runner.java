package jbse;

import jbse.apps.run.Run;
import jbse.apps.run.RunParameters;
import jbse.bc.Signature;
import org.jetbrains.research.kfg.Package;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static jbse.apps.run.RunParameters.DecisionProcedureType.Z3;
import static jbse.apps.run.RunParameters.StateFormatMode.DESCRIPTOR;
import static jbse.apps.run.RunParameters.StepShowMode.LEAVES;

public class Runner {
    @Option(name = "-cp", usage = "target class path", required = true)
    private String userClassPath;

    @Option(name = "-jbse", usage = "jbse lib path", required = true)
    private String jbseLib;

    @Option(name = "-kex", usage = "kex config path", required = true)
    private String kexConf;

    @Option(name = "-target", usage = "target package", required = true)
    private String targetPackage;

    public static void main(String[] args) throws IOException {
        try {
            new Runner().doMain(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);

        // if you have a wider console, you could increase the value;
        // here 80 is also the default
        parser.setUsageWidth(80);

        try {
            parser.parseArgument(args);
            if (args.length == 0)
                throw new CmdLineException(parser, "No argument is given");
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        final RunParameters p = new RunParameters();
        initParams(p);
        run(p, p.getUserClasspath());
    }

    private void initParams(RunParameters p) {
        p.addUserClasspath(userClassPath);
        p.setJBSELibPath(jbseLib);
        p.setKexConfig(kexConf);
        p.setDecisionProcedureType(Z3);
        p.setExternalDecisionProcedurePath("/usr/bin/z3");
        p.setOutputFileName("./out/runIf_z3.txt");
        p.setStateFormatMode(DESCRIPTOR);
        p.setStepShowMode(LEAVES);
        p.setAPackage(targetPackage);
    }

    private static List<Signature> getMethodsFromJar(Path jarPath, org.jetbrains.research.kfg.Package pkg) throws IOException {
        List<Signature> result = new ArrayList<>();
        JarFile jar = new JarFile(jarPath.toFile());
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class") && pkg.isParent(entry.getName())) {
                result.addAll(getSignaturesFromKlass(jar, entry));
            }
        }
        return result;
    }

    private static List<Signature> getSignaturesFromKlass(JarFile file, JarEntry klass) throws IOException {
        ClassNode node = readClassNode(file.getInputStream(klass));
        List<Signature> result = new ArrayList<>();
        for (MethodNode mn : node.methods) {
            result.add(new Signature(node.name, mn.desc, mn.name));
        }
        return result;
    }

    private static ClassNode readClassNode(InputStream input) throws IOException {
        ClassReader classReader = new ClassReader(input);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    private void run(RunParameters init, Path jarFile) throws IOException {
        List<Signature> methods = getMethodsFromJar(jarFile, init.getaPackage());
        for (Signature method : methods) {
            System.out.println("Running on method " + method);
            RunParameters newParams = init.clone();
            newParams.setMethodSignature(method.getClassName(), method.getDescriptor(), method.getName());
            final Run r = new Run(newParams);
            r.run();
        }
    }
}
