package com.sunflower.catchtherainbow;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sunflower.catchtherainbow.AudioClasses.AudioFile;
import com.sunflower.catchtherainbow.AudioClasses.PlayListPlayer;
import com.sunflower.catchtherainbow.AudioClasses.SuperAudioPlayer;
import com.sunflower.catchtherainbow.Views.AudioChooserFragment;
import com.sunflower.catchtherainbow.Views.AudioChooserFragment.OnFragmentInteractionListener;
import com.sunflower.catchtherainbow.Views.AudioProgressView;
import com.sunflower.catchtherainbow.Views.AudioVisualizerView;
import com.sunflower.catchtherainbow.Views.Editing.MainAreaFragment;
import com.sunflower.catchtherainbow.Views.Editing.SuperWaveformFragment;
import com.sunflower.catchtherainbow.Views.Effects.DefaultEffectsFragment;
import com.sunflower.catchtherainbow.Views.Effects.EffectsHostFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.effects.DelayEffect;
import be.tarsos.dsp.effects.FlangerEffect;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.resample.RateTransposer;

public class ProjectActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnFragmentInteractionListener, View.OnClickListener, EffectsHostFragment.OnEffectsHostListener
{
    private ActionMenuView amvMenu;
    private ImageButton playStopButt, bNext, bPrev;
    private AudioProgressView progressView;
    private View viewContentProject;
    private AudioVisualizerView visualizerView;
    private TextView audioInfo;

    private RelativeLayout waveFormViewContainer;

    // temp
    private boolean isPlaying = false, isDragging = false;
    private PlayListPlayer player;

    private DelayEffect delayEffect;
    private FlangerEffect flangerEffect;
    private RateTransposer rateTransposer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

        viewContentProject = (View) findViewById(R.id.content_project);

