package com.chriniko.example.multitenant.wrapper.core.process;

import com.chriniko.example.multitenant.wrapper.domain.DbPopulatorProcess;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ProcessManager {

    public void killProcess(String pid) {

        try {
            String cmd = "kill -9 %s";

            Process process = Runtime.getRuntime().exec(String.format(cmd, pid));
            process.waitFor();

        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    public boolean isProcessAlive(String pid) {

        try {

            String[] cmd = {"/bin/sh", "-c", "ps -p " + pid + " | wc -l"};

            Process process = Runtime.getRuntime().exec(cmd);

            process.waitFor();

            try (BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                StringBuilder output = new StringBuilder();

                while ((line = buf.readLine()) != null) {
                    output.append(line).append("\n");
                }

                String temp = output
                        .toString()
                        .replace("\n", "")
                        .replace("\r", "");

                return Integer.valueOf(temp) == 2;
            }

        } catch (Exception error) {
            throw new RuntimeException(error);
        }

    }

    public long extractPid(Process p) {

        try {

            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {

                long pid;

                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);

                pid = f.getLong(p);
                f.setAccessible(false);

                return pid;
            }

            throw new IllegalStateException("Only UNIX supported.");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Runnable storeOutputOfDbPopulatorProcessWork(String pid,
                                                        DbPopulatorProcess dbPopulatorProcess,
                                                        AtomicBoolean timeToFinish) {

        return () -> {
            try {

                Process process = dbPopulatorProcess.getProcess();

                String fileName = "db_execution_output__"
                        + pid
                        + "__"
                        + System.currentTimeMillis()
                        + ".txt";

                Writer output = new BufferedWriter(new FileWriter(fileName));

                InputStream in = process.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;

                while (!timeToFinish.get()) {

                    while ((line = br.readLine()) != null) {
                        output.write(line);
                        output.write("\n");
                    }

                }
                output.close();

            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        };

    }

}
