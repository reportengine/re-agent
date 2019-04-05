package org.re.agent.resourcemonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.DirUsage;
import org.hyperic.sigar.DiskUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Swap;
import org.re.agent.ReportEngineClient;
import org.re.agent.model.ControlMessage;
import org.re.agent.model.ReMetric;
import org.re.agent.utils.Utils;

import com.google.common.base.CaseFormat;

import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Runner {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static Timer TIMER = null;
    private static ControlMessage ctlMessage = null;

    public static boolean isRunning() {
        return RUNNING.get();
    }

    private static String getMeasurementSuffix(String resourceName) {
        return String.format("%s_%s", ctlMessage.getMeasurementSuffix(), resourceName);
    }

    public static void execute(ControlMessage message) {
        if (message.getCommand().equalsIgnoreCase("start")) {
            if (isRunning()) {
                logger.info("Service already running. {}", ctlMessage);
                return;
            }
            ctlMessage = message;
            start();
        } else if (message.getCommand().equalsIgnoreCase("stop")) {
            if (!isRunning()) {
                logger.info("Service not running.");
                return;
            }
            ctlMessage = message;
            stop();
        } else {
            logger.warn("Unknown command: {}", message);
            return;
        }
    }

    public static void start() {
        if (RUNNING.get()) {
            return;
        }
        try {
            TIMER = new Timer();
            TIMER.schedule(new MonitorResouces(ctlMessage), 10000L, ctlMessage.monitorIntervalInMillisecond());
            logger.debug("Start triggered. {}", ctlMessage);
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        } finally {
            RUNNING.set(true);
        }
    }

    public static void stop() {
        if (!RUNNING.get()) {
            return;
        }
        try {
            if (TIMER != null) {
                TIMER.cancel();
            }
            logger.debug("Stop triggered. {}", ctlMessage);
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        } finally {
            RUNNING.set(false);
        }
    }

    static class MonitorResouces extends TimerTask {
        ControlMessage _ctlMessage = null;

        public MonitorResouces(ControlMessage ctlMessage) {
            this._ctlMessage = ctlMessage;
        }

        private String ip() {
            try {
                return SigarUtils.getSigar().getNetInterfaceConfig().getAddress();
            } catch (Exception ex) {
                logger.error("Exception,", ex);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (!ctlMessage.isActive()) {
                stop();
                return;
            }
            long timestamp = System.currentTimeMillis();
            List<ReMetric> metrics = new ArrayList<>();
            for (String rawResource : _ctlMessage.getResources()) {
                Resource resource = Resource.get(rawResource);
                Map<String, Object> data = new HashMap<>();
                Map<String, String> labels = new HashMap<>();
                try {
                    switch (resource.getName()) {
                        case "cpu":
                            CpuPerc cpu = SigarUtils.getSigar().getCpuPerc();
                            data.put("combined", cpu.getCombined());
                            data.put("idle", cpu.getIdle());
                            data.put("irq", cpu.getIrq());
                            data.put("nice", cpu.getNice());
                            data.put("softIrq", cpu.getSoftIrq());
                            data.put("stolen", cpu.getStolen());
                            data.put("sys", cpu.getSys());
                            data.put("user", cpu.getUser());
                            data.put("wait", cpu.getWait());
                            break;
                        case "memory":
                            Mem mem = SigarUtils.getSigar().getMem();
                            data.put("actualFree", mem.getActualFree());
                            data.put("actualUsed", mem.getActualUsed());
                            data.put("free", mem.getFree());
                            data.put("freePercent", mem.getFreePercent());
                            data.put("ram", mem.getRam());
                            data.put("total", mem.getTotal());
                            data.put("used", mem.getUsed());
                            data.put("usedPercent", mem.getUsedPercent());
                            break;
                        case "swap":
                            Swap swap = SigarUtils.getSigar().getSwap();
                            data.put("free", swap.getFree());
                            data.put("pageIn", swap.getPageIn());
                            data.put("pageOut", swap.getPageOut());
                            data.put("total", swap.getTotal());
                            data.put("used", swap.getUsed());
                            break;
                        case "disk":
                            DiskUsage diskUsage = SigarUtils.getSigar().getDiskUsage(resource.getIdentifier());
                            data.put("queue", diskUsage.getQueue());
                            data.put("readBytes", diskUsage.getReadBytes());
                            data.put("reads", diskUsage.getReads());
                            data.put("serviceTime", diskUsage.getServiceTime());
                            data.put("writeBytes", diskUsage.getWriteBytes());
                            data.put("writes", diskUsage.getWrites());
                            break;
                        case "dir":
                            DirUsage dirUsage = SigarUtils.getSigar().getDirUsage(resource.getIdentifier());
                            data.put("dir", resource.getIdentifier());
                            data.put("blkdevs", dirUsage.getBlkdevs());
                            data.put("chrdevs", dirUsage.getChrdevs());
                            data.put("diskUsage", dirUsage.getDiskUsage());
                            data.put("files", dirUsage.getFiles());
                            data.put("sockets", dirUsage.getSockets());
                            data.put("subdirs", dirUsage.getSubdirs());
                            data.put("symlinks", dirUsage.getSymlinks());
                            data.put("total", dirUsage.getTotal());
                            break;
                        case "jvm":
                            JVM jvm = new JVM(resource.getIdentifier());
                            jvm.connect();
                            data = jvm.metrics();
                            if (data.get("labels") != null) {
                                labels = (Map<String, String>) data.remove("labels");
                            }
                            jvm.close();
                            break;
                    }
                } catch (Exception ex) {
                    logger.error("Exception,", ex);
                }

                if (data != null) {
                    Map<String, Object> snakeCase = new HashMap<>();
                    for (String key : data.keySet()) {
                        snakeCase.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), data.get(key));
                    }
                    // update labels
                    labels.put("ip", ip());

                    metrics.add(ReMetric.builder()
                            .suiteId(ctlMessage.getSuiteId())
                            .measurementSuffix(getMeasurementSuffix(resource.getName()))
                            .timestamp(timestamp)
                            .data(snakeCase)
                            .labels(labels)
                            .build());
                }
            }

            if (metrics.size() > 0) {
                ReportEngineClient client = Utils.client();
                client.addMetricsData(metrics);
            }
        }
    }
}
