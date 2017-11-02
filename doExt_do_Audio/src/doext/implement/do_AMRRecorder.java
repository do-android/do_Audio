package doext.implement;

import android.media.MediaRecorder;
import android.os.Handler;
import core.DoServiceContainer;

public class do_AMRRecorder extends RecorderBase implements Runnable {

	@Override
	public void startRecord(final int time, final String quality, final String outPath) {
		super.startRecord(time, quality, outPath);
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风  
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);// 设置输出文件格式  
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);// 设置编码格式  
		mediaRecorder.setOutputFile(outPath);// 使用绝对路径进行保存文件  
		mediaRecorder.setAudioSamplingRate(8000);
		try {
			// 开始录音
			mediaRecorder.prepare();
			mediaRecorder.start();
			updateMicStatus();
			onRecordTimeChangeTask();
			if (onRecordListener != null) {
				onRecordListener.onStart();
			}
		} catch (Exception e) {
			stopRecord();
			if (onRecordListener != null) {
				onRecordListener.onError();
			}
			DoServiceContainer.getLogEngine().writeError("AMR录音写入失败：", e);
			e.printStackTrace();
		}
		new Thread(this).start();
	}

	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		while (isRecording) {
			long endTimeMillis = System.currentTimeMillis();
			totalTimeMillis = endTimeMillis - startTime;
			if (time != -1 && (totalTimeMillis > time)) {
				stopRecord();
			}
		}
	}

	//////////////////////////amr 获取录音音量
	private int BASE = 1;
	private int SPACE = 100;// 间隔取样时间  
	private final Handler mHandler = new Handler();
	private Runnable mUpdateMicStatusTimer = new Runnable() {
		public void run() {
			updateMicStatus();
		}
	};

	/**
	 * 更新话筒状态
	 */
	private void updateMicStatus() {
		if (mediaRecorder != null) {
			double ratio = (double) mediaRecorder.getMaxAmplitude() / BASE;
			//分贝  
			if (ratio > 1) {
				volume = 20 * Math.log10(ratio);
			}
			//方法2  振幅范围mediaRecorder.getMaxAmplitude():1-32767
			//volume = 100 * mediaRecorder.getMaxAmplitude() / 32768 + 1;

			if (onRecordListener != null) {
				onRecordListener.onRecordVolumeChange(volume);
			}
			mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
		}
	}
}
