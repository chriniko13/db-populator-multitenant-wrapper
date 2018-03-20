package com.chriniko.example.multitenant.wrapper.core;

import com.chriniko.example.multitenant.wrapper.core.process.ProcessManager;
import com.chriniko.example.multitenant.wrapper.core.properties.PropertiesManager;
import com.chriniko.example.multitenant.wrapper.domain.DbPopulatorProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class WrapperKickstart {

    private static final String DB_POPULATOR_PROPERTIES_FILE = "dbPopulatorForTenant%d.properties";

    private static int TIME_BETWEEN_DB_POPULATOR_FIRE_IN_MS = 50;

    private static final String DB_POPULATOR_RUN_COMMAND = "java -jar db-populator-example-1.0-SNAPSHOT-jar-with-dependencies.jar %s";

    private final List<String> propertiesFilesAccumulator;
    private final ProcessManager processManager;
    private final PropertiesManager propertiesManager;
    private final ExecutorService executorService;

    private Map<String, DbPopulatorProcess> pidToProcessBindings;

    @Autowired
    public WrapperKickstart(ProcessManager processManager,
                            PropertiesManager propertiesManager,
                            Environment environment) {

        int numberOfTenants = Integer.parseInt(environment.getProperty("no.of.tenants"));

        propertiesFilesAccumulator = IntStream
                .rangeClosed(1, numberOfTenants)
                .boxed()
                .map(idx -> String.format(DB_POPULATOR_PROPERTIES_FILE, idx))
                .collect(Collectors.toList());

        executorService = Executors.newFixedThreadPool(propertiesFilesAccumulator.size());

        Runtime.getRuntime()
                .addShutdownHook(new Thread(
                                () -> {
                                    System.out.println("Shutting down operations for WrapperKickstart...");
                                    pidToProcessBindings.keySet().forEach(processManager::killProcess);
                                    executorService.shutdownNow();
                                }
                        )
                );

        this.processManager = processManager;
        this.propertiesManager = propertiesManager;
    }

    public void run() {

        try {

            final AtomicBoolean timeToFinish = new AtomicBoolean(false);

            pidToProcessBindings = fireDbPopulators();

            storeOutputOfDbPopulators(pidToProcessBindings, timeToFinish);

            monitorDbPopulators(pidToProcessBindings); // Note: this blocks until all db populators finish work.

            sleepInMs(5000); // Note: for throttling purposes (logs flush, etc).
            timeToFinish.getAndSet(true);

            sleepInMs(2000); // Note: for throttling purposes (executor service shutdown operation).
            System.exit(0);

        } catch (Exception error) {
            System.err.println("WrapperKickstart#run --- error occurred, error = " + error);
            error.printStackTrace(System.err);
            throw new RuntimeException(error);
        }
    }

    private Map<String, DbPopulatorProcess> fireDbPopulators() throws IOException {

        final Map<String, DbPopulatorProcess> pidToProcessBindings = new HashMap<>(propertiesFilesAccumulator.size());

        int currentTenant = 0;

        for (String propertyFile : propertiesFilesAccumulator) {

            // Note: fire off a db populator.
            String commandToFire = String.format(DB_POPULATOR_RUN_COMMAND, propertyFile);
            System.out.println("Will fire command: " + commandToFire);

            Process dbPopulatorProcess = Runtime
                    .getRuntime()
                    .exec(commandToFire);

            // Note: hold necessary info about fired off db populator.
            Properties dbPopulatorPropertiesFile = propertiesManager.loadProperties(propertyFile);

            String dbPopulatorProcessPid = String.valueOf(processManager.extractPid(dbPopulatorProcess));

            pidToProcessBindings.put(
                    dbPopulatorProcessPid,
                    new DbPopulatorProcess(
                            propertiesManager.getJdbcUrl(dbPopulatorPropertiesFile),
                            dbPopulatorProcess
                    )
            );


            // Note: do some printing and throttling.
            System.out.println("Fired off db populator for tenant: "
                    + (++currentTenant)
                    + " --- pid: "
                    + dbPopulatorProcessPid
                    + " --- process: "
                    + dbPopulatorProcess);
            System.out.println();

            sleepInMs(TIME_BETWEEN_DB_POPULATOR_FIRE_IN_MS);
        }

        return pidToProcessBindings;
    }

    private void storeOutputOfDbPopulators(Map<String, DbPopulatorProcess> pidToProcessBindings,
                                           AtomicBoolean timeToFinish) {

        pidToProcessBindings.forEach((pid, dbPopulatorProcess) -> {

            Runnable workToDo = processManager.storeOutputOfDbPopulatorProcessWork(
                    pid,
                    dbPopulatorProcess,
                    timeToFinish);

            executorService.submit(workToDo);

        });
    }

    private void monitorDbPopulators(Map<String, DbPopulatorProcess> pidToProcessBindings) {

        int printStatuses = 5;
        int counter = 0;

        while (!allDbPopulatorsHaveFinished(pidToProcessBindings)) {

            String statusesForFiredDbPopulators = pidToProcessBindings
                    .entrySet()
                    .stream()
                    .map((entry) -> {

                        String pid = entry.getKey();
                        DbPopulatorProcess dbPopulatorProcess = entry.getValue();

                        return String.format(
                                "process id: %s --- "
                                        + "process jdbc url: %s --- "
                                        + "still runs: %s",

                                pid,
                                dbPopulatorProcess.getJdbcUrl(),
                                processManager.isProcessAlive(pid)

                        );

                    })
                    .collect(Collectors.joining("\n", "[\n", "\n]"));

            // Note: print on fixed intervals...
            counter++;
            if (counter == printStatuses) {
                System.out.println(statusesForFiredDbPopulators);
                System.out.println();
                counter = 0;
            }
        }
    }

    private boolean allDbPopulatorsHaveFinished(Map<String, DbPopulatorProcess> pidToProcessBindings) {

        final List<Boolean> dbPopulatorsStatuses = pidToProcessBindings
                .keySet()
                .stream()
                .map(processManager::isProcessAlive)
                .collect(Collectors.toList());

        return dbPopulatorsStatuses.stream().noneMatch(Boolean.TRUE::equals);
    }

    private void sleepInMs(int ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

}
