package xyz.juniverse.babylistener.detection

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import xyz.juniverse.babylistener.etc.console
import java.lang.Thread.sleep

/**
 * Created by juniverse on 23/11/2017.
 */
class Detector {
    companion object {
        private val detectLevelScale = 20
        val range = IntRange(1, 1000)
        val valueRange = IntRange(range.first * detectLevelScale, range.last * detectLevelScale)
        val defaultValue: Int
            get() = range.last / 2 - 1
    }

    interface DetectorInterface {
        fun onStateChanged(state: State)
        fun onDetecting(level: ShortArray)
        fun onDetected()
    }

    private lateinit var detectorInterface: DetectorInterface
    fun setDetectorInterface(onStateChanged: (State) -> Unit,
                             onDetecting: (ShortArray) -> Unit,
                             onDetected: () -> Unit) {
        detectorInterface = object: DetectorInterface {
            override fun onStateChanged(state: State) = onStateChanged(state)
            override fun onDetecting(level: ShortArray) = onDetecting(level)
            override fun onDetected() = onDetected()
        }
    }

    enum class State {
        NONE, STOPPED, PREPARING, RUNNING, PAUSED
    }
    var state: State = State.NONE
    val isRunning: Boolean
        get() = state >= State.RUNNING

    var sensitivity: Int = 0
        set(value) {
            field = (if (value + 1 > range.last) range.last else value + 1) * detectLevelScale
        }

    private val minPauseTime: Long = 30 * 1000
    private var maxPauseTime: Long = 60 * 1000
        set(value) {
            if (state == State.NONE || state == State.STOPPED)
                field = if (value < minPauseTime) minPauseTime else value
        }

    private var initPauseTime: Long = -1
    private val processor = Runnable {
        if (!startRecording()) {
            // todo should tell why...
            state = State.NONE
            detectorInterface.onStateChanged(state)
            return@Runnable
        }

        state = State.RUNNING
        detectorInterface.onStateChanged(state)
        while (state >= State.RUNNING) {

            // if paused, just wait for a second. if waited to long, stop.
            if (state == State.PAUSED) {
                sleep(1000)
                if (System.currentTimeMillis() - initPauseTime > maxPauseTime)
                    break
                else
                    continue
            }

            if (detect()) {
                detectorInterface.onDetected()
                pause()
            }
        }

        stopRecording()
        state = State.STOPPED
        detectorInterface.onStateChanged(state)

        return@Runnable
    }

    fun start() {
        if (state != State.NONE && state != State.STOPPED) return

        state = State.PREPARING
        console.d("sensitivity?", sensitivity)
        Thread(processor).start()
    }

    fun stop() {
        state = State.STOPPED
    }

    fun pause() {
        state = State.PAUSED
        initPauseTime = System.currentTimeMillis()
        detectorInterface.onStateChanged(state)
        recorder.stop()
    }

    fun resume(): Boolean {
        return if (state == State.PAUSED) {
            state = State.RUNNING
            detectorInterface.onStateChanged(state)
            recorder.startRecording()
            true
        } else
            false
    }

    private val samplingRate = 8000
    private var bufferSize: Int = 0
    private var maxDetectLength = 0
    private var hitCount = 10
    private lateinit var soundBuffer: ShortArray
    private lateinit var recorder: AudioRecord
    private fun startRecording(): Boolean {
        bufferSize = AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        maxDetectLength = bufferSize / 2
        hitCount = maxDetectLength / 4
        soundBuffer = ShortArray(bufferSize)
        console.d("variables?", bufferSize, maxDetectLength, hitCount)
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (recorder.state != AudioRecord.STATE_INITIALIZED)
            return false

        recorder.startRecording()

        return true
    }

    private fun detect(): Boolean {
        val byteRead = recorder.read(soundBuffer, 0, bufferSize)
        var begin = 0
        var end = byteRead
//        if (byteRead > maxDetectLength) {
//            begin = (byteRead - maxDetectLength) shr 1
//            end = begin + maxDetectLength
//        }

        var hits = 0
        while (begin < end) {
            if ( soundBuffer[begin] > sensitivity || soundBuffer[begin] < -sensitivity ) {
                if (++hits >= hitCount) {
                    detectorInterface.onDetecting(soundBuffer)
                    return true
                }
            }
            begin++
        }

        detectorInterface.onDetecting(soundBuffer)
        return false
    }

    private fun stopRecording() {
        recorder.stop()
        recorder.release()
    }
}