package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.List;

/** Windows Job Object configured with KILL_ON_JOB_CLOSE. */
final class WindowsJobObjectContainment implements ManagedProcessContainment {
    private static final int JOB_OBJECT_EXTENDED_LIMIT_INFORMATION = 9;
    private static final int JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000;
    private static final int PROCESS_TERMINATE = 0x0001;
    private final WinNT.HANDLE job;
    private String diagnostics;

    private WindowsJobObjectContainment(WinNT.HANDLE job, String diagnostics) {
        this.job = job;
        this.diagnostics = diagnostics;
    }

    static ManagedProcessContainment open() {
        try {
            WinNT.HANDLE handle = JobKernel32.INSTANCE.CreateJobObjectW(null, null);
            if (handle == null || WinNT.INVALID_HANDLE_VALUE.equals(handle)) {
                return new WindowsJobObjectContainment(null,
                        "job-create-failed:" + Native.getLastError());
            }
            ExtendedLimitInformation limits = new ExtendedLimitInformation();
            limits.basicLimitInformation.limitFlags =
                    new WinDef.DWORD(JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE);
            limits.write();
            boolean configured = JobKernel32.INSTANCE.SetInformationJobObject(
                    handle, JOB_OBJECT_EXTENDED_LIMIT_INFORMATION,
                    limits.getPointer(), limits.size());
            if (!configured) {
                Kernel32.INSTANCE.CloseHandle(handle);
                return new WindowsJobObjectContainment(null,
                        "job-configure-failed:" + Native.getLastError());
            }
            return new WindowsJobObjectContainment(handle, "windows-job-object");
        } catch (LinkageError | RuntimeException failure) {
            return new WindowsJobObjectContainment(null,
                    "job-unavailable:" + failure.getClass().getSimpleName());
        }
    }

    @Override
    public boolean attach(Process process) {
        if (job == null || process == null) return false;
        WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_SET_QUOTA | WinNT.PROCESS_SET_INFORMATION
                        | PROCESS_TERMINATE, false, Math.toIntExact(process.pid()));
        if (processHandle == null) {
            diagnostics = "job-open-process-failed:" + Native.getLastError();
            return false;
        }
        try {
            boolean attached = JobKernel32.INSTANCE.AssignProcessToJobObject(job, processHandle);
            if (!attached) diagnostics = "job-attach-failed:" + Native.getLastError();
            return attached;
        } finally {
            Kernel32.INSTANCE.CloseHandle(processHandle);
        }
    }

    @Override public String diagnostics() { return diagnostics; }

    @Override
    public void close() {
        if (job != null) Kernel32.INSTANCE.CloseHandle(job);
    }

    interface JobKernel32 extends StdCallLibrary {
        JobKernel32 INSTANCE = Native.load("kernel32", JobKernel32.class,
                W32APIOptions.DEFAULT_OPTIONS);

        WinNT.HANDLE CreateJobObjectW(Pointer securityAttributes, WString name);
        boolean SetInformationJobObject(WinNT.HANDLE job, int informationClass,
                                        Pointer information, int informationLength);
        boolean AssignProcessToJobObject(WinNT.HANDLE job, WinNT.HANDLE process);
    }

    @Structure.FieldOrder({
            "perProcessUserTimeLimit", "perJobUserTimeLimit", "limitFlags",
            "minimumWorkingSetSize", "maximumWorkingSetSize", "activeProcessLimit",
            "affinity", "priorityClass", "schedulingClass"
    })
    public static class BasicLimitInformation extends Structure {
        public long perProcessUserTimeLimit;
        public long perJobUserTimeLimit;
        public WinDef.DWORD limitFlags;
        public BaseTSD.SIZE_T minimumWorkingSetSize;
        public BaseTSD.SIZE_T maximumWorkingSetSize;
        public WinDef.DWORD activeProcessLimit;
        public BaseTSD.ULONG_PTR affinity;
        public WinDef.DWORD priorityClass;
        public WinDef.DWORD schedulingClass;
    }

    @Structure.FieldOrder({
            "readOperationCount", "writeOperationCount", "otherOperationCount",
            "readTransferCount", "writeTransferCount", "otherTransferCount"
    })
    public static class IoCounters extends Structure {
        public long readOperationCount;
        public long writeOperationCount;
        public long otherOperationCount;
        public long readTransferCount;
        public long writeTransferCount;
        public long otherTransferCount;
    }

    @Structure.FieldOrder({
            "basicLimitInformation", "ioInfo", "processMemoryLimit", "jobMemoryLimit",
            "peakProcessMemoryUsed", "peakJobMemoryUsed"
    })
    public static class ExtendedLimitInformation extends Structure {
        public BasicLimitInformation basicLimitInformation = new BasicLimitInformation();
        public IoCounters ioInfo = new IoCounters();
        public BaseTSD.SIZE_T processMemoryLimit;
        public BaseTSD.SIZE_T jobMemoryLimit;
        public BaseTSD.SIZE_T peakProcessMemoryUsed;
        public BaseTSD.SIZE_T peakJobMemoryUsed;
    }
}
