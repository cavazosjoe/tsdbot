package org.tsd.tsdbot.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.hyperic.sigar.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;

@Singleton
public class SystemStats implements Stats {

    private static final Logger log = LoggerFactory.getLogger(SystemStats.class);

    private static final String deviceFmt =
            "TxBytes=%s, TxErrors=%s, TxPackets=%s, TxDropped=%s, RxBytes=%s, RxErrors=%s, RxPackets=%s, RxDropped=%s";
    private static final DecimalFormat decimalFormat = new DecimalFormat("##0.0000");

    @Inject
    private Sigar sigar;

    @Override
    public LinkedHashMap<String, Object> getReport() {

        LinkedHashMap<String, Object> report = new LinkedHashMap<>();

        try {
            CpuPerc cpuPerc = sigar.getCpuPerc();
            report.put("CPU Usage", CpuPerc.format(cpuPerc.getCombined()));
            report.put("# Cores", sigar.getCpuInfoList().length);
        } catch (SigarException e) {
            log.error("Error getting system CPU info", e);
        }

        try {
            Mem mem = sigar.getMem();
            report.put("RAM", mem.getRam());
            report.put("Mem Usage", decimalFormat.format(mem.getUsedPercent()));
        } catch (Exception e) {
            log.error("Error getting system memory info", e);
        }

        try {
            NetConnection[] netCnx = sigar.getNetConnectionList(NetFlags.CONN_CLIENT | NetFlags.CONN_PROTOCOLS);
            report.put("Connections", netCnx.length);

            String[] netDevices = sigar.getNetInterfaceList();
            for(String device : netDevices) {
                NetInterfaceConfig config = sigar.getNetInterfaceConfig(device);
                NetInterfaceStat stat = sigar.getNetInterfaceStat(device);
                if(stat.getTxBytes() > 0) {
                    report.put(config.getName(), String.format(deviceFmt,
                            stat.getTxBytes(), stat.getTxErrors(), stat.getTxPackets(), stat.getTxDropped(),
                            stat.getRxBytes(), stat.getRxErrors(), stat.getRxPackets(), stat.getRxDropped()
                    ));
                }
            }
        } catch (SigarException e) {
            log.error("Error getting network stats", e);
        }

        return report;
    }

    @Override public void processMessage(String channel, String sender, String login, String hostname, String message) {}
    @Override public void processAction(String sender, String login, String hostname, String target, String action) {}
}
