package doext.implement;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.deviceone.lame.DoMP3lame;

import core.DoServiceContainer;
import core.helper.DoIOHelper;

public class do_MP3Recorder extends RecorderBase implements Runnable {

	@Override
	public void startRecord(final int time, final String quality, final String outPath) {
		super.startRecord(time, quality, outPath);
		init(this);
	}

	@Override
	public void run() {
		String temp = outPath + ".raw";
		DataOutputStream output = null;
		try {
			output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp)));
		} catch (FileNotFoundException e) {
		}
		try {
			mAudioRecord.startRecording(); // 开启录音获取音频数据
			onRecordTimeChangeTask();
			if (onRecordListener != null) {
				onRecordListener.onStart();
			}
			// 开始录音
			long startTimeMillis = System.currentTimeMillis();
			int readSize = 0;
			while (isRecording) {
				readSize = mAudioRecord.read(mBuffer, 0, mBufferSize);
				for (int i = 0; i < readSize; i++) {
					output.writeShort(mBuffer[i]);
				}

				///////////////////获取音量
				long v = 0;
				// 将 buffer 内容取出，进行平方和运算  
				for (int i = 0; i < mBuffer.length; i++) {
					v += mBuffer[i] * mBuffer[i];
				}
				// 平方和除以数据总长度，得到音量大小。  
				double mean = v / (double) readSize;
				volume = 10 * Math.log10(mean);
				if (onRecordListener != null) {
					onRecordListener.onRecordVolumeChange(volume);
				}
				////////////////////

				long endTimeMillis = System.currentTimeMillis();
				totalTimeMillis = endTimeMillis - startTimeMillis;
				if ((totalTimeMillis > time) && time != -1) {
					stopRecord();
				}
			}
			output.flush();
			DoMP3lame lame = new DoMP3lame(1, mSampleRate, 96);
			lame.toMP3(temp, outPath);
			fireFinishedEvent();
			DoIOHelper.deleteFile(temp);
		} catch (Exception e) {
			stopRecord();
			if (onRecordListener != null) {
				onRecordListener.onError();
			}
			DoServiceContainer.getLogEngine().writeError("录音失败：startRecord", e);
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
