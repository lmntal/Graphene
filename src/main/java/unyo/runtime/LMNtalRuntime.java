package unyo.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;

import unyo.entity.Graphy;
import unyo.entity.Graph;

public class LMNtalRuntime {

    private static List<String> readStdout(Process process) {
        final List<String> ret = new ArrayList<String>();
        final Process p = process;

        Thread inThread = new Thread() {
            @Override
            public void run() {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String buf;
                    while ((buf = br.readLine()) != null) {
                        ret.add(buf);
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread outThread = new Thread() {
            @Override
            public void run() {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));
                while (!pw.checkError()) {
                    pw.println("");
                }
                pw.close();
            }
        };

        inThread.start();
        outThread.start();

        try {
            inThread.join();
            outThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static Graph execute(File file, List<String> options) {

        try {
            ProcessBuilder pb = new ProcessBuilder(java.util.Arrays.asList("/Users/charlie/Documents/slim/slim/src/slim", "--json-dump", file.getAbsolutePath()));
            pb.redirectErrorStream(true);

            Process p = pb.start();
            List<String> lines = readStdout(p);

            return Graphy.fromString(lines.get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
