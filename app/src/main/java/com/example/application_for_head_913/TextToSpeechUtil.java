package com.example.application_for_head_913;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

public class TextToSpeechUtil {
    private static final String TAG = TextToSpeechUtil.class.getSimpleName();
    private static volatile TextToSpeechUtil instance;
    private SpeechSynthesizer mTts;
    private Context mContext;
    private String voicer = "xiaoyan";
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private int mPercentForBuffering = 0;
    private int mPercentForPlaying = 0;
    private String currentText = "";

    // 新增：用于回调的接口
    public interface TTSListener {
        void onSpeechCompleted();
    }

    private TTSListener mListener;

    private TextToSpeechUtil(Context context) {
        this.mContext = context.getApplicationContext();
        mTts = SpeechSynthesizer.createSynthesizer(mContext, mTtsInitListener);
    }

    public static TextToSpeechUtil getInstance(Context context) {
        if (instance == null) {
            synchronized (TextToSpeechUtil.class) {
                if (instance == null) {
                    instance = new TextToSpeechUtil(context);
                }
            }
        }
        if (instance.mTts == null) {
            Log.e(TAG, "语音合成对象未初始化");
            Toast.makeText(context, "语音合成初始化失败", Toast.LENGTH_SHORT).show();
        }
        return instance;
    }

    private InitListener mTtsInitListener = code -> {
        if (code != ErrorCode.SUCCESS) {
            Log.e(TAG, "初始化失败，错误码：" + code);
            showTip("初始化失败，错误码：" + code);
        } else {
            Log.d(TAG, "初始化成功");
        }
    };

    private void setParam() {
        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
        mTts.setParameter(SpeechConstant.SPEED, "80");
        mTts.setParameter(SpeechConstant.PITCH, "50");
        mTts.setParameter(SpeechConstant.VOLUME, "50");
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
    }

    public void speakText(String text) {
        if (mTts == null) {
            Log.e(TAG, "语音合成对象未初始化");
            Toast.makeText(mContext, "语音合成未初始化", Toast.LENGTH_SHORT).show();
            return;
        }
        currentText = text;
        Log.d(TAG, "正在播放的文本: " + text);
        setParam();
        int code = mTts.startSpeaking(text, mTtsListener);
        if (code != ErrorCode.SUCCESS) {
            Log.e(TAG, "语音合成失败，错误码：" + code);
            Toast.makeText(mContext, "语音合成失败，错误码：" + code, Toast.LENGTH_SHORT).show();
        }
    }

    public void stopSpeaking() {
        if (mTts != null) {
            mTts.stopSpeaking();
        }
    }

    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            Log.d(TAG, "开始播放");
            showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            Log.d(TAG, "暂停播放");
            showTip("暂停播放");
        }


        @Override
        public void onSpeakResumed() {
            Log.d(TAG, "继续播放");
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
            mPercentForBuffering = percent;
            Log.d(TAG, String.format("缓冲进度：%d%%，播放进度：%d%%", mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            mPercentForPlaying = percent;
            Log.d(TAG, String.format("缓冲进度：%d%%，播放进度：%d%%", mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error != null) {
                Log.e(TAG, "播放错误: " + error.getPlainDescription(true));
                showTip("播放错误: " + error.getPlainDescription(true));
            } else {
                Log.d(TAG, "播放完成");
                showTip("播放完成");

                // 调用监听器的方法
                if (mListener != null) {
                    mListener.onSpeechCompleted();
                }
            }
            currentText = "";
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 处理事件
        }
    };
    public void init() {
        if (mTts == null) {
            mTts = SpeechSynthesizer.createSynthesizer(mContext, mTtsInitListener);
        }
    }
    public void release() {
        if (mTts != null) {
            mTts.stopSpeaking();
            mTts.destroy();
            mTts = null;
        }
        instance = null;
    }

    private void showTip(final String str) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show());
    }

    // 新增：设置监听器方法
    public void setListener(TTSListener listener) {
        this.mListener = listener;
    }
}
