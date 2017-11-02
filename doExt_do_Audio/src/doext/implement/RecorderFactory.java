package doext.implement;

import doext.define.do_IRecord;

public class RecorderFactory {

	protected static do_IRecord getRecorder(String type) {
		if ("amr".equalsIgnoreCase(type)) {
			return new do_AMRRecorder();
		} else if ("aac".equalsIgnoreCase(type)) {
			return new do_AACRecorder();
		} else if ("mp3".equalsIgnoreCase(type)) {
			return new do_MP3Recorder();
		}
		return null;
	}

}
