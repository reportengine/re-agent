package org.re.agent.resourcemonitor;

import java.io.File;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.management.ThreadMXBean;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JVM {
    private String name;
    private JMXConnector connector;
    private MBeanServerConnection mbsc;
    private String pid;

    public JVM(String name) {
        this.name = name;
    }

    private VirtualMachineDescriptor getVmDescriptor() {
        List<VirtualMachineDescriptor> vmDescriptorList = VirtualMachine.list();
        try {
            for (VirtualMachineDescriptor vmDesc : vmDescriptorList) {
                if (name != null) {
                    String vmDisplayName = vmDesc.displayName();
                    String[] targetVMDescs = name.split(",");
                    for (String targetVMDesc : targetVMDescs) {
                        if (vmDisplayName.contains(targetVMDesc)) {
                            return vmDesc;
                        }
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Error while scanning JVMs,", ex);
        }
        return null;
    }

    public void connect() {
        if (connector == null) {
            VirtualMachineDescriptor vmDesc = getVmDescriptor();
            if (vmDesc != null) {
                try {
                    pid = vmDesc.id();
                    VirtualMachine vm = VirtualMachine.attach(vmDesc);
                    String localConnectorAddr = vm.getAgentProperties()
                            .getProperty("com.sun.management.jmxremote.localConnectorAddress");
                    logger.trace("System Properties: " + vm.getSystemProperties());
                    logger.trace("Agent Properties: " + vm.getAgentProperties());
                    if (localConnectorAddr == null) {
                        String javaHome = vm.getSystemProperties().getProperty("java.home");
                        String agentJar = javaHome + File.separator + "lib" + File.separator + "management-agent.jar";
                        vm.loadAgent(agentJar);
                        localConnectorAddr = vm.getAgentProperties()
                                .getProperty("com.sun.management.jmxremote.localConnectorAddress");
                    }
                    vm.detach();

                    JMXServiceURL serviceURL = new JMXServiceURL(localConnectorAddr);
                    connector = JMXConnectorFactory.connect(serviceURL);
                    mbsc = connector.getMBeanServerConnection();
                } catch (Exception ex) {
                    logger.error("Exception,", ex);
                }
            }
        }
    }

    public void close() {
        if (connector != null) {
            try {
                connector.close();
                connector = null;
            } catch (IOException ex) {
                logger.error("Exception,", ex);
            }
        }
    }

    public Map<String, Object> metrics() {
        Map<String, Object> data = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        data.put("labels", labels);
        try {
            // update labels
            labels.put("pid", pid);

            // runtime bean
            ObjectName runtimeMXObject = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
            RuntimeMXBean runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                    runtimeMXObject.toString(), RuntimeMXBean.class);
            labels.put("name", runtimeMXBean.getName());

            // memory bean
            ObjectName memoryMXObject = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
            MemoryMXBean memoryMXBean = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                    memoryMXObject.toString(), MemoryMXBean.class);
            // update heap
            MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
            data.put("heap_commited", heap.getCommitted());
            data.put("heap_init", heap.getInit());
            data.put("heap_max", heap.getMax());
            data.put("heap_used", heap.getUsed());
            data.put("heap_used_perc", (heap.getUsed() * 100.0) / heap.getMax());
            // update non heap
            MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
            data.put("non_heap_commited", nonHeap.getCommitted());
            data.put("non_heap_init", nonHeap.getInit());
            data.put("non_heap_used", nonHeap.getUsed());

            // thread bean
            ObjectName threadMXObject = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
            ThreadMXBean threadMXBean = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                    threadMXObject.toString(), ThreadMXBean.class);
            data.put("threads_peak", threadMXBean.getPeakThreadCount());
            data.put("threads_live", threadMXBean.getThreadCount());
            data.put("threads_daemon", threadMXBean.getDaemonThreadCount());
            data.put("threads_started_total", threadMXBean.getTotalStartedThreadCount());

            // class bean
            ObjectName classLoadingMXObject = new ObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME);
            ClassLoadingMXBean classLoadingMXBean = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                    classLoadingMXObject.toString(), ClassLoadingMXBean.class);
            data.put("class_loaded", classLoadingMXBean.getLoadedClassCount());
            data.put("class_loaded_total", classLoadingMXBean.getTotalLoadedClassCount());

            // gc bean
            ObjectName gcObjectNames = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");
            for (ObjectName gcMXObject : mbsc.queryNames(gcObjectNames, null)) {
                GarbageCollectorMXBean gcMXBean = ManagementFactory.newPlatformMXBeanProxy(
                        mbsc, gcMXObject.getCanonicalName(), GarbageCollectorMXBean.class);
                data.put(String.format("gc_collection_count_%s", gcMXBean.getName().replaceAll(" ", "")),
                        gcMXBean.getCollectionCount());
                data.put(String.format("gc_collection_time_%s", gcMXBean.getName().replaceAll(" ", "")),
                        gcMXBean.getCollectionTime());
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
        return data;
    }
}
