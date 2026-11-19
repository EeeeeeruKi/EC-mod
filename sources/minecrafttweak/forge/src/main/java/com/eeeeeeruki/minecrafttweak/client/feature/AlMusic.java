package com.eeeeeeruki.minecrafttweak.client.feature;

import org.lwjgl.openal.AL10;

/**
 * 曲を1つ、OpenAL で直接鳴らす（マイクラの音の仕組みとは別に、同じ OpenAL を使う）。
 * 必ずゲームのメインスレッドから使う。
 */
public final class AlMusic {
    private int buffer;
    private int source;

    public AlMusic(AudioLoader.Audio audio) {
        buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, audio.channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                audio.pcm, audio.sampleRate);
        source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSource3f(source, AL10.AL_POSITION, 0f, 0f, 0f);
        AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0f);
    }

    /** 音の仕組みが作り直されたりして、もう使えないなら false。 */
    public boolean valid() {
        return source != 0 && AL10.alIsSource(source) && AL10.alIsBuffer(buffer);
    }

    public void setLooping(boolean loop) {
        AL10.alSourcei(source, AL10.AL_LOOPING, loop ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    public void setGain(float gain) {
        AL10.alSourcef(source, AL10.AL_GAIN, Math.max(0f, gain));
    }

    public void play() {
        AL10.alSourcePlay(source);
    }

    public void pause() {
        AL10.alSourcePause(source);
    }

    public boolean playing() {
        return AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    public boolean paused() {
        return AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PAUSED;
    }

    public void delete() {
        try {
            if (source != 0 && AL10.alIsSource(source)) {
                AL10.alSourceStop(source);
                AL10.alDeleteSources(source);
            }
            if (buffer != 0 && AL10.alIsBuffer(buffer)) {
                AL10.alDeleteBuffers(buffer);
            }
        } catch (Throwable ignored) {
            // すでに消えている
        }
        source = 0;
        buffer = 0;
    }
}
