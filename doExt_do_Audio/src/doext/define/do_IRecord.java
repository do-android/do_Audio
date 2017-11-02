package doext.define;

public interface do_IRecord {

	interface OnRecordListener {

		void onStart();

		void onRecordTimeChange(long totalTimeMillis);
		
		void onRecordVolumeChange(double volume);

		void onError();

		void onFinished(String outPath, long totalTimeMillis, long fileSize);
	}

	void startRecord(int time, String quality, String outPath);

	void stopRecord();

	void setOnRecordListener(OnRecordListener onRecordListener);

	void fireFinishedEvent();
}
