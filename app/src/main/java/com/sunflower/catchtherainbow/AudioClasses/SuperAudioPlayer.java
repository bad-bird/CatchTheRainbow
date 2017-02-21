package com.sunflower.catchtherainbow.AudioClasses;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.sunflower.catchtherainbow.Helper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.DetermineDurationProcessor;
import be.tarsos.dsp.io.PipedAudioStream;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;


/**
 * Super audio player
 */

public class SuperAudioPlayer implements AudioProcessor
{
    private  String logTag = "Super Audio Player";

    protected Activity context;

    // list of effects we can add. Add them before playing.
    protected ArrayList<AudioProcessor> additionalAudioProcessors = new ArrayList<>();

    protected ArrayList<AudioPlayerListener> playerListeners = new ArrayList<>();
    protected PlayerState state = PlayerState.NO_FILE_LOADED;
    protected File loadedFile;

    protected AndroidAudioPlayer audioPlayer;
    protected DetermineDurationProcessor durationProc;

    protected double durationInSeconds;
    protected double currentTime;
    protected double pausedAt;

    AudioDispatcher dispatcher;

    int sampleRate = 44100;
    int bufferSize = 4096;

    public SuperAudioPlayer(Activity context/*, TarsosDSPAudioFormat audioFormat, int bufferSizeInSamples, int streamType*/)
    {
        //bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT); // Too big!!!

        this.context = context;
        state = PlayerState.NO_FILE_LOADED;
    }

    public synchronized void load(File file) throws Exception
    {
        if(file == null) throw new NullPointerException("File can't be null !");

        if(state != PlayerState.NO_FILE_LOADED)
        {
            eject();
        }
        loadedFile = file;

        if(!file.exists()) throw new Exception("File does not exist!");

        // does not work at all (returns -1)
        //durationInSeconds = new PipeDecoder().getDuration(AudioResourceUtils.sanitizeResource(file.getAbsolutePath()));// ddp.getDurationInSeconds();
       // Log.d("aaa", durationInSeconds+"");

        // retrieves duration
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getAbsolutePath());
        String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mmr.release();

        durationInSeconds = Helper.millisecondsToSeconds(Double.parseDouble(duration));

        Log.d(logTag, "Duration: " +  durationInSeconds+"");

        pausedAt = 0;
        currentTime = 0;
        setState(PlayerState.FILE_LOADED);

