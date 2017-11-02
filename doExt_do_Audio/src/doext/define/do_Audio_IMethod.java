package doext.define;

import org.json.JSONObject;

import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;

/**
 * 声明自定义扩展组件方法
 */
public interface do_Audio_IMethod {
	void pause(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void play(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void resume(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void stop(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void startRecord(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void stopRecord(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void stopRecordAsync(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName);
	void playAsync(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception;
}