        ////////////////////////////////////////////////////permission
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAPTURE_AUDIO_OUTPUT,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MEDIA_CONTENT_CONTROL};

        if (!hasPermissions(this, PERMISSIONS))
        {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        //////////////////////////////////////////////////////////end permission


        // Init toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        amvMenu = (ActionMenuView) findViewById(R.id.amvMenu);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Init drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // Init nav view
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // -----------------------------------------------------FOR TESTING PURPOSES---------------------------------------
        visualizerView = (AudioVisualizerView) findViewById(R.id.audioVisualizerView);

        waveFormViewContainer = (RelativeLayout) findViewById(R.id.mainAreaContainer);

        audioInfo = (TextView) findViewById(R.id.audioInfoTextView);

        // play/stop/next/prev
        playStopButt = (ImageButton) findViewById(R.id.Sacha);
        bNext = (ImageButton) findViewById(R.id.playNext);
        bPrev = (ImageButton) findViewById(R.id.playPrev);

        bPrev.setOnClickListener(this);
        bNext.setOnClickListener(this);

        progressView = (AudioProgressView) findViewById(R.id.audioProgressView);
        progressView.setOnSeekBar(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b)
            {
                if (player != null)
                    progressView.setCurrent(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                isDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if (player != null)
                {
                    player.setCurrentTime(seekBar.getProgress(), true);
                }
                isDragging = false;
            }
        });

        // --------------------------------------AUDIO STUFF-------------------------------------------------
        new AndroidFFMPEGLocator(this);

        player = new PlayListPlayer(this);

        delayEffect = new DelayEffect(1, 0, 44100);
        rateTransposer = new RateTransposer(1.f);
        flangerEffect = new FlangerEffect(5.f, 0, 44100, 3);

        // apply the effects
        player.addProcessor(rateTransposer);
        player.addProcessor(delayEffect);
        player.addProcessor(flangerEffect);

        player.addPlayerListener(new SuperAudioPlayer.AudioPlayerListener()
        {
            @Override
            public void OnInitialized(final File file)
            {
                progressView.setMax((float) player.getDurationInSeconds());

                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(file.getAbsolutePath());
                String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                audioInfo.setText(artist + " - " + album + " - " + title);
                mmr.release();

                //waveFormViewContainer.setSoundFile(CheapSoundFile.create(file.getAbsolutePath(), null));
                //waveFormViewContainer.invalidate();
            }

            @Override
            public void OnUpdate(AudioEvent audioEvent, double realCurrentTime)
            {
                //  final float duration = (float) audioEvent.getFrameLength() / audioEvent.getfra();
                final double currentTime = realCurrentTime;// (float) audioEvent.getTimeStamp();
                final float[] audioData = audioEvent.getFloatBuffer();

                //progressView.setMax(duration);
                if (!isDragging) progressView.setCurrent((float) currentTime);
                visualizerView.updateVisualizer(audioData);
            }

            @Override
            public void OnFinish(){}
        });

        MainAreaFragment mainAreaFragment = MainAreaFragment.newInstance("", "");

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainAreaContainer, mainAreaFragment)
                .commit();

        // play stop handler
        playStopButt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!isPlaying)
                {
                    try
                    {
                        isPlaying = true;
                        playStopButt.setImageResource(R.drawable.ic_pause);
                        player.play();
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(ProjectActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                else
                {
                    try
                    {
                        player.pause();
                        isPlaying = false;
                        playStopButt.setImageResource(R.drawable.ic_play);
                    } catch (Exception e)
                    {
                        Toast.makeText(ProjectActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        });
        // -----------------------------------------------
    }

    @Override
    protected void onStop()
    {
        player.stop();
        super.onStop();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.playNext:
            {
                if (player != null)
                {
                    try
                    {
                        player.playNext();
                    } catch (Exception ex)
                    {
                        Toast.makeText(this, "Cannot be played!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case R.id.playPrev:
            {
                if (player != null)
                {
                    try
                    {
                        player.playPrev();
                    } catch (Exception ex)
                    {
                        Toast.makeText(this, "Cannot be played!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        } else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.project, /*amvMenu.getMenu() /**/menu); // adds menu items to the left side. If it's not needed replace second param with default menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentById(R.id.SuperAudioChooser);
            if (prev != null)  ft.remove(prev);
            ft.addToBackStack(null);
            // Create and show the dialog.
            AudioChooserFragment newFragment = AudioChooserFragment.newInstance();
            newFragment.show(ft, "Song chooser dialog");
            return true;
        }
        if (id == R.id.action_effects)
        {
            //EffectsHostFragment fragment = (EffectsHostFragment) createNewDialog(R.id.effectsFragment, EffectsHostFragment.class, true);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentById(R.id.effectsFragment);
            if (prev != null) ft.remove(prev);
            ft.addToBackStack(null);
            // Create and show the dialog.

            DefaultEffectsFragment effectsFragment = DefaultEffectsFragment.newInstance();
            effectsFragment.setEffects(delayEffect, rateTransposer, flangerEffect);

            EffectsHostFragment hostFragment = EffectsHostFragment.newInstance();
            hostFragment.setEffectsFragment(effectsFragment);
            hostFragment.show(ft, "Effects dialog");

            return true;

        }

        return super.onOptionsItemSelected(item);
    }



    // Universal method for creating dialog fragments
    private DialogFragment createNewDialog(int fragmentId, Class<? extends DialogFragment> fragmentClass, boolean showImmediately)
    {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentById(fragmentId);
        if (prev != null)
        {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        // Create and show the dialog.
        DialogFragment newFragment = null;
        try
        {
            newFragment = fragmentClass.newInstance();
            if (showImmediately) newFragment.show(ft, "Effects dialog");
            return newFragment;
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera)
        {
            // Handle the camera action
        } else if (id == R.id.nav_gallery)
        {

        } else if (id == R.id.nav_slideshow)
        {

        } else if (id == R.id.nav_manage)
        {

        } else if (id == R.id.nav_share)
        {

        } else if (id == R.id.nav_send)
        {

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // ---------------------------------SONG CHOOSER LISTENER INTERFACE----------------------------------

    @Override
    public void onOk(ArrayList<AudioFile> selectedAudioFiles)
    {
        //Random rand = new Random();
        // List<String> songs = Helper.getAllSongsOnDevice(ProjectActivity.this);
        ArrayList<AudioFile> songs = selectedAudioFiles;
        long seed = System.nanoTime();
        Collections.shuffle(songs, new Random(seed));
        try
        {
            player.setAudioFiles(songs);
            isPlaying = true;
            playStopButt.setImageResource(R.drawable.ic_pause);
            player.play();
        } catch (Exception e)
        {
            Toast.makeText(ProjectActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onCancel()
    {

    }

    // ---------------------------------Effects Host LISTENER INTERFACE----------------------------------

    @Override
    public void onEffectsConfirmed()
    {

    }

    @Override
    public void onEffectsCancelled()
    {

    }



    // ----- permissions ----------
    public static boolean hasPermissions(Context context, String... permissions)
    {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }
}