        context.runOnUiThread(new Runnable()
        {
            public void run()
            {
                for (AudioPlayerListener listener : playerListeners)
                    listener.OnInitialized(loadedFile);
            }
        });
    }

    // stops the dispatcher and unloads the file
    public void eject()
    {
        disposeResources();
        loadedFile = null;
        stop();
        setState(PlayerState.NO_FILE_LOADED);
    }

    public synchronized void play()
    {
        if(state == PlayerState.NO_FILE_LOADED)
        {
            throw new IllegalStateException("Cannot play when no file is loaded");
        }
        else if(state == PlayerState.PAUZED)
        {
            play(pausedAt);
        }
        else
        {
            play(0);
        }
    }

    public void play(double startTime)
    {
        if (state == PlayerState.NO_FILE_LOADED)
        {
            throw new IllegalStateException("Can not play when no file is loaded");
        } else
        {
            if (state == PlayerState.PLAYING) stop();

            pausedAt = startTime;

            // read data from a file
            PipedAudioStream file = new PipedAudioStream(loadedFile.getPath());
            TarsosDSPAudioInputStream stream = file.getMonoStream(sampleRate, startTime);

            // buffers size really maters!!! A lesser value will result in more frequent updated which is very useful(e.g. for visualization)
            dispatcher = new AudioDispatcher(stream, bufferSize, 0); //AudioDispatcherFactory.fromPipe(loadedFile.getAbsolutePath(), sampleRate, 4096, 2048);
            //dispatcher.skip(startTime);

            // initialize the processors
            audioPlayer = new AndroidAudioPlayer(dispatcher.getFormat());
            durationProc = new DetermineDurationProcessor();

            // Add all of the additional processors
            for (AudioProcessor audioProcessor : additionalAudioProcessors)
                dispatcher.addAudioProcessor(audioProcessor);

            // add the default processors
            dispatcher.addAudioProcessor(durationProc);

            dispatcher.addAudioProcessor(this);
            dispatcher.addAudioProcessor(audioPlayer);

            // start the dispatcher in a new thread
            Thread th = new Thread(dispatcher, "Audio Player Thread");
            th.setUncaughtExceptionHandler(dispatcherExceptionHandler);
            th.start();
            state = PlayerState.PLAYING;
        }
    }

    public void pause()
    {
        pause(currentTime);
    }

    public void pause(double pauseAt)
    {
        if(state == PlayerState.PLAYING || state == PlayerState.PAUZED)
        {
            disposeResources();
            setState(PlayerState.PAUZED);
            pausedAt = pauseAt;
        }
        else
        {
            return;
            //throw new IllegalStateException("Can not pause when nothing is playing");
        }
    }

    public void stop()
    {
        if(state == PlayerState.PLAYING || state == PlayerState.PAUZED)
        {
            disposeResources();
            state = PlayerState.STOPPED;
        }
        else if(state != PlayerState.STOPPED)
        {
            //throw new IllegalStateException("Can not Stop Playing when nothing is playing. Crap(");
            return;
        }

    }


    public void addProcessor(AudioProcessor processor)
    {
        additionalAudioProcessors.add(processor);

        // not tested!!!!
        if(state == PlayerState.PLAYING)
            dispatcher.addAudioProcessor(processor);
    }

    public void removeProcessor(AudioProcessor processor)
    {
        additionalAudioProcessors.remove(processor);
    }

    // removes all of the additional audio effects
    public void clearProcessors()
    {
        additionalAudioProcessors.clear();
    }

    @Override
    public boolean process(final AudioEvent audioEvent)
    {
        // TODO: Find a better way to represent paused value
        // update time
        currentTime = audioEvent.getTimeStamp() + pausedAt;

        // notify all of the subscribers about update
        context.runOnUiThread(new Runnable()
        {
            public void run()
            {
                for(AudioPlayerListener listener: playerListeners)
                    listener.OnUpdate(audioEvent, currentTime);
            }
        });

        return true;
    }

    protected boolean isDisposing = false;
    @Override
    public void processingFinished()
    {
        if(state != PlayerState.STOPPED)
        {
            state = PlayerState.STOPPED;
        }

        // audio has naturally finished
        if(!isDisposing)
        {
            pausedAt = 0;
            // notify all of the subscribers about finish
            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    for (AudioPlayerListener listener : playerListeners)
                        listener.OnFinish();
                }
            });
        }

    }

    void disposeResources()
    {
        isDisposing = true;
        if (dispatcher != null)
        {
            dispatcher.removeAudioProcessor(this);

            for (AudioProcessor audioProcessor : additionalAudioProcessors)
                dispatcher.removeAudioProcessor(audioProcessor);

            dispatcher.removeAudioProcessor(audioPlayer);
            //dispatcher.stop();
            dispatcher = null;
        }
        isDisposing = false;
    }

    // handles dispatcher thread exceptions.
    // They often occur when it's frequently being stopped and some processors are not destroyed.
    private Thread.UncaughtExceptionHandler dispatcherExceptionHandler = new Thread.UncaughtExceptionHandler()
    {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable)
        {
           throwable.printStackTrace();
           Log.e(logTag, throwable.getLocalizedMessage());
        }
    };

    private void setState(PlayerState newState)
    {
        PlayerState oldState = state;
        state = newState;
    }

    public PlayerState getState() {

        return state;
    }

    public ArrayList<AudioPlayerListener> getPlayerListeners()
    {
        return playerListeners;
    }

    public void addPlayerListener(AudioPlayerListener playerListener)
    {
        playerListeners.add(playerListener);
    }

    public double getDurationInSeconds()
    {
        return durationInSeconds;
    }

    public String getDurationString()
    {
        Date date = new Date((long)(durationInSeconds*1000));

        String formattedDate = new SimpleDateFormat("mm:ss").format(date);
        return formattedDate;
    }

    public int getDurationInMilliseconds()
    {
        long millis = TimeUnit.SECONDS.toMillis((long) durationInSeconds);

        return (int)millis;
    }

    public int getCurrentTimeInMilliseconds()
    {
        long millis = TimeUnit.SECONDS.toMillis((long) getCurrentTime());

        return (int)millis;
    }

    public synchronized void setCurrentTime(double currentTime, boolean updatePlayback)
    {
        if(state == PlayerState.FILE_LOADED)
        {
            return;
        }

        if(state == PlayerState.PAUZED)
            pause(currentTime);
        else if(state == PlayerState.PLAYING && updatePlayback)
        {
            disposeResources();

            play(currentTime);
        }

    }

    public double getCurrentTime()
    {
        return currentTime;
    }

    public String getCurrentTimeString()
    {
        Date date = new Date((long)(getCurrentTime() *1000));

        String formattedDate = new SimpleDateFormat("mm:ss").format(date);
        return formattedDate;
    }

    public double getPausedAt()
    {
        return pausedAt;
    }

    public interface AudioPlayerListener
    {
        void OnInitialized(File file);
        void OnUpdate(AudioEvent audioEvent, double realCurrentTime);
        void OnFinish();
    }

    /**
     * Defines the state of the audio player.
     * @author Joren Six
     */
    public enum PlayerState
    {
        /**
         * No file is loaded.
         */
        NO_FILE_LOADED,
        /**
         * A file is loaded and ready to be played.
         */
        FILE_LOADED,
        /**
         * The file is playing
         */
        PLAYING,
        /**
         * Audio play back is paused.
         */
        PAUZED,
        /**
         * Audio play back is stopped.
         */
        STOPPED
    }

}
