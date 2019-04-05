package org.re.agent.model;

import org.hyperic.sigar.Sigar;

import lombok.AccessLevel;

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Getter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class SigarVersion {
    private String buildDate = Sigar.BUILD_DATE;
    private String nativeBuildDate = Sigar.NATIVE_BUILD_DATE;
    private String scmVersion = Sigar.SCM_REVISION;
    private String nativeScmVersion = Sigar.NATIVE_SCM_REVISION;
    private long fieldNotImpl = Sigar.FIELD_NOTIMPL;
    private String version = Sigar.VERSION_STRING;
    private String nativeVersion = Sigar.NATIVE_VERSION_STRING;

    public static SigarVersion get() {
        return new SigarVersion();
    }
}
