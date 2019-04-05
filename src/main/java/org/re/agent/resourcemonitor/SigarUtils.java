package org.re.agent.resourcemonitor;

import org.hyperic.sigar.Sigar;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SigarUtils {
    private static final Sigar SINGAR = new Sigar();

    public static Sigar getSigar() {
        return SINGAR;
    }
}
