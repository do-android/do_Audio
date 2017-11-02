package doext.implement;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sinaapp.bashell.VoAACEncoder;

import core.DoServiceContainer;

public class do_AACRecorder extends RecorderBase implements Runnable {

	@Override
	public void startRecord(int time, String quality, String outPath) {
		super.startRecord(time, quality, outPath);
		init(this);
	}

	@Override
	public void run() {
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(outPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		VoAACEncoder vo = new VoAACEncoder();
		vo.Init(mSampleRate, 16000, (short) 1, (short) 1);// 采样率:16000,bitRate:32k,声道数:1，编码:0.raw
		try {
			mAudioRecord.startRecording();
			onRecordTimeChangeTask();
			if (onRecordListener != null) {
				onRecordListener.onStart();
			}
			long startTimeMillis = System.currentTimeMillis();
			while (isRecording) {
				byte[] mBuffer = new byte[mBufferSize];
				int bufferRead = mAudioRecord.read(mBuffer, 0, mBufferSize);
				byte[] ret = vo.Enc(mBuffer);
				if (bufferRead > 0) {
					try {
						output.write(ret);
					} catch (IOException _err) {
						DoServiceContainer.getLogEngine().writeError("AAC录音写入失败：", _err);
						throw new Exception(_err);
					}
				}

				///////////////////获取音量
				long v = 0;
				// 将 buffer 内容取出，进行平方和运算  
				for (int i = 0; i < mBuffer.length; i++) {
					v += mBuffer[i] * mBuffer[i];
				}
				// 平方和除以数据总长度，得到音量大小。  
				double mean = v / (double) bufferRead;
//				volume = 10 * Math.log10(mean);
				//不明白，感觉幅度太小了，所以乘以2
				volume = 20 * Math.log10(mean);

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
			fireFinishedEvent();
		} catch (Exception e) {
			stopRecord();
			if (onRecordListener != null) {
				onRecordListener.onError();
			}
			DoServiceContainer.getLogEngine().writeError("录音失败：startRecord", e);
			e.printStackTrace();
		} finally {
			vo.Uninit();
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
