package doext.implement;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
import doext.define.do_Audio_IMethod;
import doext.define.do_IRecord;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_AudioPlay_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_Audio_Model extends DoSingletonModule implements do_Audio_IMethod {

	private MediaPlayer mediaPlayer;
	private do_IRecord mRecord;
	private boolean isStop;
	private String outPath;
	private Timer timer;
	private String currentRecordType;
	//判断Record是否是开始状态
	private boolean isStart;

	public do_Audio_Model() throws Exception {
		super();
		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setOnErrorListener(new OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					getEventCenter().fireEvent("error", new DoInvokeResult(getUniqueKey()));
					stopPlayer();
					return false;
				}
			});
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					getEventCenter().fireEvent("playFinished", new DoInvokeResult(getUniqueKey()));
					stopPlayer();
				}
			});
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("play".equals(_methodName)) {
			play(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("stop".equals(_methodName)) {
			stop(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("pause".equals(_methodName)) {
			pause(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("resume".equals(_methodName)) {
			resume(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("startRecord".equals(_methodName)) {
			startRecord(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("stopRecord".equals(_methodName)) {
			stopRecord(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("playAsync".equals(_methodName)) {
			playAsync(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		if ("stopRecordAsync".equals(_methodName)) {
			stopRecordAsync(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	/**
	 * 开始播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void play(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String path = DoJsonHelper.getString(_dictParas, "path", "");
		int position = DoJsonHelper.getInt(_dictParas, "point", 0);
		if (null == DoIOHelper.getHttpUrlPath(path)) {
			path = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), path);
		} else {
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		}
		isStop = false;
		mediaPlayer.reset();//把各项参数恢复到初始状态
		if (DoIOHelper.isAssets(path)) {
			Context mContext = DoServiceContainer.getPageViewFactory().getAppContext();
			AssetFileDescriptor _mFileDescriptor = mContext.getAssets().openFd(DoIOHelper.getAssetsRelPath(path));
			mediaPlayer.setDataSource(_mFileDescriptor.getFileDescriptor(), _mFileDescriptor.getStartOffset(), _mFileDescriptor.getLength());
		} else {
			mediaPlayer.setDataSource(path);
		}
		mediaPlayer.prepare();//进行缓冲  
		mediaPlayer.setOnPreparedListener(new PreparedListener(position));//注册一个监听器
	}
	/**
	 * 开始播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void playAsync(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String path = DoJsonHelper.getString(_dictParas, "path", "");
		int position = DoJsonHelper.getInt(_dictParas, "point", 0);
		if (null == DoIOHelper.getHttpUrlPath(path)) {
			path = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), path);
		} else {
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		}
		isStop = false;
		mediaPlayer.reset();//把各项参数恢复到初始状态
		if (DoIOHelper.isAssets(path)) {
			Context mContext = DoServiceContainer.getPageViewFactory().getAppContext();
			AssetFileDescriptor _mFileDescriptor = mContext.getAssets().openFd(DoIOHelper.getAssetsRelPath(path));
			mediaPlayer.setDataSource(_mFileDescriptor.getFileDescriptor(), _mFileDescriptor.getStartOffset(), _mFileDescriptor.getLength());
		} else {
			mediaPlayer.setDataSource(path);
		}
		mediaPlayer.prepareAsync();//进行缓冲  
		mediaPlayer.setOnPreparedListener(new PreparedListener(position));//注册一个监听器
	}

	/**
	 * 暂停播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void pause(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			_invokeResult.setResultInteger(mediaPlayer.getCurrentPosition());
		}
	}

	/**
	 * 继续播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void resume(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
			try {
				if (!isStop) {
					mediaPlayer.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 停止播放；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void stop(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			isStop = true;
			mediaPlayer.stop();
			stopPlayer();
		}
	}

	//释放
	private void stopPlayer() {
		if (null != timer) {
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * 开始录音；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	//不能连续的点击startRecord，1.5s之内无效
	private static long lastClickTime;

	public static boolean isFastDoubleClick() {
		long time = System.currentTimeMillis();
		long timeD = time - lastClickTime;
		if (0 < timeD && timeD < 1500) {
			return true;
		}
		lastClickTime = time;
		return false;
	}

	@Override
	public void startRecord(JSONObject _dictParas, DoIScriptEngine _scriptEngine, final DoInvokeResult _invokeResult) throws Exception {
		//防止重复点击
		if (isFastDoubleClick()) {
			return;
		}
		//已经是开始状态
		if (isStart) {
			return;
		}
		String path = DoJsonHelper.getString(_dictParas, "path", "");
		String type = DoJsonHelper.getString(_dictParas, "type", "mp3");//录音输出格式mp3、amr、aac
		String quality = DoJsonHelper.getString(_dictParas, "quality", "normal");//录音输出质量high、normal、low
		int limit = DoJsonHelper.getInt(_dictParas, "limit", -1);//录音时长限制
		if (mRecord != null) {
			if (currentRecordType != null && type.equals(currentRecordType)) {
				return;
			}
			mRecord.stopRecord();
		}
		do_IRecord currRecord = RecorderFactory.getRecorder(type);
		if (currRecord == null) {
			DoServiceContainer.getLogEngine().writeError("do_Audio", new RuntimeException("开始录音失败，type：" + type));
			return;
		}
		String recordName = System.currentTimeMillis() + "." + type;
		if (path.equals("data://") || path.endsWith("/")) {
			outPath = path + recordName;
		} else {
			outPath = path + File.separator + recordName;
		}
		String fileFullPath = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), outPath);
		DoIOHelper.createFile(fileFullPath);
		currRecord.setOnRecordListener(new do_IRecord.OnRecordListener() {
			@Override
			public void onStart() {
				isStart = true;
				_invokeResult.setResultBoolean(true);
			}

			@Override
			public void onError() {
				isStart = false;
				_invokeResult.setResultBoolean(false);
				mRecord = null;
			}

			@Override
			public void onRecordTimeChange(long totalTimeMillis) {
				DoInvokeResult jsonResult = new DoInvokeResult(getUniqueKey());
				jsonResult.setResultText(totalTimeMillis + "");
				getEventCenter().fireEvent("recordProgress", jsonResult);
			}

			@Override
			public void onRecordVolumeChange(double volume) {
				DoInvokeResult jsonResult = new DoInvokeResult(getUniqueKey());
				JSONObject _obj = new JSONObject();
				try {
					_obj.put("volume", volume);
				} catch (JSONException e) {
				}
				jsonResult.setResultNode(_obj);
				getEventCenter().fireEvent("recordVolume", jsonResult);
			}

			@Override
			public void onFinished(String outFillPath, long totalTimeMillis, long fileSize) {
				isStart = false;
				DoInvokeResult _result = new DoInvokeResult(getUniqueKey());
				JSONObject _obj = new JSONObject();
				try {
					_obj.put("path", outPath);
					_obj.put("time", totalTimeMillis);
					_obj.put("size", fileSize);
				} catch (JSONException e) {
				}
				_result.setResultNode(_obj);
				getEventCenter().fireEvent("recordFinished", _result);
				mRecord = null;
			}
		});
		currRecord.startRecord(limit, quality, fileFullPath);
		currentRecordType = type;
		mRecord = currRecord;

	}

	/**
	 * 结束录音；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	@Deprecated
	public void stopRecord(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		if (null != mRecord) {
			mRecord.stopRecord();
			_invokeResult.setResultText(outPath);
			mRecord = null;
		}
	}

	@Override
	public void stopRecordAsync(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		if (null != mRecord) {
			//判断是否调用了startRecord()
			try {
				mRecord.stopRecord();
			} catch (Exception e) {
				e.printStackTrace();
			}
			mRecord = null;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (null != mediaPlayer) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	private final class PreparedListener implements OnPreparedListener {
		private int positon;

		public PreparedListener(int positon) {
			this.positon = positon;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mediaPlayer.start();//开始播放 
			if (positon > 0) {//如果音乐不是从头播放  
				mediaPlayer.seekTo(positon);
			}
			if (null == timer) {
				timer = new Timer();
			}
			onPlayPositionChange();
		}
	}

	private void onPlayPositionChange() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					if (mediaPlayer != null && mediaPlayer.isPlaying()) {
						DoInvokeResult jsonResult = new DoInvokeResult(getUniqueKey());
						JSONObject json = new JSONObject();
						json.put("currentTime", mediaPlayer.getCurrentPosition());
						json.put("totalTime", mediaPlayer.getDuration());
						jsonResult.setResultNode(json);
						getEventCenter().fireEvent("playProgress", jsonResult);
					}
				} catch (JSONException e) {
					DoServiceContainer.getLogEngine().writeError("do_Audio->playProgress", e);
					e.printStackTrace();
				}
			}
		};
		timer.schedule(task, 0, 500);
	}
}