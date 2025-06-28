package org.mtr.announcement.data;

public record Voice(String id, String runtimeCode, String ckptPath, String pthPath, String voiceSamplePath, String voiceSampleText) {
}
