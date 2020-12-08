package jbse;

import jbse.apps.run.Run;
import jbse.apps.run.RunParameters;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
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

    @Option(name = "-klass", usage = "target klass", required = true)
    private String klass;

    @Option(name = "-method", usage = "target method name", required = true)
    private String methodName;

    @Option(name = "-desc", usage = "target method descriptor", required = true)
    private String desc;

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
        set(p);
        final Run r = new Run(p);
        r.run();
    }

    private void set(RunParameters p) {
        p.addUserClasspath(userClassPath);
        p.setJBSELibPath(jbseLib);
        p.setMethodSignature(klass, desc, methodName);
        p.setKexConfig(kexConf);
        p.setDecisionProcedureType(Z3);
        p.setExternalDecisionProcedurePath("/usr/bin/z3");
        p.setOutputFileName("./out/runIf_z3.txt");
        p.setStateFormatMode(DESCRIPTOR);
        p.setStepShowMode(LEAVES);
    }
}
